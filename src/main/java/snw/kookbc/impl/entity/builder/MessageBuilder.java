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

import com.fasterxml.jackson.databind.JsonNode;
import snw.kookbc.util.JacksonUtil;
import snw.kookbc.util.JacksonCardUtil;
import snw.jkook.entity.User;
import snw.jkook.entity.channel.TextChannel;
import snw.jkook.entity.channel.VoiceChannel;
import snw.jkook.message.ChannelMessage;
import snw.jkook.message.Message;
import snw.jkook.message.PrivateMessage;
import snw.jkook.message.component.*;
import snw.jkook.message.component.card.CardComponent;
import snw.jkook.message.component.card.MultipleCardComponent;
import snw.jkook.message.component.card.Size;
import snw.jkook.message.component.card.Theme;
import snw.jkook.message.component.card.module.FileModule;
import snw.kookbc.impl.KBCClient;
import snw.kookbc.impl.entity.channel.TextChannelImpl;
import snw.kookbc.impl.entity.channel.VoiceChannelImpl;
import snw.kookbc.impl.message.*;

import java.util.NoSuchElementException;

import static snw.kookbc.util.JacksonUtil.*;

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
            return new Object[] { 10, JacksonCardUtil.toJson(component) };
        } else if (component instanceof MultipleCardComponent) {
            return new Object[]{10, JacksonCardUtil.toJson(component)};
        } else if (component instanceof TemplateMessage) {
            return new Object[]{ ((TemplateMessage) component).getType(), JacksonCardUtil.toJson(component) };
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

    // ===== Jackson API - 高性能版本 =====

    public PrivateMessage buildPrivateMessage(JsonNode node) {
        final String id = JacksonUtil.get(node, "msg_id").asText();
        final JsonNode extra = JacksonUtil.get(node, "extra");
        final User author = getAuthor(extra);
        final long timeStamp = JacksonUtil.get(node, "msg_timestamp").asLong();
        final Message quote = getQuote(extra);
        return new PrivateMessageImpl(client, id, author, buildComponent(node), timeStamp, quote);
    }

    public ChannelMessage buildChannelMessage(JsonNode node) {
        final String id = JacksonUtil.get(node, "msg_id").asText();
        final JsonNode extra = JacksonUtil.get(node, "extra");
        final User author = getAuthor(extra);
        final long timeStamp = JacksonUtil.get(node, "msg_timestamp").asLong();
        final Message quote = getQuote(extra);
        final String targetId = JacksonUtil.get(node, "target_id").asText();
        final int channelType = JacksonUtil.get(extra, "channel_type").asInt();
        return buildMessage(id, author, buildComponent(node), timeStamp, quote, targetId, channelType);
    }

    private User getAuthor(JsonNode extra) {
        final JsonNode authorObj = JacksonUtil.get(extra, "author");
        final String id = authorObj.get("id").asText();
        return client.getStorage().getUser(id, authorObj); // 直接使用Jackson版本
    }

    private Message getQuote(JsonNode extra) {
        try {
            final JsonNode quoteNode = extra.get("quote");
            if (quoteNode == null || quoteNode.isNull()) {
                return null;
            }
            final String quoteId = quoteNode.get("rong_id").asText();
            Message quote = client.getStorage().getMessage(quoteId);
            if (quote == null) {
                quote = client.getCore().getHttpAPI().getChannelMessage(quoteId);
            }
            return quote;
        } catch (Exception e) {
            return null;
        }
    }

    public BaseComponent buildComponent(JsonNode node) {
        // we use text channel message format
        String content = JacksonUtil.get(node, "content").asText();
        switch (JacksonUtil.get(node, "type").asInt()) {
            case 9:
                return new MarkdownComponent(content);
            case 10:
                // 直接使用Jackson版本的CardBuilder方法
                JsonNode contentNode = JacksonUtil.parse(content);
                if (contentNode.isArray()) {
                    MultipleCardComponent card = CardBuilder.buildCardArray(contentNode);
                    if (card.getComponents().size() == 1) {
                        return card.getComponents().get(0);
                    } else {
                        return card;
                    }
                } else {
                    return CardBuilder.buildCardObject(contentNode);
                }
            case 2:
            case 3:
            case 4:
                String url;
                String title = "";
                int size = -1;
                FileComponent.Type type = FileComponent.Type.FILE;
                if (node.has("extra")) { // standard component format
                    JsonNode attachment = node.get("extra").get("attachments");
                    url = attachment.get("url").asText();
                    title = attachment.get("name").asText();
                    // -1 for image files, because Kook does not provide size for image files.
                    if (attachment.has("size") && !attachment.get("size").isNull()) {
                        size = attachment.get("size").asInt();
                    }
                    String ftype = attachment.get("type").asText();
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
                            if (attachment.get("file_type").asText().startsWith("audio")) {
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

    public Message buildQuote(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        final String id = node.get("rong_id").asText(); // WARNING: this is not described in Kook developer document,
                                                        // maybe unavailable in the future
        final BaseComponent component = buildComponent(node);
        final long timeStamp = node.get("create_at").asLong();
        final JsonNode rawUser = node.get("author");
        final User author = client.getStorage().getUser(rawUser.get("id").asText(), rawUser); // 直接使用Jackson版本
        return new QuoteImpl(component, id, author, timeStamp);
    }
}
