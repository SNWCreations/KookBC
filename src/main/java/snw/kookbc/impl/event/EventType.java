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

import java.util.HashMap;
import java.util.Map;

// The basic mapping for various events that provided by Kook
public enum EventType {

    CHANNEL_USER_ADD_REACTION("added_reaction"),
    CHANNEL_USER_REMOVE_REACTION("deleted_reaction"),
    CHANNEL_MESSAGE_UPDATE("updated_message"),
    CHANNEL_MESSAGE_DELETE("deleted_message"),
    CHANNEL_CREATE("added_channel"),
    CHANNEL_UPDATE("updated_channel"),
    CHANNEL_DELETE("deleted_channel"),
    CHANNEL_MESSAGE_PINNED("pinned_message"),
    CHANNEL_MESSAGE_UNPINNED("unpinned_message"),
    GUILD_USER_UPDATE("updated_guild_member"),
    GUILD_USER_ONLINE("guild_member_online"),
    GUILD_USER_OFFLINE("guild_member_offline"),
    GUILD_ADD_ROLE("added_role"),
    GUILD_REMOVE_ROLE("deleted_role"),
    GUILD_UPDATE_ROLE("updated_role"),
    GUILD_UPDATE("updated_guild"),
    GUILD_DELETE("deleted_guild"),
    GUILD_BAN("added_block_list"),
    GUILD_UNBAN("deleted_block_list"),
    ADD_EMOJI("added_emoji"),
    DELETE_EMOJI("deleted_emoji"),
    UPDATE_EMOJI("updated_emoji"),
    PM_UPDATE("updated_private_message"),
    PM_DELETE("deleted_private_message"),
    PM_ADD_REACTION("private_added_reaction"),
    PM_REMOVE_REACTION("private_deleted_reaction"),
    USER_JOINED_GUILD("joined_guild"),
    USER_LEFT_GUILD("exited_guild"),
    USER_JOINED_VOICE_CHANNEL("joined_channel"),
    USER_LEFT_VOICE_CHANNEL("exited_channel"),
    USER_CLICK_BUTTON("message_btn_click"),
    // the following one type will sometimes be called, but not always.
    USER_UPDATE("user_updated"),
    SELF_JOINED_GUILD("self_joined_guild"),
    SELF_LEFT_GUILD("self_exited_guild");


    private static final Map<String, EventType> values = new HashMap<>();

    static {
        for (EventType value : values()) {
            values.put(value.getValue(), value);
        }
    }

    private final String value;

    EventType(String value) {
        this.value = value;
    }

    public static EventType value(String name) {
        return values.get(name);
    }

    public String getValue() {
        return value;
    }
}
