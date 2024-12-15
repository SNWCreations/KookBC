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
package snw.kookbc.impl.command.litecommands;

import dev.rollczi.litecommands.scheduler.AbstractMainThreadBasedScheduler;
import snw.jkook.plugin.Plugin;
import snw.kookbc.impl.KBCClient;

import java.time.Duration;

public class KookScheduler extends AbstractMainThreadBasedScheduler {
    private final KBCClient client;
    private final Plugin plugin;

    public KookScheduler(KBCClient client, Plugin plugin) {
        this.client = client;
        this.plugin = plugin;
    }


    @Override
    public void shutdown() {

    }

    @Override
    protected void runSynchronous(Runnable task, Duration delay) {
        if (delay.isZero() && client.isPrimaryThread()) {
            task.run();
        } else {
            plugin.getCore().getScheduler().runTaskLater(plugin, task, delay.toMillis());
        }
    }

    @Override
    protected void runAsynchronous(Runnable task, Duration delay) {
        plugin.getCore().getScheduler().runTaskLater(plugin, task, delay.toMillis());
    }
}
