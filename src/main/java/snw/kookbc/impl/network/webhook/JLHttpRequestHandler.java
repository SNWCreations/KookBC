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

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import snw.kookbc.impl.KBCClient;
import snw.kookbc.impl.network.Frame;
import snw.kookbc.interfaces.network.FrameHandler;
import snw.kookbc.interfaces.network.webhook.Request;
import snw.kookbc.interfaces.network.webhook.RequestHandler;

import static snw.kookbc.util.GsonUtil.*;

public class JLHttpRequestHandler implements RequestHandler {
    private final KBCClient client;
    private final String ourToken;
    private final FrameHandler handler;

    public JLHttpRequestHandler(KBCClient client, FrameHandler handler) {
        this.client = client;
        this.ourToken = client.getConfig().getString("webhook-verify-token", "");
        this.handler = handler;
        if (this.ourToken.isEmpty()) {
            throw new IllegalArgumentException("Webhook verify token is not specified");
        }
    }

    @Override
    public void handle(Request<?> request) {
        final String body = request.getRawBody();
        final String decryptedBody = EncryptUtils.decrypt(client, body);
        JsonObject object = JsonParser.parseString(decryptedBody).getAsJsonObject();
        final int signalType = get(object, "s").getAsInt();
        final int sn;
        if (has(object, "sn")) {
            sn = get(object, "sn").getAsInt();
        } else {
            sn = -1;
        }
        final JsonObject data = get(object, "d").getAsJsonObject();
        Frame frame = new Frame(signalType, sn, data);
        final String gotToken = get(frame.getData(), "verify_token").getAsString();
        if (!ourToken.equals(gotToken)) {
            request.reply(400, "");
            return;
        }
        // challenge part
        if (has(frame.getData(), "channel_type")) {
            final String channelType = get(frame.getData(), "channel_type").getAsString();
            if ("WEBHOOK_CHALLENGE".equals(channelType)) {
                String challengeValue = frame.getData().get("challenge").getAsString();
                JsonObject obj = new JsonObject();
                obj.addProperty("challenge", challengeValue);
                String challengeJson = NORMAL_GSON.toJson(obj);
                request.reply(200, challengeJson);
            }
        }
        // end challenge part
        else {
            handler.handle(frame);
        }
    }
}
