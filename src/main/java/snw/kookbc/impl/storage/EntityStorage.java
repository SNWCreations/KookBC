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

package snw.kookbc.impl.storage;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.google.gson.JsonObject;
import snw.jkook.entity.*;
import snw.jkook.entity.channel.Channel;
import snw.jkook.message.Message;
import snw.kookbc.SharedConstants;
import snw.kookbc.impl.KBCClient;
import snw.kookbc.impl.network.HttpAPIRoute;
import snw.kookbc.impl.network.exceptions.BadResponseException;
import snw.kookbc.util.Util;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public class EntityStorage {
    public static final int RETRY_TIMES = 1;

    private final KBCClient client;

    // See the notes of these member variables in the constructor.
    private final LoadingCache<String, User> users;
    private final LoadingCache<String, Guild> guilds;
    private final ConcurrentMap<String, ChannelReference> channels;
    private final Function<String, Channel> channelLoader;
    private final ReferenceQueue<Channel> refQueue;

    // The following data types can be loaded manually, but it costs too many network resource.
    // So we won't remove them if the memory is enough.
    private final Cache<String, Role> roles;
    private final Cache<String, CustomEmoji> emojis;
    private final Cache<String, Message> msgs;
    private final Cache<String, Reaction> reactions;
    private final Cache<Integer, Game> games;

    public EntityStorage(KBCClient client) {
        this.client = client;
        this.users = newCaffeineBuilderWithWeakRef()
                .build(withRetry(id ->
                        client.getEntityBuilder().buildUser(
                                client.getNetworkClient().get(
                                        String.format("%s?user_id=%s", HttpAPIRoute.USER_WHO.toFullURL(), id)
                                )
                        )
                ));
        this.guilds = newCaffeineBuilderWithWeakRef()
                .build(withRetry(id -> {
                    try {
                        return client.getEntityBuilder().buildGuild(
                                client.getNetworkClient().get(String.format("%s?guild_id=%s", HttpAPIRoute.GUILD_INFO.toFullURL(), id))
                        );
                    } catch (BadResponseException e) {
                        if (!(e.getCode() == 403)) throw e; // 403 maybe happened?
                    }
                    return null;
                }));
        this.channels = new ConcurrentHashMap<>();
        this.msgs = newCaffeineBuilderWithSoftRef().build(); // key: msg id
        this.roles = newCaffeineBuilderWithSoftRef().build(); // key format: GUILD_ID#ROLE_ID
        this.emojis = newCaffeineBuilderWithSoftRef().build(); // key: emoji ID
        this.reactions = newCaffeineBuilderWithSoftRef().build(); // key format: MSG_ID#EMOJI_ID#SENDER_ID
        this.games = newCaffeineBuilderWithSoftRef().build(); // key: game id

        this.channelLoader = Util.withRetry(
                id ->
                client.getEntityBuilder().buildChannel(
                        client.getNetworkClient().get(
                                String.format("%s?target_id=%s", HttpAPIRoute.CHANNEL_INFO.toFullURL(), id)
                        )
                )
        );
        this.refQueue = new ReferenceQueue<>();
        new RefCleaner().start();
    }

    public Game getGame(int id) {
        return games.getIfPresent(id);
    }

    public Message getMessage(String id) {
        return msgs.getIfPresent(id);
    }

    public User getUser(String id) {
        return users.get(id);
    }

    public Guild getGuild(String id) {
        return guilds.get(id);
    }

    public Channel getChannel(String id) {
        Channel channel = getChannel0(id);
        if (channel == null) {
            channel = channelLoader.apply(id);
            channels.putIfAbsent(id, new ChannelReference(id, channel, refQueue)); // prevent JDK-8062841
        }
        return channel;
    }

    public Role getRole(Guild guild, int id) {
        return roles.getIfPresent(guild.getId() + "#" + id);
    }

    public CustomEmoji getEmoji(String id) {
        return emojis.getIfPresent(id);
    }

    public User getUser(String id, JsonObject def) {
        // use getIfPresent, because the def should not be wasted
        User result = users.getIfPresent(id);
        if (result == null) {
            result = client.getEntityBuilder().buildUser(def);
            addUser(result);
        } else {
            client.getEntityUpdater().updateUser(def, result);
        }
        return result;
    }

    public Guild getGuild(String id, JsonObject def) {
        Guild result = guilds.getIfPresent(id);
        if (result == null) {
            result = client.getEntityBuilder().buildGuild(def);
            addGuild(result);
        } else {
            client.getEntityUpdater().updateGuild(def, result);
        }
        return result;
    }

    public Channel getChannel(String id, JsonObject def) {
        Channel result = getChannel0(id);
        if (result == null) {
            result = client.getEntityBuilder().buildChannel(def);
            addChannel(result);
        } else {
            client.getEntityUpdater().updateChannel(def, result);
        }
        return result;
    }

    public Role getRole(Guild guild, int id, JsonObject def) {
        // getRole is Nullable
        Role result = getRole(guild, id);
        if (result == null) {
            result = client.getEntityBuilder().buildRole(guild, def);
            addRole(guild, result);
        } else {
            client.getEntityUpdater().updateRole(def, result);
        }
        return result;
    }

    public CustomEmoji getEmoji(String id, JsonObject def) {
        CustomEmoji emoji = getEmoji(id);
        if (emoji == null) {
            emoji = client.getEntityBuilder().buildEmoji(def);
            addEmoji(emoji);
        } else {
            client.getEntityUpdater().updateEmoji(def, emoji);
        }
        return emoji;
    }

    public Reaction getReaction(String msgId, CustomEmoji emoji, User sender) {
        return reactions.getIfPresent(msgId + "#" + emoji.getId() + "#" + sender.getId());
    }

    public void addGame(Game game) {
        games.put(game.getId(), game);
    }

    public void addReaction(Reaction reaction) {
        reactions.put(reaction.getMessageId() + "#" + reaction.getEmoji().getId() + "#" + reaction.getSender().getId(), reaction);
    }

    public void addMessage(Message message) {
        msgs.put(message.getId(), message);
    }

    public void addEmoji(CustomEmoji emoji) {
        emojis.put(emoji.getId(), emoji);
    }

    public void addUser(User user) {
        users.put(user.getId(), user);
    }

    public void addGuild(Guild guild) {
        guilds.put(guild.getId(), guild);
    }

    public void addChannel(Channel channel) {
        channels.put(channel.getId(), new ChannelReference(channel.getId(), channel, refQueue));
    }

    public void addRole(Guild guild, Role role) {
        roles.put(guild.getId() + "#" + role.getId(), role);
    }

    public void removeReaction(Reaction reaction) {
        reactions.invalidate(reaction.getMessageId() + "#" + reaction.getEmoji().getId() + "#" + reaction.getSender().getId());
    }

    // Only called when the message is invalid
    public void removeMessage(String id) {
        msgs.invalidate(id);
        reactions.asMap().keySet().removeIf(i -> i.startsWith(id));
    }

    public void removeChannel(String id) {
        channels.remove(id);
    }

    public void removeGuild(String id) {
        guilds.invalidate(id);
    }

    private static Caffeine<Object, Object> newCaffeineBuilderWithWeakRef() {
        return Caffeine.newBuilder()
                .weakValues()
                .expireAfterAccess(10, TimeUnit.MINUTES);
    }

    private static Caffeine<Object, Object> newCaffeineBuilderWithSoftRef() {
        return Caffeine.newBuilder()
                .softValues();
    }

    private static <K, V> CacheLoader<K, V> withRetry(CacheLoader<K, V> original) {
        return k -> {
            int retries = RETRY_TIMES;
            Exception latestException;
            do {
                try {
                    return original.load(k);
                } catch (Exception e) {
                    latestException = e;
                }
            } while (retries-- > 0);
            throw new RuntimeException("Unable to load resource", latestException);
        };
    }

    // It was removed since we added Caffeine, but it was "respawned" here...
    private Channel getChannel0(String id) {
        ChannelReference object = channels.get(id);
        if (object == null) {
            return null;
        }
        return object.get();
    }

    class RefCleaner extends Thread {
        public RefCleaner() {
            super(SharedConstants.IMPL_NAME + " - WeakReference Cleaner");
            setDaemon(true);
            setPriority(Thread.MAX_PRIORITY);
        }

        @Override
        public void run() {
            try {
                run0();
            } catch (Exception ignored) {}
        }

        private void run0() throws Exception {
            while (client.isRunning()) {
                ChannelReference res = ((ChannelReference) refQueue.remove());
                removeChannel(res.getId());
            }
        }
    }
}

class ChannelReference extends WeakReference<Channel> {
    private final String id;

    public ChannelReference(String id, Channel referent, ReferenceQueue<? super Channel> q) {
        super(referent, q);
        this.id = id;
    }

    public String getId() {
        return id;
    }
}