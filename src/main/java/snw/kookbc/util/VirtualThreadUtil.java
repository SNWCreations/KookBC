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

package snw.kookbc.util;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.Map;
import java.util.Set;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.stream.Collectors;

/**
 * 虚拟线程工具类 - 提供基于 Java 21 虚拟线程的高性能 ExecutorService
 *
 * <p>虚拟线程的优势：
 * <ul>
 *   <li>极低的内存占用 (~1KB vs 传统线程的 ~2MB)</li>
 *   <li>支持数百万并发线程</li>
 *   <li>自动的阻塞操作优化</li>
 *   <li>更好的伸缩性和响应性</li>
 * </ul>
 *
 * @since Java 21
 */
public final class VirtualThreadUtil {

    private VirtualThreadUtil() {
        // 工具类，禁止实例化
    }

    // ===== 专用执行器管理 =====

    // 专用执行器缓存，避免重复创建
    private static final Map<String, ExecutorService> EXECUTOR_CACHE = new ConcurrentHashMap<>();

    // HTTP 请求专用虚拟线程执行器
    private static volatile ExecutorService httpExecutor;

    // 文件 I/O 专用虚拟线程执行器
    private static volatile ExecutorService fileIoExecutor;

    // 插件操作专用虚拟线程执行器
    private static volatile ExecutorService pluginExecutor;

    // 数据库操作专用虚拟线程执行器
    private static volatile ExecutorService databaseExecutor;

    // 缓存操作专用虚拟线程执行器
    private static volatile ExecutorService cacheExecutor;

    // 性能统计
    private static final AtomicLong totalVirtualThreadsCreated = new AtomicLong(0);
    private static final AtomicLong totalTasksExecuted = new AtomicLong(0);

    // ===== 专用执行器获取方法 =====

    /**
     * 获取 HTTP 请求专用虚拟线程执行器
     *
     * <p>专为 HTTP API 调用优化，支持大量并发请求而不阻塞系统
     *
     * @return HTTP 专用执行器
     */
    public static ExecutorService getHttpExecutor() {
        if (httpExecutor == null) {
            synchronized (VirtualThreadUtil.class) {
                if (httpExecutor == null) {
                    httpExecutor = createInstrumentedExecutor("HTTP-VirtualThread");
                }
            }
        }
        return httpExecutor;
    }

    /**
     * 获取文件 I/O 专用虚拟线程执行器
     *
     * <p>专为文件读写操作优化，避免阻塞主线程
     *
     * @return 文件 I/O 专用执行器
     */
    public static ExecutorService getFileIoExecutor() {
        if (fileIoExecutor == null) {
            synchronized (VirtualThreadUtil.class) {
                if (fileIoExecutor == null) {
                    fileIoExecutor = createInstrumentedExecutor("FileIO-VirtualThread");
                }
            }
        }
        return fileIoExecutor;
    }

    /**
     * 获取插件操作专用虚拟线程执行器
     *
     * <p>专为插件加载、执行等操作优化
     *
     * @return 插件操作专用执行器
     */
    public static ExecutorService getPluginExecutor() {
        if (pluginExecutor == null) {
            synchronized (VirtualThreadUtil.class) {
                if (pluginExecutor == null) {
                    pluginExecutor = createInstrumentedExecutor("Plugin-VirtualThread");
                }
            }
        }
        return pluginExecutor;
    }

    /**
     * 获取数据库操作专用虚拟线程执行器
     *
     * <p>专为数据库查询、更新等操作优化
     *
     * @return 数据库操作专用执行器
     */
    public static ExecutorService getDatabaseExecutor() {
        if (databaseExecutor == null) {
            synchronized (VirtualThreadUtil.class) {
                if (databaseExecutor == null) {
                    databaseExecutor = createInstrumentedExecutor("Database-VirtualThread");
                }
            }
        }
        return databaseExecutor;
    }

    /**
     * 获取缓存操作专用虚拟线程执行器
     *
     * <p>专为缓存读写、清理等操作优化
     *
     * @return 缓存操作专用执行器
     */
    public static ExecutorService getCacheExecutor() {
        if (cacheExecutor == null) {
            synchronized (VirtualThreadUtil.class) {
                if (cacheExecutor == null) {
                    cacheExecutor = createInstrumentedExecutor("Cache-VirtualThread");
                }
            }
        }
        return cacheExecutor;
    }

    // ===== 执行器管理和监控 =====

    /**
     * 创建带性能监控的虚拟线程执行器
     *
     * @param namePrefix 线程名称前缀
     * @return 带监控的执行器
     */
    private static ExecutorService createInstrumentedExecutor(String namePrefix) {
        ThreadFactory factory = Thread.ofVirtual()
            .name(namePrefix, 0)
            .factory();

        return Executors.newThreadPerTaskExecutor(new InstrumentedThreadFactory(factory, namePrefix));
    }

    /**
     * 获取或创建指定名称的虚拟线程执行器
     *
     * @param executorName 执行器名称
     * @return 虚拟线程执行器
     */
    public static ExecutorService getOrCreateExecutor(String executorName) {
        return EXECUTOR_CACHE.computeIfAbsent(executorName,
            name -> createInstrumentedExecutor(name + "-VirtualThread"));
    }

    /**
     * 获取当前活跃的虚拟线程统计信息
     *
     * @return 虚拟线程统计信息
     */
    public static VirtualThreadStats getVirtualThreadStats() {
        Set<Thread> virtualThreads = Thread.getAllStackTraces().keySet().stream()
            .filter(Thread::isVirtual)
            .collect(Collectors.toSet());

        Map<String, Long> threadsByCategory = virtualThreads.stream()
            .collect(Collectors.groupingBy(
                thread -> {
                    String name = thread.getName();
                    int dashIndex = name.indexOf("-");
                    return dashIndex > 0 ? name.substring(0, dashIndex) : "Other";
                },
                Collectors.counting()
            ));

        return new VirtualThreadStats(
            virtualThreads.size(),
            totalVirtualThreadsCreated.get(),
            totalTasksExecuted.get(),
            threadsByCategory
        );
    }

    /**
     * 打印虚拟线程使用统计
     */
    public static void printVirtualThreadStats() {
        VirtualThreadStats stats = getVirtualThreadStats();
        System.out.println("=== 虚拟线程使用统计 ===");
        System.out.println("当前活跃虚拟线程数: " + stats.getActiveVirtualThreads());
        System.out.println("累计创建虚拟线程数: " + stats.getTotalVirtualThreadsCreated());
        System.out.println("累计执行任务数: " + stats.getTotalTasksExecuted());
        System.out.println("线程分类统计:");
        stats.getThreadsByCategory().forEach((category, count) ->
            System.out.println("  " + category + ": " + count + " 个线程"));
    }

    /**
     * 关闭所有专用执行器
     *
     * <p>应在应用关闭时调用以确保资源正确释放
     */
    public static void shutdownAllExecutors() {
        shutdownExecutor("HTTP", httpExecutor);
        shutdownExecutor("FileIO", fileIoExecutor);
        shutdownExecutor("Plugin", pluginExecutor);
        shutdownExecutor("Database", databaseExecutor);
        shutdownExecutor("Cache", cacheExecutor);

        // 关闭缓存中的执行器
        EXECUTOR_CACHE.forEach((name, executor) -> shutdownExecutor(name, executor));
        EXECUTOR_CACHE.clear();
    }

    /**
     * 安全关闭执行器
     */
    private static void shutdownExecutor(String name, ExecutorService executor) {
        if (executor != null && !executor.isShutdown()) {
            try {
                executor.shutdown();
                System.out.println("虚拟线程执行器 " + name + " 已关闭");
            } catch (Exception e) {
                System.err.println("关闭虚拟线程执行器 " + name + " 时出错: " + e.getMessage());
            }
        }
    }

    // ===== 工具方法 =====

    /**
     * 在虚拟线程中异步执行任务
     *
     * @param task 要执行的任务
     * @param executorType 执行器类型
     */
    public static void executeAsync(Runnable task, ExecutorType executorType) {
        ExecutorService executor = switch (executorType) {
            case HTTP -> getHttpExecutor();
            case FILE_IO -> getFileIoExecutor();
            case PLUGIN -> getPluginExecutor();
            case DATABASE -> getDatabaseExecutor();
            case CACHE -> getCacheExecutor();
        };
        executor.execute(task);
    }

    /**
     * 执行器类型枚举
     */
    public enum ExecutorType {
        HTTP, FILE_IO, PLUGIN, DATABASE, CACHE
    }

    /**
     * 带监控的线程工厂
     */
    private static class InstrumentedThreadFactory implements ThreadFactory {
        private final ThreadFactory delegate;
        private final String categoryName;

        public InstrumentedThreadFactory(ThreadFactory delegate, String categoryName) {
            this.delegate = delegate;
            this.categoryName = categoryName;
        }

        @Override
        public Thread newThread(Runnable r) {
            totalVirtualThreadsCreated.incrementAndGet();
            return delegate.newThread(() -> {
                totalTasksExecuted.incrementAndGet();
                r.run();
            });
        }
    }

    /**
     * 虚拟线程统计信息
     */
    public static class VirtualThreadStats {
        private final int activeVirtualThreads;
        private final long totalVirtualThreadsCreated;
        private final long totalTasksExecuted;
        private final Map<String, Long> threadsByCategory;

        public VirtualThreadStats(int activeVirtualThreads, long totalVirtualThreadsCreated,
                                long totalTasksExecuted, Map<String, Long> threadsByCategory) {
            this.activeVirtualThreads = activeVirtualThreads;
            this.totalVirtualThreadsCreated = totalVirtualThreadsCreated;
            this.totalTasksExecuted = totalTasksExecuted;
            this.threadsByCategory = threadsByCategory;
        }

        public int getActiveVirtualThreads() { return activeVirtualThreads; }
        public long getTotalVirtualThreadsCreated() { return totalVirtualThreadsCreated; }
        public long getTotalTasksExecuted() { return totalTasksExecuted; }
        public Map<String, Long> getThreadsByCategory() { return threadsByCategory; }
    }

    /**
     * 创建一个基于虚拟线程的 ExecutorService
     *
     * <p>适用于 I/O 密集型任务，可以创建数百万个虚拟线程
     * 而不会像传统线程那样消耗大量内存
     *
     * @return 基于虚拟线程的 ExecutorService
     */
    public static ExecutorService newVirtualThreadExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }

    /**
     * 创建一个带名称前缀的基于虚拟线程的 ExecutorService
     *
     * @param namePrefix 虚拟线程名称前缀
     * @return 基于虚拟线程的 ExecutorService
     */
    public static ExecutorService newVirtualThreadExecutor(String namePrefix) {
        ThreadFactory factory = Thread.ofVirtual()
                .name(namePrefix, 0)
                .factory();
        return Executors.newThreadPerTaskExecutor(factory);
    }

    /**
     * 创建一个基于虚拟线程的 ScheduledExecutorService
     *
     * <p>注意：由于虚拟线程的特性，这里使用单线程调度器作为调度核心，
     * 但实际的任务执行会在虚拟线程中进行，提供了更好的并发性能
     *
     * @return 基于虚拟线程的 ScheduledExecutorService
     */
    public static ScheduledExecutorService newVirtualThreadScheduledExecutor() {
        return Executors.newSingleThreadScheduledExecutor(
            Thread.ofVirtual().name("VirtualThread-Scheduler").factory()
        );
    }

    /**
     * 创建一个带名称的基于虚拟线程的 ScheduledExecutorService
     *
     * @param schedulerName 调度器线程名称
     * @return 基于虚拟线程的 ScheduledExecutorService
     */
    public static ScheduledExecutorService newVirtualThreadScheduledExecutor(String schedulerName) {
        return Executors.newSingleThreadScheduledExecutor(
            Thread.ofVirtual().name(schedulerName).factory()
        );
    }

    /**
     * 创建一个多核心基于虚拟线程的 ScheduledExecutorService
     *
     * <p>使用多个调度核心提高调度性能，但每个任务仍在虚拟线程中执行
     *
     * @param corePoolSize 调度核心数量
     * @param namePrefix 调度器线程名称前缀
     * @return 基于虚拟线程的 ScheduledExecutorService
     */
    public static ScheduledExecutorService newVirtualThreadScheduledExecutor(int corePoolSize, String namePrefix) {
        return Executors.newScheduledThreadPool(
            corePoolSize,
            Thread.ofVirtual().name(namePrefix, 0).factory()
        );
    }

    /**
     * 创建虚拟线程 ThreadFactory
     *
     * @param namePrefix 线程名称前缀
     * @return 虚拟线程 ThreadFactory
     */
    public static ThreadFactory newVirtualThreadFactory(String namePrefix) {
        return Thread.ofVirtual()
                .name(namePrefix, 0)
                .factory();
    }

    /**
     * 直接创建并启动一个虚拟线程
     *
     * @param task 要执行的任务
     * @param threadName 线程名称
     * @return 创建的虚拟线程
     */
    public static Thread startVirtualThread(Runnable task, String threadName) {
        return Thread.ofVirtual()
                .name(threadName)
                .start(task);
    }

    /**
     * 直接创建并启动一个虚拟线程（系统自动命名）
     *
     * @param task 要执行的任务
     * @return 创建的虚拟线程
     */
    public static Thread startVirtualThread(Runnable task) {
        return Thread.ofVirtual().start(task);
    }

    /**
     * 检查当前线程是否为虚拟线程
     *
     * @return 如果当前线程是虚拟线程则返回 true
     */
    public static boolean isVirtualThread() {
        return Thread.currentThread().isVirtual();
    }

    /**
     * 检查指定线程是否为虚拟线程
     *
     * @param thread 要检查的线程
     * @return 如果指定线程是虚拟线程则返回 true
     */
    public static boolean isVirtualThread(Thread thread) {
        return thread.isVirtual();
    }
}