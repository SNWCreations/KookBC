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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import snw.kookbc.util.GsonUtil;
import snw.kookbc.util.JacksonUtil;

import java.util.concurrent.TimeUnit;

/**
 * JMH 基准测试 - Jackson vs Gson JSON 处理性能对比
 *
 * 测试场景：
 * 1. JSON 解析性能
 * 2. JSON 序列化性能
 * 3. 复杂嵌套对象处理
 * 4. 大数据量处理
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 3, time = 2, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 3, timeUnit = TimeUnit.SECONDS)
@Fork(1)
public class JsonProcessingBenchmark {

    // 测试 JSON 数据
    private String simpleJson;
    private String complexJson;
    private String arrayJson;
    private String kookEventJson;

    // JSON 处理器
    private ObjectMapper jacksonMapper;
    private JsonParser gsonParser;

    // 测试对象
    private TestMessage testMessage;

    @Setup
    public void setup() {
        // 初始化 JSON 处理器
        jacksonMapper = JacksonUtil.getMapper();
        gsonParser = new JsonParser();

        // 简单 JSON
        simpleJson = "{\"id\":\"123\",\"content\":\"Hello World\",\"timestamp\":1640995200000}";

        // 复杂嵌套 JSON
        complexJson = """
                {
                  "s": 0,
                  "d": {
                    "channel_type": "GROUP",
                    "type": 1,
                    "target_id": "7404679802432328",
                    "author_id": "2862900000",
                    "content": "Hello from KookBC!",
                    "msg_id": "f96a5a08-c6c3-4df4-acd1-e14d1b70000",
                    "msg_timestamp": 1640995200000,
                    "nonce": "",
                    "extra": {
                      "type": 1,
                      "guild_id": "7404679802432328",
                      "channel_name": "文字频道",
                      "mention": [],
                      "mention_all": false,
                      "mention_roles": [],
                      "mention_here": false,
                      "author": {
                        "id": "2862900000",
                        "username": "KookBot",
                        "identify_num": "0001",
                        "online": false,
                        "avatar": "https://img.kook.cn/avatars/2020-02/xxxx.jpg",
                        "roles": [1, 2, 3]
                      }
                    }
                  }
                }
                """;

        // 数组 JSON
        arrayJson = """
                [
                  {"id": "1", "name": "User1", "roles": [1, 2]},
                  {"id": "2", "name": "User2", "roles": [2, 3]},
                  {"id": "3", "name": "User3", "roles": [1, 3]},
                  {"id": "4", "name": "User4", "roles": [1, 2, 3]},
                  {"id": "5", "name": "User5", "roles": [2]}
                ]
                """;

        // 真实的 Kook 事件 JSON（缩减版）
        kookEventJson = """
                {
                  "s": 0,
                  "d": {
                    "channel_type": "GROUP",
                    "type": 9,
                    "target_id": "7404679802432328",
                    "author_id": "2862900000",
                    "content": "",
                    "msg_id": "f96a5a08-c6c3-4df4-acd1-e14d1b70000",
                    "msg_timestamp": 1640995200000,
                    "nonce": "",
                    "extra": {
                      "type": 9,
                      "guild_id": "7404679802432328",
                      "channel_name": "语音频道",
                      "mention": [],
                      "mention_all": false,
                      "mention_roles": [],
                      "mention_here": false,
                      "body": {
                        "user_id": "2862900000",
                        "joined_channel": [
                          {
                            "id": "7404679802432328",
                            "name": "语音频道",
                            "user_limit": 0,
                            "voice_quality": "2"
                          }
                        ],
                        "exited_channel": [],
                        "joined_channel_users": [
                          ["2862900000"]
                        ],
                        "exited_channel_users": []
                      }
                    }
                  }
                }
                """;

        // 测试对象
        testMessage = new TestMessage();
        testMessage.id = "test-123";
        testMessage.content = "This is a test message for serialization benchmark";
        testMessage.timestamp = System.currentTimeMillis();
        testMessage.author = new TestAuthor();
        testMessage.author.id = "user-456";
        testMessage.author.username = "TestUser";
        testMessage.author.roles = new int[]{1, 2, 3};
    }

    // ========== Jackson 基准测试 ==========

    @Benchmark
    public JsonNode jacksonParseSimple() throws Exception {
        return jacksonMapper.readTree(simpleJson);
    }

    @Benchmark
    public JsonNode jacksonParseComplex() throws Exception {
        return jacksonMapper.readTree(complexJson);
    }

    @Benchmark
    public JsonNode jacksonParseArray() throws Exception {
        return jacksonMapper.readTree(arrayJson);
    }

    @Benchmark
    public JsonNode jacksonParseKookEvent() throws Exception {
        return jacksonMapper.readTree(kookEventJson);
    }

    @Benchmark
    public String jacksonSerialize() throws Exception {
        return jacksonMapper.writeValueAsString(testMessage);
    }

    @Benchmark
    public TestMessage jacksonDeserialize() throws Exception {
        String json = jacksonMapper.writeValueAsString(testMessage);
        return jacksonMapper.readValue(json, TestMessage.class);
    }

    // ========== Gson 基准测试 ==========

    @Benchmark
    public JsonObject gsonParseSimple() {
        return gsonParser.parse(simpleJson).getAsJsonObject();
    }

    @Benchmark
    public JsonObject gsonParseComplex() {
        return gsonParser.parse(complexJson).getAsJsonObject();
    }

    @Benchmark
    public JsonObject gsonParseArray() {
        return gsonParser.parse(arrayJson).getAsJsonObject();
    }

    @Benchmark
    public JsonObject gsonParseKookEvent() {
        return gsonParser.parse(kookEventJson).getAsJsonObject();
    }

    @Benchmark
    public String gsonSerialize() {
        return GsonUtil.NORMAL_GSON.toJson(testMessage);
    }

    @Benchmark
    public TestMessage gsonDeserialize() {
        String json = GsonUtil.NORMAL_GSON.toJson(testMessage);
        return GsonUtil.NORMAL_GSON.fromJson(json, TestMessage.class);
    }

    // ========== 工具方法性能测试 ==========

    @Benchmark
    public String jacksonUtilGetString() throws Exception {
        JsonNode node = jacksonMapper.readTree(complexJson);
        return JacksonUtil.get(node.get("d"), "content").asText();
    }

    @Benchmark
    public String gsonUtilGetString() {
        JsonObject obj = gsonParser.parse(complexJson).getAsJsonObject();
        JsonObject d = GsonUtil.get(obj, "d").getAsJsonObject();
        return GsonUtil.getAsString(d, "content");
    }

    @Benchmark
    public boolean jacksonUtilHas() throws Exception {
        JsonNode node = jacksonMapper.readTree(complexJson);
        return JacksonUtil.has(node.get("d"), "extra");
    }

    @Benchmark
    public boolean gsonUtilHas() {
        JsonObject obj = gsonParser.parse(complexJson).getAsJsonObject();
        JsonObject d = GsonUtil.get(obj, "d").getAsJsonObject();
        return GsonUtil.has(d, "extra");
    }

    // ========== 测试数据类 ==========

    public static class TestMessage {
        public String id;
        public String content;
        public long timestamp;
        public TestAuthor author;
    }

    public static class TestAuthor {
        public String id;
        public String username;
        public int[] roles;
    }

    /**
     * 运行 JSON 处理基准测试
     */
    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(JsonProcessingBenchmark.class.getSimpleName())
                .forks(1)
                .build();

        new Runner(opt).run();
    }
}