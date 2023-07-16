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
import net.freeutils.httpserver.HTTPServer;
import snw.kookbc.impl.KBCClient;
import snw.kookbc.interfaces.network.FrameHandler;
import snw.kookbc.interfaces.network.webhook.RequestHandler;
import snw.kookbc.interfaces.network.webhook.WebhookServer;
import snw.kookbc.util.PrefixThreadFactory;

import java.io.IOException;
import java.util.concurrent.Executors;

public class JLHttpWebhookServer implements WebhookServer {
    private final KBCClient client;
    private final HTTPServer server;
    private String route;

    public JLHttpWebhookServer(KBCClient client, int port, FrameHandler listener) {
        this.client = client;
        this.server = new HTTPServer(port);
        this.server.setExecutor(Executors.newCachedThreadPool(new PrefixThreadFactory("Webhook Thread #")));
        this.setHandler(new JLHttpRequestHandler(client, listener));
    }

    @Override
    public void start() {
        try {
            server.start();
        } catch (IOException e) {
            throw new RuntimeException("Unable to start Webhook server", e);
        }
    }

    @Override
    public void stop() {
        server.stop();
    }

    @Override
    public void setHandler(RequestHandler<JsonObject> handler) {
        if (server != null) {
            HTTPServer.VirtualHost virtualHost = server.getVirtualHost(null);
            virtualHost.addContext('/' + route, new JLHttpRequestWrapper(client, handler), "POST");
        }
    }

    @Override
    public void setEndpoint(String path) {
        this.route = path;
    }
}
