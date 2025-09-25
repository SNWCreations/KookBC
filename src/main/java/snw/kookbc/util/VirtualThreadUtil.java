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