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

import com.sun.net.httpserver.HttpServer;
import snw.jkook.config.file.YamlConfiguration;
import snw.kookbc.impl.CoreImpl;
import snw.kookbc.impl.KBCClient;
import snw.kookbc.util.PrefixThreadFactory;

import java.io.File;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.Executors;

public class WebHookClient extends KBCClient {
    protected HttpServer server;

    public WebHookClient(CoreImpl core, YamlConfiguration config, File pluginsFolder, String token) {
        super(core, config, pluginsFolder, token);
    }

    @Override
    protected void startNetwork() {
        try {
            startNetwork0();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected void startNetwork0() throws Exception {
        String route = getConfig().getString("webhook-route");
        if (route == null || route.isEmpty()) {
            throw new IllegalArgumentException("Invalid route path!");
        }
        int port = getConfig().getInt("webhook-port");
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext(String.format("/%s", route), new SimpleHttpHandler(this));
        server.setExecutor(Executors.newFixedThreadPool(Math.max(1, Runtime.getRuntime().availableProcessors() / 4), new PrefixThreadFactory("Webhook Thread #")));
        server.start();
        getCore().getLogger().info("Server is listening on port {}", port);
        getCore().getLogger().debug("Initializing SN from local file.");
        int initSN = 0;
        File snfile = new File(getPluginsFolder(), "sn");
        if (snfile.exists()) {
            List<String> lines = Files.readAllLines(Paths.get(snfile.toURI()));
            if (!lines.isEmpty()) {
                initSN = Integer.parseInt(lines.get(0));
            }
        }
        getSession().getSN().set(initSN);
    }

    @Override
    protected void shutdownNetwork() {
        if (server != null) {
            getCore().getLogger().info("Stopping Webhook HTTP server");
            server.stop(0);
        }
    }
}
