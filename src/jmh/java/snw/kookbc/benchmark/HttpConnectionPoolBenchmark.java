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

import okhttp3.*;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import snw.kookbc.util.VirtualThreadUtil;

import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * HTTP 连接池优化性能基准测试
 *
 * <p>测试内容包括：
 * <ul>
 *   <li>连接池配置对并发性能的影响</li>
 *   <li>虚拟线程 vs 传统线程池性能</li>
 *   <li>HTTP/2 vs HTTP/1.1 性能对比</li>
 *   <li>连接复用效率测试</li>
 *   <li>大并发场景下的稳定性</li>
 * </ul>
 *
 * <p>对比基准：
 * <ul>
 *   <li>默认 OkHttp 配置</li>
 *   <li>优化后的连接池配置</li>
 *   <li>不同调度器策略</li>
 * </ul>
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 2, time = 3, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 3, time = 5, timeUnit = TimeUnit.SECONDS)
@Fork(1)
public class HttpConnectionPoolBenchmark {

    // ===== 测试配置 =====

    private static final String TEST_URL = "https://httpbin.org/json"; // 测试用的HTTP服务
    private static final String LARGE_RESPONSE_URL = "https://httpbin.org/bytes/10240"; // 10KB响应
    private static final int CONCURRENT_REQUESTS = 50; // 并发请求数

    // ===== HTTP 客户端配置 =====

    private OkHttpClient defaultClient;          // 默认配置客户端
    private OkHttpClient optimizedClient;        // 优化配置客户端
    private OkHttpClient virtualThreadClient;    // 虚拟线程客户端
    private OkHttpClient http2OnlyClient;        // 纯 HTTP/2 客户端

    @Setup
    public void setup() {
        // 默认 OkHttp 客户端
        defaultClient = new OkHttpClient.Builder()
                .connectTimeout(Duration.ofSeconds(10))
                .readTimeout(Duration.ofSeconds(30))
                .writeTimeout(Duration.ofSeconds(30))
                .build();

        // 优化配置的客户端
        ConnectionPool optimizedPool = new ConnectionPool(
            50,                             // 更大的连接池
            15,                             // 更长的连接存活时间
            TimeUnit.MINUTES
        );

        Dispatcher optimizedDispatcher = new Dispatcher();
        optimizedDispatcher.setMaxRequests(200);
        optimizedDispatcher.setMaxRequestsPerHost(50);

        optimizedClient = new OkHttpClient.Builder()
                .connectionPool(optimizedPool)
                .dispatcher(optimizedDispatcher)
                .connectTimeout(Duration.ofSeconds(10))
                .readTimeout(Duration.ofSeconds(45))
                .writeTimeout(Duration.ofSeconds(30))
                .callTimeout(Duration.ofMinutes(3))
                .retryOnConnectionFailure(true)
                .followRedirects(true)
                .protocols(Arrays.asList(Protocol.HTTP_2, Protocol.HTTP_1_1))
                .build();

        // 虚拟线程配置的客户端
        Dispatcher virtualThreadDispatcher = new Dispatcher(VirtualThreadUtil.getHttpExecutor());
        virtualThreadDispatcher.setMaxRequests(200);
        virtualThreadDispatcher.setMaxRequestsPerHost(50);

        virtualThreadClient = new OkHttpClient.Builder()
                .connectionPool(optimizedPool)
                .dispatcher(virtualThreadDispatcher)
                .connectTimeout(Duration.ofSeconds(10))
                .readTimeout(Duration.ofSeconds(45))
                .writeTimeout(Duration.ofSeconds(30))
                .callTimeout(Duration.ofMinutes(3))
                .retryOnConnectionFailure(true)
                .protocols(Arrays.asList(Protocol.HTTP_2, Protocol.HTTP_1_1))
                .build();

        // 纯 HTTP/2 客户端
        http2OnlyClient = new OkHttpClient.Builder()
                .connectionPool(optimizedPool)
                .dispatcher(virtualThreadDispatcher)
                .connectTimeout(Duration.ofSeconds(10))
                .readTimeout(Duration.ofSeconds(45))
                .writeTimeout(Duration.ofSeconds(30))
                .protocols(List.of(Protocol.HTTP_2)) // 仅使用 HTTP/2
                .build();
    }

    @TearDown
    public void tearDown() {
        // 清理资源
        if (defaultClient != null) {
            defaultClient.dispatcher().executorService().shutdown();
            defaultClient.connectionPool().evictAll();
        }
        if (optimizedClient != null) {
            optimizedClient.dispatcher().executorService().shutdown();
            optimizedClient.connectionPool().evictAll();
        }
        if (virtualThreadClient != null) {
            virtualThreadClient.dispatcher().executorService().shutdown();
            virtualThreadClient.connectionPool().evictAll();
        }
        if (http2OnlyClient != null) {
            http2OnlyClient.dispatcher().executorService().shutdown();
            http2OnlyClient.connectionPool().evictAll();
        }
    }

    // ===== 单个请求性能测试 =====

    @Benchmark
    public Response singleRequestDefault() throws IOException {
        Request request = new Request.Builder()
                .url(TEST_URL)
                .build();
        return defaultClient.newCall(request).execute();
    }

    @Benchmark
    public Response singleRequestOptimized() throws IOException {
        Request request = new Request.Builder()
                .url(TEST_URL)
                .build();
        return optimizedClient.newCall(request).execute();
    }

    @Benchmark
    public Response singleRequestVirtualThread() throws IOException {
        Request request = new Request.Builder()
                .url(TEST_URL)
                .build();
        return virtualThreadClient.newCall(request).execute();
    }

    // ===== 并发请求性能测试 =====

    @Benchmark
    public List<Response> concurrentRequestsDefault() {
        return executeConcurrentRequests(defaultClient, CONCURRENT_REQUESTS);
    }

    @Benchmark
    public List<Response> concurrentRequestsOptimized() {
        return executeConcurrentRequests(optimizedClient, CONCURRENT_REQUESTS);
    }

    @Benchmark
    public List<Response> concurrentRequestsVirtualThread() {
        return executeConcurrentRequests(virtualThreadClient, CONCURRENT_REQUESTS);
    }

    // ===== HTTP/2 vs HTTP/1.1 性能对比 =====

    @Benchmark
    public Response http2OnlyRequest() throws IOException {
        Request request = new Request.Builder()
                .url(TEST_URL)
                .build();
        return http2OnlyClient.newCall(request).execute();
    }

    @Benchmark
    public List<Response> http2ConcurrentRequests() {
        return executeConcurrentRequests(http2OnlyClient, CONCURRENT_REQUESTS);
    }

    // ===== 大响应数据处理测试 =====

    @Benchmark
    public Response largeResponseDefault() throws IOException {
        Request request = new Request.Builder()
                .url(LARGE_RESPONSE_URL)
                .build();
        return defaultClient.newCall(request).execute();
    }

    @Benchmark
    public Response largeResponseOptimized() throws IOException {
        Request request = new Request.Builder()
                .url(LARGE_RESPONSE_URL)
                .build();
        return optimizedClient.newCall(request).execute();
    }

    @Benchmark
    public Response largeResponseVirtualThread() throws IOException {
        Request request = new Request.Builder()
                .url(LARGE_RESPONSE_URL)
                .build();
        return virtualThreadClient.newCall(request).execute();
    }

    // ===== 连接复用效率测试 =====

    @Benchmark
    public List<Response> connectionReuseDefault() {
        // 使用相同的主机进行多次请求，测试连接复用
        return executeSequentialRequests(defaultClient, 10);
    }

    @Benchmark
    public List<Response> connectionReuseOptimized() {
        return executeSequentialRequests(optimizedClient, 10);
    }

    @Benchmark
    public List<Response> connectionReuseVirtualThread() {
        return executeSequentialRequests(virtualThreadClient, 10);
    }

    // ===== 异步请求性能测试 =====

    @Benchmark
    public List<String> asyncRequestsDefault() {
        return executeAsyncRequests(defaultClient, CONCURRENT_REQUESTS);
    }

    @Benchmark
    public List<String> asyncRequestsOptimized() {
        return executeAsyncRequests(optimizedClient, CONCURRENT_REQUESTS);
    }

    @Benchmark
    public List<String> asyncRequestsVirtualThread() {
        return executeAsyncRequests(virtualThreadClient, CONCURRENT_REQUESTS);
    }

    // ===== 混合负载测试 =====

    @Benchmark
    public MixedLoadResult mixedLoadDefault() {
        return executeMixedLoad(defaultClient);
    }

    @Benchmark
    public MixedLoadResult mixedLoadOptimized() {
        return executeMixedLoad(optimizedClient);
    }

    @Benchmark
    public MixedLoadResult mixedLoadVirtualThread() {
        return executeMixedLoad(virtualThreadClient);
    }

    // ===== 辅助方法 =====

    private List<Response> executeConcurrentRequests(OkHttpClient client, int count) {
        List<CompletableFuture<Response>> futures = IntStream.range(0, count)
                .mapToObj(i -> CompletableFuture.supplyAsync(() -> {
                    Request request = new Request.Builder()
                            .url(TEST_URL + "?id=" + i)
                            .build();
                    try {
                        return client.newCall(request).execute();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }, VirtualThreadUtil.getHttpExecutor()))
                .toList();

        return futures.stream()
                .map(CompletableFuture::join)
                .toList();
    }

    private List<Response> executeSequentialRequests(OkHttpClient client, int count) {
        return IntStream.range(0, count)
                .mapToObj(i -> {
                    Request request = new Request.Builder()
                            .url(TEST_URL + "?seq=" + i)
                            .build();
                    try {
                        return client.newCall(request).execute();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                })
                .toList();
    }

    private List<String> executeAsyncRequests(OkHttpClient client, int count) {
        List<CompletableFuture<String>> futures = IntStream.range(0, count)
                .mapToObj(i -> {
                    CompletableFuture<String> future = new CompletableFuture<>();
                    Request request = new Request.Builder()
                            .url(TEST_URL + "?async=" + i)
                            .build();

                    client.newCall(request).enqueue(new Callback() {
                        @Override
                        public void onFailure(Call call, IOException e) {
                            future.completeExceptionally(e);
                        }

                        @Override
                        public void onResponse(Call call, Response response) throws IOException {
                            try (ResponseBody body = response.body()) {
                                future.complete(body != null ? body.string() : "");
                            }
                        }
                    });

                    return future;
                })
                .toList();

        return futures.stream()
                .map(CompletableFuture::join)
                .toList();
    }

    private MixedLoadResult executeMixedLoad(OkHttpClient client) {
        long startTime = System.nanoTime();

        // 混合负载：同时执行不同类型的请求
        List<CompletableFuture<Void>> tasks = List.of(
            // 小请求高并发
            CompletableFuture.runAsync(() -> executeConcurrentRequests(client, 20), VirtualThreadUtil.getHttpExecutor()),

            // 大响应请求
            CompletableFuture.runAsync(() -> {
                try {
                    largeResponseOptimized();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }, VirtualThreadUtil.getHttpExecutor()),

            // 顺序请求（测试连接复用）
            CompletableFuture.runAsync(() -> executeSequentialRequests(client, 5), VirtualThreadUtil.getHttpExecutor())
        );

        CompletableFuture.allOf(tasks.toArray(new CompletableFuture[0])).join();

        long endTime = System.nanoTime();
        long duration = endTime - startTime;

        return new MixedLoadResult(
            duration / 1_000_000, // 转换为毫秒
            client.connectionPool().connectionCount(),
            client.connectionPool().idleConnectionCount()
        );
    }

    // ===== 结果类 =====

    public static class MixedLoadResult {
        public final long durationMs;
        public final int totalConnections;
        public final int idleConnections;

        public MixedLoadResult(long durationMs, int totalConnections, int idleConnections) {
            this.durationMs = durationMs;
            this.totalConnections = totalConnections;
            this.idleConnections = idleConnections;
        }

        @Override
        public String toString() {
            return String.format("MixedLoadResult{duration=%dms, total=%d, idle=%d}",
                    durationMs, totalConnections, idleConnections);
        }
    }

    /**
     * 运行 HTTP 连接池性能基准测试
     */
    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(HttpConnectionPoolBenchmark.class.getSimpleName())
                .forks(1)
                .build();

        new Runner(opt).run();
    }
}