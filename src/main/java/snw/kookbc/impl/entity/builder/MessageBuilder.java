/*
 *     KookBC -- The Kook Bot Client & JKook API standard implementation for Java.
 *     Copyright (C) 2022 KookBC contributors
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

import com.google.gson.Gson;
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
            return new Object[]{10, new Gson().toJson(CardBuilder.serialize((CardComponent) component))};
        } else if (component instanceof MultipleCardComponent) {
            return new Object[]{10, new Gson().toJson(CardBuilder.serialize((MultipleCardComponent) component))};
        } else if (component instanceof FileComponent) {
            FileComponent fileComponent = (FileComponent) component;
            MultipleCardComponent fileCard = new snw.jkook.message.component.card.CardBuilder()
                    .setTheme(Theme.NONE)
                    .setSize(Size.LG)
                    .addModule(
                            new FileModule(fileComponent.getType() != FileComponent.Type.IMAGE ? fileComponent.getType() : FileComponent.Type.FILE, fileComponent.getUrl(), fileComponent.getTitle(), null)
                    )
                    .build();
            return serialize(fileCard); // actually, this is not a loop call
        }
        throw new RuntimeException("Unsupported component");
    }

    public PrivateMessage buildPrivateMessage(JsonObject object) {
        String id = object.get("msg_id").getAsString();
        JsonObject authorObj = object.getAsJsonObject("extra").getAsJsonObject("author");
        User author = client.getStorage().getUser(authorObj.get("id").getAsString(), authorObj);
        long timeStamp = object.get("msg_timestamp").getAsLong();
        return new PrivateMessageImpl(client, id, author, buildComponent(object), timeStamp, buildQuote(object.getAsJsonObject("extra").getAsJsonObject("quote")));
    }

    public TextChannelMessage buildTextChannelMessage(JsonObject object) {
        String id = object.get("msg_id").getAsString();
        JsonObject authorObj = object.getAsJsonObject("extra").getAsJsonObject("author");
        User author = client.getStorage().getUser(authorObj.get("id").getAsString(), authorObj);
        TextChannel channel = (TextChannel) client.getStorage().getChannel(object.get("target_id").getAsString());
        long timeStamp = object.get("msg_timestamp").getAsLong();
        return new TextChannelMessageImpl(client, id, author, buildComponent(object), timeStamp, buildQuote(object.getAsJsonObject("extra").getAsJsonObject("quote")), channel);
    }

    public Message buildQuote(JsonObject object) {
        if (object == null) return null;

        String id = object.get("rong_id").getAsString(); // WARNING: this is not described in Kook developer document, maybe unavailable in the future
        Message message = client.getStorage().getMessage(id);
        if (message != null) return message; // prevent resource leak

        BaseComponent component = buildComponent(object);
        long timeStamp = object.get("create_at").getAsLong();
        JsonObject rawUser = object.getAsJsonObject("author");
        User author = client.getStorage().getUser(rawUser.get("id").getAsString(), rawUser);
        return new QuoteImpl(component, id, author, timeStamp);
    }

    public BaseComponent buildComponent(JsonObject object) {
        // we use text channel message format
        String content = object.get("content").getAsString();
        switch (object.get("type").getAsInt()) {
            case 1:
                return new TextComponent(content);
            case 2:
            case 3:
            case 4:
                JsonObject attachment = object.getAsJsonObject("extra").getAsJsonObject("attachments");
                String url = attachment.get("url").getAsString();
                String title = attachment.get("name").getAsString();
                // -1 for image files, because Kook does not provide size for image files.
                int size = attachment.get("size") != null ? attachment.get("size").getAsInt() : -1;
                FileComponent.Type type;
                String ftype = attachment.get("type").getAsString();
                switch (ftype) {
                    case "file":
                        type = FileComponent.Type.FILE;
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
                return new FileComponent(
                        url,
                        title,
                        size,
                        type
                );
            case 9:
                return new MarkdownComponent(content);
            case 10:
                MultipleCardComponent card = CardBuilder.buildCard(JsonParser.parseString(content).getAsJsonArray());
                if (card.getComponents().size() == 1) {
                    return card.getComponents().get(0);
                } else {
                    return card;
                }
        }
        throw new RuntimeException("Unknown component type");
    }
}
