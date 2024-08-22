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
import static snw.kookbc.util.GsonUtil.NORMAL_GSON;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

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

    public JsonObject get(String fullUrl) {
        return checkResponse(JsonParser.parseString(getRawContent(fullUrl)).getAsJsonObject()).getAsJsonObject("data");
    }

    public JsonObject post(String fullUrl, Map<?, ?> body) {
        return checkResponse(JsonParser.parseString(postContent(fullUrl, body)).getAsJsonObject())
                .getAsJsonObject("data");
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
