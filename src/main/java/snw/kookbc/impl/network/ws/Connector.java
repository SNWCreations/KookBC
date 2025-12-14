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

package snw.kookbc.impl.network.ws;

import okhttp3.Request;
import okhttp3.WebSocket;
import snw.jkook.util.Validate;
import snw.kookbc.impl.KBCClient;
import snw.kookbc.impl.network.HttpAPIRoute;

import java.util.concurrent.TimeUnit;

// The Connector. It will communicate with Kook WebSocket Server.
public class Connector {
    private final KBCClient kbcClient;
    private final ReconnectStrategy reconnectStrategy;
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
        this.reconnectStrategy = new ReconnectStrategy();
        new PingThread().start();
        new Reconnector(kbcClient, reconnectLock, this).start();
    }

    // should only be called on startup
    public void start() {
        start0();
        firstConnected = true;
    }

    private void start0() {
        try {
            getGateway();
            start1();
        } catch (Exception e) {
            kbcClient.getCore().getLogger().error("连接启动失败: {}", e.getMessage(), e);
            throw e; // 向上抛出以便 restart() 处理
        }
    }

    private void start1() {
        do {
            connected = false;
            try {
                // if self connected is true, call shutdownHttp()
                if (kbcClient.getNetworkClient().get(HttpAPIRoute.USER_ME.toFullURL()).get("online").asBoolean()) {
                    shutdownHttp();
                }
            } catch (Exception e) {
                kbcClient.getCore().getLogger().warn("检查在线状态失败（可能是网络问题），继续尝试连接: {}", e.getMessage());
            }

            int times = 0;
            do {
                try {
                    ws = kbcClient.getNetworkClient().newWebSocket(
                            new Request.Builder()
                                    .url(wsLink)
                                    .build(),
                            new WebSocketMessageProcessor(kbcClient, this)
                    );
                    long ts = System.currentTimeMillis();
                    // 增加超时时间从 6 秒到 15 秒，适应网络波动
                    while (System.currentTimeMillis() - ts < 15000L) {
                        if (connected) {
                            break;
                        }
                        Thread.sleep(100); // 避免忙等待
                    }
                } catch (Exception e) {
                    kbcClient.getCore().getLogger().warn("WebSocket 连接尝试失败 (第 {} 次): {}", times + 1, e.getMessage());
                }

                if (!connected) {
                    shutdownWs();
                    times++;
                }
            } while (!connected && times < 2);

            if (!connected) {
                // if this round failed, then we need to get a new WS link
                try {
                    getGateway();
                } catch (Exception e) {
                    kbcClient.getCore().getLogger().error("获取 Gateway 失败（可能是 DNS 解析或网络问题）: {}", e.getMessage());
                    throw new RuntimeException("无法获取 WebSocket Gateway", e);
                }
            }
        } while (!connected);

        kbcClient.getCore().getLogger().info("WebSocket 连接成功");
        // 连接成功后通知重连策略
        reconnectStrategy.onConnectionSuccess();
    }

    private void getGateway() {
        try {
            wsLink = kbcClient.getNetworkClient().get(HttpAPIRoute.GATEWAY.toFullURL()).get("url").asText();
            kbcClient.getCore().getLogger().debug("成功获取 WebSocket Gateway: {}", wsLink);
        } catch (Exception e) {
            kbcClient.getCore().getLogger().error("获取 WebSocket Gateway 失败: {}", e.getMessage(), e);
            // 抛出异常以便上层处理
            throw new RuntimeException("无法获取 WebSocket Gateway（可能是 DNS 解析失败或网络连接问题）", e);
        }
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
            kbcClient.getCore().getLogger().debug("已调用 HTTP Bot 离线 API，响应: {}", kbcClient.getNetworkClient().postContent(HttpAPIRoute.USER_BOT_OFFLINE.toFullURL(), "", ""));
        } catch (Exception e) {
            kbcClient.getCore().getLogger().error("尝试请求 HTTP Bot 离线 API 时发生意外异常", e);
        }
    }

    // following methods should be called by other class:

    public synchronized void restart() {
        restart(null);
    }

    public synchronized void restart(Throwable exception) {
        // 检查是否应该重连
        if (!reconnectStrategy.shouldReconnect(exception)) {
            kbcClient.getCore().getLogger().error("重连策略决定不再重连，停止重连尝试");
            kbcClient.getCore().getLogger().info(reconnectStrategy.getStatisticsReport());
            return;
        }

        // 关闭当前连接
        shutdown();
        kbcClient.getSession().getSN().set(0);
        kbcClient.getSession().getBuffer().clear();

        // 计算延迟并等待
        int delay = reconnectStrategy.getNextDelay();
        kbcClient.getCore().getLogger().info("准备在 {} 秒后重连...", delay);

        if (!reconnectStrategy.waitBeforeReconnect(delay)) {
            kbcClient.getCore().getLogger().warn("重连等待被中断，取消重连");
            return;
        }

        // 尝试重连
        try {
            kbcClient.getCore().getLogger().info("开始重连...");
            start0();
            reconnectStrategy.onConnectionSuccess();
        } catch (Exception e) {
            kbcClient.getCore().getLogger().error("重连过程中发生异常", e);
            reconnectStrategy.onConnectionFailure();
            // 递归重试（会被 shouldReconnect 限制次数）
            restart(e);
        }
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
        kbcClient.getCore().getLogger().trace("正在尝试 PING");
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

    public ReconnectStrategy getReconnectStrategy() {
        return reconnectStrategy;
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
