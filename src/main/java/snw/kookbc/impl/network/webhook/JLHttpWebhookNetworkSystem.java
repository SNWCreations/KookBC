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

import org.jetbrains.annotations.Nullable;
import snw.kookbc.impl.KBCClient;
import snw.kookbc.impl.network.Listener;
import snw.kookbc.impl.network.ListenerFactory;
import snw.kookbc.interfaces.network.FrameHandler;
import snw.kookbc.interfaces.network.webhook.WebhookNetworkSystem;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class JLHttpWebhookNetworkSystem implements WebhookNetworkSystem {
    private final KBCClient client;
    private final JLHttpWebhookServer server;
    private final int port;

    public JLHttpWebhookNetworkSystem(KBCClient client, @Nullable FrameHandler handler) {
        final int port = client.getConfig().getInt("webhook-port");
        String route = client.getConfig().getString("webhook-route");
        if (route == null) {
            throw new IllegalArgumentException("No route provided!");
        }
        if (handler == null) {
            @SuppressWarnings("deprecation")
            final Listener listener = ListenerFactory.getListener(client, null);
            handler = listener::executeEvent;
        }
        this.client = client;
        this.server = new JLHttpWebhookServer(client,route, port, handler);
        this.port = port;
    }

    @Override
    public void start() {
        try {
            client.getCore().getLogger().debug("正在从本地文件初始化 SN");
            int initSN = 0;
            File snfile = new File(client.getPluginsFolder(), "sn");
            if (snfile.exists()) {
                List<String> lines = Files.readAllLines(Paths.get(snfile.toURI()));
                if (!lines.isEmpty()) {
                    initSN = Integer.parseInt(lines.get(0));
                }
            }
            client.getSession().getSN().set(initSN);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        server.start();
        client.getCore().getLogger().info("Webhook HTTP 服务器正在监听端口 " + port);
    }

    @Override
    public void stop() {
        client.getCore().getLogger().info("正在停止 Webhook HTTP 服务器");
        server.stop();
    }
}
