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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.atomic.AtomicLong;

/**
 * JSON 引擎选择器 - 已完全迁移到 Jackson
 *
 * <p>此类原用于在 Jackson 和 GSON 之间智能选择。现在项目已完全迁移到 Jackson，
 * 此类保留仅用于向后兼容，所有方法都直接使用 Jackson。
 *
 * @deprecated 项目已完全使用 Jackson，不再需要引擎选择。请直接使用 {@link JacksonUtil}
 * @since 0.52.0 已弃用，完全基于 Jackson
 */
@Deprecated
public final class JsonEngineSelector {

    /**
     * JSON 引擎类型枚举（仅为兼容性保留）
     * @deprecated 仅保留 JACKSON 类型
     */
    @Deprecated
    public enum EngineType {
        JACKSON("Jackson", "高性能 JSON 处理引擎");

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
     * 数据类型分类（仅为兼容性保留）
     * @deprecated 不再需要根据数据类型选择引擎
     */
    @Deprecated
    public enum DataType {
        CARD_MESSAGE,
        TEMPLATE_MESSAGE,
        HTTP_API_RESPONSE,
        EVENT_DATA,
        LARGE_ARRAY,
        SIMPLE_OBJECT,
        COMPLEX_NESTED,
        UNKNOWN
    }

    private static final AtomicLong jacksonUsageCount = new AtomicLong(0);
    private static final AtomicLong totalProcessingTime = new AtomicLong(0);

    private JsonEngineSelector() {
        throw new UnsupportedOperationException("Utility class");
    }

    // ===== 核心方法（所有都返回 JACKSON）=====

    /**
     * 选择 JSON 引擎（始终返回 Jackson）
     * @deprecated 始终返回 Jackson，不再需要调用此方法
     */
    @Deprecated
    @NotNull
    public static EngineType selectEngine(@Nullable String jsonString) {
        jacksonUsageCount.incrementAndGet();
        return EngineType.JACKSON;
    }

    /**
     * 根据数据类型选择引擎（始终返回 Jackson）
     * @deprecated 始终返回 Jackson，不再需要调用此方法
     */
    @Deprecated
    @NotNull
    public static EngineType selectEngineByDataType(@NotNull DataType dataType, @Nullable String context) {
        jacksonUsageCount.incrementAndGet();
        return EngineType.JACKSON;
    }

    /**
     * 分析数据类型
     * @deprecated 不再需要数据类型分析
     */
    @Deprecated
    @NotNull
    private static DataType analyzeDataType(@NotNull String jsonString) {
        return DataType.SIMPLE_OBJECT;
    }

    // ===== JSON 处理方法（直接委托给 Jackson）=====

    /**
     * 解析 JSON 字符串
     * @deprecated 请直接使用 {@link JacksonUtil#parse(String)}
     */
    @Deprecated
    @NotNull
    public static JsonNode parseJson(@NotNull String jsonString) {
        return JacksonUtil.parse(jsonString);
    }

    /**
     * 序列化对象为 JSON
     * @deprecated 请直接使用 {@link JacksonUtil#toJson(Object)}
     */
    @Deprecated
    @NotNull
    public static String toJson(@NotNull Object object) {
        return JacksonUtil.toJson(object);
    }

    // ===== 性能监控方法 =====

    /**
     * 获取 Jackson 使用次数
     */
    public static long getJacksonUsageCount() {
        return jacksonUsageCount.get();
    }

    /**
     * 获取 GSON 使用次数（始终返回 0）
     * @deprecated GSON 已移除
     */
    @Deprecated
    public static long getGsonUsageCount() {
        return 0;
    }

    /**
     * 获取总处理时间
     */
    public static long getTotalProcessingTime() {
        return totalProcessingTime.get();
    }

    /**
     * 获取平均处理时间
     */
    public static long getAverageProcessingTime() {
        long totalUsage = jacksonUsageCount.get();
        return totalUsage > 0 ? totalProcessingTime.get() / totalUsage : 0;
    }

    /**
     * 获取性能统计信息
     */
    @NotNull
    public static String getPerformanceStats() {
        long jacksonCount = getJacksonUsageCount();
        long avgTime = getAverageProcessingTime();

        if (jacksonCount == 0) {
            return "JsonEngineSelector Stats: No usage recorded";
        }

        return String.format(
            "JsonEngineSelector Stats - Total: %d, Jackson: %d (100%%), Avg Time: %dns",
            jacksonCount, jacksonCount, avgTime
        );
    }

    /**
     * 重置性能统计
     */
    public static void resetStats() {
        jacksonUsageCount.set(0);
        totalProcessingTime.set(0);
    }
}