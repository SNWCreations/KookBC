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

package snw.kookbc.impl.network.webhook;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.freeutils.httpserver.HTTPServer;
import snw.kookbc.impl.KBCClient;
import snw.kookbc.impl.network.Frame;
import snw.kookbc.impl.network.Listener;
import snw.kookbc.impl.network.ListenerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import static snw.kookbc.util.GsonUtil.*;
import static snw.kookbc.util.Util.decompressDeflate;
import static snw.kookbc.util.Util.inputStreamToByteArray;

public class SimpleHttpHandler implements HTTPServer.ContextHandler {
    protected final KBCClient client;
    protected final Listener listener;

    public SimpleHttpHandler(KBCClient client) {
        this.client = client;
        listener = ListenerFactory.getListener(client);
    }

    @Override
    public int serve(HTTPServer.Request request, HTTPServer.Response response) throws IOException {
        try {
            return serve0(request, response);
        } catch (Exception e) {
            client.getCore().getLogger().error("Unable to process request", e);
            throw new IOException(e);
        }
    }

    private int serve0(HTTPServer.Request request, HTTPServer.Response response) throws Exception {
        client.getCore().getLogger().debug("Got request!");
        String res;
        byte[] bytes = inputStreamToByteArray(request.getBody());
        if (bytes.length == 0) {
            return 403;
        }
        if (!"0".equals(request.getParams().get("compress"))) {
            res = new String(decompressDeflate(bytes));
        } else {
            res = new String(bytes);
        }
        JsonObject object = JsonParser.parseString(EncryptUtils.decrypt(client, res)).getAsJsonObject();
        Frame frame = new Frame(
                get(object, "s").getAsInt(),
                has(object, "sn") ? get(object, "sn").getAsInt() : -1,
                object.getAsJsonObject("d")
        );
        if (!Objects.equals(
                frame.getData().get("verify_token").getAsString(),
                client.getConfig().getString("webhook-verify-token"))
        ) {
            return 403; // Illegal access
        } else {
            // challenge part
            JsonElement channelType = frame.getData().get("channel_type");
            if (channelType != null && Objects.equals(channelType.getAsString(), "WEBHOOK_CHALLENGE")) {
                String finalChallengeResponse = frame.getData().get("challenge").getAsString();
                JsonObject obj = new JsonObject();
                obj.addProperty("challenge", finalChallengeResponse);
                String challengeJson = NORMAL_GSON.toJson(obj);

                // the following part is copied from HTTPServer.Response.send method.
                // I just edited the value of the "contentType" parameter.
                byte[] content = challengeJson.getBytes(StandardCharsets.UTF_8);
                response.sendHeaders(
                        200,
                        content.length,
                        -1L,
                        "W/\"" + Integer.toHexString(challengeJson.hashCode()) + "\"",
                        "application/json; charset=utf-8",
                        null
                );
                OutputStream out = response.getBody();
                if (out != null) {
                    out.write(content);
                }
            }
            // end challenge part
            else {
                listener.executeEvent(frame);
                response.send(200, "");
            }
        }
        return 0;
    }

}
