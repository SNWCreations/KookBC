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
import net.freeutils.httpserver.HTTPServer;
import snw.kookbc.impl.KBCClient;
import snw.kookbc.interfaces.network.webhook.Request;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static snw.kookbc.util.Util.decompressDeflate;
import static snw.kookbc.util.Util.inputStreamToByteArray;

public class JLHttpRequest implements Request<JsonObject> {
    private final KBCClient client;
    private final HTTPServer.Request request;
    private final HTTPServer.Response response;
    private final AtomicReference<String> body = new AtomicReference<>();
    private boolean replySent = false;

    public JLHttpRequest(KBCClient client, HTTPServer.Request request, HTTPServer.Response response) {
        this.client = client;
        this.request = request;
        this.response = response;
    }

    @Override
    public String getRawBody() {
        return body.updateAndGet(dat -> {
            if (dat == null) {
                try {
                    byte[] bytes = inputStreamToByteArray(request.getBody());
                    if (bytes.length == 0) {
                        return "";
                    }
                    if (isCompressed()) {
                        return new String(decompressDeflate(bytes));
                    } else {
                        return new String(bytes);
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
            return dat;
        });
    }

    @Override
    public JsonObject toJson() {
        final String decryptedBody = EncryptUtils.decrypt(client, getRawBody());
        return JsonParser.parseString(decryptedBody).getAsJsonObject();
    }

    @Override
    public boolean isCompressed() {
        try {
            return !"0".equals(request.getParams().get("compress"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void reply(int status, String content) {
        try {
            // the following part is copied from HTTPServer.Response.send method.
            // I just edited the value of the "contentType" parameter.
            byte[] contentBytes = content.getBytes(StandardCharsets.UTF_8);
            response.sendHeaders(
                    status,
                    contentBytes.length,
                    -1L,
                    "W/\"" + Integer.toHexString(content.hashCode()) + "\"",
                    "application/json; charset=utf-8",
                    null
            );
            OutputStream out = response.getBody();
            if (out != null) {
                out.write(contentBytes);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        replySent = true;
    }

    @Override
    public boolean isReplyPresent() {
        return replySent;
    }
}
