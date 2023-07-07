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

package snw.kookbc.impl.tasks;

import snw.kookbc.SharedConstants;
import snw.kookbc.impl.KBCClient;

import java.io.File;

public class StopSignalListener extends Thread {
    private final KBCClient client;

    public StopSignalListener(KBCClient client) {
        super(SharedConstants.IMPL_NAME + " - StopSignalListener");
        this.setDaemon(true);
        this.client = client;
    }

    @Override
    public void run() {
        final KBCClient client = this.client;
        final File localFile = new File("./KOOKBC_STOP");
        while (client.isRunning()) {
            try {
                //noinspection BusyWait
                Thread.sleep(1000L);
            } catch (InterruptedException e) {
                continue;
            }
            if (localFile.exists()) {
                //noinspection ResultOfMethodCallIgnored
                localFile.delete();
                client.getCore().getLogger().info("Received stop signal by new file. Stopping!");
                client.shutdown();
                return;
            }
        }
    }
}
