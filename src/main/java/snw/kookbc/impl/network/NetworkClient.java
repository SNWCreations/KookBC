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

import static snw.kookbc.CLIOptions.NO_BUCKET;
import static snw.kookbc.util.JacksonUtil.parse;
import static snw.kookbc.util.JacksonUtil.toJson;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.List;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.fasterxml.jackson.databind.JsonNode;
import snw.kookbc.util.JacksonUtil;
import snw.kookbc.util.VirtualThreadUtil;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import snw.jkook.exceptions.BadResponseException;
import snw.kookbc.impl.KBCClient;

// provide the basic HTTP/WebSocket call feature. Authenticated with Bot Token.
public class NetworkClient {
    private final KBCClient kbcClient;
    private final String tokenWithPrefix;

    private final OkHttpClient client;

    public NetworkClient(KBCClient kbcClient, String token) {
        this.kbcClient = kbcClient;
        tokenWithPrefix = "Bot " + token;

        final OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .writeTimeout(Duration.ofMinutes(1))
                .readTimeout(Duration.ofMinutes(1));
        if (kbcClient.getConfig().getBoolean("ignore-ssl")) {
            kbcClient.getCore().getLogger().warn("Ignoring SSL verification for networking!!!");
            builder.sslSocketFactory(IgnoreSSLHelper.getSSLSocketFactory(), IgnoreSSLHelper.TRUST_MANAGER)
                    .hostnameVerifier(IgnoreSSLHelper.getHostnameVerifier());
        }
        client = builder.build();
    }

    public OkHttpClient getOkHttpClient() {
        return client;
    }

    // Jackson API - 高性能JSON处理
    public JsonNode get(String fullUrl) {
        return checkResponseJackson(parse(getRawContent(fullUrl))).get("data");
    }

    public JsonNode post(String fullUrl, Map<?, ?> body) {
        return checkResponseJackson(parse(postContent(fullUrl, body)))
                .get("data");
    }

    public String getRawContent(String fullUrl) {
        logRequest("GET", fullUrl, null);
        Request request = new Request.Builder()
                .get()
                .url(fullUrl)
                .addHeader("Authorization", tokenWithPrefix)
                .build();
        return call(request);
    }

    public String postContent(String fullUrl, Map<?, ?> body) {
        return postContent(fullUrl, toJson(body), "application/json");
    }

    public String postContent(String fullUrl, String body, String mediaType) {
        logRequest("POST", fullUrl, body);
        Request request = new Request.Builder()
                .post(RequestBody.create(body, MediaType.parse(mediaType)))
                .url(fullUrl)
                .addHeader("Authorization", tokenWithPrefix)
                .build();
        return call(request);
    }

    public String call(Request request) {
        Bucket bucket;
        if (!NO_BUCKET) {
            bucket = getBucket(request);
            bucket.check();
        } else {
            bucket = null;
        }
        try (Response response = client.newCall(request).execute()) {
            // region Bucket process
            int remaining = Integer.parseInt(Objects.requireNonNull(response.header("X-Rate-Limit-Remaining")));
            int reset = Integer.parseInt(Objects.requireNonNull(response.header("X-Rate-Limit-Reset")));
            if (bucket != null) {
                bucket.update(remaining, reset);
            }
            // endregion

            final String body = Objects.requireNonNull(response.body()).string();
            if (!response.isSuccessful()) {
                kbcClient.getCore().getLogger().debug("Request failed. Full response object: {}", response);
                throw new BadResponseException(response.code(), body);
            }
            return body;
        } catch (IOException e) {
            throw new RuntimeException("Unexpected IOException when we attempting to call request.", e);
        }
    }

    @NotNull
    public WebSocket newWebSocket(@NotNull Request request, @NotNull WebSocketListener listener) {
        return client.newWebSocket(request, listener);
    }

    public String getTokenWithPrefix() {
        return tokenWithPrefix;
    }

    protected Bucket getBucket(Request request) {
        String path = request.url().url().getPath().substring(4);
        return Bucket.get(kbcClient, HttpAPIRoute.value(path));
    }

    protected void logRequest(String method, String fullUrl, @Nullable String postBodyJson) {
        kbcClient.getCore().getLogger().debug("Sending HTTP API Request: Method {}, URL: {}, Body (POST only): {}",
                method, fullUrl, postBodyJson);
    }

    // ===== 虚拟线程异步 API =====

    /**
     * 异步 GET 请求 - 使用虚拟线程
     *
     * @param fullUrl 完整 URL
     * @return 异步结果
     */
    public CompletableFuture<JsonNode> getAsync(String fullUrl) {
        return CompletableFuture.supplyAsync(() -> get(fullUrl), VirtualThreadUtil.getHttpExecutor());
    }

    /**
     * 异步 POST 请求 - 使用虚拟线程
     *
     * @param fullUrl 完整 URL
     * @param body 请求体
     * @return 异步结果
     */
    public CompletableFuture<JsonNode> postAsync(String fullUrl, Map<?, ?> body) {
        return CompletableFuture.supplyAsync(() -> post(fullUrl, body), VirtualThreadUtil.getHttpExecutor());
    }

    /**
     * 异步获取原始内容 - 使用虚拟线程
     *
     * @param fullUrl 完整 URL
     * @return 异步结果
     */
    public CompletableFuture<String> getRawContentAsync(String fullUrl) {
        return CompletableFuture.supplyAsync(() -> getRawContent(fullUrl), VirtualThreadUtil.getHttpExecutor());
    }

    /**
     * 异步 POST 原始内容 - 使用虚拟线程
     *
     * @param fullUrl 完整 URL
     * @param body 请求体
     * @param mediaType 媒体类型
     * @return 异步结果
     */
    public CompletableFuture<String> postContentAsync(String fullUrl, String body, String mediaType) {
        return CompletableFuture.supplyAsync(() -> postContent(fullUrl, body, mediaType), VirtualThreadUtil.getHttpExecutor());
    }

    /**
     * 批量异步 GET 请求 - 使用虚拟线程
     *
     * <p>所有请求并行执行，显著提升性能
     *
     * @param urls URL 列表
     * @return 批量异步结果
     */
    public CompletableFuture<List<JsonNode>> batchGetAsync(List<String> urls) {
        List<CompletableFuture<JsonNode>> futures = urls.stream()
            .map(this::getAsync)
            .collect(Collectors.toList());

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .thenApply(v -> futures.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toList()));
    }

    /**
     * 批量异步 POST 请求 - 使用虚拟线程
     *
     * @param requests 请求列表 (URL 和 Body 的映射)
     * @return 批量异步结果
     */
    public CompletableFuture<List<JsonNode>> batchPostAsync(Map<String, Map<?, ?>> requests) {
        List<CompletableFuture<JsonNode>> futures = requests.entrySet().stream()
            .map(entry -> postAsync(entry.getKey(), entry.getValue()))
            .collect(Collectors.toList());

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .thenApply(v -> futures.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toList()));
    }

    /**
     * 异步调用请求 - 使用虚拟线程
     *
     * <p>底层方法，支持自定义 Request 对象
     *
     * @param request OkHttp Request 对象
     * @return 异步结果
     */
    public CompletableFuture<String> callAsync(Request request) {
        return CompletableFuture.supplyAsync(() -> call(request), VirtualThreadUtil.getHttpExecutor());
    }

    // ===== 原有同步方法（保持向后兼容）=====

    // Jackson响应检查
    public JsonNode checkResponseJackson(JsonNode response) {
        int code = response.get("code").asInt();

        if (code != 0) {
            String message = response.get("message").asText();
            throw new BadResponseException(code, message);
        }
        return response;
    }

    // Gson响应检查 - 向后兼容性支持
    public com.google.gson.JsonObject checkResponse(com.google.gson.JsonObject response) {
        int code = response.get("code").getAsInt();

        if (code != 0) {
            String message = response.get("message").getAsString();
            throw new BadResponseException(code, message);
        }
        return response;
    }

}
