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

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.fasterxml.jackson.databind.JsonNode;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import java.net.URL;
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
import snw.kookbc.util.VirtualThreadUtil;
import snw.kookbc.interfaces.AsyncHttpAPI;

public class HttpAPIImpl implements HttpAPI, AsyncHttpAPI {
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

    // ===== 异步 API 增强 - 智能请求合并器 =====

    /**
     * 智能请求合并器 - 避免重复请求，提升性能
     */
    private final RequestCoalescer requestCoalescer = new RequestCoalescer();

    /**
     * 异步执行器 - 专用于 HTTP API 操作
     */
    private final ExecutorService httpExecutor = VirtualThreadUtil.getHttpExecutor();


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
        // 内部使用异步实现，但对外保持同步接口
        return uploadFileAsync(file).join();
    }

    @Override
    public String uploadFile(String filename, byte[] content) {
        // 内部使用异步实现，但对外保持同步接口
        return uploadFileAsync(filename, content).join();
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
        // 内部使用异步实现，但对外保持同步接口
        removeInviteAsync(urlCode).join();
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
        JsonNode response = client.getNetworkClient().post(HttpAPIRoute.GAME_CREATE.toFullURL(), body);
        Game game = client.getEntityBuilder().buildGame(response);
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
                JsonNode object = client.getNetworkClient().get(HttpAPIRoute.FRIEND_LIST.toFullURL());
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
                User user = client.getStorage().getUser(get(userObj, "id").asText(), userObj);
                FriendRequestImpl requestObj = new FriendRequestImpl(id, user);
                requestCollection.add(requestObj);
            }
        }

        @Override
        public Collection<User> getBlockedUsers() {
            return blocked.updateAndGet(i -> {
                if (i == null) {
                    JsonNode object = client.getNetworkClient().get(HttpAPIRoute.FRIEND_LIST.toFullURL() + "?type=block");
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
                    JsonNode object = client.getNetworkClient().get(HttpAPIRoute.FRIEND_LIST.toFullURL() + "?type=friend");
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
                    JsonNode object = client.getNetworkClient().get(HttpAPIRoute.FRIEND_LIST.toFullURL());
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
                    User user = client.getStorage().getUser(get(userObj, "id").asText(), userObj);
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

    // ===== 异步 API 实现 - 内部使用，提供给插件的异步版本 =====

    /**
     * 异步文件上传 - File 版本
     *
     * @param file 要上传的文件
     * @return 异步上传结果，包含文件 URL
     */
    public CompletableFuture<String> uploadFileAsync(File file) {
        return requestCoalescer.coalesce("upload_file_" + file.getName() + "_" + file.length(), () ->
            CompletableFuture.supplyAsync(() -> {
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
            }, httpExecutor)
        );
    }

    /**
     * 异步文件上传 - 字节数组版本
     *
     * @param filename 文件名
     * @param content 文件内容字节数组
     * @return 异步上传结果，包含文件 URL
     */
    public CompletableFuture<String> uploadFileAsync(String filename, byte[] content) {
        return requestCoalescer.coalesce("upload_file_" + filename + "_" + content.length, () ->
            CompletableFuture.supplyAsync(() -> {
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
            }, httpExecutor)
        );
    }

    /**
     * 异步文件上传 - URL 版本
     *
     * @param fileName 文件名
     * @param url 文件 URL
     * @return 异步上传结果，包含文件 URL
     */
    public CompletableFuture<String> uploadFileAsync(String fileName, String url) {
        return requestCoalescer.coalesce("upload_file_url_" + url, () ->
            CompletableFuture.supplyAsync(() -> {
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
                    byte[] bytes = body.bytes();
                    return uploadFileAsync(fileName, bytes).join();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }, httpExecutor)
        );
    }

    /**
     * 异步删除邀请链接
     *
     * @param urlCode 邀请链接代码
     * @return 异步删除结果
     */
    public CompletableFuture<Void> removeInviteAsync(String urlCode) {
        return requestCoalescer.coalesce("remove_invite_" + urlCode, () ->
            CompletableFuture.runAsync(() -> {
                client.getNetworkClient().post(HttpAPIRoute.INVITE_DELETE.toFullURL(),
                        Collections.singletonMap("url_code", urlCode));
            }, httpExecutor)
        );
    }

    /**
     * 异步获取用户信息
     *
     * @param id 用户 ID
     * @return 异步用户信息
     */
    public CompletableFuture<User> getUserAsync(String id) {
        return requestCoalescer.coalesce("get_user_" + id, () ->
            CompletableFuture.supplyAsync(() -> client.getStorage().getUser(id), httpExecutor)
        );
    }

    /**
     * 异步获取服务器信息
     *
     * @param id 服务器 ID
     * @return 异步服务器信息
     */
    public CompletableFuture<Guild> getGuildAsync(String id) {
        return requestCoalescer.coalesce("get_guild_" + id, () ->
            CompletableFuture.supplyAsync(() -> client.getStorage().getGuild(id), httpExecutor)
        );
    }

    /**
     * 异步获取频道信息
     *
     * @param id 频道 ID
     * @return 异步频道信息
     */
    public CompletableFuture<Channel> getChannelAsync(String id) {
        return requestCoalescer.coalesce("get_channel_" + id, () ->
            CompletableFuture.supplyAsync(() -> client.getStorage().getChannel(id), httpExecutor)
        );
    }

    /**
     * 批量异步获取用户信息
     *
     * @param userIds 用户 ID 列表
     * @return 异步用户信息列表
     */
    public CompletableFuture<List<User>> getBatchUsersAsync(List<String> userIds) {
        return CompletableFuture.supplyAsync(() -> {
            List<CompletableFuture<User>> futures = userIds.stream()
                    .map(this::getUserAsync)
                    .collect(Collectors.toList());

            return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .thenApply(v -> futures.stream()
                            .map(CompletableFuture::join)
                            .collect(Collectors.toList()))
                    .join();
        }, httpExecutor);
    }

    /**
     * 批量异步获取服务器信息
     *
     * @param guildIds 服务器 ID 列表
     * @return 异步服务器信息列表
     */
    public CompletableFuture<List<Guild>> getBatchGuildsAsync(List<String> guildIds) {
        return CompletableFuture.supplyAsync(() -> {
            List<CompletableFuture<Guild>> futures = guildIds.stream()
                    .map(this::getGuildAsync)
                    .collect(Collectors.toList());

            return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .thenApply(v -> futures.stream()
                            .map(CompletableFuture::join)
                            .collect(Collectors.toList()))
                    .join();
        }, httpExecutor);
    }

    /**
     * 批量异步获取频道信息
     *
     * @param channelIds 频道 ID 列表
     * @return 异步频道信息列表
     */
    public CompletableFuture<List<Channel>> getBatchChannelsAsync(List<String> channelIds) {
        return CompletableFuture.supplyAsync(() -> {
            List<CompletableFuture<Channel>> futures = channelIds.stream()
                    .map(this::getChannelAsync)
                    .collect(Collectors.toList());

            return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .thenApply(v -> futures.stream()
                            .map(CompletableFuture::join)
                            .collect(Collectors.toList()))
                    .join();
        }, httpExecutor);
    }

    // ===== 智能请求合并器实现 =====

    /**
     * 智能请求合并器 - 避免重复的并发请求，提升性能
     */
    private static class RequestCoalescer {
        private final Map<String, CompletableFuture<?>> ongoingRequests = new ConcurrentHashMap<>();

        /**
         * 合并相同的请求，避免重复执行
         *
         * @param key 请求的唯一标识
         * @param supplier 请求的执行逻辑
         * @param <T> 请求结果类型
         * @return 合并后的请求结果
         */
        @SuppressWarnings("unchecked")
        public <T> CompletableFuture<T> coalesce(String key, Supplier<CompletableFuture<T>> supplier) {
            return (CompletableFuture<T>) ongoingRequests.computeIfAbsent(key, k -> {
                CompletableFuture<T> future = supplier.get();
                // 请求完成后清理缓存
                future.whenComplete((result, exception) -> ongoingRequests.remove(k));
                return future;
            });
        }

        /**
         * 获取当前正在进行的请求数量
         *
         * @return 正在进行的请求数量
         */
        public int getOngoingRequestCount() {
            return ongoingRequests.size();
        }

        /**
         * 清理所有请求缓存（用于测试或特殊情况）
         */
        public void clearCache() {
            ongoingRequests.clear();
        }
    }

    // ===== AsyncHttpAPI 接口实现 =====

    @Override
    public int getOngoingRequestCount() {
        return requestCoalescer.getOngoingRequestCount();
    }

    @Override
    public void clearRequestCache() {
        requestCoalescer.clearCache();
    }
}
