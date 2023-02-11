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

package snw.kookbc.impl.network;

import okhttp3.Request;
import okhttp3.WebSocket;
import snw.jkook.util.Validate;
import snw.kookbc.impl.KBCClient;

import java.util.concurrent.TimeUnit;

// The Connector. It will communicate with Kook WebSocket Server.
public class Connector {
    private final KBCClient kbcClient;
    private String wsLink = "";
    private WebSocket ws;
    private volatile boolean firstConnected = false; // sub-threads should not work on startup
    private volatile boolean connected = false;
    private volatile boolean timeout = false;
    private volatile boolean pingOk = false;
    private volatile boolean requireReconnect = false;
    private final Object reconnectLock = new Object();

    public Connector(KBCClient kbcClient) {
        this.kbcClient = kbcClient;
        new PingThread().start();
        new Reconnector(kbcClient, reconnectLock).start();
    }

    // should only be called on startup
    public void start() {
        start0();
        firstConnected = true;
    }

    private void start0() {
        getGateway();
        start1();
    }

    private void start1() {
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
        kbcClient.getCore().getLogger().info("WebSocket Connection OK");
    }

    private void getGateway() {
        wsLink = kbcClient.getNetworkClient().get(HttpAPIRoute.GATEWAY.toFullURL()).get("url").getAsString();
    }

    public void shutdown() {
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
            kbcClient.getCore().getLogger().debug("Called HTTP Bot offline API. Response: {}", kbcClient.getNetworkClient().postContent(HttpAPIRoute.USER_BOT_OFFLINE.toFullURL(), "", ""));
        } catch (Exception e) {
            kbcClient.getCore().getLogger().error("Unexpected Exception when we attempting to request HTTP Bot offline API.", e);
        }
    }

    // following methods should be called by other class:

    public synchronized void restart() {
        shutdown();
        kbcClient.getSession().getSN().set(0);
        kbcClient.getSession().getBuffer().clear();
        start0();
    }

    public void setConnected(boolean connected) {
        this.connected = connected;
    }

    public boolean isTimeout() {
        return timeout;
    }

    public void setTimeout(boolean timeout) {
        if (timeout) {
            kbcClient.getCore().getLogger().warn("PING failed. Status is now TIMEOUT.");
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
        if (!connected) { // Let PING Thread wait until the connector success.
            setTimeout(false);
            setPingOk(true);
            return;
        }
        kbcClient.getCore().getLogger().trace("Attempting to PING.");
        setPingOk(false);
        boolean queued = ws.send(String.format("{\"s\":2,\"sn\":%s}", kbcClient.getSession().getSN().get()));
        Validate.isTrue(queued, "Unable to queue ping request");
    }

    public void pong() {
        setPingOk(true);
        if (isTimeout()) {
            setTimeout(false);
        }
    }

    public void requestReconnect() {
        if (!requireReconnect) {
            synchronized (reconnectLock) {
                if (!requireReconnect) {
                    requireReconnect = true;
                    connected = false;
                    reconnectLock.notifyAll();
                }
            }
        }
    }

    public KBCClient getParent() {
        return kbcClient;
    }

    public void reconnectOk() {
        requireReconnect = false;
    }

    public boolean isRequireReconnect() {
        return requireReconnect && firstConnected;
    }

    public boolean isConnected() {
        return connected;
    }

    protected class PingThread extends Thread {

        public PingThread() {
            super("Ping Thread");
            this.setDaemon(true);
        }

        @Override
        public void run() {
            try {
                run0();
            } catch (InterruptedException ignored) {
            } catch (Throwable e) {
                kbcClient.getCore().getLogger().error("PING Thread terminated by critical exception.", e);
            }
        }

        private void run0() throws InterruptedException {
            while (kbcClient.isRunning()) {
                sleep(30);
                if (!connected) continue;
                ping();
                sleep(6);
                if (isTimeout()) {
                    int times = 0;
                    do {
                        ping();
                        sleep(++times == 1 ? 2 : 4);
                        if (isPingOk()) {
                            break; // why should I ping again????
                        }
                    } while (times < 2);
                    if (Thread.currentThread().isInterrupted()) {
                        return;
                    }
                    if (!isPingOk()) {
                        kbcClient.getCore().getLogger().warn("PING failed. Attempting to reconnect.");
                        requestReconnect();
                        // actually, we should try to RESUME at this time.
                        // but RESUME always fail (e.g. received RECONNECT after RESUME_ACK)
                        // so we won't support RESUME until the problem be solved.
                    }
                }
            }
        }

        private void sleep(int sec) throws InterruptedException {
            Thread.sleep(TimeUnit.SECONDS.toMillis(sec));
            if (!kbcClient.isRunning()) throw new InterruptedException();
        }
    }
}
