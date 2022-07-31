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

package snw.kookbc.impl.network;

import okhttp3.Request;
import okhttp3.WebSocket;
import snw.jkook.JKook;
import snw.kookbc.impl.KBCClient;
import snw.jkook.util.Validate;

import java.util.concurrent.TimeUnit;

// The Connector that will communicate with Kook WebSocket Server.
public class Connector {
    private final KBCClient kbcClient;
    private String wsLink = "";
    private WebSocket ws;
    private volatile boolean connected = false;
    private volatile boolean timeout = false;
    private volatile boolean pingOk = false;
    private volatile boolean requireReconnect = false;

    public Connector(KBCClient kbcClient) {
        this.kbcClient = kbcClient;
        new Thread(
                () -> {
                    while (kbcClient.isRunning()) {
                        if (requireReconnect) {
                            try {
                                restart();
                            } catch (Exception e) { // make sure thread won't exit if something unexpected happened!
                                continue;
                            }
                            requireReconnect = false;
                        }
                    }
                }
                , "Reconnect Thread"
        ).start();
        new Thread(
                () -> {
                    while (kbcClient.isRunning()) {
                        try {
                            //noinspection BusyWait
                            Thread.sleep(TimeUnit.SECONDS.toMillis(30L));
                        } catch (InterruptedException e) {
                            return;
                        }
                        if (!connected) continue;
                        try {
                            ping();
                        } catch (Exception e) {
                            setTimeout(true);
                        }
                        if (isTimeout()) {
                            int times = 0;
                            do {
                                try {
                                    ping();
                                } catch (Exception e) {
                                    times++;
                                    continue;
                                }
                                if (isPingOk()) {
                                    continue;
                                }
                                times++;
                            } while (times < 2);
                            JKook.getLogger().warn("PING failed. Attempting to reconnect.");
                            setRequireReconnect(true);
                        }
                    }
                }, "Ping Thread"
        ).start();
    }

    public void start() {
        getGateway();
        start0();
    }

    public void start0() {
        do {
            connected = false;
            // if self connected is true, call shutdownHttp()
            if (kbcClient.getNetworkClient().get(HttpAPIRoute.USER_ME.toFullURL()).get("online").getAsBoolean()) {
                shutdownHttp();
            }
            int times = 0;
            do {
                ws = kbcClient.getNetworkClient().newWebSocket(
                        new Request.Builder()
                                .url(wsLink)
                                .build(),
                        new MessageProcessor(kbcClient)
                );
                long ts = System.currentTimeMillis();
                while (System.currentTimeMillis() - ts < 6000L) {
                    if (connected) {
                        break;
                    }
                }
                if (!connected) { // I WASTE 2 HOURS ON THIS
                    shutdownWs();
                    times++;
                }
            } while (!connected && times < 2);
            if (!connected) { // if this round failed, then we need to get a new WS link
                getGateway();
            }
        } while (!connected);
        JKook.getLogger().info("WebSocket Connection OK");
    }

    private void getGateway() {
        wsLink = kbcClient.getNetworkClient().get(HttpAPIRoute.GATEWAY.toFullURL()).get("url").getAsString();
    }

    public void shutdown() {
//        if (pingTask.isScheduled()) {
//            pingTask.cancel();
//        }
        setTimeout(false);
        shutdownWs();
        shutdownHttp();
    }

    private void shutdownWs() {
        if (ws != null) {
            ws.close(1000, "User Closed Service");
        }
    }

    public void shutdownHttp() {
        try {
            JKook.getLogger().debug("Called HTTP Bot offline API. Response: {}", kbcClient.getNetworkClient().postContent(HttpAPIRoute.USER_BOT_OFFLINE.toFullURL(), "", ""));
        } catch (Exception e) {
            JKook.getLogger().error("Unexpected Exception when we attempting to request HTTP Bot offline API.", e);
        }
    }

    // following methods should be called by other class:

    public synchronized void restart() {
        try {
            shutdown();
        } catch (Exception e) {
            throw new Error("Unable to shutdown. Please restart the application manually.", e);
        }
        kbcClient.getSession().getSN().set(0);
        kbcClient.getSession().getBuffer().clear();
        start();
    }

    public void setConnected(boolean connected) {
        this.connected = connected;
    }

    public boolean isTimeout() {
        return timeout;
    }

    public void setTimeout(boolean timeout) {
        if (timeout) {
            JKook.getLogger().warn("PING failed. Status is now TIMEOUT.");
        }
        this.timeout = timeout;
    }

    public boolean isPingOk() {
        return pingOk;
    }

    public void setPingOk(boolean pingOk) {
        this.pingOk = pingOk;
    }

    public void ping() {
        if (!kbcClient.isRunning()) {
            throw new RuntimeException("The client is not running! (If you see this when the client is stopping, please ignore this)");
        }
        JKook.getLogger().debug("Attempting to PING.");
        setPingOk(false);
        boolean queued = ws.send(String.format("{\"s\":2,\"sn\":%s}", kbcClient.getSession().getSN().get()));
        Validate.isTrue(queued, "Unable to queue ping request");
        long ts = System.currentTimeMillis();
        while (System.currentTimeMillis() - ts < 6000L) {
            if (isPingOk()) {
                break;
            }
        }
        if (!isPingOk()) {
            setTimeout(true);
        }
    }

    public void pong() {
        setPingOk(true);
        if (isTimeout()) {
            setTimeout(false);
            JKook.getLogger().info("We have recovered from TIMEOUT successfully, attempting to resume.");
            ws.send(String.format("{\"s\":4,\"sn\":%s}", kbcClient.getSession().getSN().get()));
        }
    }

    public void setRequireReconnect(boolean requireReconnect) {
        this.requireReconnect = requireReconnect;
    }
}
