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

import com.fasterxml.jackson.databind.JsonNode;
import org.jetbrains.annotations.Nullable;
import snw.jkook.entity.CustomEmoji;
import snw.jkook.entity.User;
import snw.jkook.exceptions.BadResponseException;
import snw.jkook.message.ChannelMessage;
import snw.jkook.message.Message;
import snw.jkook.message.component.BaseComponent;
import snw.jkook.message.component.MarkdownComponent;
import snw.jkook.message.component.TemplateMessage;
import snw.jkook.message.component.card.CardComponent;
import snw.jkook.message.component.card.MultipleCardComponent;
import snw.kookbc.impl.KBCClient;
import snw.kookbc.impl.entity.builder.MessageBuilder;
import snw.kookbc.impl.network.HttpAPIRoute;
import snw.kookbc.interfaces.LazyLoadable;
import snw.kookbc.util.MapBuilder;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import static java.util.Objects.requireNonNull;

public abstract class MessageImpl implements Message, LazyLoadable {
    protected final KBCClient client;
    private final String id;
    private User user;
    protected BaseComponent component;
    protected long timeStamp;
    protected Message quote;
    protected boolean completed;

    public MessageImpl(KBCClient client, String id) {
        this.client = requireNonNull(client);
        this.id = requireNonNull(id);
    }

    public MessageImpl(KBCClient client, String id, User user) {
        this(client, id);
        this.user = user;
    }

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
        initIfNeeded();
        return component;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public @Nullable Message getQuote() {
        initIfNeeded();
        return quote;
    }

    @Override
    public User getSender() {
        initIfNeeded();
        return user;
    }

    @Override
    public long getTimeStamp() {
        initIfNeeded();
        return timeStamp;
    }

    @Override
    public Collection<User> getUserByReaction(CustomEmoji customEmoji) {
        JsonNode array;
        try {
            String rawStr = client.getNetworkClient().getRawContent(
                    String.format(
                            "%s?msg_id=%s&emoji=%s",
                            ((this instanceof ChannelMessage) ? HttpAPIRoute.CHANNEL_MESSAGE_REACTION_LIST
                                    : HttpAPIRoute.USER_CHAT_MESSAGE_REACTION_LIST)
                                    .toFullURL(),
                            getId(),
                            URLEncoder.encode(customEmoji.getId(), StandardCharsets.UTF_8.name())));
            JsonNode root = snw.kookbc.util.JacksonUtil.parse(rawStr);
            array = root.get("data");
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
        for (JsonNode element : array) {
            result.add(client.getStorage().getUser(element.get("id").asText()));
        }
        return Collections.unmodifiableCollection(result);
    }

    @Override
    public void setComponent(BaseComponent component) {
        checkCompatibleComponentType(component);
        Object content = MessageBuilder.serialize(component)[1];
        Map<String, Object> body = new MapBuilder()
                .put("msg_id", getId())
                .put("content", content)
                .putIfInstance("template_id", component, TemplateMessage.class, TemplateMessage::getId)
                .build();
        client.getNetworkClient().post(
                ((this instanceof ChannelMessage) ? HttpAPIRoute.CHANNEL_MESSAGE_UPDATE
                        : HttpAPIRoute.USER_CHAT_MESSAGE_UPDATE).toFullURL(),
                body);
    }

    @Override
    public void setComponent(String s) {
        setComponent(new MarkdownComponent(s));
    }

    public void setComponent0(BaseComponent component) {
        this.component = component;
    }

    protected final void checkCompatibleComponentType(BaseComponent newIncoming) {
        if (this.component == null) {
            return; // we don't know, let HTTP API check
        }
        boolean compatible;
        if (newIncoming instanceof TemplateMessage) {
            int oldType = (int) MessageBuilder.serialize(this.component)[0];
            compatible = ((TemplateMessage) newIncoming).getType() == oldType;
        } else {
            compatible = isCompatibleComponentType(this.component, newIncoming);
        }
        if (!compatible) {
            throw new IllegalArgumentException("Incompatible component type, tried updating from "
                    + this.component.getClass() + " to " + newIncoming.getClass());
        }
    }

    private static boolean isCompatibleComponentType(BaseComponent a, BaseComponent b) {
        final Class<? extends BaseComponent> aClass = a.getClass();
        final Class<? extends BaseComponent> bClass = b.getClass();
        if (!aClass.isAssignableFrom(bClass)) {
            if (CardComponent.class.isAssignableFrom(aClass)) {
                return MultipleCardComponent.class.isAssignableFrom(bClass);
            } else if (CardComponent.class.isAssignableFrom(bClass)) {
                return MultipleCardComponent.class.isAssignableFrom(aClass);
            } else {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean isCompleted() {
        return completed;
    }
}
