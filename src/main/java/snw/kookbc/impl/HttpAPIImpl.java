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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import snw.jkook.HttpAPI;
import snw.jkook.entity.Game;
import snw.jkook.entity.Guild;
import snw.jkook.entity.User;
import snw.jkook.entity.channel.Category;
import snw.jkook.entity.channel.Channel;
import snw.jkook.util.PageIterator;
import snw.jkook.util.Validate;
import snw.kookbc.impl.network.HttpAPIRoute;
import snw.kookbc.impl.pageiter.GameIterator;
import snw.kookbc.impl.pageiter.JoinedGuildIterator;
import snw.kookbc.util.MapBuilder;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

import static snw.kookbc.util.GsonUtil.get;

public class HttpAPIImpl implements HttpAPI {
    private static final MediaType OCTET_STREAM;
    private static final Collection<String> SUPPORTED_MUSIC_SOFTWARES;
//    private static final long UPLOAD_FILE_LENGTH_LIMIT = 25; // in MB

    static {
        OCTET_STREAM = MediaType.parse("application/octet-stream");
        SUPPORTED_MUSIC_SOFTWARES = Collections.unmodifiableCollection(
            Arrays.asList(
                "cloudmusic", "qqmusic", "kugou"
            )
        );
    }

    private final KBCClient client;
    private final OkHttpClient okHttpClient;

    public HttpAPIImpl(KBCClient client) {
        this.client = client;
        this.okHttpClient = new OkHttpClient();
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
    public Category getCategory(String s) {
        try {
            return (Category) client.getStorage().getChannel(s);
        } catch (ClassCastException e) {
            throw new RuntimeException("The object that you requests is not a Category.", e);
        }
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
        return JsonParser.parseString(client.getNetworkClient().call(request)).getAsJsonObject().getAsJsonObject("data").get("url").getAsString();
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
        return JsonParser.parseString(client.getNetworkClient().call(request)).getAsJsonObject().getAsJsonObject("data").get("url").getAsString();
    }

    @Override
    public String uploadFile(String fileName, String url) throws IllegalArgumentException {
        try {
            new URL(url);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Cannot upload file: Malformed URL", e);
        }
        try (Response response = this.okHttpClient.newCall(
                new Request.Builder()
                        .get()
                        .url(url)
                        .build()
        ).execute()) {
            ResponseBody body = Objects.requireNonNull(response.body(), "Cannot upload file at " + url + ": Response body should not be null");
            long contentLength = body.contentLength();
//            if (contentLength > UPLOAD_FILE_LENGTH_LIMIT * 1024) {
//                throw new IllegalArgumentException("Cannot upload file at " + url + ": Too big file");
//            }
            byte[] bytes = body.bytes();
            return uploadFile(fileName, bytes);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void removeInvite(String urlCode) {
        client.getNetworkClient().post(HttpAPIRoute.INVITE_DELETE.toFullURL(),
                new MapBuilder()
                        .put("url_code", urlCode)
                        .build()
        );
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
        MapBuilder builder = new MapBuilder()
                .put("name", name);
        if (icon != null) {
            builder.put("icon", icon);
        }
        JsonObject object = client.getNetworkClient().post(HttpAPIRoute.GAME_CREATE.toFullURL(), builder.build());
        Game game = client.getEntityBuilder().buildGame(object);
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
            client.getNetworkClient().post(HttpAPIRoute.GAME_DELETE_ACTIVITY.toFullURL(), new MapBuilder().put("data_type", 1).build());
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
        client.getNetworkClient().post(HttpAPIRoute.GAME_DELETE_ACTIVITY.toFullURL(), new MapBuilder().put("data_type", 2).build());
    }

    // -------- Friend API --------

    protected static class FriendStateImpl implements HttpAPI.FriendState {
        private final Collection<User> friends;
        private final Collection<User> blocked;
        private final Collection<FriendRequest> requests;

        public FriendStateImpl(Collection<User> friends, Collection<User> blocked, Collection<FriendRequest> requests) {
            this.friends = Collections.unmodifiableCollection(friends);
            this.blocked = Collections.unmodifiableCollection(blocked);
            this.requests = Collections.unmodifiableCollection(requests);
        }

        @Override
        public Collection<User> getBlockedUsers() {
            return blocked;
        }

        @Override
        public Collection<User> getFriends() {
            return friends;
        }

        @Override
        public Collection<FriendRequest> getPendingFriendRequests() {
            return requests;
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
        }}

    @Override
    public void addFriend(String userCode, int method, String guildId) {
        if (method == 2) {
            if (guildId == null) {
                throw new IllegalArgumentException("Guild ID should be NOT NULL if method is 2");
            }
        }
        MapBuilder builder = new MapBuilder()
                .put("user_code", userCode)
                .put("from", method);
        if (guildId != null) {
            builder.put("guild_id", guildId);
        }
        Map<String, Object> body = builder.build();
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
    public Collection<User> getBlockedUsers() {
        JsonObject object = client.getNetworkClient().get(HttpAPIRoute.FRIEND_LIST.toFullURL() + "?type=block");
        JsonArray blocked = get(object, "blocked").getAsJsonArray();
        return buildUserListFromFriendStateArray(blocked);
    }

    @Override
    public FriendState getFriendState() {
        JsonObject object = client.getNetworkClient().get(HttpAPIRoute.FRIEND_LIST.toFullURL());
        JsonArray request = get(object, "request").getAsJsonArray();
        Collection<FriendRequest> requestCollection;
        if (!request.isEmpty()) {
            requestCollection = new ArrayList<>(request.size());
            for (JsonElement element : request) {
                JsonObject obj = element.getAsJsonObject();
                int id = get(obj, "id").getAsInt();
                JsonObject userObj = get(element.getAsJsonObject(), "friend_info").getAsJsonObject();
                User user = client.getStorage().getUser(get(userObj, "id").getAsString(), userObj);
                FriendRequestImpl requestObj = new FriendRequestImpl(id, user);
                requestCollection.add(requestObj);
            }
        } else {
            requestCollection = Collections.emptyList();
        }
        JsonArray blocked = get(object, "blocked").getAsJsonArray();
        Collection<User> blockedUsers = buildUserListFromFriendStateArray(blocked);
        JsonArray friend = get(object, "friend").getAsJsonArray();
        Collection<User> friends = buildUserListFromFriendStateArray(friend);
        return new FriendStateImpl(
                friends, blockedUsers, requestCollection
        );
    }

    @Override
    public Collection<User> getFriends() {
        JsonObject object = client.getNetworkClient().get(HttpAPIRoute.FRIEND_LIST.toFullURL() + "?type=friend");
        JsonArray friend = get(object, "friend").getAsJsonArray();
        return buildUserListFromFriendStateArray(friend);
    }

    @Override
    public void handleFriendRequest(int id, boolean accept) {
        Map<String, Object> body = new MapBuilder()
                .put("id", id)
                .put("accept", accept ? 1 : 0)
                .build();
        HttpAPIImpl.this.client.getNetworkClient().post(HttpAPIRoute.FRIEND_HANDLE_REQUEST.toFullURL(), body);
    }

    protected final Collection<User> buildUserListFromFriendStateArray(JsonArray array) {
        if (!array.isEmpty()) {
            Collection<User> c = new ArrayList<>(array.size());
            for (JsonElement element : array) {
                JsonObject userObj = get(element.getAsJsonObject(), "friend_info").getAsJsonObject();
                User user = client.getStorage().getUser(get(userObj, "id").getAsString(), userObj);
                c.add(user);
            }
            return Collections.unmodifiableCollection(c);
        }
        return Collections.emptyList();
    }
}
