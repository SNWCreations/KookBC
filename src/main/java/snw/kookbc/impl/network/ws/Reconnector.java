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

import snw.kookbc.impl.KBCClient;

public class Reconnector extends Thread {
    private final KBCClient client;
    private final Object lock;
    private final Connector connector;

    public Reconnector(KBCClient client, Object lock, Connector connector) {
        super("Reconnect Thread");
        this.client = client;
        this.lock = lock;
        this.connector = connector;
        this.setDaemon(true);
    }

    @Override
    public void run() {
        while (client.isRunning()) {
            synchronized (lock) {
                // 等待重连请求
                while (!connector.isRequireReconnect()) {
                    try {
                        lock.wait();
                    } catch (InterruptedException e) {
                        client.getCore().getLogger().debug("Reconnector 线程被中断");
                        Thread.currentThread().interrupt();
                        return;
                    }
                }

                // 再次检查客户端是否还在运行
                if (!client.isRunning()) {
                    client.getCore().getLogger().debug("客户端已停止，Reconnector 退出");
                    return;
                }

                // 在同一个锁内检查连接状态并执行重连，避免 TOCTOU 竞态条件
                if (!connector.isConnected()) {
                    try {
                        client.getCore().getLogger().info("Reconnector 检测到断线，开始重连流程");
                        connector.restart();
                        client.getCore().getLogger().info("Reconnector 重连流程完成");
                    } catch (Exception e) {
                        client.getCore().getLogger().error("Reconnector 重连过程中发生未捕获异常", e);
                        // 异常已经在 connector.restart() 中处理，这里只是记录
                    } finally {
                        // 无论成功或失败，都标记重连请求已处理
                        connector.reconnectOk();
                    }
                } else {
                    // 已经连接上了，可能是其他地方已经处理了重连
                    client.getCore().getLogger().debug("Reconnector 检测到连接已恢复，跳过重连");
                    connector.reconnectOk();
                }
            }
        }
        client.getCore().getLogger().debug("Reconnector 线程退出");
    }
}
