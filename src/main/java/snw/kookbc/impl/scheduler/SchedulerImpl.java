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

package snw.kookbc.impl.scheduler;

import snw.jkook.plugin.Plugin;
import snw.jkook.scheduler.Scheduler;
import snw.jkook.scheduler.Task;
import snw.jkook.util.Validate;
import snw.kookbc.impl.KBCClient;
import snw.kookbc.util.ThreadFactoryBuilder;

import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class SchedulerImpl implements Scheduler {
    private final KBCClient client;
    private final ScheduledExecutorService pool;
    private final AtomicInteger ids = new AtomicInteger(1);
    private final Map<Integer, TaskImpl> scheduledTasks = new ConcurrentHashMap<>();

    public SchedulerImpl(KBCClient client) {
        this(client, new ThreadFactoryBuilder("Scheduler Thread #").build());
    }

    public SchedulerImpl(KBCClient client, ThreadFactory factory) {
        this(client, 2, factory);
    }

    public SchedulerImpl(KBCClient client, int corePoolSize, ThreadFactory factory) {
        this.client = client;
        pool = Executors.newScheduledThreadPool(corePoolSize, factory);
    }


    @Override
    public void runTask(Runnable runnable) {
        Validate.notNull(runnable);
        pool.execute(runnable);
    }

    @Override
    public Task runTaskLater(Plugin plugin, Runnable runnable, long delay) {
        int id = nextId();
        TaskImpl task = new TaskImpl(this, pool.schedule(wrap(runnable, id), delay, TimeUnit.MILLISECONDS), id, plugin);
        scheduledTasks.put(id, task);
        return task;
    }

    @Override
    public Task runTaskTimer(Plugin plugin, Runnable runnable, long period, long delay) {
        int id = nextId();
        TaskImpl task = new TaskImpl(this, pool.scheduleAtFixedRate(wrap(runnable, id), period, delay, TimeUnit.MILLISECONDS), id, plugin);
        scheduledTasks.put(id, task);
        return task;
    }

    @Override
    public boolean isScheduled(int taskId) {
        return scheduledTasks.containsKey(taskId);
    }

    @Override
    public void cancelTask(int taskId) {
        if (isScheduled(taskId)) {
            scheduledTasks.remove(taskId).cancel0();
        }
    }

    @Override
    public void cancelTasks(Plugin plugin) {
        for (TaskImpl task : scheduledTasks.values()) {
            if (task.getPlugin() == plugin) {
                cancelTask(task.getTaskId());
            }
        }
    }

    private int nextId() {
        int id;
        do {
            id = this.ids.updateAndGet((previous) -> previous == Integer.MAX_VALUE ? 1 : previous + 1);
        } while (this.scheduledTasks.containsKey(id));

        return id;
    }

    private Runnable wrap(Runnable runnable, int id) {
        return () -> {
            try {
                runnable.run();
            } finally {
                scheduledTasks.remove(id);
            }
        };
    }

    public void shutdown() {
        scheduledTasks.keySet().forEach(this::cancelTask);
        if (!pool.isShutdown()) {
            pool.shutdownNow();
            try {
                //noinspection ResultOfMethodCallIgnored
                pool.awaitTermination(30, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                client.getCore().getLogger().error("Unexpected interrupt happened while we waiting the scheduler got fully stopped.", e);
            }
        }
    }
}
