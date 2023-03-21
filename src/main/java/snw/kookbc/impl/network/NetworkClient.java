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

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import snw.kookbc.impl.KBCClient;
import snw.kookbc.impl.network.exceptions.BadResponseException;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;

import static snw.kookbc.util.GsonUtil.NORMAL_GSON;

// provide the basic HTTP/WebSocket call feature. Authenticated with Bot Token.
public class NetworkClient {
    private final KBCClient kbcClient;
    private final String tokenWithPrefix;
    private final OkHttpClient client = new OkHttpClient.Builder()
            .writeTimeout(Duration.ofMinutes(1))
            .readTimeout(Duration.ofMinutes(1))
            .build();

    public NetworkClient(KBCClient kbcClient, String token) {
        this.kbcClient = kbcClient;
        tokenWithPrefix = "Bot " + token;
    }

    public JsonObject get(String fullUrl) {
        return checkResponse(JsonParser.parseString(getRawContent(fullUrl)).getAsJsonObject()).getAsJsonObject("data");
    }

    public JsonObject post(String fullUrl, Map<?, ?> body) {
        return checkResponse(JsonParser.parseString(postContent(fullUrl, body)).getAsJsonObject()).getAsJsonObject("data");
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
        return postContent(fullUrl, NORMAL_GSON.toJson(body), "application/json");
    }

    public String postContent(String fullUrl, String body, String mediaType) {
        logRequest("POST", fullUrl, body);
        Request request = new Request.Builder()
                .post(
                        RequestBody.create(body, MediaType.parse(mediaType))
                )
                .url(fullUrl)
                .addHeader("Authorization", tokenWithPrefix)
                .build();
        return call(request);
    }

    public String call(Request request) {
        Bucket bucket = getBucket(request);
        bucket.check();
        try (Response res = client.newCall(request).execute()) {

            // region Bucket post process
            if (bucket.availableTimes.get() == -1) {
                bucket.availableTimes.set(Integer.parseInt(res.header("X-Rate-Limit-Remaining")));
            } else {
                int limit = Integer.parseInt(res.header("X-Rate-Limit-Limit"));
                int reset = Integer.parseInt(res.header("X-Rate-Limit-Reset"));
                if (reset == 0) {
                    bucket.availableTimes.set(limit);
                } else {
                    bucket.scheduleUpdateAvailableTimes(limit, reset);
                }

            }

            if (!res.isSuccessful()) {
                throw new BadResponseException(res.code(), "UNKNOWN");
            }
            return res.body() != null ? res.body().string() : "";
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

    // Return original object if check OK
    public JsonObject checkResponse(JsonObject response) {
        int code = response.get("code").getAsInt();

        if (code != 0) {
            String message = response.get("message").getAsString();
            throw new BadResponseException(code, message);
        }
        return response;
    }
}