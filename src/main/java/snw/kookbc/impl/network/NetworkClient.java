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

package snw.kookbc.impl.network;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import okhttp3.*;
import snw.jkook.util.Validate;
import snw.kookbc.impl.network.exceptions.BadResponseException;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;

// provide the basic HTTP/WebSocket call feature. Authenticated with Bot Token.
public class NetworkClient {
    private final String tokenWithPrefix;
    private final OkHttpClient client = new OkHttpClient();

    public NetworkClient(String token) {
        tokenWithPrefix = "Bot " + token;
    }

    public JsonObject get(HttpAPIRoute route, Map<String, Object> body) {
        return JsonParser.parseString(getRawContent(route, body)).getAsJsonObject().getAsJsonObject("data");
    }

    public JsonObject post(HttpAPIRoute route, Map<?, ?> body) {
        return JsonParser.parseString(postContent(route, body)).getAsJsonObject().getAsJsonObject("data");
    }

    public String getRawContent(HttpAPIRoute route, Map<String, Object> body) {
        Request request = new Request.Builder()
                .get()
                .url(route.toFullURL() + buildQueryParam(body))
                .addHeader("Authorization", tokenWithPrefix)
                .build();
        return call(route, request);
    }

    public String postContent(HttpAPIRoute route, Map<?, ?> body) {
        return postContent(route, new Gson().toJson(body), "application/json");
    }

    public String postContent(HttpAPIRoute route, String body, String mediaType) {
        Request request = new Request.Builder()
                .post(
                        RequestBody.create(body, MediaType.parse(mediaType))
                )
                .url(route.toFullURL())
                .addHeader("Authorization", tokenWithPrefix)
                .build();
        return call(route, request);
    }

    public String call(HttpAPIRoute route, Request request) {
        // TODO add bucket check at here
        try (Response res = client.newCall(request).execute()) {
            if (res.body() != null) {
                String resStr = res.body().string();
                JsonObject object = JsonParser.parseString(resStr).getAsJsonObject();
                int status = object.get("code").getAsInt();
                if (status != 0) {
                    throw new BadResponseException(status, object.get("message").getAsString());
                }
                return resStr;
            }
            return "";
        } catch (IOException e) {
            throw new RuntimeException("Unexpected IOException when we attempting to call request.", e);
        }
    }

    @NotNull
    public WebSocket newWebSocket(@NotNull Request request, @NotNull WebSocketListener listener) {
        return client.newWebSocket(request, listener);
    }

    private String buildQueryParam(Map<String, Object> param) {
        if (param == null || param.isEmpty()) {
            return "";
        }
        Validate.isTrue(param.values().contains(null), "Detected null in query parameyer.");
        StringBuilder builder = new StringBuilder("?");
        for (Entry<String, Object> entry : param.entrySet()) {
            builder.append(entry.getKey() + "=" + String.valueOf(entry.getValue()));
        }
        return builder.toString();
    }
}