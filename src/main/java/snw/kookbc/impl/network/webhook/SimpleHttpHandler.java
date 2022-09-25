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

package snw.kookbc.impl.network.webhook;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import snw.kookbc.impl.KBCClient;
import snw.kookbc.impl.network.Frame;
import snw.kookbc.impl.network.Listener;
import snw.kookbc.impl.network.ListenerImpl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

import static snw.kookbc.impl.network.MessageProcessor.decompressDeflate;

public class SimpleHttpHandler implements HttpHandler {
    protected final KBCClient client;
    protected final Listener listener;

    public SimpleHttpHandler(KBCClient client) {
        this.client = client;
        listener = new ListenerImpl(client);
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            handle0(exchange);
        } catch (Exception e) {
            client.getCore().getLogger().error("Something unexpected occurred while we attempting to process the request from remote.", e);
            exchange.sendResponseHeaders(500, -1);
        }
        exchange.close();
    }

    public void handle0(HttpExchange exchange) throws Exception {
        client.getCore().getLogger().debug("Got request!");
        if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
            client.getCore().getLogger().debug("Got the request that not using POST. Rejected.");
            exchange.sendResponseHeaders(405, -1);
        } else {
            client.getCore().getLogger().debug("Got POST request");
            String res;
            byte[] bytes = inputStreamToByteArray(exchange.getRequestBody());
            if (client.getConfig().getBoolean("compress")) {
                res = new String(decompressDeflate(bytes));
            } else {
                res = new String(bytes);
            }
            client.getCore().getLogger().debug("Got remote request: {}", res);
            JsonObject object = JsonParser.parseString(
                    EncryptUtils.decrypt(client, res)
            ).getAsJsonObject();
            Frame frame = new Frame(object.get("s").getAsInt(), object.get("sn") != null ? object.get("sn").getAsInt() : -1, object.getAsJsonObject("d"));
            if (!Objects.equals(frame.getData().get("verify_token").getAsString(), client.getConfig().getString("webhook-verify-token"))) {
                exchange.sendResponseHeaders(403, -1); // illegal access!
            } else {
                // challenge part
                JsonElement channelType = frame.getData().get("channel_type");
                if (channelType != null && Objects.equals(channelType.getAsString(), "WEBHOOK_CHALLENGE")) {
                    String finalChallengeResponse = frame.getData().get("challenge").getAsString();
                    JsonObject obj = new JsonObject();
                    obj.addProperty("challenge", finalChallengeResponse);
                    String s = new Gson().toJson(obj);
                    exchange.sendResponseHeaders(200, 0);
                    exchange.getResponseBody().write(s.getBytes());
                    exchange.getResponseBody().flush();
                }
                // end challenge part
                else {
                    listener.executeEvent(frame);
                    exchange.sendResponseHeaders(200, -1);
                }
            }
        }
    }

    protected byte[] inputStreamToByteArray(InputStream stream) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        int nRead;
        byte[] data = new byte[16384];

        while ((nRead = stream.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }

        return buffer.toByteArray();
    }
}
