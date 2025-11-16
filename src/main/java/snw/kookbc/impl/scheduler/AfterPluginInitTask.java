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
import snw.jkook.scheduler.Task;
import snw.jkook.util.Validate;

public class AfterPluginInitTask implements Task {
    private final Plugin plugin;
    private final int id;
    private final Runnable runnable;
    private boolean cancelled = false;
    private boolean executed = false;

    public AfterPluginInitTask(Plugin plugin, int id, Runnable runnable) {
        this.plugin = plugin;
        this.id = id;
        this.runnable = runnable;
    }

    public void run() {
        if (isExecuted()) {
            throw new IllegalStateException("This task has already executed!");
        }
        if (!plugin.isEnabled() || isCancelled()) {
            return;
        }
        try {
            runnable.run();
        } catch (Throwable e) {
            plugin.getLogger().warn("运行插件初始化后任务时发生异常", e);
        }
        executed = true;
    }

    @Override
    public Plugin getPlugin() {
        return plugin;
    }

    @Override
    public void cancel() throws IllegalStateException {
        Validate.isTrue(!isCancelled(), "This task has already cancelled.");
        cancelled = true;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public boolean isExecuted() {
        return executed;
    }

    @Override
    public int getTaskId() {
        return id;
    }
}
