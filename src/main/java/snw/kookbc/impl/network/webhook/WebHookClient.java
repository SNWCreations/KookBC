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
import snw.jkook.Core;
import snw.jkook.JKook;
import snw.jkook.config.file.YamlConfiguration;
import snw.kookbc.impl.CoreImpl;
import snw.kookbc.impl.KBCClient;
import snw.kookbc.impl.bot.SimpleBotClassLoader;
import snw.kookbc.impl.network.Session;
import snw.kookbc.util.ThreadFactoryBuilder;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class WebHookClient extends KBCClient {
    protected HttpServer server;

    public WebHookClient(Core core, YamlConfiguration config, File botDataFolder) {
        super(core, config, botDataFolder);
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
        int port = getConfig().getInt("webhook-port");
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/kookbc-webhook", new SimpleHttpHandler(this));
        server.setExecutor(Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() / 4, new ThreadFactoryBuilder("Webhook Thread #").build()));
        server.start();
        getCore().getLogger().info("Server is listening on port {}", port);
        getCore().getLogger().debug("Initializing SN update listener.");
        int initSN = 0;
        File snfile = new File(getBotDataFolder(), "sn");
        if (snfile.exists()) {
            List<String> lines = Files.readAllLines(Paths.get(snfile.toURI()));
            if (!lines.isEmpty()) {
                initSN = Integer.parseInt(lines.get(0));
            }
        }
        getConnector().setSession(new Session(null, new AtomicInteger(initSN)));
        new SNUpdateListener(this).start();
    }

    @Override
    public void shutdown() {
        getCore().getLogger().debug("Client shutdown request received");
        if (!((CoreImpl) getCore()).isRunning()) {
            getCore().getLogger().debug("The client has already stopped");
            return;
        }

        getCore().getLogger().info("Stopping client");
        if (getBot() != null) {
            getBot().getLogger().info("Disabling " + getBot().getDescription().getName() + " version " + getBot().getDescription().getVersion());
            getBot().onDisable();
            // why do I check this? because in some environments,
            // the bot won't be loaded by using SimpleClassLoader, maybe another type?
            // And the Bot can be constructed without any check by BotClassLoader,
            // so we should check this before casting it.
            if (getBot().getClass().getClassLoader() instanceof SimpleBotClassLoader) {
                try {
                    ((SimpleBotClassLoader) getBot().getClass().getClassLoader()).close();
                } catch (IOException e) {
                    JKook.getLogger().error("Unexpected IOException while we attempting to close the Bot ClassLoader.", e);
                }
            }
        }

        if (server != null) {
            long httpStopTimeStamp = System.currentTimeMillis();
            getCore().getLogger().info("Stopping Webhook HTTP server");
            server.stop(30);
            getCore().getLogger().debug("Webhook HTTP server stopped, elapsed {}", TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - httpStopTimeStamp));
        }

        getCore().shutdown();
        getCore().getLogger().info("Client stopped");
    }
}
