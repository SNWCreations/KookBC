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

import com.fasterxml.jackson.databind.JsonNode;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import snw.kookbc.util.JacksonUtil;
import snw.kookbc.util.VirtualThreadUtil;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * JMH 基准测试 - KookBC 系统整体性能测试
 *
 * 模拟真实的 KookBC 工作负载：
 * 1. 高并发事件处理
 * 2. JSON 解析和处理
 * 3. 虚拟线程调度器性能
 * 4. 混合工作负载测试
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 3, time = 3, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 5, timeUnit = TimeUnit.SECONDS)
@Fork(1)
public class SystemPerformanceBenchmark {

    private static final int EVENT_COUNT = 500;

    // 执行器
    private ExecutorService virtualExecutor;
    private ExecutorService platformExecutor;
    private ScheduledExecutorService virtualScheduler;
    private ScheduledExecutorService platformScheduler;

    // 测试数据
    private String[] testEvents;
    private AtomicInteger processedEvents;

    @Setup
    public void setup() {
        // 初始化执行器
        virtualExecutor = VirtualThreadUtil.newVirtualThreadExecutor("System-Virtual");
        platformExecutor = Executors.newFixedThreadPool(
            Runtime.getRuntime().availableProcessors() * 2,
            r -> new Thread(r, "System-Platform"));

        virtualScheduler = VirtualThreadUtil.newVirtualThreadScheduledExecutor("System-Virtual-Scheduler");
        platformScheduler = Executors.newScheduledThreadPool(
            Runtime.getRuntime().availableProcessors(),
            r -> new Thread(r, "System-Platform-Scheduler"));

        // 初始化测试数据
        processedEvents = new AtomicInteger(0);
        generateTestEvents();
    }

    @TearDown
    public void teardown() {
        shutdownExecutor(virtualExecutor);
        shutdownExecutor(platformExecutor);
        shutdownExecutor(virtualScheduler);
        shutdownExecutor(platformScheduler);
    }

    private void shutdownExecutor(ExecutorService executor) {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    private void generateTestEvents() {
        testEvents = new String[EVENT_COUNT];

        String[] eventTemplates = {
            // 文本消息事件
            """
            {"s":0,"d":{"channel_type":"GROUP","type":1,"target_id":"7404679802432328","author_id":"2862900000","content":"Hello %s!","msg_id":"msg-%d","msg_timestamp":%d,"extra":{"type":1,"guild_id":"7404679802432328","author":{"id":"2862900000","username":"User%d"}}}}
            """,
            // 语音频道事件
            """
            {"s":0,"d":{"channel_type":"GROUP","type":9,"target_id":"7404679802432328","author_id":"2862900000","extra":{"type":9,"guild_id":"7404679802432328","body":{"user_id":"2862900000","joined_channel":[{"id":"channel-%d","name":"Voice %d"}]}}}}
            """,
            // 用户加入事件
            """
            {"s":0,"d":{"channel_type":"PERSON","type":255,"extra":{"type":"joined_guild","body":{"user_id":"user-%d","joined_at":%d,"guild_id":"guild-%d"}}}}
            """
        };

        for (int i = 0; i < EVENT_COUNT; i++) {
            String template = eventTemplates[i % eventTemplates.length];
            long timestamp = System.currentTimeMillis() - (i * 1000L);

            testEvents[i] = String.format(template,
                "World", i, timestamp, i % 100, i, i % 10, i, timestamp, i);
        }
    }

    /**
     * 基准测试：虚拟线程 - 完整事件处理流程（Jackson）
     */
    @Benchmark
    public void virtualThreadEventProcessingJackson() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(EVENT_COUNT);
        processedEvents.set(0);

        for (String eventJson : testEvents) {
            virtualExecutor.submit(() -> {
                try {
                    processEventWithJackson(eventJson);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
    }

    /**
     * 基准测试：传统线程 - 完整事件处理流程（Jackson）
     */
    @Benchmark
    public void platformThreadEventProcessingJackson() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(EVENT_COUNT);
        processedEvents.set(0);

        for (String eventJson : testEvents) {
            platformExecutor.submit(() -> {
                try {
                    processEventWithJackson(eventJson);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
    }

    /**
     * 基准测试：虚拟线程 - 完整事件处理流程（Gson）
     */
    @Benchmark
    public void virtualThreadEventProcessingGson() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(EVENT_COUNT);
        processedEvents.set(0);

        for (String eventJson : testEvents) {
            virtualExecutor.submit(() -> {
                try {
                    processEventWithGson(eventJson);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
    }

    /**
     * 基准测试：传统线程 - 完整事件处理流程（Gson）
     */
    @Benchmark
    public void platformThreadEventProcessingGson() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(EVENT_COUNT);
        processedEvents.set(0);

        for (String eventJson : testEvents) {
            platformExecutor.submit(() -> {
                try {
                    processEventWithGson(eventJson);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
    }

    /**
     * 基准测试：虚拟线程调度器性能
     */
    @Benchmark
    public void virtualThreadSchedulerPerformance() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(EVENT_COUNT);

        for (int i = 0; i < EVENT_COUNT; i++) {
            // 随机延迟 0-10ms
            int delay = i % 10;
            virtualScheduler.schedule(() -> {
                try {
                    // 模拟定时任务
                    processScheduledTask();
                } finally {
                    latch.countDown();
                }
            }, delay, TimeUnit.MILLISECONDS);
        }

        latch.await();
    }

    /**
     * 基准测试：传统线程调度器性能
     */
    @Benchmark
    public void platformThreadSchedulerPerformance() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(EVENT_COUNT);

        for (int i = 0; i < EVENT_COUNT; i++) {
            // 随机延迟 0-10ms
            int delay = i % 10;
            platformScheduler.schedule(() -> {
                try {
                    // 模拟定时任务
                    processScheduledTask();
                } finally {
                    latch.countDown();
                }
            }, delay, TimeUnit.MILLISECONDS);
        }

        latch.await();
    }

    /**
     * 基准测试：混合工作负载 - 虚拟线程（事件处理 + 定时任务）
     */
    @Benchmark
    public void virtualThreadMixedWorkload() throws InterruptedException {
        CountDownLatch eventLatch = new CountDownLatch(EVENT_COUNT / 2);
        CountDownLatch schedulerLatch = new CountDownLatch(EVENT_COUNT / 2);

        // 事件处理任务
        for (int i = 0; i < EVENT_COUNT / 2; i++) {
            String eventJson = testEvents[i];
            virtualExecutor.submit(() -> {
                try {
                    processEventWithJackson(eventJson);
                } finally {
                    eventLatch.countDown();
                }
            });
        }

        // 调度任务
        for (int i = 0; i < EVENT_COUNT / 2; i++) {
            int delay = i % 5;
            virtualScheduler.schedule(() -> {
                try {
                    processScheduledTask();
                } finally {
                    schedulerLatch.countDown();
                }
            }, delay, TimeUnit.MILLISECONDS);
        }

        eventLatch.await();
        schedulerLatch.await();
    }

    /**
     * 基准测试：混合工作负载 - 传统线程（事件处理 + 定时任务）
     */
    @Benchmark
    public void platformThreadMixedWorkload() throws InterruptedException {
        CountDownLatch eventLatch = new CountDownLatch(EVENT_COUNT / 2);
        CountDownLatch schedulerLatch = new CountDownLatch(EVENT_COUNT / 2);

        // 事件处理任务
        for (int i = 0; i < EVENT_COUNT / 2; i++) {
            String eventJson = testEvents[i];
            platformExecutor.submit(() -> {
                try {
                    processEventWithJackson(eventJson);
                } finally {
                    eventLatch.countDown();
                }
            });
        }

        // 调度任务
        for (int i = 0; i < EVENT_COUNT / 2; i++) {
            int delay = i % 5;
            platformScheduler.schedule(() -> {
                try {
                    processScheduledTask();
                } finally {
                    schedulerLatch.countDown();
                }
            }, delay, TimeUnit.MILLISECONDS);
        }

        eventLatch.await();
        schedulerLatch.await();
    }

    // ========== 事件处理方法 ==========

    private void processEventWithJackson(String eventJson) {
        try {
            JsonNode root = JacksonUtil.parse(eventJson);
            JsonNode data = JacksonUtil.get(root, "d");

            // 解析事件类型
            int type = JacksonUtil.get(data, "type").asInt();
            String channelType = JacksonUtil.get(data, "channel_type").asText();

            // 模拟业务逻辑处理
            if (type == 1) { // 文本消息
                String content = JacksonUtil.get(data, "content").asText();
                if (content.length() > 0) {
                    processedEvents.incrementAndGet();
                }
            } else if (type == 9) { // 语音频道事件
                JsonNode extra = JacksonUtil.get(data, "extra");
                if (JacksonUtil.has(extra, "body")) {
                    processedEvents.incrementAndGet();
                }
            }

            // 模拟数据库操作延迟
            Thread.sleep(1);
        } catch (Exception e) {
            // 忽略错误，专注性能测试
        }
    }

    private void processEventWithGson(String eventJson) {
        try {
            JsonNode root = JacksonUtil.parse(eventJson);
            JsonNode data = JacksonUtil.get(root, "d");

            // 解析事件类型
            int type = JacksonUtil.getIntOrDefault(data, "type", 0);
            String channelType = JacksonUtil.getStringOrDefault(data, "channel_type", "");

            // 模拟业务逻辑处理
            if (type == 1) { // 文本消息
                String content = JacksonUtil.getStringOrDefault(data, "content", "");
                if (content.length() > 0) {
                    processedEvents.incrementAndGet();
                }
            } else if (type == 9) { // 语音频道事件
                JsonNode extra = JacksonUtil.get(data, "extra");
                if (JacksonUtil.has(extra, "body")) {
                    processedEvents.incrementAndGet();
                }
            }

            // 模拟数据库操作延迟
            Thread.sleep(1);
        } catch (Exception e) {
            // 忽略错误，专注性能测试
        }
    }

    private void processScheduledTask() {
        try {
            // 模拟定时任务：心跳检测、状态更新等
            long currentTime = System.currentTimeMillis();

            // 模拟一些计算
            double result = Math.sin(currentTime) * Math.cos(currentTime);

            // 模拟状态检查
            if (result > -1.0) {
                processedEvents.incrementAndGet();
            }
        } catch (Exception e) {
            // 忽略错误
        }
    }

    /**
     * 运行系统性能基准测试
     */
    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(SystemPerformanceBenchmark.class.getSimpleName())
                .forks(1)
                .jvmArgs("-Xmx2g", "-Xms1g") // 设置合适的堆内存
                .build();

        new Runner(opt).run();
    }
}