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

package snw.kookbc.benchmark;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import snw.kookbc.util.VirtualThreadUtil;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * JMH 基准测试 - 虚拟线程 vs 传统线程性能对比
 *
 * 测试场景：
 * 1. 高并发任务执行
 * 2. I/O 密集型操作模拟
 * 3. 线程创建和销毁开销
 * 4. 内存使用效率
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 3, time = 2, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 3, timeUnit = TimeUnit.SECONDS)
@Fork(1)
public class VirtualThreadBenchmark {

    private static final int TASK_COUNT = 1000;
    private ExecutorService virtualThreadExecutor;
    private ExecutorService platformThreadExecutor;
    private ExecutorService cachedThreadPool;

    @Setup
    public void setup() {
        virtualThreadExecutor = VirtualThreadUtil.newVirtualThreadExecutor("Benchmark-Virtual");
        platformThreadExecutor = Executors.newFixedThreadPool(
            Runtime.getRuntime().availableProcessors() * 2);
        cachedThreadPool = Executors.newCachedThreadPool();
    }

    @TearDown
    public void teardown() {
        shutdownExecutor(virtualThreadExecutor);
        shutdownExecutor(platformThreadExecutor);
        shutdownExecutor(cachedThreadPool);
    }

    private void shutdownExecutor(ExecutorService executor) {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 基准测试：虚拟线程 - CPU 密集型任务
     */
    @Benchmark
    public void virtualThreadCpuIntensive() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(TASK_COUNT);

        for (int i = 0; i < TASK_COUNT; i++) {
            virtualThreadExecutor.submit(() -> {
                try {
                    // 模拟 CPU 密集型计算
                    long sum = 0;
                    for (int j = 0; j < 10000; j++) {
                        sum += Math.sqrt(j) * Math.sin(j);
                    }
                    // 防止编译器优化
                    if (sum == Long.MIN_VALUE) {
                        System.out.println("Unlikely result");
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
    }

    /**
     * 基准测试：传统线程池 - CPU 密集型任务
     */
    @Benchmark
    public void platformThreadCpuIntensive() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(TASK_COUNT);

        for (int i = 0; i < TASK_COUNT; i++) {
            platformThreadExecutor.submit(() -> {
                try {
                    // 模拟 CPU 密集型计算
                    long sum = 0;
                    for (int j = 0; j < 10000; j++) {
                        sum += Math.sqrt(j) * Math.sin(j);
                    }
                    // 防止编译器优化
                    if (sum == Long.MIN_VALUE) {
                        System.out.println("Unlikely result");
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
    }

    /**
     * 基准测试：虚拟线程 - I/O 密集型任务
     */
    @Benchmark
    public void virtualThreadIoIntensive() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(TASK_COUNT);

        for (int i = 0; i < TASK_COUNT; i++) {
            virtualThreadExecutor.submit(() -> {
                try {
                    // 模拟 I/O 等待（虚拟线程的优势场景）
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
    }

    /**
     * 基准测试：传统线程池 - I/O 密集型任务
     */
    @Benchmark
    public void platformThreadIoIntensive() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(TASK_COUNT);

        for (int i = 0; i < TASK_COUNT; i++) {
            platformThreadExecutor.submit(() -> {
                try {
                    // 模拟 I/O 等待
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
    }

    /**
     * 基准测试：虚拟线程 - 线程创建开销
     */
    @Benchmark
    public void virtualThreadCreation() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(TASK_COUNT);

        for (int i = 0; i < TASK_COUNT; i++) {
            Thread.ofVirtual()
                .name("benchmark-virtual-" + i)
                .start(() -> {
                    try {
                        // 简单任务，测试线程创建开销
                        Math.random();
                    } finally {
                        latch.countDown();
                    }
                });
        }

        latch.await();
    }

    /**
     * 基准测试：传统线程 - 线程创建开销
     */
    @Benchmark
    public void platformThreadCreation() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(TASK_COUNT);

        for (int i = 0; i < TASK_COUNT; i++) {
            Thread thread = new Thread(() -> {
                try {
                    // 简单任务，测试线程创建开销
                    Math.random();
                } finally {
                    latch.countDown();
                }
            }, "benchmark-platform-" + i);
            thread.start();
        }

        latch.await();
    }

    /**
     * 基准测试：虚拟线程 - 高并发场景（事件处理模拟）
     */
    @Benchmark
    public void virtualThreadEventProcessing() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(TASK_COUNT);
        AtomicInteger eventCounter = new AtomicInteger(0);

        for (int i = 0; i < TASK_COUNT; i++) {
            virtualThreadExecutor.submit(() -> {
                try {
                    // 模拟事件处理：JSON 解析、业务逻辑、数据存储
                    String jsonData = "{\"type\":\"message\",\"content\":\"test\",\"timestamp\":" + System.currentTimeMillis() + "}";

                    // 模拟 JSON 解析开销
                    boolean isValid = jsonData.contains("message") && jsonData.length() > 10;

                    // 模拟业务逻辑处理
                    if (isValid) {
                        eventCounter.incrementAndGet();
                    }

                    // 模拟短暂的 I/O 等待（数据库写入等）
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
    }

    /**
     * 基准测试：传统线程池 - 高并发场景（事件处理模拟）
     */
    @Benchmark
    public void platformThreadEventProcessing() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(TASK_COUNT);
        AtomicInteger eventCounter = new AtomicInteger(0);

        for (int i = 0; i < TASK_COUNT; i++) {
            platformThreadExecutor.submit(() -> {
                try {
                    // 模拟事件处理：JSON 解析、业务逻辑、数据存储
                    String jsonData = "{\"type\":\"message\",\"content\":\"test\",\"timestamp\":" + System.currentTimeMillis() + "}";

                    // 模拟 JSON 解析开销
                    boolean isValid = jsonData.contains("message") && jsonData.length() > 10;

                    // 模拟业务逻辑处理
                    if (isValid) {
                        eventCounter.incrementAndGet();
                    }

                    // 模拟短暂的 I/O 等待（数据库写入等）
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
    }

    /**
     * 运行所有基准测试
     */
    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(VirtualThreadBenchmark.class.getSimpleName())
                .forks(1)
                .build();

        new Runner(opt).run();
    }
}