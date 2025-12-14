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

package snw.kookbc.impl.serializer.event.jackson;

import com.fasterxml.jackson.databind.module.SimpleModule;
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
import snw.kookbc.impl.serializer.event.jackson.channel.*;
import snw.kookbc.impl.serializer.event.jackson.guild.*;
import snw.kookbc.impl.serializer.event.jackson.item.ItemConsumedEventJacksonDeserializer;
import snw.kookbc.impl.serializer.event.jackson.pm.PrivateMessageDeleteEventJacksonDeserializer;
import snw.kookbc.impl.serializer.event.jackson.pm.PrivateMessageReceivedEventJacksonDeserializer;
import snw.kookbc.impl.serializer.event.jackson.pm.PrivateMessageUpdateEventJacksonDeserializer;
import snw.kookbc.impl.serializer.event.jackson.role.RoleCreateEventJacksonDeserializer;
import snw.kookbc.impl.serializer.event.jackson.role.RoleDeleteEventJacksonDeserializer;
import snw.kookbc.impl.serializer.event.jackson.role.RoleInfoUpdateEventJacksonDeserializer;
import snw.kookbc.impl.serializer.event.jackson.user.*;

/**
 * Jackson 自定义模块 - JKook 事件反序列化
 *
 * <p>该模块为所有 JKook 事件类注册了 Jackson 自定义反序列化器，
 * 使得 Jackson 能够正确反序列化没有无参构造函数的事件对象。
 *
 * <p>架构设计：
 * <ul>
 *   <li>每个事件类型对应一个专用的 Jackson Deserializer</li>
 *   <li>反序列化器从 JsonNode 提取数据，然后调用事件类构造函数</li>
 *   <li>提供更好的 null-safe 处理和性能优化</li>
 *   <li>完全替代 GSON 反序列化器</li>
 * </ul>
 *
 * @since KookBC 0.33.0
 * @see com.fasterxml.jackson.databind.module.SimpleModule
 */
public class JKookEventModule extends SimpleModule {

    private final KBCClient client;

    /**
     * 创建 JKook 事件反序列化模块
     *
     * @param client KBCClient 实例，用于传递给反序列化器
     */
    public JKookEventModule(KBCClient client) {
        super("JKookEventModule");
        this.client = client;
        registerDeserializers();
    }

    /**
     * 注册所有事件反序列化器
     */
    private void registerDeserializers() {
        // === Channel Events ===
        addDeserializer(ChannelMessageEvent.class, new ChannelMessageEventJacksonDeserializer(client));
        addDeserializer(ChannelCreateEvent.class, new ChannelCreateEventJacksonDeserializer(client));
        addDeserializer(ChannelDeleteEvent.class, new ChannelDeleteEventJacksonDeserializer(client));
        addDeserializer(ChannelInfoUpdateEvent.class, new ChannelInfoUpdateEventJacksonDeserializer(client));
        addDeserializer(ChannelMessageDeleteEvent.class, new ChannelMessageDeleteEventJacksonDeserializer(client));
        addDeserializer(ChannelMessagePinEvent.class, new ChannelMessagePinEventJacksonDeserializer(client));
        addDeserializer(ChannelMessageUnpinEvent.class, new ChannelMessageUnpinEventJacksonDeserializer(client));
        addDeserializer(ChannelMessageUpdateEvent.class, new ChannelMessageUpdateEventJacksonDeserializer(client));

        // === Guild Events ===
        addDeserializer(GuildAddEmojiEvent.class, new GuildAddEmojiEventJacksonDeserializer(client));
        addDeserializer(GuildBanUserEvent.class, new GuildBanUserEventJacksonDeserializer(client));
        addDeserializer(GuildDeleteEvent.class, new GuildDeleteEventJacksonDeserializer(client));
        addDeserializer(GuildInfoUpdateEvent.class, new GuildInfoUpdateEventJacksonDeserializer(client));
        addDeserializer(GuildRemoveEmojiEvent.class, new GuildRemoveEmojiEventJacksonDeserializer(client));
        addDeserializer(GuildUnbanUserEvent.class, new GuildUnbanUserEventJacksonDeserializer(client));
        addDeserializer(GuildUpdateEmojiEvent.class, new GuildUpdateEmojiEventJacksonDeserializer(client));
        addDeserializer(GuildUserNickNameUpdateEvent.class, new GuildUserNickNameUpdateEventJacksonDeserializer(client));

        // === Private Message Events ===
        addDeserializer(PrivateMessageReceivedEvent.class, new PrivateMessageReceivedEventJacksonDeserializer(client));
        addDeserializer(PrivateMessageDeleteEvent.class, new PrivateMessageDeleteEventJacksonDeserializer(client));
        addDeserializer(PrivateMessageUpdateEvent.class, new PrivateMessageUpdateEventJacksonDeserializer(client));

        // === Role Events ===
        addDeserializer(RoleCreateEvent.class, new RoleCreateEventJacksonDeserializer(client));
        addDeserializer(RoleDeleteEvent.class, new RoleDeleteEventJacksonDeserializer(client));
        addDeserializer(RoleInfoUpdateEvent.class, new RoleInfoUpdateEventJacksonDeserializer(client));

        // === User Events ===
        addDeserializer(UserAddReactionEvent.class, new UserAddReactionEventJacksonDeserializer(client));
        addDeserializer(UserClickButtonEvent.class, new UserClickButtonEventJacksonDeserializer(client));
        addDeserializer(UserInfoUpdateEvent.class, new UserInfoUpdateEventJacksonDeserializer(client));
        addDeserializer(UserJoinGuildEvent.class, new UserJoinGuildEventJacksonDeserializer(client));
        addDeserializer(UserJoinVoiceChannelEvent.class, new UserJoinVoiceChannelEventJacksonDeserializer(client));
        addDeserializer(UserLeaveGuildEvent.class, new UserLeaveGuildEventJacksonDeserializer(client));
        addDeserializer(UserLeaveVoiceChannelEvent.class, new UserLeaveVoiceChannelEventJacksonDeserializer(client));
        addDeserializer(UserOfflineEvent.class, new UserOfflineEventJacksonDeserializer(client));
        addDeserializer(UserOnlineEvent.class, new UserOnlineEventJacksonDeserializer(client));
        addDeserializer(UserRemoveReactionEvent.class, new UserRemoveReactionEventJacksonDeserializer(client));

        // === Item Events ===
        addDeserializer(ItemConsumedEvent.class, new ItemConsumedEventJacksonDeserializer(client));
    }

    @Override
    public String getModuleName() {
        return "JKookEventModule";
    }
}
