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
import snw.kookbc.impl.network.ws.Connector;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static snw.kookbc.util.VirtualThreadUtil.startVirtualThread;

public class IgnoreSNListenerImpl extends ListenerImpl {
    private final List<Integer> processedSN = new LinkedList<>();

    public IgnoreSNListenerImpl(KBCClient client, Connector connector) {
        super(client, connector);
        startVirtualThread(() -> {
            while (client.isRunning()) {
                try {
                    //noinspection BusyWait
                    Thread.sleep(TimeUnit.SECONDS.toMillis(90));
                } catch (InterruptedException e) {
                    return;
                }
                synchronized (lck) {
                    while (processedSN.size() > 700) {
                        processedSN.remove(0);
                    }
                }
            }
        }, "SN Cleaner").start();
    }

    @Override
    protected void event(Frame frame) {
        synchronized (lck) {
            if (processedSN.contains(frame.getSN())) {
                client.getCore().getLogger().warn("收到来自远程的重复消息，已忽略");
                return;
            }
            client.getSession().increaseSN();
            event0(frame);
            processedSN.add(frame.getSN());
            saveSN();
        }
    }
}
