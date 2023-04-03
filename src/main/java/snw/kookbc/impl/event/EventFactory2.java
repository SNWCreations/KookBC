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
import snw.jkook.event.Event;
import snw.jkook.event.channel.ChannelCreateEvent;
import snw.jkook.event.channel.ChannelDeleteEvent;
import snw.jkook.event.channel.ChannelInfoUpdateEvent;
import snw.jkook.event.channel.ChannelMessageDeleteEvent;
import snw.jkook.event.channel.ChannelMessageEvent;
import snw.jkook.event.channel.ChannelMessagePinEvent;
import snw.jkook.event.channel.ChannelMessageUnpinEvent;
import snw.jkook.event.channel.ChannelMessageUpdateEvent;
import snw.jkook.event.guild.GuildAddEmojiEvent;
import snw.jkook.event.guild.GuildBanUserEvent;
import snw.jkook.event.guild.GuildDeleteEvent;
import snw.jkook.event.guild.GuildInfoUpdateEvent;
import snw.jkook.event.guild.GuildRemoveEmojiEvent;
import snw.jkook.event.guild.GuildUnbanUserEvent;
import snw.jkook.event.guild.GuildUpdateEmojiEvent;
import snw.jkook.event.guild.GuildUserNickNameUpdateEvent;
import snw.jkook.event.item.ItemConsumedEvent;
import snw.jkook.event.pm.PrivateMessageDeleteEvent;
import snw.jkook.event.pm.PrivateMessageReceivedEvent;
import snw.jkook.event.pm.PrivateMessageUpdateEvent;
import snw.jkook.event.role.RoleCreateEvent;
import snw.jkook.event.role.RoleDeleteEvent;
import snw.jkook.event.role.RoleInfoUpdateEvent;
import snw.jkook.event.user.UserAddReactionEvent;
import snw.jkook.event.user.UserClickButtonEvent;
import snw.jkook.event.user.UserInfoUpdateEvent;
import snw.jkook.event.user.UserJoinGuildEvent;
import snw.jkook.event.user.UserJoinVoiceChannelEvent;
import snw.jkook.event.user.UserLeaveGuildEvent;
import snw.jkook.event.user.UserLeaveVoiceChannelEvent;
import snw.jkook.event.user.UserOfflineEvent;
import snw.jkook.event.user.UserOnlineEvent;
import snw.jkook.event.user.UserRemoveReactionEvent;
import snw.kookbc.impl.KBCClient;
import snw.kookbc.impl.serializer.event.channel.ChannelCreateEventDeserializer;
import snw.kookbc.impl.serializer.event.channel.ChannelDeleteEventDeserializer;
import snw.kookbc.impl.serializer.event.channel.ChannelInfoUpdateEventDeserializer;
import snw.kookbc.impl.serializer.event.channel.ChannelMessageDeleteEventDeserializer;
import snw.kookbc.impl.serializer.event.channel.ChannelMessageEventDeserializer;
import snw.kookbc.impl.serializer.event.channel.ChannelMessagePinEventDeserializer;
import snw.kookbc.impl.serializer.event.channel.ChannelMessageUnpinEventDeserializer;
import snw.kookbc.impl.serializer.event.channel.ChannelMessageUpdateEventDeserializer;
import snw.kookbc.impl.serializer.event.guild.GuildAddEmojiEventDerserializer;
import snw.kookbc.impl.serializer.event.guild.GuildBanUserEventDerserializer;
import snw.kookbc.impl.serializer.event.guild.GuildDeleteEventDerserializer;
import snw.kookbc.impl.serializer.event.guild.GuildInfoUpdateEventDerserializer;
import snw.kookbc.impl.serializer.event.guild.GuildRemoveEmojiEventDerserializer;
import snw.kookbc.impl.serializer.event.guild.GuildUnbanUserEventDerserializer;
import snw.kookbc.impl.serializer.event.guild.GuildUpdateEmojiEventDerserializer;
import snw.kookbc.impl.serializer.event.guild.GuildUserNickNameUpdateEventDerserializer;
import snw.kookbc.impl.serializer.event.item.ItemConsumedEventDeserializer;
import snw.kookbc.impl.serializer.event.pm.PrivateMessageDeleteEventDerserializer;
import snw.kookbc.impl.serializer.event.pm.PrivateMessageReceivedEventDeserializer;
import snw.kookbc.impl.serializer.event.pm.PrivateMessageUpdateEventDerserializer;
import snw.kookbc.impl.serializer.event.role.RoleCreateEventDerserializer;
import snw.kookbc.impl.serializer.event.role.RoleDeleteEventDerserializer;
import snw.kookbc.impl.serializer.event.role.RoleInfoUpdateEventDerserializer;
import snw.kookbc.impl.serializer.event.user.UserAddReactionEventDeserializer;
import snw.kookbc.impl.serializer.event.user.UserClickButtonEventDerserializer;
import snw.kookbc.impl.serializer.event.user.UserInfoUpdateEventDerserializer;
import snw.kookbc.impl.serializer.event.user.UserJoinGuildEventDerserializer;
import snw.kookbc.impl.serializer.event.user.UserJoinVoiceChannelEventDerserializer;
import snw.kookbc.impl.serializer.event.user.UserLeaveGuildEventDerserializer;
import snw.kookbc.impl.serializer.event.user.UserLeaveVoiceChannelEventDerserializer;
import snw.kookbc.impl.serializer.event.user.UserOfflineEventDerserializer;
import snw.kookbc.impl.serializer.event.user.UserOnlineEventDerserializer;
import snw.kookbc.impl.serializer.event.user.UserRemoveReactionEventDerserializer;

import static snw.kookbc.util.GsonUtil.get;

// TODO replace EventFactory class using this class
// after the works on this branch is done.
public class EventFactory2 {
    protected final KBCClient client;
    protected final Gson gson;

    public EventFactory2(KBCClient client) {
        this.client = client;
        this.gson = createGson();
    }

    public Event createEvent(JsonObject object) {
        return this.gson.fromJson(object, parseEventType(object));
    }

    protected Class<? extends Event> parseEventType(JsonObject object) {
        final String type = get(get(object, "extra").getAsJsonObject(), "type").getAsString();
        if ("12".equals(type)) {
            return ItemConsumedEvent.class;
        }
        if (EventTypeMap.MAP.containsKey(type)) {
            return EventTypeMap.MAP.get(type);
        }
        // must be number at this time?
        if ("PERSON".equals(get(object, "channel_type").getAsString())) {
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
                .registerTypeAdapter(GuildAddEmojiEvent.class, new GuildAddEmojiEventDerserializer(client))
                .registerTypeAdapter(GuildBanUserEvent.class, new GuildBanUserEventDerserializer(client))
                .registerTypeAdapter(GuildDeleteEvent.class, new GuildDeleteEventDerserializer(client))
                .registerTypeAdapter(GuildInfoUpdateEvent.class, new GuildInfoUpdateEventDerserializer(client))
                .registerTypeAdapter(GuildRemoveEmojiEvent.class, new GuildRemoveEmojiEventDerserializer(client))
                .registerTypeAdapter(GuildUnbanUserEvent.class, new GuildUnbanUserEventDerserializer(client))
                .registerTypeAdapter(GuildUpdateEmojiEvent.class, new GuildUpdateEmojiEventDerserializer(client))
                .registerTypeAdapter(GuildUserNickNameUpdateEvent.class, new GuildUserNickNameUpdateEventDerserializer(client))

                // PrivateMessage Event
                .registerTypeAdapter(PrivateMessageDeleteEvent.class, new PrivateMessageDeleteEventDerserializer(client))
                .registerTypeAdapter(PrivateMessageUpdateEvent.class, new PrivateMessageUpdateEventDerserializer(client))

                // Role Event
                .registerTypeAdapter(RoleCreateEvent.class, new RoleCreateEventDerserializer(client))
                .registerTypeAdapter(RoleDeleteEvent.class, new RoleDeleteEventDerserializer(client))
                .registerTypeAdapter(RoleInfoUpdateEvent.class, new RoleInfoUpdateEventDerserializer(client))

                // User Event
                .registerTypeAdapter(UserAddReactionEvent.class, new UserAddReactionEventDeserializer(client))
                .registerTypeAdapter(UserClickButtonEvent.class, new UserClickButtonEventDerserializer(client))
                .registerTypeAdapter(UserInfoUpdateEvent.class, new UserInfoUpdateEventDerserializer(client))
                .registerTypeAdapter(UserJoinGuildEvent.class, new UserJoinGuildEventDerserializer(client))
                .registerTypeAdapter(UserJoinVoiceChannelEvent.class, new UserJoinVoiceChannelEventDerserializer(client))
                .registerTypeAdapter(UserLeaveGuildEvent.class, new UserLeaveGuildEventDerserializer(client))
                .registerTypeAdapter(UserLeaveVoiceChannelEvent.class, new UserLeaveVoiceChannelEventDerserializer(client))
                .registerTypeAdapter(UserOfflineEvent.class, new UserOfflineEventDerserializer(client))
                .registerTypeAdapter(UserOnlineEvent.class, new UserOnlineEventDerserializer(client))
                .registerTypeAdapter(UserRemoveReactionEvent.class, new UserRemoveReactionEventDerserializer(client))
                .create();
    }
}
