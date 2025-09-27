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

package snw.kookbc.impl.event;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import snw.jkook.event.Event;
import snw.jkook.event.channel.*;
import snw.jkook.event.guild.*;
import snw.jkook.event.item.ItemConsumedEvent;
import snw.jkook.event.pm.PrivateMessageDeleteEvent;
import snw.jkook.event.pm.PrivateMessageReceivedEvent;
import snw.jkook.event.pm.PrivateMessageUpdateEvent;
import snw.jkook.event.role.RoleCreateEvent;
import snw.jkook.event.role.RoleDeleteEvent;
import snw.jkook.event.role.RoleInfoUpdateEvent;
import snw.jkook.event.user.*;
import snw.kookbc.impl.KBCClient;
import snw.kookbc.impl.serializer.event.channel.*;
import snw.kookbc.impl.serializer.event.guild.*;
import snw.kookbc.impl.serializer.event.item.ItemConsumedEventDeserializer;
import snw.kookbc.impl.serializer.event.pm.PrivateMessageDeleteEventDeserializer;
import snw.kookbc.impl.serializer.event.pm.PrivateMessageReceivedEventDeserializer;
import snw.kookbc.impl.serializer.event.pm.PrivateMessageUpdateEventDeserializer;
import snw.kookbc.impl.serializer.event.role.RoleCreateEventDeserializer;
import snw.kookbc.impl.serializer.event.role.RoleDeleteEventDeserializer;
import snw.kookbc.impl.serializer.event.role.RoleInfoUpdateEventDeserializer;
import snw.kookbc.impl.serializer.event.user.*;

import static snw.kookbc.util.JacksonUtil.get;
import static snw.kookbc.util.JacksonUtil.has;
import static snw.kookbc.util.JacksonUtil.getAsString;
import static snw.kookbc.util.JacksonUtil.getMapper;

public class EventFactory {
    protected final KBCClient client;
    protected final EventManagerImpl eventManager;
    protected final Gson gson;
    protected final ObjectMapper jacksonMapper;  // Jackson高性能处理器

    public EventFactory(KBCClient client) {
        this.client = client;
        this.eventManager = ((EventManagerImpl) client.getCore().getEventManager());
        this.gson = createGson();
        this.jacksonMapper = createJacksonMapper();
    }

    /**
     * Gson兼容版本的事件创建方法
     * 为了向后兼容，保留此方法但委托给Jackson版本
     */
    public Event getEvent(JsonObject object) {
        // 转换为Jackson JsonNode并调用新方法
        try {
            JsonNode node = jacksonMapper.readTree(object.toString());
            return createEvent(node);
        } catch (Exception e) {
            client.getCore().getLogger().error("Failed to convert JsonObject to JsonNode", e);
            return null;
        }
    }

    public Event createEvent(JsonNode object) {
        final Class<? extends Event> eventType = parseEventType(object);
        if (eventType == null) {
            return null; // unknown event type
        }
        if (!eventManager.isSubscribed(eventType)) {
            // if not message event, ensure command system can receive event.
            if (eventType != ChannelMessageEvent.class && eventType != PrivateMessageReceivedEvent.class) {
                return null;
            }
        }
        if (eventType == GuildInfoUpdateEvent.class) {
            if (has(
                    get(get(object, "extra"), "body"),
                    "my_nickname")) {
                // 使用Jackson处理GuildUserNickNameUpdateEvent - 避免Gson运行时调用
                try {
                    return getMapper().readValue(object.toString(), GuildUserNickNameUpdateEvent.class);
                } catch (Exception e) {
                    client.getCore().getLogger().warn("Failed to parse GuildUserNickNameUpdateEvent with Jackson, fallback to Gson", e);
                    return this.gson.fromJson(object.toString(), GuildUserNickNameUpdateEvent.class); // fallback
                }
            }
        }

        // ===== Jackson Migration Support =====
        // 优先使用Jackson进行反序列化，提供更好的null-safe处理
        Event result = createEventWithJackson(object, eventType);
        if (result != null) {
            return result;
        }

        // 回退到Gson（向后兼容）
        result = this.gson.fromJson(object.toString(), eventType);

        // why the second condition? see ChannelInfoUpdateEventDeserializer
        if (result == null && !(eventType == ChannelInfoUpdateEvent.class)) {
            client.getCore().getLogger().error("We cannot understand the frame.");
            client.getCore().getLogger().error("Frame content: {}", object);
        }
        return result;
    }

    /**
     * Jackson版本的事件创建方法 - 处理Kook不完整JSON数据
     * 提供更好的null-safe处理和性能
     */
    protected Event createEventWithJackson(JsonNode object, Class<? extends Event> eventType) {
        try {
            // 检查反序列化器是否支持Jackson
            if (supportsJacksonDeserialization(eventType)) {
                // 这里可以添加Jackson版本的事件反序列化逻辑
                // 目前先返回null，让BaseEventDeserializer的子类逐步迁移
                return null;
            }
        } catch (Exception e) {
            client.getCore().getLogger().debug("Jackson deserialization failed for {}, falling back to Gson", eventType.getSimpleName(), e);
        }
        return null;
    }

    /**
     * 检查事件类型是否支持Jackson反序列化
     * 随着事件反序列化器逐步迁移，这个方法会返回更多true
     */
    protected boolean supportsJacksonDeserialization(Class<? extends Event> eventType) {
        // 目前所有事件反序列化器都继承自BaseEventDeserializer
        // BaseEventDeserializer已经支持Jackson，但子类需要逐步迁移
        return false; // 暂时返回false，等待子类迁移
    }

    protected Class<? extends Event> parseEventType(JsonNode object) {
        final String type = get(get(object, "extra"), "type").asText();
        if ("12".equals(type)) {
            return ItemConsumedEvent.class;
        }
        if (EventTypeMap.MAP.containsKey(type)) {
            return EventTypeMap.MAP.get(type);
        }
        try {
            Integer.parseInt(type);
        } catch (NumberFormatException e) {
            return null; // unknown event type
        }
        // must be number at this time?
        if ("PERSON".equals(get(object, "channel_type").asText())) {
            return PrivateMessageReceivedEvent.class;
        } else {
            return ChannelMessageEvent.class;
        }
    }

    // NOT static, so it can be override.
    protected Gson createGson() {
        final KBCClient client = this.client;
        return new GsonBuilder()
                // --- UNUSUAL EVENTS START ---
                .registerTypeAdapter(ChannelMessageEvent.class, new ChannelMessageEventDeserializer(client))
                .registerTypeAdapter(ItemConsumedEvent.class, new ItemConsumedEventDeserializer(client))
                .registerTypeAdapter(PrivateMessageReceivedEvent.class, new PrivateMessageReceivedEventDeserializer(client))
                // --- UNUSUAL EVENTS END   ---

                // Channel Event
                .registerTypeAdapter(ChannelCreateEvent.class, new ChannelCreateEventDeserializer(client))
                .registerTypeAdapter(ChannelDeleteEvent.class, new ChannelDeleteEventDeserializer(client))
                .registerTypeAdapter(ChannelInfoUpdateEvent.class, new ChannelInfoUpdateEventDeserializer(client))
                .registerTypeAdapter(ChannelMessageDeleteEvent.class, new ChannelMessageDeleteEventDeserializer(client))
                .registerTypeAdapter(ChannelMessagePinEvent.class, new ChannelMessagePinEventDeserializer(client))
                .registerTypeAdapter(ChannelMessageUnpinEvent.class, new ChannelMessageUnpinEventDeserializer(client))
                .registerTypeAdapter(ChannelMessageUpdateEvent.class, new ChannelMessageUpdateEventDeserializer(client))

                // Guild Event
                .registerTypeAdapter(GuildAddEmojiEvent.class, new GuildAddEmojiEventDeserializer(client))
                .registerTypeAdapter(GuildBanUserEvent.class, new GuildBanUserEventDeserializer(client))
                .registerTypeAdapter(GuildDeleteEvent.class, new GuildDeleteEventDeserializer(client))
                .registerTypeAdapter(GuildInfoUpdateEvent.class, new GuildInfoUpdateEventDeserializer(client))
                .registerTypeAdapter(GuildRemoveEmojiEvent.class, new GuildRemoveEmojiEventDeserializer(client))
                .registerTypeAdapter(GuildUnbanUserEvent.class, new GuildUnbanUserEventDeserializer(client))
                .registerTypeAdapter(GuildUpdateEmojiEvent.class, new GuildUpdateEmojiEventDeserializer(client))
                .registerTypeAdapter(GuildUserNickNameUpdateEvent.class, new GuildUserNickNameUpdateEventDeserializer(client))

                // PrivateMessage Event
                .registerTypeAdapter(PrivateMessageDeleteEvent.class, new PrivateMessageDeleteEventDeserializer(client))
                .registerTypeAdapter(PrivateMessageUpdateEvent.class, new PrivateMessageUpdateEventDeserializer(client))

                // Role Event
                .registerTypeAdapter(RoleCreateEvent.class, new RoleCreateEventDeserializer(client))
                .registerTypeAdapter(RoleDeleteEvent.class, new RoleDeleteEventDeserializer(client))
                .registerTypeAdapter(RoleInfoUpdateEvent.class, new RoleInfoUpdateEventDeserializer(client))

                // User Event
                .registerTypeAdapter(UserAddReactionEvent.class, new UserAddReactionEventDeserializer(client))
                .registerTypeAdapter(UserClickButtonEvent.class, new UserClickButtonEventDeserializer(client))
                .registerTypeAdapter(UserInfoUpdateEvent.class, new UserInfoUpdateEventDeserializer(client))
                .registerTypeAdapter(UserJoinGuildEvent.class, new UserJoinGuildEventDeserializer(client))
                .registerTypeAdapter(UserJoinVoiceChannelEvent.class, new UserJoinVoiceChannelEventDeserializer(client))
                .registerTypeAdapter(UserLeaveGuildEvent.class, new UserLeaveGuildEventDeserializer(client))
                .registerTypeAdapter(UserLeaveVoiceChannelEvent.class, new UserLeaveVoiceChannelEventDeserializer(client))
                .registerTypeAdapter(UserOfflineEvent.class, new UserOfflineEventDeserializer(client))
                .registerTypeAdapter(UserOnlineEvent.class, new UserOnlineEventDeserializer(client))
                .registerTypeAdapter(UserRemoveReactionEvent.class, new UserRemoveReactionEventDeserializer(client))
                .create();
    }

    // Jackson ObjectMapper配置
    protected ObjectMapper createJacksonMapper() {
        ObjectMapper mapper = new ObjectMapper();
        // 添加事件反序列化器配置 - 如需要的话
        return mapper;
    }
}

