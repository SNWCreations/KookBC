/*
 *     KookBC -- The Kook Bot Client & JKook API standard implementation for Java.
 *     Copyright (C) 2022 KookBC contributors
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

package snw.kookbc.impl.network;

import java.util.HashMap;
import java.util.Map;

// Current API version: v3
public enum HttpAPIRoute {

    BASE_URL("https://www.kookapp.cn/api"),

    // ------ GUILD ------

    // direct operations to guild
    GUILD_JOINED_LIST("/v3/guild/list"),
    GUILD_INFO("/v3/guild/view"),
    GUILD_USERS("/v3/guild/user-list"),
    GUILD_CHANGE_OTHERS_NICKNAME("/v3/guild/nickname"),
    GUILD_LEAVE("/v3/guild/leave"),
    GUILD_KICK("/v3/guild/kickout"),
    GUILD_BOOST_HISTORY("/v3/guild-boost/history"),

    // mute
    MUTE_LIST("/v3/guild-mute/list"),
    MUTE_ADD("/v3/guild-mute/create"),
    MUTE_REMOVE("/v3/guild-mute/delete"),

    // ------ CHANNEL ------

    CHANNEL_LIST("/v3/channel/list"),
    CHANNEL_INFO("/v3/channel/view"),
    CHANNEL_CREATE("/v3/channel/create"),
    CHANNEL_UPDATE("/v3/channel/update"),
    CHANNEL_DELETE("/v3/channel/delete"),
    CHANNEL_USER_LIST("/v3/channel/user-list"),
    MOVE_USER("/v3/channel/move-user"),

    // ------ CHANNEL ROLE (NOT "ROLE") -------
    CHANNEL_ROLE_CREATE("/v3/channel-role/create"),
    CHANNEL_ROLE_UPDATE("/v3/channel-role/update"),
    CHANNEL_ROLE_DELETE("/v3/channel-role/delete"),

    // ------ CHANNEL MESSAGE ------
    CHANNEL_MESSAGE_QUERY("/v3/message/list"),
    CHANNEL_MESSAGE_INFO("/v3/message/view"),
    CHANNEL_MESSAGE_SEND("/v3/message/create"),
    CHANNEL_MESSAGE_UPDATE("/v3/message/update"),
    CHANNEL_MESSAGE_DELETE("/v3/message/delete"),
    CHANNEL_MESSAGE_REACTION_LIST("/v3/message/reaction-list"),
    CHANNEL_MESSAGE_REACTION_ADD("/v3/message/add-reaction"),
    CHANNEL_MESSAGE_REACTION_REMOVE("/v3/message/delete-reaction"),

    // ------ CHANNEL USER ------

    CHANNEL_USER_VOICE_CHANNEL("/v3/channel-user/get-joined-channel"),

    // ------ USER CHAT / SESSION ------

    USER_CHAT_LIST("/v3/user-chat/list"),
    USER_CHAT_INFO("/v3/user-chat/view"),
    USER_CHAT_SESSION_CREATE("/v3/user-chat/create"),
    USER_CHAT_SESSION_DELETE("/v3/user-chat/delete"),

    // ------ USER CHAT / MESSAGE ------

    USER_CHAT_MESSAGE_LIST("/v3/direct-message/list"),
    USER_CHAT_MESSAGE_CREATE("/v3/direct-message/create"),
    USER_CHAT_MESSAGE_UPDATE("/v3/direct-message/update"),
    USER_CHAT_MESSAGE_DELETE("/v3/direct-message/delete"),
    USER_CHAT_MESSAGE_REACTION_LIST("/v3/direct-message/reaction-list"),
    USER_CHAT_MESSAGE_REACTION_ADD("/v3/direct-message/add-reaction"),
    USER_CHAT_MESSAGE_REACTION_REMOVE("/v3/direct-message/delete-reaction"),

    // ------ GATEWAY ------

    GATEWAY("/v3/gateway/index"),

    // ------ USER ------

    USER_ME("/v3/user/me"),
    USER_WHO("/v3/user/view"),
    USER_BOT_OFFLINE("/v3/user/offline"),

    // ------ ASSET ------

    ASSET_UPLOAD("/v3/asset/create"),

    // ------ ROLE PERMISSION -------

    ROLE_LIST("/v3/guild-role/list"),
    ROLE_CREATE("/v3/guild-role/create"),
    ROLE_UPDATE("/v3/guild-role/update"),
    ROLE_DELETE("/v3/guild-role/delete"),
    ROLE_GRANT("/v3/guild-role/grant"),
    ROLE_REVOKE("/v3/guild-role/revoke"),

    // ------ INTIMACY ------

    INTIMACY_INFO("/v3/intimacy/index"),
    INTIMACY_UPDATE("/v3/intimacy/update"),

    // ------ GUILD EMOJI ------

    GUILD_EMOJI_LIST("/v3/guild-emoji/list"),
    GUILD_EMOJI_CREATE("/v3/guild-emoji/create"),
    GUILD_EMOJI_UPDATE("/v3/guild-emoji/update"),
    GUILD_EMOJI_DELETE("/v3/guild-emoji/delete"),

    // ------ INVITE ------

    INVITE_LIST("/v3/invite/list"),
    INVITE_CREATE("/v3/invite/create"),
    INVITE_DELETE("/v3/invite/delete"),

    // ------ BLACKLIST ------

    BLACKLIST_LIST("/v3/blacklist/list"),
    BLACKLIST_CREATE("/v3/blacklist/create"),
    BLACKLIST_DELETE("/v3/blacklist/delete"),

    // ------ GAME ------

    GAME_LIST("/v3/game"),
    GAME_CREATE("/v3/game/create"),
    GAME_UPDATE("/v3/game/update"),
    GAME_DELETE("/v3/game/delete"),
    GAME_CREATE_ACTIVITY("/v3/game/activity"),
    GAME_DELETE_ACTIVITY("/v3/game/delete-activity");
    private static final Map<String, HttpAPIRoute> map = new HashMap<>();

    static {
        for (HttpAPIRoute value : values()) {
            map.put(value.getRoute(), value);
        }
    }

    private final String route;

    HttpAPIRoute(String route) {
        this.route = route;
    }

    public String getRoute() {
        return route;
    }

    public String toFullURL() {
        return (this == BASE_URL ? "" : BASE_URL.getRoute()) + this.getRoute();
    }

    public static HttpAPIRoute value(String route) {
        return map.get(route);
    }
}
