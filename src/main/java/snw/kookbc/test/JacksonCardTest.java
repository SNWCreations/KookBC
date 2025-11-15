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
package snw.kookbc.test;

import snw.jkook.message.component.card.CardComponent;
import snw.kookbc.impl.entity.builder.CardBuilder;
import snw.kookbc.util.JacksonCardUtil;

/**
 * Jackson卡片系统测试工具
 */
public class JacksonCardTest {

    /**
     * 测试复杂卡片JSON的反序列化
     * 模拟用户报告的错误场景
     */
    public static void testComplexCardDeserialization() {
        // 这是用户提供的复杂卡片JSON
        String complexCardJson = "[{\"theme\":\"info\",\"color\":\"\",\"size\":\"lg\",\"expand\":false,\"modules\":[{\"type\":\"section\",\"mode\":\"right\",\"accessory\":{\"type\":\"button\",\"theme\":\"secondary\",\"value\":\"{\\n    \\\"action\\\": \\\"播放卡片按钮\\\",\\n    \\\"voiceChannelID\\\": \\\"8418843659211643\\\",\\n    \\\"event\\\": \\\"歌曲列表\\\"\\n}\",\"click\":\"return-val\",\"text\":{\"type\":\"kmarkdown\",\"content\":\"142 \\/ 326\",\"elements\":[]},\"external\":true,\"elements\":[]},\"text\":{\"type\":\"kmarkdown\",\"content\":\"**[**⛏minecraft高手⛏**]**\\t\\t| 正在为你播放 😊 \",\"elements\":[]},\"elements\":[]},{\"type\":\"section\",\"mode\":\"left\",\"accessory\":{\"type\":\"image\",\"src\":\"https:\\/\\/img.kookapp.cn\\/attachments\\/2025-09\\/26\\/VbW1qWRpB814z14z.jpeg\",\"alt\":\"\",\"size\":\"sm\",\"circle\":false,\"title\":\"\",\"fallbackUrl\":\"\",\"elements\":[]},\"text\":{\"type\":\"plain-text\",\"emoji\":true,\"content\":\"  The des Alizes - Foxtail-Grass Studio\",\"elements\":[]},\"elements\":[]},{\"type\":\"context\",\"elements\":[{\"type\":\"plain-text\",\"emoji\":true,\"content\":\"音源: \",\"elements\":[]},{\"type\":\"image\",\"src\":\"https:\\/\\/img.kookapp.cn\\/assets\\/2023-05\\/hULgrDPVq200w00w.png\",\"alt\":\"\",\"size\":\"sm\",\"circle\":true,\"title\":\"\",\"fallbackUrl\":\"\",\"elements\":[]},{\"type\":\"plain-text\",\"emoji\":true,\"content\":\"  |  模式: 随机播放\",\"elements\":[]},{\"type\":\"plain-text\",\"emoji\":true,\"content\":\"  |  音量: 0.5\",\"elements\":[]},{\"type\":\"kmarkdown\",\"content\":\"  |  如果有问题欢迎加入-> [官方服务器](https:\\/\\/kook.top\\/JOHwp4) \",\"elements\":[]}]},{\"type\":\"action-group\",\"elements\":[{\"type\":\"button\",\"theme\":\"primary\",\"value\":\"{\\n    \\\"action\\\": \\\"播放卡片按钮\\\",\\n    \\\"voiceChannelID\\\": \\\"8418843659211643\\\",\\n    \\\"event\\\": \\\"上一首歌\\\"\\n}\",\"click\":\"return-val\",\"text\":{\"type\":\"plain-text\",\"emoji\":true,\"content\":\"上一首歌\",\"elements\":[]},\"external\":true,\"elements\":[]},{\"type\":\"button\",\"theme\":\"danger\",\"value\":\"{\\n    \\\"action\\\": \\\"播放卡片按钮\\\",\\n    \\\"voiceChannelID\\\": \\\"8418843659211643\\\",\\n    \\\"event\\\": \\\"暂停播放\\\"\\n}\",\"click\":\"return-val\",\"text\":{\"type\":\"plain-text\",\"emoji\":true,\"content\":\"暂停播放\",\"elements\":[]},\"external\":true,\"elements\":[]},{\"type\":\"button\",\"theme\":\"primary\",\"value\":\"{\\n    \\\"action\\\": \\\"播放卡片按钮\\\",\\n    \\\"voiceChannelID\\\": \\\"8418843659211643\\\",\\n    \\\"event\\\": \\\"下一首歌\\\"\\n}\",\"click\":\"return-val\",\"text\":{\"type\":\"plain-text\",\"emoji\":true,\"content\":\"下一首歌\",\"elements\":[]},\"external\":true,\"elements\":[]},{\"type\":\"button\",\"theme\":\"secondary\",\"value\":\"{\\n    \\\"action\\\": \\\"播放卡片按钮\\\",\\n    \\\"voiceChannelID\\\": \\\"8418843659211643\\\",\\n    \\\"event\\\": \\\"切换模式\\\"\\n}\",\"click\":\"return-val\",\"text\":{\"type\":\"plain-text\",\"emoji\":true,\"content\":\"切换模式\",\"elements\":[]},\"external\":true,\"elements\":[]}]}],\"type\":\"card\"}]";

        try {
            System.out.println("=== Jackson卡片系统测试 ===");
            System.out.println("开始测试复杂卡片JSON反序列化...");

            // 使用Jackson解析
            Object result = CardBuilder.buildCard(complexCardJson);

            if (result != null) {
                System.out.println("✅ Jackson反序列化成功!");
                System.out.println("结果类型: " + result.getClass().getSimpleName());

                // 测试序列化回JSON
                String serializedJson;
                if (result instanceof CardComponent) {
                    serializedJson = JacksonCardUtil.toJson(result);
                } else {
                    serializedJson = JacksonCardUtil.toJson(result);
                }

                System.out.println("✅ Jackson序列化成功!");
                System.out.println("序列化JSON长度: " + serializedJson.length());

                // 验证往返转换
                Object roundTrip = CardBuilder.buildCard(serializedJson);
                if (roundTrip != null) {
                    System.out.println("✅ JSON往返转换成功!");
                } else {
                    System.out.println("❌ JSON往返转换失败");
                }

            } else {
                System.out.println("❌ Jackson反序列化返回null");
            }

        } catch (Exception e) {
            System.out.println("❌ Jackson测试失败: " + e.getClass().getSimpleName());
            System.out.println("错误信息: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 测试缺失字段处理
     */
    public static void testMissingFieldHandling() {
        System.out.println("\n=== 缺失字段处理测试 ===");

        // 测试缺少可选字段的卡片
        String incompleteCardJson = "[{\"type\":\"card\",\"size\":\"lg\",\"modules\":[{\"type\":\"section\",\"text\":{\"type\":\"plain-text\",\"content\":\"简单文本\"}}]}]";

        try {
            Object result = CardBuilder.buildCard(incompleteCardJson);
            if (result != null) {
                System.out.println("✅ 缺失字段处理成功!");
            } else {
                System.out.println("❌ 缺失字段处理失败");
            }
        } catch (Exception e) {
            System.out.println("❌ 缺失字段处理异常: " + e.getMessage());
        }
    }

    /**
     * 主测试方法
     */
    public static void main(String[] args) {
        testComplexCardDeserialization();
        testMissingFieldHandling();
        System.out.println("\n=== Jackson卡片系统测试完成 ===");
    }
}