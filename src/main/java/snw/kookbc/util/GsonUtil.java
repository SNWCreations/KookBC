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
import com.fasterxml.jackson.databind.ObjectMapper;
import snw.jkook.util.Validate;

import java.lang.reflect.Type;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * JSON 工具类 - 基于 Jackson 的 JSON 处理
 *
 * <p>此类已从 GSON 完全迁移到 Jackson。所有方法都使用 Jackson 实现。
 *
 * <p><b>迁移说明</b>：
 * <ul>
 *   <li>卡片消息处理：使用 {@link JacksonCardUtil}</li>
 *   <li>通用 JSON 处理：使用 {@link JacksonUtil}</li>
 *   <li>GSON 兼容 API：使用 {@link GsonCompat}</li>
 * </ul>
 *
 * @since 0.52.0 完全基于 Jackson
 */
public final class GsonUtil {

    /**
     * 卡片消息专用 ObjectMapper
     * @deprecated 请使用 {@link JacksonCardUtil#getMapper()}
     */
    @Deprecated
    public static final ObjectMapper CARD_GSON = JacksonCardUtil.getMapper();

    /**
     * 通用 ObjectMapper
     * @deprecated 请使用 {@link JacksonUtil#getMapper()}
     */
    @Deprecated
    public static final ObjectMapper NORMAL_GSON = JacksonUtil.getMapper();

    private GsonUtil() {
        throw new UnsupportedOperationException("Utility class");
    }

    // ===== 卡片处理方法 =====

    /**
     * 序列化卡片组件为 JSON
     */
    public static String toCardJson(Object cardComponent) {
        return JacksonCardUtil.toJson(cardComponent);
    }

    /**
     * 从 JSON 反序列化卡片组件
     */
    public static <T> T fromCardJson(String json, Class<T> clazz) {
        return JacksonCardUtil.fromJson(json, clazz);
    }

    // ===== 类型工具方法 =====

    /**
     * 创建 List 类型
     */
    public static Type createListType(Class<?> elementType) {
        Validate.notNull(elementType);
        return JacksonUtil.getMapper().getTypeFactory()
            .constructCollectionType(List.class, elementType);
    }

    // ===== JSON 节点访问方法（兼容 GSON API）=====

    /**
     * 检查节点是否包含指定键
     */
    public static boolean has(JsonNode node, String key) {
        return GsonCompat.has(node, key);
    }

    /**
     * 获取节点中的元素
     */
    public static JsonNode get(JsonNode node, String key) {
        return GsonCompat.get(node, key);
    }

    /**
     * 获取字符串值
     */
    public static String getAsString(JsonNode node, String key) {
        return GsonCompat.getAsString(node, key);
    }

    /**
     * 获取整数值
     */
    public static int getAsInt(JsonNode node, String key) {
        return GsonCompat.getAsInt(node, key);
    }

    /**
     * 获取长整数值
     */
    public static long getAsLong(JsonNode node, String key) {
        return GsonCompat.getAsLong(node, key);
    }

    /**
     * 获取双精度浮点数值
     */
    public static double getAsDouble(JsonNode node, String key) {
        return GsonCompat.getAsDouble(node, key);
    }

    /**
     * 获取布尔值
     */
    public static boolean getAsBoolean(JsonNode node, String key) {
        return GsonCompat.getAsBoolean(node, key);
    }

    /**
     * 获取 JSON 对象
     */
    public static JsonNode getAsJsonObject(JsonNode node, String key) {
        return GsonCompat.getAsJsonObject(node, key);
    }

    /**
     * 获取 JSON 数组
     */
    public static JsonNode getAsJsonArray(JsonNode node, String key) {
        return GsonCompat.getAsJsonArray(node, key);
    }

    /**
     * 获取原始类型值
     */
    public static JsonNode getAsJsonPrimitive(JsonNode node, String key) {
        return GsonCompat.getAsJsonPrimitive(node, key);
    }
}