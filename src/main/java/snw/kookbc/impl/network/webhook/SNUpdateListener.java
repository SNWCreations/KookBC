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

import snw.jkook.JKook;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class SNUpdateListener extends Thread {
    private int prev;
    private final WebHookClient client;

    public SNUpdateListener(WebHookClient client) {
        this.client = client;
    }

    @Override
    public void run() {
        while (client.isRunning()) {
            int current = client.getSession().getSN().get();
            if (current > prev) {
                File snfile = new File(client.getPluginsFolder(), "sn");
                try {
                    if (!snfile.exists()) {
                        // noinspection ResultOfMethodCallIgnored
                        snfile.createNewFile();
                    }
                    FileWriter writer = new FileWriter(snfile, false);
                    writer.write(String.valueOf(current));
                    writer.close();
                } catch (IOException e) {
                    JKook.getLogger().warn("Unable to write SN to local.", e);
                }
                prev = current;
            }
        }
    }
}
