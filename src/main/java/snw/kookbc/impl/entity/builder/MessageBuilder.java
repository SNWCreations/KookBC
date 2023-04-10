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

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import snw.jkook.entity.User;
import snw.jkook.entity.channel.TextChannel;
import snw.jkook.message.Message;
import snw.jkook.message.PrivateMessage;
import snw.jkook.message.TextChannelMessage;
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
import snw.kookbc.impl.message.PrivateMessageImpl;
import snw.kookbc.impl.message.QuoteImpl;
import snw.kookbc.impl.message.TextChannelMessageImpl;

import static snw.kookbc.util.GsonUtil.CARD_GSON;
import static snw.kookbc.util.GsonUtil.get;

public class MessageBuilder {
    private final KBCClient client;

    public MessageBuilder(KBCClient client) {
        this.client = client;
    }

    // result format: {type, json}
    public static Object[] serialize(BaseComponent component) {
        if (component instanceof MarkdownComponent) {
            return new Object[]{9, component.toString()};
        } else if (component instanceof TextComponent) {
            return new Object[]{1, component.toString()};
        } else if (component instanceof CardComponent) {
            return new Object[]{10, CARD_GSON.toJson(CardBuilder.serialize((CardComponent) component))};
        } else if (component instanceof MultipleCardComponent) {
            return new Object[]{10, CARD_GSON.toJson(CardBuilder.serialize((MultipleCardComponent) component))};
        } else if (component instanceof FileComponent) {
            FileComponent fileComponent = (FileComponent) component;
            MultipleCardComponent fileCard;
            if (fileComponent.getType() == FileComponent.Type.IMAGE) { // special condition for better performance
                return new Object[]{2, fileComponent.getUrl()};
            } else if (fileComponent.getType() == FileComponent.Type.VIDEO) { // special condition for better performance
                return new Object[]{3, fileComponent.getUrl()};
            } else {
                fileCard = new snw.jkook.message.component.card.CardBuilder()
                        .setTheme(Theme.NONE)
                        .setSize(Size.LG)
                        .addModule(
                                new FileModule(fileComponent.getType(), fileComponent.getUrl(), fileComponent.getTitle(), null)
                        )
                        .build();
                return serialize(fileCard); // actually, this is not a loop call
            }
        }
        throw new RuntimeException("Unsupported component");
    }

    public PrivateMessage buildPrivateMessage(JsonObject object) {
        String id = get(object, "msg_id").getAsString();
        JsonObject authorObj = object.getAsJsonObject("extra").getAsJsonObject("author");
        User author = client.getStorage().getUser(authorObj.get("id").getAsString(), authorObj);
        long timeStamp = get(object, "msg_timestamp").getAsLong();
        return new PrivateMessageImpl(client, id, author, buildComponent(object), timeStamp, buildQuote(object.getAsJsonObject("extra").getAsJsonObject("quote")));
    }

    public TextChannelMessage buildTextChannelMessage(JsonObject object) {
        String id = get(object, "msg_id").getAsString();
        JsonObject authorObj = object.getAsJsonObject("extra").getAsJsonObject("author");
        User author = client.getStorage().getUser(authorObj.get("id").getAsString(), authorObj);
        TextChannel channel = (TextChannel) client.getStorage().getChannel(get(object, "target_id").getAsString());
        long timeStamp = get(object, "msg_timestamp").getAsLong();
        return new TextChannelMessageImpl(client, id, author, buildComponent(object), timeStamp, buildQuote(object.getAsJsonObject("extra").getAsJsonObject("quote")), channel);
    }

    public Message buildQuote(JsonObject object) {
        if (object == null) return null;

        String id = get(object, "rong_id").getAsString(); // WARNING: this is not described in Kook developer document, maybe unavailable in the future
        Message message = client.getStorage().getMessage(id);
        if (message != null) return message; // prevent resource leak

        BaseComponent component = buildComponent(object);
        long timeStamp = get(object, "create_at").getAsLong();
        JsonObject rawUser = object.getAsJsonObject("author");
        User author = client.getStorage().getUser(rawUser.get("id").getAsString(), rawUser);
        return new QuoteImpl(component, id, author, timeStamp);
    }

    public BaseComponent buildComponent(JsonObject object) {
        // we use text channel message format
        String content = get(object, "content").getAsString();
        switch (get(object, "type").getAsInt()) {
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
                    url = attachment.get("url").getAsString();
                    title = attachment.get("name").getAsString();
                    // -1 for image files, because Kook does not provide size for image files.
                    if (attachment.has("size") && !attachment.get("size").isJsonNull()) {
                        size = attachment.get("size").getAsInt();
                    }
                    String ftype = attachment.get("type").getAsString();
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
                            if (attachment.get("file_type").getAsString().startsWith("audio")) {
                                type = FileComponent.Type.AUDIO;
                            } else {
                                throw new RuntimeException("Unexpected file_type");
                            }
                    }
                } else { // quote object?
                    url = content;
                }
                return new FileComponent(
                        url,
                        title,
                        size,
                        type
                );
            case 1: // Are you sure? This message type was deprecated. KOOK converts plain text (TextComponent) into KMarkdown (MarkdownComponent)
                return new TextComponent(content);
        }
        throw new RuntimeException("Unknown component type");
    }
}
