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

import net.freeutils.httpserver.HTTPServer;
import snw.kookbc.impl.KBCClient;
import snw.kookbc.interfaces.network.webhook.RequestHandler;

import java.io.IOException;

public class JLHttpRequestWrapper implements HTTPServer.ContextHandler {
    private final KBCClient client;
    private final RequestHandler handler;

    public JLHttpRequestWrapper(KBCClient client, RequestHandler handler) {
        this.client = client;
        this.handler = handler;
    }

    @Override
    public int serve(HTTPServer.Request request, HTTPServer.Response response) throws IOException {
        final JLHttpRequest wrapped = new JLHttpRequest(request, response);
        final String body = wrapped.getRawBody();
        if (body.isEmpty()) {
            return 400;
        }
        try {
            handler.handle(wrapped);
        } catch (Exception e) {
            client.getCore().getLogger().error("Unable to process request", e);
            throw new IOException(e);
        }
        if (!wrapped.isReplyPresent()) {
            wrapped.reply(200, "");
        }
        return 0;
    }
}
