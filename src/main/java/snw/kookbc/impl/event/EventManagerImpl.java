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

package snw.kookbc.impl.event;

import net.kyori.event.EventBus;
import net.kyori.event.PostResult;
import net.kyori.event.SimpleEventBus;
import net.kyori.event.method.MethodSubscriptionAdapter;
import net.kyori.event.method.SimpleMethodSubscriptionAdapter;
import snw.jkook.event.Event;
import snw.jkook.event.EventManager;
import snw.jkook.event.Listener;
import snw.jkook.plugin.Plugin;
import snw.kookbc.impl.KBCClient;
import snw.kookbc.util.VirtualThreadUtil;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;
import java.util.stream.Collectors;

import static snw.kookbc.util.Util.ensurePluginEnabled;

public class EventManagerImpl implements EventManager {
    private final KBCClient client;
    private final EventBus<Event> bus;
    private final MethodSubscriptionAdapter<Listener> msa;
    private final Map<Plugin, List<Listener>> listeners = new ConcurrentHashMap<>();

    // 优化的并行事件处理
    private final ExecutorService eventExecutor;
    private final boolean parallelEventProcessing;
    private final boolean performanceMonitoringEnabled;

    // 性能监控
    private final AtomicLong totalEventsProcessed = new AtomicLong(0);
    private final LongAdder totalProcessingTime = new LongAdder();
    private final AtomicLong parallelEventsProcessed = new AtomicLong(0);
    private final Map<Class<? extends Event>, AtomicLong> eventTypeCounters = new ConcurrentHashMap<>();

    // 定期性能报告
    private final ScheduledExecutorService reportScheduler;
    private ScheduledFuture<?> reportTask;

    public EventManagerImpl(KBCClient client) {
        this.client = client;
        this.bus = new SimpleEventBus<>(Event.class);
        this.msa = new SimpleMethodSubscriptionAdapter<>(bus, EventExecutorFactoryImpl.INSTANCE, MethodScannerImpl.INSTANCE);

        // 从配置读取是否启用并行事件处理
        this.parallelEventProcessing = client.getConfig().getBoolean("enable-parallel-event-processing", true);
        this.performanceMonitoringEnabled = client.getConfig().getBoolean("enable-event-performance-monitoring", true);

        // 创建专用的虚拟线程执行器用于并行事件处理
        this.eventExecutor = parallelEventProcessing ?
            VirtualThreadUtil.newVirtualThreadExecutor() :
            null;

        // 创建定期报告调度器
        this.reportScheduler = performanceMonitoringEnabled ?
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "EventManager-PerformanceReporter");
                t.setDaemon(true);
                return t;
            }) : null;

        // 启动定期性能报告（如果启用）
        int reportInterval = client.getConfig().getInt("event-performance-report-interval", 30);
        if (performanceMonitoringEnabled && reportScheduler != null && reportInterval > 0) {
            this.reportTask = reportScheduler.scheduleAtFixedRate(
                this::printPerformanceReport,
                reportInterval,
                reportInterval,
                TimeUnit.MINUTES
            );
            client.getCore().getLogger().info("定期性能报告已启用，间隔: {}分钟", reportInterval);
        } else {
            this.reportTask = null;
        }

        client.getCore().getLogger().info("事件管理器初始化完成 - 并行处理: {}, 性能监控: {}",
            parallelEventProcessing ? "启用" : "禁用",
            performanceMonitoringEnabled ? "启用" : "禁用");
    }

    @Override
    public void callEvent(Event event) {
        if (event == null) {
            return;
        }

        long startTime = performanceMonitoringEnabled ? System.nanoTime() : 0;

        // 更新事件计数器（仅在启用监控时）
        if (performanceMonitoringEnabled) {
            totalEventsProcessed.incrementAndGet();
            eventTypeCounters.computeIfAbsent(event.getClass(), k -> new AtomicLong(0)).incrementAndGet();
        }

        try {
            if (parallelEventProcessing && eventExecutor != null) {
                // 并行模式：使用虚拟线程执行事件处理
                callEventParallel(event);
                if (performanceMonitoringEnabled) {
                    parallelEventsProcessed.incrementAndGet();
                }
            } else {
                // 传统同步模式：保持向后兼容
                callEventSync(event);
            }
        } finally {
            // 记录处理时间（仅在启用监控时）
            if (performanceMonitoringEnabled && startTime > 0) {
                long processingTime = System.nanoTime() - startTime;
                totalProcessingTime.add(processingTime);
            }
        }
    }

    /**
     * 并行事件处理方法 - 符合 Kook SN 顺序要求
     *
     * 关键设计：
     * 1. 全局事件顺序已由 ListenerImpl 保证（通过 SN 检查）
     * 2. 单个事件内部的监听器可以并行处理
     * 3. 使用虚拟线程提高吞吐量，减少上下文切换开销
     */
    private void callEventParallel(Event event) {
        CompletableFuture<PostResult> future = CompletableFuture.supplyAsync(() -> {
            return bus.post(event);
        }, eventExecutor);

        try {
            // 等待事件处理完成，不设置超时以避免中断重要事件
            PostResult result = future.get();
            handlePostResult(result, event);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            client.getCore().getLogger().warn("事件处理被中断: {}", event.getClass().getSimpleName(), e);
            // 回退到同步处理
            callEventSync(event);
        } catch (ExecutionException e) {
            client.getCore().getLogger().error("并行事件处理异常: {}", event.getClass().getSimpleName(), e.getCause());
            // 回退到同步处理
            callEventSync(event);
        }
    }

    /**
     * 传统同步事件处理方法
     */
    private void callEventSync(Event event) {
        final PostResult result = bus.post(event);
        handlePostResult(result, event);
    }

    /**
     * 处理事件处理结果
     */
    private void handlePostResult(PostResult result, Event event) {
        if (!result.wasSuccessful()) {
            client.getCore().getLogger().error("事件处理异常: {}", event.getClass().getSimpleName());
            for (final Throwable t : result.exceptions().values()) {
                client.getCore().getLogger().error("监听器异常", t);
            }
        }
    }

    @Override
    public void registerHandlers(Plugin plugin, Listener listener) {
        ensurePluginEnabled(plugin);
        try {
            msa.register(listener);
        } catch (SimpleMethodSubscriptionAdapter.SubscriberGenerationException e) {
            msa.unregister(listener); // rollback
            throw e; // rethrow
        }
        getListeners(plugin).add(listener);
    }

    @Override
    public void unregisterAllHandlers(Plugin plugin) {
        if (!listeners.containsKey(plugin)) {
            return; // it is not necessary to waste a List.
        }
        getListeners(plugin).forEach(this::unregisterHandlers);
        listeners.remove(plugin);
    }

    @Override
    public void unregisterHandlers(Listener listener) {
        msa.unregister(listener);
    }

    public boolean isSubscribed(Class<? extends Event> type) {
        return bus.hasSubscribers(type);
    }

    /**
     * 获取性能统计信息
     */
    public EventPerformanceStats getPerformanceStats() {
        return new EventPerformanceStats(
            totalEventsProcessed.get(),
            parallelEventsProcessed.get(),
            totalProcessingTime.sum(),
            Map.copyOf(eventTypeCounters.entrySet().stream()
                .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    entry -> entry.getValue().get()
                )))
        );
    }

    /**
     * 重置性能统计
     */
    public void resetPerformanceStats() {
        totalEventsProcessed.set(0);
        parallelEventsProcessed.set(0);
        totalProcessingTime.reset();
        eventTypeCounters.clear();
        client.getCore().getLogger().debug("事件管理器性能统计已重置");
    }

    /**
     * 打印性能统计报告
     */
    public void printPerformanceReport() {
        EventPerformanceStats stats = getPerformanceStats();
        long totalEvents = stats.getTotalEventsProcessed();

        client.getCore().getLogger().info("=== 事件管理器性能报告 ===");
        client.getCore().getLogger().info("总事件处理数: {}", totalEvents);
        client.getCore().getLogger().info("并行事件处理数: {} ({:.1f}%)",
            stats.getParallelEventsProcessed(),
            totalEvents > 0 ? (stats.getParallelEventsProcessed() * 100.0 / totalEvents) : 0);

        if (totalEvents > 0) {
            client.getCore().getLogger().info("平均处理时间: {:.2f}ms",
                stats.getTotalProcessingTimeNanos() / 1_000_000.0 / totalEvents);
        }

        client.getCore().getLogger().info("事件类型分布:");
        stats.getEventTypeCounts().entrySet().stream()
            .sorted(Map.Entry.<Class<? extends Event>, Long>comparingByValue().reversed())
            .limit(10)
            .forEach(entry -> client.getCore().getLogger().info("  {}: {}",
                entry.getKey().getSimpleName(),
                entry.getValue()));
    }

    /**
     * 关闭事件管理器，清理资源
     */
    public void shutdown() {
        client.getCore().getLogger().info("正在关闭事件管理器...");

        // 取消定期报告任务
        if (reportTask != null && !reportTask.isCancelled()) {
            reportTask.cancel(false);
        }

        // 关闭报告调度器
        if (reportScheduler != null && !reportScheduler.isShutdown()) {
            reportScheduler.shutdown();
            try {
                if (!reportScheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    reportScheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                reportScheduler.shutdownNow();
            }
        }

        // 关闭事件执行器
        if (eventExecutor != null && !eventExecutor.isShutdown()) {
            client.getCore().getLogger().info("正在关闭事件处理器...");
            eventExecutor.shutdown();
            try {
                if (!eventExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                    client.getCore().getLogger().warn("事件处理器未在10秒内正常关闭，强制关闭");
                    eventExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                eventExecutor.shutdownNow();
            }
        }

        // 打印最终性能报告
        if (performanceMonitoringEnabled) {
            client.getCore().getLogger().info("=== 事件管理器关闭 - 最终性能报告 ===");
            printPerformanceReport();
        }

        client.getCore().getLogger().info("事件管理器已关闭");
    }

    /**
     * 事件性能统计数据类
     */
    public static class EventPerformanceStats {
        private final long totalEventsProcessed;
        private final long parallelEventsProcessed;
        private final long totalProcessingTimeNanos;
        private final Map<Class<? extends Event>, Long> eventTypeCounts;

        public EventPerformanceStats(long totalEventsProcessed, long parallelEventsProcessed,
                                   long totalProcessingTimeNanos, Map<Class<? extends Event>, Long> eventTypeCounts) {
            this.totalEventsProcessed = totalEventsProcessed;
            this.parallelEventsProcessed = parallelEventsProcessed;
            this.totalProcessingTimeNanos = totalProcessingTimeNanos;
            this.eventTypeCounts = eventTypeCounts;
        }

        public long getTotalEventsProcessed() { return totalEventsProcessed; }
        public long getParallelEventsProcessed() { return parallelEventsProcessed; }
        public long getTotalProcessingTimeNanos() { return totalProcessingTimeNanos; }
        public Map<Class<? extends Event>, Long> getEventTypeCounts() { return eventTypeCounts; }
    }

    private List<Listener> getListeners(Plugin plugin) {
        return listeners.computeIfAbsent(plugin, p -> new LinkedList<>());
    }

}
