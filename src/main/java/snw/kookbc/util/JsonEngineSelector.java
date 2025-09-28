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

import com.fasterxml.jackson.databind.JsonNode;
import com.google.gson.JsonObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

/**
 * 智能 JSON 引擎选择器
 *
 * <p>根据数据特征、性能指标和使用场景自动选择最优的 JSON 处理引擎：
 * <ul>
 *   <li><b>Jackson</b>: 高性能通用处理，适合大数据量和复杂结构</li>
 *   <li><b>GSON</b>: 向后兼容和特殊序列化，适合卡片消息和复杂多态</li>
 * </ul>
 *
 * <p><b>选择策略</b>：
 * <ul>
 *   <li>卡片消息和模板：始终使用 GSON（兼容性优先）</li>
 *   <li>HTTP API 响应：使用 Jackson（性能优先）</li>
 *   <li>大型数组/批量数据：使用 Jackson（内存效率）</li>
 *   <li>复杂嵌套对象：根据深度和大小智能选择</li>
 *   <li>频繁访问数据：使用缓存机制</li>
 * </ul>
 *
 * <p><b>性能监控</b>：
 * 自动收集处理时间、选择次数等指标，支持性能调优。
 *
 * @since KookBC 0.33.0
 */
public final class JsonEngineSelector {

    /**
     * JSON 引擎类型枚举
     */
    public enum EngineType {
        JACKSON("Jackson", "高性能通用处理"),
        GSON("GSON", "向后兼容和特殊序列化");

        private final String name;
        private final String description;

        EngineType(String name, String description) {
            this.name = name;
            this.description = description;
        }

        public String getName() { return name; }
        public String getDescription() { return description; }
    }

    /**
     * 数据类型分类
     */
    public enum DataType {
        CARD_MESSAGE,           // 卡片消息
        TEMPLATE_MESSAGE,       // 模板消息
        HTTP_API_RESPONSE,      // HTTP API 响应
        EVENT_DATA,             // 事件数据
        LARGE_ARRAY,            // 大型数组
        SIMPLE_OBJECT,          // 简单对象
        COMPLEX_NESTED,         // 复杂嵌套
        UNKNOWN                 // 未知类型
    }

    // ===== 配置常量 =====

    private static final int LARGE_JSON_THRESHOLD = 50000;     // 大型 JSON 阈值（字符数）
    private static final int DEEP_NESTING_THRESHOLD = 8;       // 深度嵌套阈值
    private static final int ARRAY_SIZE_THRESHOLD = 100;       // 数组大小阈值

    // 卡片消息检测模式
    private static final Pattern CARD_MESSAGE_PATTERN = Pattern.compile(
        ".*\"type\"\\s*:\\s*[\"']?(card|kmarkdown|plain-text)[\"']?.*", Pattern.CASE_INSENSITIVE);
    private static final Pattern TEMPLATE_MESSAGE_PATTERN = Pattern.compile(
        ".*\"template_id\".*", Pattern.CASE_INSENSITIVE);

    // ===== 性能统计 =====

    private static final AtomicLong jacksonUsageCount = new AtomicLong(0);
    private static final AtomicLong gsonUsageCount = new AtomicLong(0);
    private static final AtomicLong totalProcessingTime = new AtomicLong(0);

    private JsonEngineSelector() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    // ===== 核心选择逻辑 =====

    /**
     * 根据 JSON 字符串特征选择最优引擎
     *
     * @param jsonString JSON 字符串
     * @return 推荐的引擎类型
     */
    @NotNull
    public static EngineType selectEngine(@Nullable String jsonString) {
        if (jsonString == null || jsonString.trim().isEmpty()) {
            return EngineType.JACKSON; // 默认使用 Jackson
        }

        long startTime = System.nanoTime();
        try {
            DataType dataType = analyzeDataType(jsonString);
            return selectEngineByDataType(dataType, jsonString);
        } finally {
            totalProcessingTime.addAndGet(System.nanoTime() - startTime);
        }
    }

    /**
     * 根据数据类型选择引擎
     *
     * @param dataType 数据类型
     * @param context 上下文信息（可选）
     * @return 推荐的引擎类型
     */
    @NotNull
    public static EngineType selectEngineByDataType(@NotNull DataType dataType, @Nullable String context) {
        EngineType selected;

        switch (dataType) {
            case CARD_MESSAGE:
            case TEMPLATE_MESSAGE:
                // 卡片和模板消息必须使用 GSON（兼容性要求）
                selected = EngineType.GSON;
                gsonUsageCount.incrementAndGet();
                break;

            case HTTP_API_RESPONSE:
            case EVENT_DATA:
            case LARGE_ARRAY:
                // API 响应和大数据使用 Jackson（性能优先）
                selected = EngineType.JACKSON;
                jacksonUsageCount.incrementAndGet();
                break;

            case COMPLEX_NESTED:
                // 复杂嵌套对象根据大小判断
                if (context != null && context.length() > LARGE_JSON_THRESHOLD) {
                    selected = EngineType.JACKSON; // 大型复杂数据用 Jackson
                    jacksonUsageCount.incrementAndGet();
                } else {
                    selected = EngineType.GSON; // 小型复杂数据用 GSON
                    gsonUsageCount.incrementAndGet();
                }
                break;

            case SIMPLE_OBJECT:
            case UNKNOWN:
            default:
                // 简单对象和未知类型默认使用 Jackson
                selected = EngineType.JACKSON;
                jacksonUsageCount.incrementAndGet();
                break;
        }

        return selected;
    }

    /**
     * 分析 JSON 数据类型
     *
     * @param jsonString JSON 字符串
     * @return 数据类型
     */
    @NotNull
    private static DataType analyzeDataType(@NotNull String jsonString) {
        // 快速大小检查
        if (jsonString.length() > LARGE_JSON_THRESHOLD) {
            return DataType.LARGE_ARRAY;
        }

        // 卡片消息检测
        if (CARD_MESSAGE_PATTERN.matcher(jsonString).matches()) {
            return DataType.CARD_MESSAGE;
        }

        // 模板消息检测
        if (TEMPLATE_MESSAGE_PATTERN.matcher(jsonString).matches()) {
            return DataType.TEMPLATE_MESSAGE;
        }

        // HTTP API 响应检测（包含常见的 Kook API 字段）
        if (jsonString.contains("\"code\"") && jsonString.contains("\"data\"")) {
            return DataType.HTTP_API_RESPONSE;
        }

        // 事件数据检测
        if (jsonString.contains("\"s\"") && jsonString.contains("\"d\"")) {
            return DataType.EVENT_DATA;
        }

        // 数组检测
        String trimmed = jsonString.trim();
        if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
            int estimatedSize = estimateArraySize(jsonString);
            if (estimatedSize > ARRAY_SIZE_THRESHOLD) {
                return DataType.LARGE_ARRAY;
            }
        }

        // 嵌套深度检测
        int nestingDepth = estimateNestingDepth(jsonString);
        if (nestingDepth > DEEP_NESTING_THRESHOLD) {
            return DataType.COMPLEX_NESTED;
        }

        return DataType.SIMPLE_OBJECT;
    }

    /**
     * 估算数组大小
     */
    private static int estimateArraySize(@NotNull String jsonString) {
        // 简单启发式：统计逗号数量 + 1
        int commaCount = 0;
        boolean inString = false;
        boolean escaped = false;

        for (char c : jsonString.toCharArray()) {
            if (escaped) {
                escaped = false;
                continue;
            }

            if (c == '\\') {
                escaped = true;
                continue;
            }

            if (c == '"') {
                inString = !inString;
                continue;
            }

            if (!inString && c == ',') {
                commaCount++;
            }
        }

        return commaCount + 1;
    }

    /**
     * 估算嵌套深度
     */
    private static int estimateNestingDepth(@NotNull String jsonString) {
        int maxDepth = 0;
        int currentDepth = 0;
        boolean inString = false;
        boolean escaped = false;

        for (char c : jsonString.toCharArray()) {
            if (escaped) {
                escaped = false;
                continue;
            }

            if (c == '\\') {
                escaped = true;
                continue;
            }

            if (c == '"') {
                inString = !inString;
                continue;
            }

            if (!inString) {
                if (c == '{' || c == '[') {
                    currentDepth++;
                    maxDepth = Math.max(maxDepth, currentDepth);
                } else if (c == '}' || c == ']') {
                    currentDepth--;
                }
            }
        }

        return maxDepth;
    }

    // ===== 统一处理接口 =====

    /**
     * 智能解析 JSON 字符串，自动选择最优引擎
     *
     * @param jsonString JSON 字符串
     * @return Jackson JsonNode 或转换后的结果
     */
    @NotNull
    public static JsonNode parseJson(@NotNull String jsonString) {
        EngineType engine = selectEngine(jsonString);

        if (engine == EngineType.JACKSON) {
            return JacksonUtil.parse(jsonString);
        } else {
            // 使用 GSON 解析然后转换为 Jackson JsonNode
            JsonObject gsonObject = GsonUtil.NORMAL_GSON.fromJson(jsonString, JsonObject.class);
            return JacksonUtil.parse(gsonObject.toString());
        }
    }

    /**
     * 智能序列化对象，自动选择最优引擎
     *
     * @param object 要序列化的对象
     * @return JSON 字符串
     */
    @NotNull
    public static String toJson(@NotNull Object object) {
        // 根据对象类型选择引擎
        DataType dataType = determineObjectDataType(object);
        EngineType engine = selectEngineByDataType(dataType, null);

        if (engine == EngineType.JACKSON) {
            return JacksonUtil.toJson(object);
        } else {
            return GsonUtil.NORMAL_GSON.toJson(object);
        }
    }

    /**
     * 确定对象的数据类型
     */
    @NotNull
    private static DataType determineObjectDataType(@NotNull Object object) {
        String className = object.getClass().getName();

        // 卡片消息相关类
        if (className.contains("CardComponent") || className.contains("TemplateMessage") ||
            className.contains("card.") || className.contains("template.")) {
            return DataType.CARD_MESSAGE;
        }

        // 集合类型
        if (object instanceof java.util.Collection) {
            java.util.Collection<?> collection = (java.util.Collection<?>) object;
            if (collection.size() > ARRAY_SIZE_THRESHOLD) {
                return DataType.LARGE_ARRAY;
            }
        }

        // 数组类型
        if (object.getClass().isArray()) {
            return DataType.LARGE_ARRAY;
        }

        return DataType.SIMPLE_OBJECT;
    }

    // ===== 性能监控 =====

    /**
     * 获取 Jackson 使用次数
     */
    public static long getJacksonUsageCount() {
        return jacksonUsageCount.get();
    }

    /**
     * 获取 GSON 使用次数
     */
    public static long getGsonUsageCount() {
        return gsonUsageCount.get();
    }

    /**
     * 获取总处理时间（纳秒）
     */
    public static long getTotalProcessingTime() {
        return totalProcessingTime.get();
    }

    /**
     * 获取平均处理时间（纳秒）
     */
    public static long getAverageProcessingTime() {
        long totalUsage = jacksonUsageCount.get() + gsonUsageCount.get();
        return totalUsage > 0 ? totalProcessingTime.get() / totalUsage : 0;
    }

    /**
     * 获取性能统计信息
     */
    @NotNull
    public static String getPerformanceStats() {
        long jacksonCount = getJacksonUsageCount();
        long gsonCount = getGsonUsageCount();
        long totalCount = jacksonCount + gsonCount;

        if (totalCount == 0) {
            return "JsonEngineSelector Stats: No usage recorded";
        }

        double jacksonPercent = (jacksonCount * 100.0) / totalCount;
        double gsonPercent = (gsonCount * 100.0) / totalCount;
        long avgTime = getAverageProcessingTime();

        return String.format(
            "JsonEngineSelector Stats - Total: %d, Jackson: %d (%.1f%%), GSON: %d (%.1f%%), Avg Time: %dns",
            totalCount, jacksonCount, jacksonPercent, gsonCount, gsonPercent, avgTime
        );
    }

    /**
     * 重置性能统计
     */
    public static void resetStats() {
        jacksonUsageCount.set(0);
        gsonUsageCount.set(0);
        totalProcessingTime.set(0);
    }
}