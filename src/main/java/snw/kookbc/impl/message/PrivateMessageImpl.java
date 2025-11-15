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

package snw.kookbc.impl.message;

import static snw.kookbc.util.JacksonUtil.get;
import static snw.kookbc.util.JacksonUtil.has;

import java.util.Collections;
import java.util.Map;
import java.util.NoSuchElementException;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.fasterxml.jackson.databind.JsonNode;

import snw.jkook.entity.CustomEmoji;
import snw.jkook.entity.User;
import snw.jkook.exceptions.BadResponseException;
import snw.jkook.message.Message;
import snw.jkook.message.PrivateMessage;
import snw.jkook.message.component.BaseComponent;
import snw.jkook.message.component.MarkdownComponent;
import snw.kookbc.impl.KBCClient;
import snw.kookbc.impl.network.HttpAPIRoute;
import snw.kookbc.util.MapBuilder;

public class PrivateMessageImpl extends MessageImpl implements PrivateMessage {

    public PrivateMessageImpl(KBCClient client, String id, User user) {
        super(client, id, user);
    }

    public PrivateMessageImpl(KBCClient client, String id, User user, BaseComponent component, long timeStamp,
            Message quote) {
        super(client, id, user, component, timeStamp, quote);
        this.completed = true;
    }

    @Override
    public void sendReaction(CustomEmoji emoji) {
        Map<String, Object> body = new MapBuilder()
                .put("msg_id", getId())
                .put("emoji", emoji.getId())
                .build();
        client.getNetworkClient().postContent(HttpAPIRoute.USER_CHAT_MESSAGE_REACTION_ADD.toFullURL(), body);
    }

    @Override
    public void removeReaction(CustomEmoji emoji) {
        Map<String, Object> body = new MapBuilder()
                .put("msg_id", getId())
                .put("emoji", emoji.getId())
                .build();
        client.getNetworkClient().postContent(HttpAPIRoute.USER_CHAT_MESSAGE_REACTION_REMOVE.toFullURL(), body);
    }

    @Override
    public String reply(String message) {
        return reply(new MarkdownComponent(message));
    }

    @Override
    public String sendToSource(String message) {
        return sendToSource(new MarkdownComponent(message));
    }

    @Override
    public String reply(BaseComponent component) {
        return getSender().sendPrivateMessage(component, this);
    }

    @Override
    public String sendToSource(BaseComponent component) {
        return getSender().sendPrivateMessage(component);
    }

    @Override
    public void delete() {
        client.getNetworkClient().postContent(HttpAPIRoute.USER_CHAT_MESSAGE_DELETE.toFullURL(),
                Collections.singletonMap("msg_id", getId()));
    }

    @Override
    public void initialize() {
        final String chatCode = get(client.getNetworkClient()
                .post(HttpAPIRoute.USER_CHAT_SESSION_CREATE.toFullURL(), // KOOK won't create multiple session
                        Collections.singletonMap("target_id", getSender().getId())),
                "code").asText();
        final JsonNode object;
        try {
            object = client.getNetworkClient()
                    .get(HttpAPIRoute.USER_CHAT_MESSAGE_INFO.toFullURL() + "?chat_code=" + chatCode + "&msg_id="
                            + getId());
        } catch (BadResponseException e) {
            if (e.getCode() == 40000) {
                throw (NoSuchElementException) // force casting is required because Throwable#initCause return Throwable
                new NoSuchElementException("No message object with provided ID " + getId() + " found")
                        .initCause(e);
            }
            throw e;
        }
        final BaseComponent component = client.getMessageBuilder().buildComponent(object);
        long timeStamp = object.get("create_at").asLong();
        PrivateMessage quote = null;
        if (object.has("quote")) {
            JsonNode rawQuote = object.get("quote");
            if (rawQuote.isObject()) {
                final String quoteId = rawQuote.get("id").asText();
                quote = client.getCore().getHttpAPI().getPrivateMessage(getSender(), quoteId);
            }
        }

        this.component = component;
        this.timeStamp = timeStamp;
        this.quote = quote;

        client.getStorage().addMessage(this);
        this.completed = true;
    }
}
