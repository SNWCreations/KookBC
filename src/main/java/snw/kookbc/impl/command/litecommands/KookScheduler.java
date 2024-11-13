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

import dev.rollczi.litecommands.scheduler.Scheduler;
import dev.rollczi.litecommands.scheduler.SchedulerPoll;
import dev.rollczi.litecommands.shared.ThrowingSupplier;
import snw.jkook.plugin.Plugin;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

public class KookScheduler implements Scheduler {
    private final Plugin plugin;
    private final ThreadLocal<Boolean> isMainThread = ThreadLocal.withInitial(() -> false);

    public KookScheduler(Plugin plugin) {
        this.plugin = plugin;
        this.isMainThread.set(true);
    }

    @Override
    public <T> CompletableFuture<T> supplyLater(SchedulerPoll type, Duration delay, ThrowingSupplier<T, Throwable> supplier) {
        SchedulerPoll resolve = type.resolve(SchedulerPoll.MAIN, SchedulerPoll.ASYNCHRONOUS);
        CompletableFuture<T> future = new CompletableFuture<>();
        if (resolve.equals(SchedulerPoll.MAIN) && delay.isZero() && isMainThread.get()) {
            tryRun(supplier, future);
        } else {
            if (delay.isZero()) {
                plugin.getCore().getScheduler().runTask(plugin, () -> {
                    tryRun(supplier, future);
                });
            } else {
                plugin.getCore().getScheduler().runTaskLater(plugin, () -> {
                    tryRun(supplier, future);
                }, delay.toMillis());
            }
        }

        return future;
    }

    private static <T> CompletableFuture<T> tryRun(ThrowingSupplier<T, Throwable> supplier, CompletableFuture<T> future) {
        try {
            future.complete(supplier.get());
        } catch (Throwable e) {
            future.completeExceptionally(e);
        }

        return future;
    }

    @Override
    public void shutdown() {

    }
}
