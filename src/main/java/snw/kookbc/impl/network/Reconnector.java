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

import snw.kookbc.impl.KBCClient;

public class Reconnector extends Thread {
    private final KBCClient client;
    private final Object lock;

    public Reconnector(KBCClient client, Object lock) {
        super("Reconnect Thread");
        this.client = client;
        this.lock = lock;
        this.setDaemon(true);
    }

    @Override
    public void run() {
        while (client.isRunning()) {
            synchronized (lock) {
                while (!client.getConnector().isRequireReconnect()) {
                    try {
                        lock.wait();
                    } catch (InterruptedException e) {
                        return;
                    }
                }
                if (!client.isRunning()) {
                    return;
                }
                if (client.getConnector().isConnected()) {
                    continue;
                }
                client.getConnector().restart();
                client.getConnector().reconnectOk();
            }
        }
    }
}
