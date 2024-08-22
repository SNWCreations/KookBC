/*
 *     KookBC -- The Kook Bot Client & JKook API standard implementation for Java.
 *     Copyright (C) 2022 - 2023 KookBC contributors
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as published
 *     by the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 *
 *     You should have received a copy of the GNU Affero General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package snw.kookbc.impl.entity.builder;

import static snw.kookbc.util.GsonUtil.CARD_GSON;
import static snw.kookbc.util.GsonUtil.getAsInt;
import static snw.kookbc.util.GsonUtil.getAsJsonObject;
import static snw.kookbc.util.GsonUtil.getAsLong;
import static snw.kookbc.util.GsonUtil.getAsString;

import java.util.NoSuchElementException;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import snw.jkook.entity.User;
import snw.jkook.entity.channel.TextChannel;
import snw.jkook.entity.channel.VoiceChannel;
import snw.jkook.message.ChannelMessage;
import snw.jkook.message.Message;
import snw.jkook.message.PrivateMessage;
import snw.jkook.message.component.BaseComponent;
import snw.jkook.message.component.FileComponent;
import snw.jkook.message.component.MarkdownComponent;
import snw.jkook.message.component.TextComponent;
import snw.jkook.message.component.card.CardComponent;
import snw.jkook.message.component.card.MultipleCardComponent;
import snw.jkook.message.component.card.Size;
import snw.jkook.message.component.card.Theme;
import snw.jkook.message.component.card.module.FileModule;
import snw.kookbc.impl.KBCClient;
import snw.kookbc.impl.entity.channel.TextChannelImpl;
import snw.kookbc.impl.entity.channel.VoiceChannelImpl;
import snw.kookbc.impl.message.ChannelMessageImpl;
import snw.kookbc.impl.message.PrivateMessageImpl;
import snw.kookbc.impl.message.QuoteImpl;
import snw.kookbc.impl.message.TextChannelMessageImpl;
import snw.kookbc.impl.message.VoiceChannelMessageImpl;

public class MessageBuilder {
    private final KBCClient client;

    public static final int CHANNEL_TYPE_TEXT = 1;
    public static final int CHANNEL_TYPE_VOICE = 2;

    public MessageBuilder(KBCClient client) {
        this.client = client;
    }

    // result format: {type, json}
    public static Object[] serialize(BaseComponent component) {
        if (component instanceof MarkdownComponent) {
            return new Object[] { 9, component.toString() };
        } else if (component instanceof TextComponent) {
            return new Object[] { 1, component.toString() };
        } else if (component instanceof CardComponent) {
            return new Object[] { 10, CARD_GSON.toJson(CardBuilder.serialize((CardComponent) component)) };
        } else if (component instanceof MultipleCardComponent) {
            return new Object[] { 10, CARD_GSON.toJson(CardBuilder.serialize((MultipleCardComponent) component)) };
        } else if (component instanceof FileComponent) {
            FileComponent fileComponent = (FileComponent) component;
            MultipleCardComponent fileCard;
            if (fileComponent.getType() == FileComponent.Type.IMAGE) { // special condition for better performance
                return new Object[] { 2, fileComponent.getUrl() };
            } else if (fileComponent.getType() == FileComponent.Type.VIDEO) { // special condition for better
                                                                              // performance
                return new Object[] { 3, fileComponent.getUrl() };
            } else {
                fileCard = new snw.jkook.message.component.card.CardBuilder()
                        .setTheme(Theme.NONE)
                        .setSize(Size.LG)
                        .addModule(
                                new FileModule(fileComponent.getType(), fileComponent.getUrl(),
                                        fileComponent.getTitle(), null))
                        .build();
                return serialize(fileCard); // actually, this is not a loop call
            }
        }
        throw new RuntimeException("Unsupported component");
    }

    public PrivateMessage buildPrivateMessage(JsonObject object) {
        final String id = getAsString(object, "msg_id");
        final JsonObject extra = getAsJsonObject(object, "extra");
        final User author = getAuthor(extra);
        final long timeStamp = getAsLong(object, "msg_timeStamp");
        final Message quote = getQuote(extra);
        return new PrivateMessageImpl(client, id, author, buildComponent(object), timeStamp, quote);
    }

    public ChannelMessage buildChannelMessage(JsonObject object) {
        final String id = getAsString(object, "msg_id");
        final JsonObject extra = getAsJsonObject(object, "extra");
        final User author = getAuthor(extra);
        final long timeStamp = getAsLong(object, "msg_timeStamp");
        final Message quote = getQuote(extra);
        final String targetId = getAsString(object, "target_id");
        final int channelType = getAsInt(extra, "channel_type");
        return buildMessage(id, author, buildComponent(object), timeStamp, quote, targetId, channelType);
    }

    private User getAuthor(JsonObject extra) {
        final JsonObject authorObj = getAsJsonObject(extra, "author");
        final String id = getAsString(authorObj, "id");
        return client.getStorage().getUser(id, authorObj);
    }

    private Message getQuote(JsonObject extra) {
        try {
            final String quoteId = getAsString(getAsJsonObject(extra, "quote"), "rong_id");
            Message quote = client.getStorage().getMessage(quoteId);
            if (quote == null) {
                quote = client.getCore().getHttpAPI().getChannelMessage(quoteId);
            }
            return quote;
        } catch (NoSuchElementException e) {
            return null;
        }
    }

    private ChannelMessageImpl buildMessage(String id, User author, BaseComponent component, long timeStamp,
            Message message, String targetId, int channelType) {
        if (channelType == CHANNEL_TYPE_TEXT) {
            final TextChannel channel = new TextChannelImpl(client, targetId);
            return new TextChannelMessageImpl(client, id, author, component, timeStamp, message, channel);
        } else if (channelType == CHANNEL_TYPE_VOICE) {
            final VoiceChannel channel = new VoiceChannelImpl(client, targetId);
            return new VoiceChannelMessageImpl(client, id, author, component, timeStamp, message, channel);
        }
        throw new RuntimeException("We can not found channel type: " + channelType);
    }

    public Message buildQuote(JsonObject object) {
        if (object == null) {
            return null;
        }
        final String id = getAsString(object, "rong_id"); // WARNING: this is not described in Kook developer document,
                                                          // maybe unavailable in the future
        final BaseComponent component = buildComponent(object);
        final long timeStamp = getAsLong(object, "create_at");
        final JsonObject rawUser = getAsJsonObject(object, "author");
        final User author = client.getStorage().getUser(getAsString(rawUser, "id"), rawUser);
        return new QuoteImpl(component, id, author, timeStamp);
    }

    public BaseComponent buildComponent(JsonObject object) {
        // we use text channel message format
        String content = getAsString(object, "content");
        switch (getAsInt(object, "type")) {
            case 9:
                return new MarkdownComponent(content);
            case 10:
                MultipleCardComponent card = CardBuilder.buildCard(JsonParser.parseString(content).getAsJsonArray());
                if (card.getComponents().size() == 1) {
                    return card.getComponents().get(0);
                } else {
                    return card;
                }
            case 2:
            case 3:
            case 4:
                String url;
                String title = "";
                int size = -1;
                FileComponent.Type type = FileComponent.Type.FILE;
                if (object.has("extra")) { // standard component format
                    JsonObject attachment = object.getAsJsonObject("extra").getAsJsonObject("attachments");
                    url = getAsString(attachment, "url");
                    title = getAsString(attachment, "name");
                    // -1 for image files, because Kook does not provide size for image files.
                    if (attachment.has("size") && !attachment.get("size").isJsonNull()) {
                        size = getAsInt(attachment, "size");
                    }
                    String ftype = getAsString(attachment, "type");
                    switch (ftype) {
                        case "file":
                            break;
                        case "video":
                            type = FileComponent.Type.VIDEO;
                            break;
                        case "image":
                            type = FileComponent.Type.IMAGE;
                            break;
                        default:
                            if (getAsString(attachment, "file_type").startsWith("audio")) {
                                type = FileComponent.Type.AUDIO;
                            } else {
                                throw new RuntimeException("Unexpected file_type");
                            }
                    }
                } else { // quote object?
                    url = content;
                }
                return new FileComponent(url, title, size, type);
            case 1: // Are you sure? This message type was deprecated. KOOK converts plain text
                    // (TextComponent) into KMarkdown (MarkdownComponent)
                return new TextComponent(content);
        }
        throw new RuntimeException("Unknown component type");
    }
}
