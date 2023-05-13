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

package snw.kookbc.impl.network;

import snw.jkook.util.Validate;
import snw.kookbc.SharedConstants;
import snw.kookbc.impl.KBCClient;
import snw.kookbc.interfaces.network.policy.RateLimitPolicy;

import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

// Represents the Bucket of Rate Limit.
// Not single instance. Created when network call requested.
// Cached.
public class Bucket {
    private static final Map<HttpAPIRoute, String> bucketNameMap = new EnumMap<>(HttpAPIRoute.class);
    private static final Map<String, Bucket> map = new ConcurrentHashMap<>();

    private final KBCClient client;
    private final String name; // defined by response header
    private final Lock updateLock = new ReentrantLock();
    private final AtomicInteger availableTimes = new AtomicInteger(Integer.MIN_VALUE);
    private final AtomicInteger resetTime = new AtomicInteger();
    private volatile boolean scheduledToUpdate;

    // Use get(KBCClient, String) method instead.
    private Bucket(KBCClient client, String name) {
        Validate.notNull(client);
        Validate.notNull(name);
        this.client = client;
        this.name = name;
    }

    public void update(int availableTimes, int resetTime) {
        this.updateLock.lock();
        try {
            this.availableTimes.set(availableTimes);
            this.resetTime.set(resetTime);
        } finally {
            this.updateLock.unlock();
        }
    }

    // throw TooFastException if too fast, or just decrease one request remaining time.
    public void check() {
        if (availableTimes.get() == Integer.MIN_VALUE) {
            // At this time, we don't know remaining time, so we can't check it
            // We should set the time after got response
            return;
        }
        if (availableTimes.get() <= 0) {
            RateLimitPolicy.getDefault().perform(name, resetTime.get());
            return;
        }
        availableTimes.decrementAndGet();
    }

    @Override
    public String toString() {
        return "Bucket{" +
                "name=" + name + "," +
                "availableTimes=" + availableTimes +
                "}";
    }

    public static Bucket get(KBCClient client, HttpAPIRoute route) {
        String bucketName = bucketNameMap.get(route);
        if (bucketName == null) {
            // This should never happen.
            // Or new API is published.
            throw new IllegalArgumentException("The bucket name of the provided route is unknown. Report to " + SharedConstants.IMPL_NAME + " authors if you saw this!");
        }
        return map.computeIfAbsent(client.hashCode() + "#" + bucketName, r -> new Bucket(client, bucketName));
    }

    static {
        // Bucket name mapping is here.
        // Why I wrote this?
        // Although these known API have their own bucket,
        // but we do not know if the API from future will
        // use the bucket of another API.
        // So the mapping is important.
        bucketNameMap.put(HttpAPIRoute.GUILD_JOINED_LIST, "guild/list");
        bucketNameMap.put(HttpAPIRoute.GUILD_INFO, "guild/view");
        bucketNameMap.put(HttpAPIRoute.GUILD_USERS, "guild/user-list");
        bucketNameMap.put(HttpAPIRoute.GUILD_CHANGE_OTHERS_NICKNAME, "guild/nickname");
        bucketNameMap.put(HttpAPIRoute.GUILD_LEAVE, "guild/leave");
        bucketNameMap.put(HttpAPIRoute.GUILD_KICK, "guild/kickout");
        bucketNameMap.put(HttpAPIRoute.GUILD_BOOST_HISTORY, "guild-boost/history");
        bucketNameMap.put(HttpAPIRoute.MUTE_LIST, "guild-mute/list");
        bucketNameMap.put(HttpAPIRoute.MUTE_ADD, "guild-mute/create");
        bucketNameMap.put(HttpAPIRoute.MUTE_REMOVE, "guild-mute/delete");
        bucketNameMap.put(HttpAPIRoute.CHANNEL_LIST, "channel/list");
        bucketNameMap.put(HttpAPIRoute.CHANNEL_INFO, "channel/view");
        bucketNameMap.put(HttpAPIRoute.CHANNEL_CREATE, "channel/create");
        bucketNameMap.put(HttpAPIRoute.CHANNEL_UPDATE, "channel/update");
        bucketNameMap.put(HttpAPIRoute.CHANNEL_DELETE, "channel/delete");
        bucketNameMap.put(HttpAPIRoute.CHANNEL_USER_LIST, "channel/user-list");
        bucketNameMap.put(HttpAPIRoute.MOVE_USER, "channel/move-user");
        bucketNameMap.put(HttpAPIRoute.CHANNEL_ROLE_CREATE, "channel-role/create");
        bucketNameMap.put(HttpAPIRoute.CHANNEL_ROLE_UPDATE, "channel-role/update");
        bucketNameMap.put(HttpAPIRoute.CHANNEL_ROLE_DELETE, "channel-role/delete");
        bucketNameMap.put(HttpAPIRoute.CHANNEL_MESSAGE_QUERY, "message/list");
        bucketNameMap.put(HttpAPIRoute.CHANNEL_MESSAGE_INFO, "message/view");
        bucketNameMap.put(HttpAPIRoute.CHANNEL_MESSAGE_SEND, "message/create");
        bucketNameMap.put(HttpAPIRoute.CHANNEL_MESSAGE_UPDATE, "message/update");
        bucketNameMap.put(HttpAPIRoute.CHANNEL_MESSAGE_DELETE, "message/delete");
        bucketNameMap.put(HttpAPIRoute.CHANNEL_MESSAGE_REACTION_LIST, "message/reaction-list");
        bucketNameMap.put(HttpAPIRoute.CHANNEL_MESSAGE_REACTION_ADD, "message/add-reaction");
        bucketNameMap.put(HttpAPIRoute.CHANNEL_MESSAGE_REACTION_REMOVE, "message/delete-reaction");
        bucketNameMap.put(HttpAPIRoute.CHANNEL_USER_VOICE_CHANNEL, "channel-user/get-joined-channel");
        bucketNameMap.put(HttpAPIRoute.USER_CHAT_LIST, "user-chat/list");
        bucketNameMap.put(HttpAPIRoute.USER_CHAT_INFO, "user-chat/view");
        bucketNameMap.put(HttpAPIRoute.USER_CHAT_SESSION_CREATE, "user-chat/create");
        bucketNameMap.put(HttpAPIRoute.USER_CHAT_SESSION_DELETE, "user-chat/delete");
        bucketNameMap.put(HttpAPIRoute.USER_CHAT_MESSAGE_LIST, "direct-message/list");
        bucketNameMap.put(HttpAPIRoute.USER_CHAT_MESSAGE_CREATE, "direct-message/create");
        bucketNameMap.put(HttpAPIRoute.USER_CHAT_MESSAGE_UPDATE, "direct-message/update");
        bucketNameMap.put(HttpAPIRoute.USER_CHAT_MESSAGE_DELETE, "direct-message/delete");
        bucketNameMap.put(HttpAPIRoute.USER_CHAT_MESSAGE_REACTION_LIST, "direct-message/reaction-list");
        bucketNameMap.put(HttpAPIRoute.USER_CHAT_MESSAGE_REACTION_ADD, "direct-message/add-reaction");
        bucketNameMap.put(HttpAPIRoute.USER_CHAT_MESSAGE_REACTION_REMOVE, "direct-message/delete-reaction");
        bucketNameMap.put(HttpAPIRoute.GATEWAY, "gateway/index");
        bucketNameMap.put(HttpAPIRoute.USER_ME, "user/me");
        bucketNameMap.put(HttpAPIRoute.USER_WHO, "user/view");
        bucketNameMap.put(HttpAPIRoute.USER_BOT_OFFLINE, "user/offline");
        bucketNameMap.put(HttpAPIRoute.ASSET_UPLOAD, "asset/create");
        bucketNameMap.put(HttpAPIRoute.ROLE_LIST, "guild-role/list");
        bucketNameMap.put(HttpAPIRoute.ROLE_CREATE, "guild-role/create");
        bucketNameMap.put(HttpAPIRoute.ROLE_UPDATE, "guild-role/update");
        bucketNameMap.put(HttpAPIRoute.ROLE_DELETE, "guild-role/delete");
        bucketNameMap.put(HttpAPIRoute.ROLE_GRANT, "guild-role/grant");
        bucketNameMap.put(HttpAPIRoute.ROLE_REVOKE, "guild-role/revoke");
        bucketNameMap.put(HttpAPIRoute.INTIMACY_INFO, "intimacy/index");
        bucketNameMap.put(HttpAPIRoute.INTIMACY_UPDATE, "intimacy/update");
        bucketNameMap.put(HttpAPIRoute.GUILD_EMOJI_LIST, "guild-emoji/list");
        bucketNameMap.put(HttpAPIRoute.GUILD_EMOJI_CREATE, "guild-emoji/create");
        bucketNameMap.put(HttpAPIRoute.GUILD_EMOJI_UPDATE, "guild-emoji/update");
        bucketNameMap.put(HttpAPIRoute.GUILD_EMOJI_DELETE, "guild-emoji/delete");
        bucketNameMap.put(HttpAPIRoute.INVITE_LIST, "invite/list");
        bucketNameMap.put(HttpAPIRoute.INVITE_CREATE, "invite/create");
        bucketNameMap.put(HttpAPIRoute.INVITE_DELETE, "invite/delete");
        bucketNameMap.put(HttpAPIRoute.BLACKLIST_LIST, "blacklist/list");
        bucketNameMap.put(HttpAPIRoute.BLACKLIST_CREATE, "blacklist/create");
        bucketNameMap.put(HttpAPIRoute.BLACKLIST_DELETE, "blacklist/delete");
        bucketNameMap.put(HttpAPIRoute.GAME_LIST, "game");
        bucketNameMap.put(HttpAPIRoute.GAME_CREATE, "game/create");
        bucketNameMap.put(HttpAPIRoute.GAME_UPDATE, "game/update");
        bucketNameMap.put(HttpAPIRoute.GAME_DELETE, "game/delete");
        bucketNameMap.put(HttpAPIRoute.GAME_CREATE_ACTIVITY, "game/activity");
        bucketNameMap.put(HttpAPIRoute.GAME_DELETE_ACTIVITY, "game/delete-activity");
    }
}
