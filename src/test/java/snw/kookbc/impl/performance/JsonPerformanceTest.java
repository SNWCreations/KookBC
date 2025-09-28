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

package snw.kookbc.impl.performance;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import snw.kookbc.util.JacksonUtil;
import snw.kookbc.util.GsonUtil;

import java.io.IOException;

/**
 * Jackson vs Gson 性能对比测试
 * 验证 JSON 解析性能提升效果
 */
public class JsonPerformanceTest {

    // 模拟复杂的API响应数据
    private static final String COMPLEX_JSON = """
        {
            "code": 0,
            "message": "success",
            "data": {
                "items": [
                    {
                        "id": "123456789",
                        "username": "testuser",
                        "identify_num": "1234",
                        "bot": false,
                        "status": 1,
                        "is_vip": true,
                        "avatar": "https://img.kaiheila.cn/avatar.jpg",
                        "vip_avatar": "https://img.kaiheila.cn/vip_avatar.jpg"
                    },
                    {
                        "id": "987654321",
                        "username": "anotheruser",
                        "identify_num": "5678",
                        "bot": true,
                        "status": 1,
                        "is_vip": false,
                        "avatar": "https://img.kaiheila.cn/bot_avatar.jpg",
                        "vip_avatar": ""
                    }
                ],
                "meta": {
                    "page": 1,
                    "page_total": 10,
                    "page_size": 50,
                    "total": 500
                }
            }
        }
        """;

    private static final int ITERATIONS = 10000;

    @Test
    public void testJacksonPerformance() {
        System.out.println("=== Jackson vs Gson 性能对比测试 ===\n");

        // 预热
        warmup();

        // Jackson 性能测试
        long jacksonTime = testJacksonParsing();

        // Gson 性能测试
        long gsonTime = testGsonParsing();

        // 结果分析
        analyzeResults(jacksonTime, gsonTime);
    }

    private void warmup() {
        System.out.println("🔥 预热阶段 (1000次)...");
        for (int i = 0; i < 1000; i++) {
            try {
                JacksonUtil.parse(COMPLEX_JSON);
                JsonParser.parseString(COMPLEX_JSON);
            } catch (Exception ignored) {
            }
        }
        System.out.println("✅ 预热完成\n");
    }

    private long testJacksonParsing() {
        System.out.println("🚀 Jackson 性能测试 (" + ITERATIONS + "次)...");

        long startTime = System.nanoTime();

        for (int i = 0; i < ITERATIONS; i++) {
            try {
                JsonNode root = JacksonUtil.parse(COMPLEX_JSON);
                JsonNode data = root.get("data");
                JsonNode items = data.get("items");

                for (JsonNode item : items) {
                    String id = item.get("id").asText();
                    String username = item.get("username").asText();
                    boolean isBot = item.get("bot").asBoolean();
                    boolean isVip = item.get("is_vip").asBoolean();
                }

                JsonNode meta = data.get("meta");
                int page = meta.get("page").asInt();
                int pageTotal = meta.get("page_total").asInt();

            } catch (Exception e) {
                System.err.println("Jackson 解析错误: " + e.getMessage());
            }
        }

        long endTime = System.nanoTime();
        long duration = endTime - startTime;

        System.out.println("⏱️ Jackson 耗时: " + (duration / 1_000_000) + "ms");
        return duration;
    }

    private long testGsonParsing() {
        System.out.println("🐌 Gson 性能测试 (" + ITERATIONS + "次)...");

        long startTime = System.nanoTime();

        for (int i = 0; i < ITERATIONS; i++) {
            try {
                JsonObject root = JsonParser.parseString(COMPLEX_JSON).getAsJsonObject();
                JsonObject data = root.getAsJsonObject("data");
                var items = data.getAsJsonArray("items");

                for (var item : items) {
                    JsonObject itemObj = item.getAsJsonObject();
                    String id = itemObj.get("id").getAsString();
                    String username = itemObj.get("username").getAsString();
                    boolean isBot = itemObj.get("bot").getAsBoolean();
                    boolean isVip = itemObj.get("is_vip").getAsBoolean();
                }

                JsonObject meta = data.getAsJsonObject("meta");
                int page = meta.get("page").getAsInt();
                int pageTotal = meta.get("page_total").getAsInt();

            } catch (Exception e) {
                System.err.println("Gson 解析错误: " + e.getMessage());
            }
        }

        long endTime = System.nanoTime();
        long duration = endTime - startTime;

        System.out.println("⏱️ Gson 耗时: " + (duration / 1_000_000) + "ms");
        return duration;
    }

    private void analyzeResults(long jacksonTime, long gsonTime) {
        System.out.println("\n📊 性能分析结果:");
        System.out.println("=====================================");

        double jacksonMs = jacksonTime / 1_000_000.0;
        double gsonMs = gsonTime / 1_000_000.0;

        System.out.printf("Jackson: %.2f ms%n", jacksonMs);
        System.out.printf("Gson:    %.2f ms%n", gsonMs);

        if (gsonTime > 0) {
            double speedup = (double) gsonTime / jacksonTime;
            double improvement = ((double) (gsonTime - jacksonTime) / gsonTime) * 100;

            System.out.printf("性能提升: %.1fx 倍速%n", speedup);
            System.out.printf("时间减少: %.1f%%%n", improvement);

            if (improvement >= 50) {
                System.out.println("🎉 Jackson 性能显著优于 Gson！");
            } else if (improvement >= 20) {
                System.out.println("✅ Jackson 性能优于 Gson");
            } else if (improvement >= 0) {
                System.out.println("📈 Jackson 略快于 Gson");
            } else {
                System.out.println("⚠️ 性能测试结果异常");
            }
        }

        System.out.println("=====================================");
    }
}