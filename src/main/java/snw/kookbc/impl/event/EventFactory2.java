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
import snw.jkook.event.channel.*;
import snw.jkook.event.guild.*;
import snw.jkook.event.item.*;
import snw.jkook.event.pm.*;
import snw.jkook.event.role.*;
import snw.jkook.event.user.*;
import snw.kookbc.impl.KBCClient;
import snw.kookbc.impl.serializer.event.channel.*;
import snw.kookbc.impl.serializer.event.guild.*;
import snw.kookbc.impl.serializer.event.item.*;
import snw.kookbc.impl.serializer.event.pm.*;
import snw.kookbc.impl.serializer.event.role.*;
import snw.kookbc.impl.serializer.event.user.*;

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
}
