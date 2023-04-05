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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.jetbrains.annotations.Nullable;
import snw.jkook.entity.CustomEmoji;
import snw.jkook.entity.User;
import snw.jkook.message.Message;
import snw.jkook.message.TextChannelMessage;
import snw.jkook.message.component.BaseComponent;
import snw.kookbc.impl.KBCClient;
import snw.kookbc.impl.entity.builder.MessageBuilder;
import snw.kookbc.impl.network.HttpAPIRoute;
import snw.kookbc.impl.network.exceptions.BadResponseException;
import snw.kookbc.util.MapBuilder;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

public abstract class MessageImpl implements Message {
    protected final KBCClient client;
    private final String id;
    private final User user;
    private BaseComponent component;
    private final long timeStamp;
    private final Message quote;

    public MessageImpl(KBCClient client, String id, User user, BaseComponent component, long timeStamp, Message quote) {
        this.client = client;
        this.id = id;
        this.user = user;
        this.component = component;
        this.timeStamp = timeStamp;
        this.quote = quote;
    }

    @Override
    public BaseComponent getComponent() {
        return component;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public @Nullable Message getQuote() {
        return quote;
    }

    @Override
    public User getSender() {
        return user;
    }

    @Override
    public long getTimeStamp() {
        return timeStamp;
    }

    @Override
    public Collection<User> getUserByReaction(CustomEmoji customEmoji) {
        JsonArray array;
        try {
            String rawStr = client.getNetworkClient().getRawContent(
                    String.format(
                            "%s?msg_id=%s&emoji=%s",
                            ((this instanceof TextChannelMessage) ?
                                    HttpAPIRoute.CHANNEL_MESSAGE_REACTION_LIST :
                                    HttpAPIRoute.USER_CHAT_MESSAGE_REACTION_LIST)
                                    .toFullURL(),
                            getId(),
                            URLEncoder.encode(customEmoji.getId(), StandardCharsets.UTF_8.name())
                    )
            );
            array = JsonParser.parseString(rawStr).getAsJsonObject().getAsJsonArray("data");
        } catch (BadResponseException e) {
            if (e.getCode() == 40300) { // 40300, so we should throw IllegalStateException
                throw new IllegalStateException(e);
            } else {
                throw e;
            }
        } catch (UnsupportedEncodingException e) {
            // should never happen
            throw new Error("No UTF-8 encoding?");
        }
        Collection<User> result = new ArrayList<>(array.size());
        for (JsonElement element : array) {
            result.add(client.getStorage().getUser(element.getAsJsonObject().get("id").getAsString()));
        }
        return Collections.unmodifiableCollection(result);
    }

    @Override
    public void setComponent(BaseComponent component) {
        Object content = MessageBuilder.serialize(component)[1];
        Map<String, Object> body = new MapBuilder()
                .put("msg_id", getId())
                .put("content", content)
                .build();
        client.getNetworkClient().post(
                ((this instanceof TextChannelMessage) ? HttpAPIRoute.CHANNEL_MESSAGE_UPDATE : HttpAPIRoute.USER_CHAT_MESSAGE_UPDATE).toFullURL(),
                body
        );
    }

    public void setComponent0(BaseComponent component) {
        this.component = component;
    }
}
