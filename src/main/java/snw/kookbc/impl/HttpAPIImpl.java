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
import snw.jkook.entity.channel.NonCategoryChannel;
import snw.jkook.entity.channel.TextChannel;
import snw.jkook.message.ChannelMessage;
import snw.jkook.message.PrivateMessage;
import snw.jkook.message.TextChannelMessage;
import snw.jkook.message.component.BaseComponent;
import snw.jkook.util.PageIterator;
import snw.jkook.util.Validate;
import snw.kookbc.impl.message.ChannelMessageImpl;
import snw.kookbc.impl.message.PrivateMessageImpl;
import snw.kookbc.impl.message.TextChannelMessageImpl;
import snw.kookbc.impl.network.HttpAPIRoute;
import snw.kookbc.impl.network.exceptions.BadResponseException;
import snw.kookbc.impl.pageiter.GameIterator;
import snw.kookbc.impl.pageiter.JoinedGuildIterator;
import snw.kookbc.util.MapBuilder;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import static snw.kookbc.util.GsonUtil.get;
import static snw.kookbc.util.GsonUtil.has;

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
                Collections.singletonMap("url_code", urlCode)
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
        Map<?, ?> body;
        if (icon != null) {
            body = new MapBuilder()
                    .put("name", name)
                    .put("icon", icon)
                    .build();
        } else {
            body = Collections.singletonMap("name", name);
        }
        JsonObject object = client.getNetworkClient().post(HttpAPIRoute.GAME_CREATE.toFullURL(), body);
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
            client.getNetworkClient().post(HttpAPIRoute.GAME_DELETE_ACTIVITY.toFullURL(), Collections.singletonMap("data_type", 1));
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
        client.getNetworkClient().post(HttpAPIRoute.GAME_DELETE_ACTIVITY.toFullURL(), Collections.singletonMap("data_type", 2));
    }

    @Override
    public TextChannelMessage getTextChannelMessage(String id) throws NoSuchElementException {
        final JsonObject object;
        try {
            object = client.getNetworkClient()
                    .get(HttpAPIRoute.CHANNEL_MESSAGE_INFO.toFullURL() + "?msg_id=" + id);
        } catch (BadResponseException e) {
            if (e.getCode() == 40000) {
                throw (NoSuchElementException) // force casting is required because Throwable#initCause return Throwable
                        new NoSuchElementException("No message object with provided ID " + id + " found")
                                .initCause(e);
            }
            throw e;
        }
        JsonObject rawSender = get(object, "author").getAsJsonObject();
        User sender = client.getStorage().getUser(get(rawSender, "id").getAsString(), rawSender);
        final BaseComponent component = client.getMessageBuilder().buildComponent(object);
        long timeStamp = get(object, "create_at").getAsLong();
        TextChannelMessage quote = null;
        if (has(object, "quote")) {
            final JsonObject rawQuote = get(object, "quote").getAsJsonObject();
            final String quoteId = get(rawQuote, "id").getAsString();
            quote = getTextChannelMessage(quoteId);
        }
        final TextChannel channel = (TextChannel) getChannel(get(object, "channel_id").getAsString());
        return new TextChannelMessageImpl(client, id, sender, component, timeStamp, quote, channel);
    }

    @Override
    public ChannelMessage getChannelMessage(String id) throws NoSuchElementException {
        final JsonObject object;
        try {
            object = client.getNetworkClient()
                    .get(HttpAPIRoute.CHANNEL_MESSAGE_INFO.toFullURL() + "?msg_id=" + id);
        } catch (BadResponseException e) {
            if (e.getCode() == 40000) {
                throw (NoSuchElementException) // force casting is required because Throwable#initCause return Throwable
                        new NoSuchElementException("No message object with provided ID " + id + " found")
                                .initCause(e);
            }
            throw e;
        }
        JsonObject rawSender = get(object, "author").getAsJsonObject();
        User sender = client.getStorage().getUser(get(rawSender, "id").getAsString(), rawSender);
        final BaseComponent component = client.getMessageBuilder().buildComponent(object);
        long timeStamp = get(object, "create_at").getAsLong();
        ChannelMessage quote = null;
        if (has(object, "quote")) {
            final JsonObject rawQuote = get(object, "quote").getAsJsonObject();
            final String quoteId = get(rawQuote, "id").getAsString();
            quote = getChannelMessage(quoteId);
        }
        final NonCategoryChannel channel = (NonCategoryChannel) getChannel(get(object, "channel_id").getAsString());
        return new ChannelMessageImpl(client, id, sender, component, timeStamp, quote, channel);
    }

    @Override
    public PrivateMessage getPrivateMessage(User user, String id) throws NoSuchElementException {
        final String chatCode = get(client.getNetworkClient()
                .post(HttpAPIRoute.USER_CHAT_SESSION_CREATE.toFullURL(), // KOOK won't create multiple session
                        Collections.singletonMap("target_id", user.getId())), "code").getAsString();
        final JsonObject object;
        try {
            object = client.getNetworkClient()
                    .get(HttpAPIRoute.USER_CHAT_MESSAGE_INFO.toFullURL() + "?chat_code=" + chatCode + "&msg_id=" + id);
        } catch (BadResponseException e) {
            if (e.getCode() == 40000) {
                throw (NoSuchElementException) // force casting is required because Throwable#initCause return Throwable
                        new NoSuchElementException("No message object with provided ID " + id + " found")
                                .initCause(e);
            }
            throw e;
        }
        final BaseComponent component = client.getMessageBuilder().buildComponent(object);
        long timeStamp = get(object, "create_at").getAsLong();
        PrivateMessage quote = null;
        if (has(object, "quote")) {
            JsonElement rawQuote = get(object, "quote");
            if (rawQuote.isJsonObject()) {
                if (!rawQuote.isJsonNull()) {
                    final JsonObject quoteObj = rawQuote.getAsJsonObject();
                    final String quoteId = get(quoteObj, "id").getAsString();
                    quote = getPrivateMessage(user, quoteId);
                }
            }else {
                if (rawQuote.getAsString().trim().isEmpty()) {
                    return new PrivateMessageImpl(client, id, user, component, timeStamp, quote);
                }
            }
        }
        return new PrivateMessageImpl(client, id, user, component, timeStamp, quote);
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
                JsonObject object = client.getNetworkClient().get(HttpAPIRoute.FRIEND_LIST.toFullURL());
                JsonArray request = get(object, "request").getAsJsonArray();
                Collection<FriendRequest> requestCollection;
                if (!request.isEmpty()) {
                    requestCollection = new ArrayList<>(request.size());
                    convertRawRequest(request, requestCollection);
                } else {
                    requestCollection = Collections.emptyList();
                }
                JsonArray blocked = get(object, "blocked").getAsJsonArray();
                Collection<User> blockedUsers = buildUserListFromFriendStateArray(blocked);
                JsonArray friend = get(object, "friend").getAsJsonArray();
                Collection<User> friends = buildUserListFromFriendStateArray(friend);

                this.friends.set(friends);
                this.blocked.set(blockedUsers);
                this.requests.set(requestCollection);
            }
        }

        protected void convertRawRequest(JsonArray request, Collection<FriendRequest> requestCollection) {
            for (JsonElement element : request) {
                JsonObject obj = element.getAsJsonObject();
                int id = get(obj, "id").getAsInt();
                JsonObject userObj = get(element.getAsJsonObject(), "friend_info").getAsJsonObject();
                User user = client.getStorage().getUser(get(userObj, "id").getAsString(), userObj);
                FriendRequestImpl requestObj = new FriendRequestImpl(id, user);
                requestCollection.add(requestObj);
            }
        }

        @Override
        public Collection<User> getBlockedUsers() {
            return blocked.updateAndGet(i -> {
                if (i == null) {
                    JsonObject object = client.getNetworkClient()
                            .get(HttpAPIRoute.FRIEND_LIST.toFullURL() + "?type=block");
                    JsonArray friend = get(object, "block").getAsJsonArray();
                    return buildUserListFromFriendStateArray(friend);
                }
                return i;
            });
        }

        @Override
        public Collection<User> getFriends() {
            return friends.updateAndGet(i -> {
                if (i == null) {
                    JsonObject object = client.getNetworkClient()
                            .get(HttpAPIRoute.FRIEND_LIST.toFullURL() + "?type=friend");
                    JsonArray friend = get(object, "friend").getAsJsonArray();
                    return buildUserListFromFriendStateArray(friend);
                }
                return i;
            });
        }

        @Override
        public Collection<FriendRequest> getPendingFriendRequests() {
            return requests.updateAndGet(i -> {
                if (i == null) {
                    JsonObject object = client.getNetworkClient().get(HttpAPIRoute.FRIEND_LIST.toFullURL());
                    JsonArray request = get(object, "request").getAsJsonArray();
                    Collection<FriendRequest> requestCollection;
                    if (!request.isEmpty()) {
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

        protected Collection<User> buildUserListFromFriendStateArray(JsonArray array) {
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
    public void handleFriendRequest(int id, boolean accept) {
        Map<String, Object> body = new MapBuilder()
                .put("id", id)
                .put("accept", accept ? 1 : 0)
                .build();
        HttpAPIImpl.this.client.getNetworkClient().post(HttpAPIRoute.FRIEND_HANDLE_REQUEST.toFullURL(), body);
    }
}
