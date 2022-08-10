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

package snw.kookbc.impl.storage;

import com.google.gson.JsonObject;
import snw.jkook.entity.*;
import snw.jkook.entity.channel.Channel;
import snw.jkook.message.Message;
import snw.kookbc.impl.KBCClient;
import snw.kookbc.impl.network.HttpAPIRoute;

import java.lang.ref.SoftReference;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class EntityStorage {
    private final KBCClient client;

    private final Map<String, SoftReference<User>> users = new ConcurrentHashMap<>();
    private final Map<String, SoftReference<Guild>> guilds = new ConcurrentHashMap<>();
    private final Map<String, SoftReference<Channel>> channels = new ConcurrentHashMap<>();
    // String key of "roles" is guild ID.
    private final Map<String, Collection<SoftReference<Role>>> roles = new ConcurrentHashMap<>();
    private final Map<String, SoftReference<CustomEmoji>> emojis = new ConcurrentHashMap<>();
    private final Map<String, SoftReference<Message>> msg = new ConcurrentHashMap<>();
    private final Set<SoftReference<Reaction>> reactions = new HashSet<>();
    private final Set<SoftReference<Game>> games = new HashSet<>();

    public EntityStorage(KBCClient client) {
        this.client = client;
    }

    public Game getGame(int id) {
        Iterator<SoftReference<Game>> iterator = games.iterator();
        while (iterator.hasNext()) {
            SoftReference<Game> ref = iterator.next();
            Game game = ref.get();
            if (game != null) {
                if (game.getId() == id) {
                    return game;
                }
            } else {
                iterator.remove();
            }
        }
        return null;
    }

    public Message getMessage(String id) {
        return get(id, msg);
    }

    public User getUser(String id) {
        User result = get(id, users);
        if (result == null) {
            result = client.getEntityBuilder().buildUser(client.getNetworkClient().get(
                    String.format("%s?user_id=%s", HttpAPIRoute.USER_WHO.toFullURL(), id)
            ));
            addUser(result);
        }
        return result;
    }

    public Guild getGuild(String id) {
        Guild result = get(id, guilds);
        if (result == null) {
            try {
                result = client.getEntityBuilder().buildGuild(
                        client.getNetworkClient().get(String.format("%s?guild_id=%s", HttpAPIRoute.GUILD_INFO.toFullURL(), id))
                );
                addGuild(result);
            } catch (RuntimeException e) {
                if (!e.getMessage().contains("403")) throw e; // 403 maybe happened?
            }
        }
        return result;
    }

    public Channel getChannel(String id) {
        Channel result = get(id, channels);
        if (result == null) {
            result = client.getEntityBuilder().buildChannel(
                    client.getNetworkClient().get(String.format("%s?target_id=%s", HttpAPIRoute.CHANNEL_INFO.toFullURL(), id))
            );
            addChannel(result);
        }
        return result;
    }

    public Role getRole(Guild guild, int id) {
        Collection<SoftReference<Role>> list = roles.get(guild.getId());
        if (list != null) {
            Iterator<SoftReference<Role>> iter = list.iterator();
            while (iter.hasNext()) {
                SoftReference<Role> ref = iter.next();
                Role refTarget = ref.get();
                if (refTarget != null) {
                    if (refTarget.getId() == id) {
                        return refTarget;
                    }
                } else {
                    iter.remove();
                }
            }
        }
        return null;
    }

    public CustomEmoji getEmoji(String id) {
        return get(id, emojis);
    }

    public User getUser(String id, JsonObject def) {
        User result = getUser(id);
        if (result == null) {
            result = client.getEntityBuilder().buildUser(def);
            addUser(result);
        } else {
            client.getEntityUpdater().updateUser(def, result);
        }
        return result;
    }

    public Guild getGuild(String id, JsonObject def) {
        Guild result = getGuild(id);
        if (result == null) {
            result = client.getEntityBuilder().buildGuild(def);
            addGuild(result);
        } else {
            client.getEntityUpdater().updateGuild(def, result);
        }
        return result;
    }

    public Channel getChannel(String id, JsonObject def) {
        Channel result = getChannel(id);
        if (result == null) {
            result = client.getEntityBuilder().buildChannel(def);
            addChannel(result);
        } else {
            client.getEntityUpdater().updateChannel(def, result);
        }
        return result;
    }

    public Role getRole(Guild guild, int id, JsonObject def) {
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
        Iterator<SoftReference<Reaction>> iterator = reactions.iterator();
        while (iterator.hasNext()) {
            SoftReference<Reaction> next = iterator.next();
            Reaction reaction = next.get();
            if (reaction == null) {
                iterator.remove();
            } else {
                if (Objects.equals(reaction.getMessageId(), msgId) && reaction.getEmoji() == emoji && reaction.getSender() == sender) {
                    return reaction;
                }
            }
        }
        return null;
    }

    public void addGame(Game game) {
        games.add(new SoftReference<>(game));
    }

    public void addReaction(Reaction reaction) {
        reactions.add(new SoftReference<>(reaction));
    }

    public void addMessage(Message message) {
        msg.put(message.getId(), new SoftReference<>(message));
    }

    public void addEmoji(CustomEmoji emoji) {
        emojis.put(emoji.getId(), new SoftReference<>(emoji));
    }

    public void addUser(User user) {
        users.put(user.getId(), new SoftReference<>(user));
    }

    public void addGuild(Guild guild) {
        guilds.put(guild.getId(), new SoftReference<>(guild));
    }

    public void addChannel(Channel channel) {
        channels.put(channel.getId(), new SoftReference<>(channel));
    }

    public void addRole(Guild guild, Role role) {
        Collection<SoftReference<Role>> list = roles.computeIfAbsent(guild.getId(), k -> new HashSet<>());
        list.add(new SoftReference<>(role));
    }

    public void removeReaction(Reaction reaction) {
        reactions.removeIf(reference -> reference.get() == reaction);
    }

    public void removeMessage(String id) {
        msg.remove(id);
    }

    public void removeChannel(String id) {
        channels.remove(id);
    }

    public void removeGuild(String id) {
        guilds.remove(id);
    }

    private <T> T get(String id, Map<String, SoftReference<T>> map) {
        SoftReference<T> object = map.get(id);
        if (object == null) {
            return null;
        }
        if (object.get() == null) {
            users.remove(id); // clean invalid ref
        }
        return object.get();
    }
}
