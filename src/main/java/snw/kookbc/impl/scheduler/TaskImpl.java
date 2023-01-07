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

package snw.kookbc.impl.scheduler;

import snw.jkook.plugin.Plugin;
import snw.jkook.scheduler.Scheduler;
import snw.jkook.scheduler.Task;
import snw.jkook.util.Validate;

import java.util.concurrent.Future;

public class TaskImpl implements Task {
    private final Scheduler scheduler;
    private final Future<?> future;
    private final int id;
    private final Plugin plugin;

    public TaskImpl(Scheduler scheduler, Future<?> future, int id, Plugin plugin) {
        this.scheduler = scheduler;
        this.future = future;
        this.id = id;
        this.plugin = plugin;
    }

    @Override
    public Plugin getPlugin() {
        return plugin;
    }

    @Override
    public void cancel() throws IllegalStateException {
        Validate.isTrue(!isCancelled(), "This task has already cancelled.");
        scheduler.cancelTask(getTaskId());
    }

    public void cancel0() {
        future.cancel(false);
    }

    @Override
    public boolean isCancelled() {
        return future.isCancelled();
    }

    @Override
    public boolean isExecuted() {
        return future.isDone() && !isCancelled();
    }

    @Override
    public int getTaskId() {
        return id;
    }
}
