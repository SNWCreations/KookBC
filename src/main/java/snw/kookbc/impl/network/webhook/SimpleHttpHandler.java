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

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.javalin.http.BadRequestResponse;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import io.javalin.http.HttpCode;
import snw.kookbc.impl.KBCClient;
import snw.kookbc.impl.network.Frame;
import snw.kookbc.impl.network.Listener;
import snw.kookbc.impl.network.ListenerFactory;

import java.util.Objects;

import static snw.kookbc.impl.network.MessageProcessor.decompressDeflate;

public class SimpleHttpHandler implements Handler {
    protected final KBCClient client;
    protected final Listener listener;

    public SimpleHttpHandler(KBCClient client) {
        this.client = client;
        listener = ListenerFactory.getListener(client);
    }

    @Override
    public void handle(Context ctx) {
        client.getCore().getLogger().debug("Got request!");
        String res;
        byte[] bytes = ctx.bodyAsBytes();
        if (bytes.length == 0) {
            throw new BadRequestResponse();
        }
        if (!"0".equals(ctx.queryParam("compress"))) {
            res = new String(decompressDeflate(bytes));
        } else {
            res = new String(bytes);
        }
        client.getCore().getLogger().debug("Got remote request: {}", res);
        JsonObject object = JsonParser.parseString(EncryptUtils.decrypt(client, res)).getAsJsonObject();
        client.getCore().getLogger().debug("Got DECRYPTED request payload: {}", object);
        Frame frame = new Frame(
                object.get("s").getAsInt(),
                object.has("sn") ? object.get("sn").getAsInt() : -1,
                object.getAsJsonObject("d")
        );
        if (!Objects.equals(
                frame.getData().get("verify_token").getAsString(),
                client.getConfig().getString("webhook-verify-token"))
        ) {
            throw new BadRequestResponse();
        } else {
            // challenge part
            JsonElement channelType = frame.getData().get("channel_type");
            if (channelType != null && Objects.equals(channelType.getAsString(), "WEBHOOK_CHALLENGE")) {
                String finalChallengeResponse = frame.getData().get("challenge").getAsString();
                JsonObject obj = new JsonObject();
                obj.addProperty("challenge", finalChallengeResponse);
                String s = new Gson().toJson(obj);
                ctx.result(s);
            }
            // end challenge part
            else {
                listener.executeEvent(frame);
            }
            ctx.status(HttpCode.OK);
        }
    }

}
