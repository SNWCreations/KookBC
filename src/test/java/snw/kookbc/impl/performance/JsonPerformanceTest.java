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
import org.junit.jupiter.api.Test;
import snw.kookbc.util.JacksonUtil;

import java.io.IOException;

/**
 * Jackson 性能测试（已移除 GSON 依赖）
 * 现在仅测试 Jackson 解析性能
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
        System.out.println("=== Jackson JSON 解析性能测试 ===\n");

        // 预热
        warmup();

        // Jackson 性能测试
        long jacksonTime = testJacksonParsing();

        // 结果分析 (与历史 GSON 性能比较)
        analyzeResults(jacksonTime, 0); // gsonTime 设为0，仅显示 Jackson 性能
    }

    private void warmup() {
        System.out.println("🔥 预热阶段 (1000次)...");
        for (int i = 0; i < 1000; i++) {
            try {
                JacksonUtil.parse(COMPLEX_JSON);
                // 使用 Jackson 作为对比基准
                JacksonUtil.parse(COMPLEX_JSON);
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
        System.out.println("⚠️ GSON 已移除 - 跳过性能测试");
        return 0; // 返回0表示未测试
    }

    private void analyzeResults(long jacksonTime, long gsonTime) {
        System.out.println("\n📊 性能分析结果:");
        System.out.println("=====================================");

        double jacksonMs = jacksonTime / 1_000_000.0;

        System.out.printf("Jackson 解析耗时: %.2f ms%n", jacksonMs);
        System.out.printf("每次迭代平均: %.4f ms%n", jacksonMs / ITERATIONS);
        System.out.println("\n✅ GSON 依赖已完全移除");
        System.out.println("🚀 项目已全面迁移到 Jackson JSON 引擎");

        System.out.println("=====================================");
    }
}