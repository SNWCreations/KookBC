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

package snw.kookbc.impl;

import static java.util.Collections.unmodifiableCollection;
import static snw.kookbc.util.JacksonUtil.get;
import static snw.kookbc.util.JacksonUtil.parse;
import static snw.kookbc.util.GsonUtil.NORMAL_GSON;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.gson.JsonObject;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import snw.jkook.HttpAPI;
import snw.jkook.entity.Game;
import snw.jkook.entity.Guild;
import snw.jkook.entity.User;
import snw.jkook.entity.channel.Category;
import snw.jkook.entity.channel.Channel;
import snw.jkook.entity.channel.TextChannel;
import snw.jkook.entity.channel.VoiceChannel;
import snw.jkook.message.ChannelMessage;
import snw.jkook.message.Message;
import snw.jkook.message.PrivateMessage;
import snw.jkook.message.TextChannelMessage;
import snw.jkook.util.PageIterator;
import snw.jkook.util.Validate;
import snw.kookbc.impl.entity.channel.CategoryImpl;
import snw.kookbc.impl.entity.channel.TextChannelImpl;
import snw.kookbc.impl.entity.channel.VoiceChannelImpl;
import snw.kookbc.impl.message.ChannelMessageImpl;
import snw.kookbc.impl.message.PrivateMessageImpl;
import snw.kookbc.impl.message.TextChannelMessageImpl;
import snw.kookbc.impl.network.HttpAPIRoute;
import snw.kookbc.impl.pageiter.GameIterator;
import snw.kookbc.impl.pageiter.JoinedGuildIterator;
import snw.kookbc.impl.pageiter.JoinedVoiceChannelsIterator;
import snw.kookbc.util.MapBuilder;

public class HttpAPIImpl implements HttpAPI {
    private static final MediaType OCTET_STREAM;
    private static final Collection<String> SUPPORTED_MUSIC_SOFTWARES;
    // private static final long UPLOAD_FILE_LENGTH_LIMIT = 25; // in MB

    static {
        OCTET_STREAM = MediaType.parse("application/octet-stream");
        SUPPORTED_MUSIC_SOFTWARES = Collections.unmodifiableCollection(
                Arrays.asList(
                        "cloudmusic", "qqmusic", "kugou"
                )
        );
    }

    private final KBCClient client;

    // 将 JsonObject (Gson) 转换为 JsonNode (Jackson)
    private JsonNode toJsonNode(JsonObject gsonObject) {
        return parse(NORMAL_GSON.toJson(gsonObject));
    }

    // 将 JsonNode (Jackson) 转换为 JsonObject (Gson)
    private JsonObject toJsonObject(JsonNode jacksonNode) {
        return NORMAL_GSON.fromJson(jacksonNode.toString(), JsonObject.class);
    }

    public HttpAPIImpl(KBCClient client) {
        this.client = client;
    }

    @Override
    public PageIterator<Collection<Guild>> getJoinedGuilds() {
        return new JoinedGuildIterator(client);
    }

    @Override
    public User getUser(String s) {
        return client.getStorage().getUser(s);
    }

    @Override
    public Guild getGuild(String s) {
        return client.getStorage().getGuild(s);
    }

    @Override
    public Channel getChannel(String s) {
        return client.getStorage().getChannel(s);
    }

    @Override
    public TextChannel getTextChannel(String s) {
        return new TextChannelImpl(client, s);
    }

    @Override
    public VoiceChannel getVoiceChannel(String s) {
        return new VoiceChannelImpl(client, s);
    }

    @Override
    public Category getCategory(String s) {
        return new CategoryImpl(client, s);
    }

    @Override
    public String uploadFile(File file) {
        RequestBody body = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", file.getName(), RequestBody.create(file, OCTET_STREAM))
                .build();
        Request request = new Request.Builder()
                .url(HttpAPIRoute.ASSET_UPLOAD.toFullURL())
                .post(body)
                .addHeader("Authorization", client.getNetworkClient().getTokenWithPrefix())
                .build();
        return get(parse(client.getNetworkClient().call(request)).get("data"), "url").asText();
    }

    @Override
    public String uploadFile(String filename, byte[] content) {
        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", filename, RequestBody.create(content, OCTET_STREAM))
                .build();
        Request request = new Request.Builder()
                .url(HttpAPIRoute.ASSET_UPLOAD.toFullURL())
                .post(requestBody)
                .addHeader("Authorization", client.getNetworkClient().getTokenWithPrefix())
                .build();
        return get(parse(client.getNetworkClient().call(request)).get("data"), "url").asText();
    }

    @Override
    public String uploadFile(String fileName, String url) throws IllegalArgumentException {
        try {
            new URL(url);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Cannot upload file: Malformed URL", e);
        }
        try (Response response = client.getNetworkClient().getOkHttpClient().newCall(
                new Request.Builder()
                        .get()
                        .url(url)
                        .build()
            ).execute()) {
            final String bodyErr = "Cannot upload file at " + url + ": Response body should not be null";
            final ResponseBody body = Objects.requireNonNull(response.body(), bodyErr);
            long contentLength = body.contentLength();
            // if (contentLength > UPLOAD_FILE_LENGTH_LIMIT * 1024) {
            //     throw new IllegalArgumentException("Cannot upload file at " + url + ": Toobig file");
            // }
            byte[] bytes = body.bytes();
            return uploadFile(fileName, bytes);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void removeInvite(String urlCode) {
        client.getNetworkClient().post(HttpAPIRoute.INVITE_DELETE.toFullURL(),
                Collections.singletonMap("url_code", urlCode));
    }

    @Override
    public PageIterator<Collection<Game>> getGames() {
        return new GameIterator(client);
    }

    @Override
    public PageIterator<Collection<Game>> getGames(int type) {
        return new GameIterator(client, type);
    }

    @Override
    public Game createGame(String name, @Nullable String icon) {
        Map<?, ?> body;
        if (icon != null) {
            body = new MapBuilder()
                    .put("name", name)
                    .put("icon", icon)
                    .build();
        } else {
            body = Collections.singletonMap("name", name);
        }
        JsonObject gsonObject = client.getNetworkClient().post(HttpAPIRoute.GAME_CREATE.toFullURL(), body);
        Game game = client.getEntityBuilder().buildGame(gsonObject);
        client.getStorage().addGame(game);
        return game;
    }

    @Override
    public void setPlaying(@Nullable Game game) {
        if (game != null) {
            Map<String, Object> body = new MapBuilder()
                    .put("data_type", 1)
                    .put("id", game.getId())
                    .build();
            client.getNetworkClient().post(HttpAPIRoute.GAME_CREATE_ACTIVITY.toFullURL(), body);
        } else {
            client.getNetworkClient().post(HttpAPIRoute.GAME_DELETE_ACTIVITY.toFullURL(),
                    Collections.singletonMap("data_type", 1));
        }
    }

    @Override
    public void setListening(@NotNull String softwareName, @NotNull String singerName, @NotNull String musicName) {
        Validate.isTrue(SUPPORTED_MUSIC_SOFTWARES.contains(softwareName), "Unsupported music software name.");
        Map<String, Object> body = new MapBuilder()
                .put("data_type", 2)
                .put("software", softwareName)
                .put("singer", singerName)
                .put("music_name", musicName)
                .build();
        client.getNetworkClient().post(HttpAPIRoute.GAME_CREATE_ACTIVITY.toFullURL(), body);
    }

    @Override
    public void stopListening() {
        client.getNetworkClient().post(HttpAPIRoute.GAME_DELETE_ACTIVITY.toFullURL(),
                Collections.singletonMap("data_type", 2));
    }

    @Override
    public TextChannelMessage getTextChannelMessage(String id) throws NoSuchElementException {
        return new TextChannelMessageImpl(client, id);
    }

    @Override
    public ChannelMessage getChannelMessage(String id) throws NoSuchElementException {
        return new ChannelMessageImpl(client, id);
    }

    @Override
    public PrivateMessage getPrivateMessage(User user, String id) throws NoSuchElementException {
        final Message cached = client.getStorage().getMessage(id);
        if (cached instanceof PrivateMessage) {
            return ((PrivateMessage) cached);
        } else {
            return new PrivateMessageImpl(client, id, user);
        }
    }

    @Override
    public FriendState getFriendState(boolean lazyInit) {
        return new FriendStateImpl(lazyInit);
    }

    // -------- Friend API --------

    protected class FriendStateImpl implements HttpAPI.FriendState {
        private final AtomicReference<Collection<User>> friends;
        private final AtomicReference<Collection<User>> blocked;
        private final AtomicReference<Collection<FriendRequest>> requests;

        public FriendStateImpl(boolean lazyInit) {
            this.friends = new AtomicReference<>();
            this.blocked = new AtomicReference<>();
            this.requests = new AtomicReference<>();
            if (!lazyInit) {
                JsonObject gsonObject = client.getNetworkClient().get(HttpAPIRoute.FRIEND_LIST.toFullURL());
                JsonNode object = toJsonNode(gsonObject);
                JsonNode request = get(object, "request");
                Collection<FriendRequest> requestCollection;
                if (request.isArray() && request.size() > 0) {
                    requestCollection = new ArrayList<>(request.size());
                    convertRawRequest(request, requestCollection);
                } else {
                    requestCollection = Collections.emptyList();
                }
                JsonNode blocked = get(object, "blocked");
                Collection<User> blockedUsers = buildUserListFromFriendStateArray(blocked);
                JsonNode friend = get(object, "friend");
                Collection<User> friends = buildUserListFromFriendStateArray(friend);

                this.friends.set(friends);
                this.blocked.set(blockedUsers);
                this.requests.set(requestCollection);
            }
        }

        protected void convertRawRequest(JsonNode request, Collection<FriendRequest> requestCollection) {
            for (JsonNode element : request) {
                JsonNode obj = element;
                int id = get(obj, "id").asInt();
                JsonNode userObj = get(element, "friend_info");
                User user = client.getStorage().getUser(get(userObj, "id").asText(), toJsonObject(userObj));
                FriendRequestImpl requestObj = new FriendRequestImpl(id, user);
                requestCollection.add(requestObj);
            }
        }

        @Override
        public Collection<User> getBlockedUsers() {
            return blocked.updateAndGet(i -> {
                if (i == null) {
                    JsonObject gsonObject = client.getNetworkClient()
                            .get(HttpAPIRoute.FRIEND_LIST.toFullURL() + "?type=block");
                    JsonNode object = toJsonNode(gsonObject);
                    JsonNode friend = get(object, "block");
                    return buildUserListFromFriendStateArray(friend);
                }
                return i;
            });
        }

        @Override
        public Collection<User> getFriends() {
            return friends.updateAndGet(i -> {
                if (i == null) {
                    JsonObject gsonObject = client.getNetworkClient()
                            .get(HttpAPIRoute.FRIEND_LIST.toFullURL() + "?type=friend");
                    JsonNode object = toJsonNode(gsonObject);
                    JsonNode friend = get(object, "friend");
                    return buildUserListFromFriendStateArray(friend);
                }
                return i;
            });
        }

        @Override
        public Collection<FriendRequest> getPendingFriendRequests() {
            return requests.updateAndGet(i -> {
                if (i == null) {
                    JsonObject gsonObject = client.getNetworkClient().get(HttpAPIRoute.FRIEND_LIST.toFullURL());
                JsonNode object = toJsonNode(gsonObject);
                    JsonNode request = get(object, "request");
                    Collection<FriendRequest> requestCollection;
                    if (request.isArray() && request.size() > 0) {
                        requestCollection = new HashSet<>(request.size());
                        convertRawRequest(request, requestCollection);
                    } else {
                        requestCollection = Collections.emptySet();
                    }
                    return Collections.unmodifiableCollection(requestCollection);
                }
                return i;
            });
        }

        protected Collection<User> buildUserListFromFriendStateArray(JsonNode array) {
            if (array.isArray() && array.size() > 0) {
                Collection<User> c = new ArrayList<>(array.size());
                for (JsonNode element : array) {
                    JsonNode userObj = get(element, "friend_info");
                    User user = client.getStorage().getUser(get(userObj, "id").asText(), toJsonObject(userObj));
                    c.add(user);
                }
                return Collections.unmodifiableCollection(c);
            }
            return Collections.emptyList();
        }

    }

    protected class FriendRequestImpl implements HttpAPI.FriendRequest {
        private final int id;
        private final User requester;

        public FriendRequestImpl(int id, User requester) {
            this.id = id;
            this.requester = requester;
        }

        @Override
        public int getId() {
            return id;
        }

        @Override
        public User getRequester() {
            return requester;
        }

        @Override
        public void handle(boolean accept) {
            HttpAPIImpl.this.handleFriendRequest(getId(), accept);
        }
    }

    @Override
    public void addFriend(User user, int method, String guildId) {
        final String userCode = user.getFullName(null);
        if (method == 2) {
            if (guildId == null) {
                throw new IllegalArgumentException("Guild ID should be NOT NULL if method is 2");
            }
        }
        Map<String, Object> body = new MapBuilder()
                .put("user_code", userCode)
                .put("from", method)
                .putIfNotNull("guild_id", guildId)
                .build();
        client.getNetworkClient().post(HttpAPIRoute.FRIEND_REQUEST.toFullURL(), body);
    }

    @Override
    public void deleteFriend(User target) {
        Map<String, Object> body = new MapBuilder()
                .put("user_id", target.getId())
                .build();
        client.getNetworkClient().post(HttpAPIRoute.FRIEND_DELETE.toFullURL(), body);
    }

    @Override
    public PageIterator<Collection<VoiceChannel>> getJoinedVoiceChannels() {
        return new JoinedVoiceChannelsIterator(client);
    }

    @Override
    public void handleFriendRequest(int id, boolean accept) {
        Map<String, Object> body = new MapBuilder()
                .put("id", id)
                .put("accept", accept ? 1 : 0)
                .build();
        HttpAPIImpl.this.client.getNetworkClient().postContent(HttpAPIRoute.FRIEND_HANDLE_REQUEST.toFullURL(), body);
    }
}
