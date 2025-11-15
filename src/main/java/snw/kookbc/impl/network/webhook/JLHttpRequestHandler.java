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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import snw.kookbc.util.JacksonUtil;
import snw.kookbc.impl.KBCClient;
import snw.kookbc.impl.network.Frame;
import snw.kookbc.interfaces.network.FrameHandler;
import snw.kookbc.interfaces.network.webhook.Request;
import snw.kookbc.interfaces.network.webhook.RequestHandler;

import static snw.kookbc.util.JacksonUtil.*;

public class JLHttpRequestHandler implements RequestHandler<JsonNode> {
    private final String ourToken;
    private final FrameHandler handler;

    public JLHttpRequestHandler(KBCClient client, FrameHandler handler) {
        this.ourToken = client.getConfig().getString("webhook-verify-token", "");
        this.handler = handler;
        if (this.ourToken.isEmpty()) {
            throw new IllegalArgumentException("Webhook verify token is not specified");
        }
    }

    public void handle(Request<JsonNode> request) {
        final JsonNode object = request.toJson();
        final int signalType = object.get("s").asInt();
        final int sn;
        JsonNode snNode = object.get("sn");
        if (snNode != null && !snNode.isNull()) {
            sn = snNode.asInt();
        } else {
            sn = -1;
        }
        final JsonNode data = object.get("d");
        Frame frame = new Frame(signalType, sn, data);

        JsonNode verifyTokenNode = frame.getData().get("verify_token");
        if (verifyTokenNode == null || verifyTokenNode.isNull()) {
            request.reply(400, "");
            return;
        }
        final String gotToken = verifyTokenNode.asText();
        if (!ourToken.equals(gotToken)) {
            request.reply(400, "");
            return;
        }

        JsonNode channelTypeNode = frame.getData().get("channel_type");
        if (channelTypeNode != null && !channelTypeNode.isNull()) {
            final String channelType = channelTypeNode.asText();
            if ("WEBHOOK_CHALLENGE".equals(channelType)) {
                // challenge part
                JsonNode challengeNode = frame.getData().get("challenge");
                if (challengeNode == null || challengeNode.isNull()) {
                    request.reply(400, "");
                    return;
                }
                String challengeValue = challengeNode.asText();
                ObjectNode obj = JacksonUtil.createObjectNode();
                obj.put("challenge", challengeValue);
                String challengeJson = JacksonUtil.toJsonString(obj);
                request.reply(200, challengeJson);
                return;
                // end challenge part
            }
        }
        handler.handle(frame);
    }
}
