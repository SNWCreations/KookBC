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

import snw.jkook.event.Event;
import snw.jkook.event.channel.*;
import snw.jkook.event.guild.*;
import snw.jkook.event.pm.PrivateMessageDeleteEvent;
import snw.jkook.event.pm.PrivateMessageUpdateEvent;
import snw.jkook.event.role.RoleCreateEvent;
import snw.jkook.event.role.RoleDeleteEvent;
import snw.jkook.event.role.RoleInfoUpdateEvent;
import snw.jkook.event.user.*;

import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class EventTypeMap {
    public static final Map<String, Class<? extends Event>> MAP;

    static {
        final Map<String, Class<? extends Event>> mutableMap = new HashMap<String, Class<? extends Event>>() {
            @Override
            public Class<? extends Event> put(String key, Class<? extends Event> value) {
                if (Modifier.isAbstract(value.getModifiers())) {
                    throw new IllegalArgumentException("You cannot put the abstract event types into this map.");
                }
                return super.put(key, value);
            }

            @Override
            public Class<? extends Event> get(Object key) {
                if (!(key instanceof String)) {
                    throw new IllegalArgumentException("Event type map requires String.");
                }
                if (!containsKey(key)) {
                    throw new IllegalStateException("Unknown event type: " + key);
                }
                return super.get(key);
            }
        };

        mutableMap.put("added_reaction", UserAddReactionEvent.class);
        mutableMap.put("deleted_reaction", UserRemoveReactionEvent.class);
        mutableMap.put("updated_message", ChannelMessageUpdateEvent.class);
        mutableMap.put("deleted_message", ChannelMessageDeleteEvent.class);
        mutableMap.put("added_channel", ChannelCreateEvent.class);
        mutableMap.put("updated_channel", ChannelInfoUpdateEvent.class);
        mutableMap.put("deleted_channel", ChannelDeleteEvent.class);
        mutableMap.put("pinned_message", ChannelMessagePinEvent.class);
        mutableMap.put("unpinned_message", ChannelMessageUnpinEvent.class);
        mutableMap.put("updated_guild_member", GuildUserNickNameUpdateEvent.class);
        mutableMap.put("guild_member_online", UserOnlineEvent.class);
        mutableMap.put("guild_member_offline", UserOfflineEvent.class);
        mutableMap.put("added_role", RoleCreateEvent.class);
        mutableMap.put("deleted_role", RoleDeleteEvent.class);
        mutableMap.put("updated_role", RoleInfoUpdateEvent.class);
        mutableMap.put("updated_guild", GuildInfoUpdateEvent.class);
        mutableMap.put("deleted_guild", GuildDeleteEvent.class);
        mutableMap.put("added_block_list", GuildBanUserEvent.class);
        mutableMap.put("deleted_block_list", GuildUnbanUserEvent.class);
        mutableMap.put("added_emoji", GuildAddEmojiEvent.class);
        mutableMap.put("deleted_emoji", GuildRemoveEmojiEvent.class);
        mutableMap.put("updated_emoji", GuildUpdateEmojiEvent.class);
        mutableMap.put("updated_private_message", PrivateMessageUpdateEvent.class);
        mutableMap.put("deleted_private_message", PrivateMessageDeleteEvent.class);
        mutableMap.put("private_added_reaction", UserAddReactionEvent.class);
        mutableMap.put("private_deleted_reaction", UserRemoveReactionEvent.class);
        mutableMap.put("joined_guild", UserJoinGuildEvent.class);
        mutableMap.put("exited_guild", UserLeaveGuildEvent.class);
        mutableMap.put("joined_channel", UserJoinVoiceChannelEvent.class);
        mutableMap.put("exited_channel", UserLeaveVoiceChannelEvent.class);
        mutableMap.put("message_btn_click", UserClickButtonEvent.class);
        mutableMap.put("user_updated", UserInfoUpdateEvent.class);
        mutableMap.put("self_joined_guild", UserJoinGuildEvent.class);
        mutableMap.put("self_exited_guild", UserLeaveGuildEvent.class);
        MAP = Collections.unmodifiableMap(mutableMap);
    }

    private EventTypeMap() {} // can't call constructor
}
