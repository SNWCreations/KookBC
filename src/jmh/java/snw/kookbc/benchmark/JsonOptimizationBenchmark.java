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
import snw.kookbc.util.JsonCacheManager;
import snw.kookbc.util.JsonEngineSelector;
import snw.kookbc.util.JsonStreamProcessor;
import snw.kookbc.util.JacksonUtil;
import snw.kookbc.util.GsonUtil;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * JSON 处理优化性能基准测试
 *
 * <p>测试内容包括：
 * <ul>
 *   <li>智能引擎选择器性能</li>
 *   <li>缓存机制效果</li>
 *   <li>流式处理性能</li>
 *   <li>大数据量处理能力</li>
 * </ul>
 *
 * <p>对比基准：
 * <ul>
 *   <li>原始 Jackson/GSON 直接处理</li>
 *   <li>带缓存的智能处理</li>
 *   <li>流式处理 vs 全量处理</li>
 * </ul>
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 3, time = 2, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 3, timeUnit = TimeUnit.SECONDS)
@Fork(1)
public class JsonOptimizationBenchmark {

    // ===== 测试数据 =====

    private String simpleJson;
    private String apiResponseJson;
    private String cardMessageJson;
    private String largeArrayJson;
    private String complexEventJson;

    @Setup
    public void setup() {
        // 简单对象 JSON
        simpleJson = """
            {"id": "123", "name": "Test User", "active": true, "score": 95.5}
            """;

        // API 响应 JSON（典型的 Kook API 格式）
        apiResponseJson = """
            {
              "code": 0,
              "message": "success",
              "data": {
                "items": [
                  {"id": "1", "username": "user1", "roles": [1, 2]},
                  {"id": "2", "username": "user2", "roles": [2, 3]},
                  {"id": "3", "username": "user3", "roles": [1, 3]}
                ],
                "meta": {
                  "page": 1,
                  "pageTotal": 10,
                  "pageSize": 50,
                  "total": 500
                }
              }
            }
            """;

        // 卡片消息 JSON
        cardMessageJson = """
            {
              "type": "card",
              "theme": "primary",
              "size": "lg",
              "modules": [
                {
                  "type": "header",
                  "text": {
                    "type": "plain-text",
                    "content": "测试卡片消息"
                  }
                },
                {
                  "type": "section",
                  "text": {
                    "type": "kmarkdown",
                    "content": "这是一个**测试**的卡片消息"
                  }
                }
              ]
            }
            """;

        // 大型数组 JSON（模拟大量用户数据）
        StringBuilder largeArrayBuilder = new StringBuilder();
        largeArrayBuilder.append("{\"users\": [");
        for (int i = 0; i < 1000; i++) {
            if (i > 0) largeArrayBuilder.append(",");
            largeArrayBuilder.append(String.format(
                "{\"id\":\"%d\",\"username\":\"user%d\",\"email\":\"user%d@example.com\",\"active\":%s,\"roles\":[%d,%d]}",
                i, i, i, i % 2 == 0 ? "true" : "false", i % 3 + 1, (i + 1) % 3 + 1
            ));
        }
        largeArrayBuilder.append("]}");
        largeArrayJson = largeArrayBuilder.toString();

        // 复杂事件 JSON
        complexEventJson = """
            {
              "s": 0,
              "d": {
                "channel_type": "GROUP",
                "type": 1,
                "target_id": "7404679802432328",
                "author_id": "2862900000",
                "content": "Hello from performance test!",
                "msg_id": "benchmark-test-message-id",
                "msg_timestamp": 1640995200000,
                "nonce": "",
                "extra": {
                  "type": 1,
                  "guild_id": "7404679802432328",
                  "channel_name": "性能测试频道",
                  "mention": [],
                  "mention_all": false,
                  "mention_roles": [],
                  "mention_here": false,
                  "author": {
                    "id": "2862900000",
                    "username": "BenchmarkBot",
                    "identify_num": "0001",
                    "online": true,
                    "avatar": "https://img.kook.cn/avatars/benchmark.jpg",
                    "roles": [1, 2, 3],
                    "status": {
                      "is_playing": false,
                      "is_music": false,
                      "game": null
                    },
                    "badges": [
                      {"id": 1, "name": "Developer", "icon": "dev.png"},
                      {"id": 2, "name": "Tester", "icon": "test.png"}
                    ]
                  }
                }
              }
            }
            """;

        // 预热缓存
        JsonCacheManager.clearAllCaches();
    }

    // ===== 智能引擎选择器性能测试 =====

    @Benchmark
    public Object smartEngineSimpleObject() {
        return JsonEngineSelector.parseJson(simpleJson);
    }

    @Benchmark
    public Object smartEngineApiResponse() {
        return JsonEngineSelector.parseJson(apiResponseJson);
    }

    @Benchmark
    public Object smartEngineCardMessage() {
        return JsonEngineSelector.parseJson(cardMessageJson);
    }

    @Benchmark
    public Object smartEngineComplexEvent() {
        return JsonEngineSelector.parseJson(complexEventJson);
    }

    // ===== 直接引擎对比测试 =====

    @Benchmark
    public Object directJacksonSimple() {
        return JacksonUtil.parse(simpleJson);
    }

    @Benchmark
    public Object directGsonSimple() {
        return GsonUtil.NORMAL_GSON.fromJson(simpleJson, com.google.gson.JsonObject.class);
    }

    @Benchmark
    public Object directJacksonApiResponse() {
        return JacksonUtil.parse(apiResponseJson);
    }

    @Benchmark
    public Object directGsonApiResponse() {
        return GsonUtil.NORMAL_GSON.fromJson(apiResponseJson, com.google.gson.JsonObject.class);
    }

    // ===== 缓存机制性能测试 =====

    @Benchmark
    public Object cachedParsingFirstTime() {
        // 清理缓存，模拟首次解析
        JsonCacheManager.clearParseCache();
        return JsonCacheManager.parseWithCache(complexEventJson);
    }

    @Benchmark
    public Object cachedParsingSecondTime() {
        // 不清理缓存，模拟缓存命中
        return JsonCacheManager.parseWithCache(complexEventJson);
    }

    @Benchmark
    public Object cachedSerializationFirstTime() {
        JsonCacheManager.clearSerializeCache();
        TestObject obj = new TestObject("test", 123, true);
        return JsonCacheManager.serializeWithCache(obj);
    }

    @Benchmark
    public Object cachedSerializationSecondTime() {
        TestObject obj = new TestObject("test", 123, true);
        return JsonCacheManager.serializeWithCache(obj);
    }

    // ===== 流式处理性能测试 =====

    @Benchmark
    public List<String> streamProcessingUsernames() {
        return JsonStreamProcessor.extractFieldValues(largeArrayJson, "users", "username");
    }

    @Benchmark
    public List<Object> streamProcessingActiveUsers() {
        return JsonStreamProcessor.parseArray(
            largeArrayJson,
            "users",
            user -> user.get("active").asBoolean(),
            user -> user.get("username").asText()
        );
    }

    @Benchmark
    public long streamCountingActiveUsers() {
        return JsonStreamProcessor.countArrayElements(
            largeArrayJson,
            "users",
            user -> user.get("active").asBoolean()
        );
    }

    // ===== 传统全量处理对比 =====

    @Benchmark
    public List<String> traditionalProcessingUsernames() {
        var root = JacksonUtil.parse(largeArrayJson);
        var users = root.get("users");
        var result = new java.util.ArrayList<String>();

        for (var user : users) {
            if (user.has("username")) {
                result.add(user.get("username").asText());
            }
        }
        return result;
    }

    @Benchmark
    public List<String> traditionalProcessingActiveUsers() {
        var root = JacksonUtil.parse(largeArrayJson);
        var users = root.get("users");
        var result = new java.util.ArrayList<String>();

        for (var user : users) {
            if (user.get("active").asBoolean()) {
                result.add(user.get("username").asText());
            }
        }
        return result;
    }

    // ===== 混合场景性能测试 =====

    @Benchmark
    public String mixedScenarioTest() {
        // 模拟真实使用场景：解析 + 处理 + 序列化

        // 1. 解析 API 响应
        var apiData = JsonEngineSelector.parseJson(apiResponseJson);

        // 2. 流式处理大数据
        var activeUsers = JsonStreamProcessor.parseArray(
            largeArrayJson,
            "users",
            user -> user.get("active").asBoolean(),
            user -> user.get("id").asText()
        );

        // 3. 构建结果对象并序列化
        var result = new MixedScenarioResult(
            apiData.get("code").asInt(),
            activeUsers.size(),
            activeUsers
        );

        return JsonEngineSelector.toJson(result);
    }

    // ===== 内存效率测试（模拟大数据场景）=====

    @Benchmark
    public long largeDataMemoryEfficiencyStream() {
        // 创建更大的测试数据
        String largeData = generateLargeJsonArray(5000);

        // 流式处理，内存占用低
        return JsonStreamProcessor.countArrayElements(
            largeData,
            "users",
            user -> user.get("active").asBoolean()
        );
    }

    @Benchmark
    public long largeDataMemoryEfficiencyTraditional() {
        // 创建更大的测试数据
        String largeData = generateLargeJsonArray(5000);

        // 传统处理，需要加载全部数据到内存
        var root = JacksonUtil.parse(largeData);
        var users = root.get("users");
        long count = 0;

        for (var user : users) {
            if (user.get("active").asBoolean()) {
                count++;
            }
        }
        return count;
    }

    // ===== 辅助方法和测试类 =====

    private String generateLargeJsonArray(int size) {
        StringBuilder builder = new StringBuilder();
        builder.append("{\"users\": [");
        for (int i = 0; i < size; i++) {
            if (i > 0) builder.append(",");
            builder.append(String.format(
                "{\"id\":\"%d\",\"username\":\"user%d\",\"active\":%s}",
                i, i, i % 3 == 0 ? "true" : "false"
            ));
        }
        builder.append("]}");
        return builder.toString();
    }

    public static class TestObject {
        public String name;
        public int value;
        public boolean active;

        public TestObject(String name, int value, boolean active) {
            this.name = name;
            this.value = value;
            this.active = active;
        }
    }

    public static class MixedScenarioResult {
        public int code;
        public int activeUserCount;
        public List<String> activeUserIds;

        public MixedScenarioResult(int code, int activeUserCount, List<String> activeUserIds) {
            this.code = code;
            this.activeUserCount = activeUserCount;
            this.activeUserIds = activeUserIds;
        }
    }

    /**
     * 运行 JSON 优化性能基准测试
     */
    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(JsonOptimizationBenchmark.class.getSimpleName())
                .forks(1)
                .build();

        new Runner(opt).run();
    }
}