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

import static snw.kookbc.util.JacksonUtil.get;
import static snw.kookbc.util.JacksonUtil.has;

/**
 * 事件工厂类 - 负责从 JSON 数据创建事件对象
 *
 * <p>已完全迁移到 Jackson,移除了 Gson 依赖
 *
 * @since 0.52.0 使用 Jackson 作为唯一 JSON 引擎
 */
public class EventFactory {
    protected final KBCClient client;
    protected final EventManagerImpl eventManager;
    protected final ObjectMapper jacksonMapper;

    public EventFactory(KBCClient client) {
        this.client = client;
        this.eventManager = ((EventManagerImpl) client.getCore().getEventManager());
        this.jacksonMapper = createJacksonMapper();
    }

    /**
     * 从 Jackson JsonNode 创建事件对象
     *
     * @param object JSON 事件数据
     * @return 事件对象,如果无法解析则返回 null
     */
    public Event createEvent(JsonNode object) {
        final Class<? extends Event> eventType = parseEventType(object);
        if (eventType == null) {
            return null; // unknown event type
        }

        // 检查是否有监听器订阅此事件,避免创建无用对象
        if (!eventManager.isSubscribed(eventType)) {
            // 特殊处理:命令系统需要接收消息事件
            if (eventType != ChannelMessageEvent.class && eventType != PrivateMessageReceivedEvent.class) {
                return null;
            }
        }

        // 特殊处理: GuildUserNickNameUpdateEvent 使用特殊字段判断
        if (eventType == GuildInfoUpdateEvent.class) {
            if (has(get(get(object, "extra"), "body"), "my_nickname")) {
                // 修正事件类型为 GuildUserNickNameUpdateEvent
                try {
                    return jacksonMapper.readValue(object.toString(), GuildUserNickNameUpdateEvent.class);
                } catch (Exception e) {
                    client.getCore().getLogger().warn("Failed to parse GuildUserNickNameUpdateEvent with Jackson", e);
                    return null;
                }
            }
        }

        // 使用 Jackson 反序列化事件对象
        try {
            Event result = jacksonMapper.readValue(object.toString(), eventType);
            if (result != null) {
                return result;
            }
        } catch (Exception e) {
            client.getCore().getLogger().error("Failed to deserialize event of type {}: {}",
                    eventType.getSimpleName(), e.getMessage());
            client.getCore().getLogger().debug("Event JSON: {}", object);
        }

        // 如果 Jackson 反序列化失败,记录错误
        if (!(eventType == ChannelInfoUpdateEvent.class)) {
            client.getCore().getLogger().error("We cannot understand the frame.");
            client.getCore().getLogger().error("Frame content: {}", object);
        }
        return null;
    }

    /**
     * 解析事件类型
     *
     * @param object JSON 事件数据
     * @return 事件类型 Class,如果无法识别则返回 null
     */
    protected Class<? extends Event> parseEventType(JsonNode object) {
        final String type = get(get(object, "extra"), "type").asText();

        // 特殊事件:物品消耗事件
        if ("12".equals(type)) {
            return ItemConsumedEvent.class;
        }

        // 标准事件映射
        if (EventTypeMap.MAP.containsKey(type)) {
            return EventTypeMap.MAP.get(type);
        }

        // 验证是否为数字类型
        try {
            Integer.parseInt(type);
        } catch (NumberFormatException e) {
            return null; // unknown event type
        }

        // 消息事件特殊处理:根据频道类型区分
        if ("PERSON".equals(get(object, "channel_type").asText())) {
            return PrivateMessageReceivedEvent.class;
        } else {
            return ChannelMessageEvent.class;
        }
    }

    /**
     * 创建并配置 Jackson ObjectMapper
     *
     * @return 配置好的 ObjectMapper 实例
     */
    protected ObjectMapper createJacksonMapper() {
        ObjectMapper mapper = new ObjectMapper();
        // 注册 JKook 事件反序列化模块
        mapper.registerModule(new snw.kookbc.impl.serializer.event.jackson.JKookEventModule(client));
        return mapper;
    }
}
