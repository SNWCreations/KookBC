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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.BiPredicate;

import static snw.kookbc.util.GsonUtil.get;
import static snw.kookbc.util.GsonUtil.has;

public class EventFactory {
    protected static final Set<Class<? extends Event>> FORCE_CREATE_TYPES;
    protected static final Map<Class<? extends Event>, BiPredicate<EventFactory, JsonObject>> CREATE_CONDITIONS;
    protected final KBCClient client;
    protected final EventManagerImpl eventManager;
    protected final Gson gson;

    private static BiPredicate<EventFactory, JsonObject> subscribed(Class<? extends Event> type) {
        return (eventFactory, json) -> eventFactory.eventManager.isSubscribed(type);
    }

    static {
        FORCE_CREATE_TYPES = new HashSet<Class<? extends Event>>() {
            {
                add(ChannelMessageEvent.class);
                add(PrivateMessageReceivedEvent.class);
                add(ChannelInfoUpdateEvent.class); // what #85 did
            }
        };
        CREATE_CONDITIONS = new HashMap<Class<? extends Event>, BiPredicate<EventFactory, JsonObject>>() {
            {
                put(ChannelMessageDeleteEvent.class, subscribed(ChannelMessageEvent.class));
                put(PrivateMessageDeleteEvent.class, subscribed(PrivateMessageReceivedEvent.class));
            }
        };
    }

    public EventFactory(KBCClient client) {
        this.client = client;
        this.eventManager = ((EventManagerImpl) client.getCore().getEventManager());
        this.gson = createGson();
    }

    public Event createEvent(JsonObject object) {
        final Class<? extends Event> eventType = parseEventType(object);
        if (eventType == null) {
            return null; // unknown event type
        }
        if (!eventManager.isSubscribed(eventType)) {
            if (!FORCE_CREATE_TYPES.contains(eventType)) {
                if (!CREATE_CONDITIONS.containsKey(eventType)
                        || !CREATE_CONDITIONS.get(eventType).test(this, object)) {
                    // don't create event if not necesssary
                    return null;
                }
            }
        }
        if (eventType == GuildInfoUpdateEvent.class) {
            if (has(
                    get(get(object, "extra").getAsJsonObject(), "body").getAsJsonObject(),
                    "my_nickname")) {
                return this.gson.fromJson(object, GuildUserNickNameUpdateEvent.class); // force convert
            }
        }
        final Event result = this.gson.fromJson(object, eventType);

        // why the second condition? see ChannelInfoUpdateEventDeserializer
        if (result == null && !(eventType == ChannelInfoUpdateEvent.class)) {
            client.getCore().getLogger().error("We cannot understand the frame.");
            client.getCore().getLogger().error("Frame content: {}", object);
        }
        return result;
    }

    protected Class<? extends Event> parseEventType(JsonObject object) {
        final String type = get(get(object, "extra").getAsJsonObject(), "type").getAsString();
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
