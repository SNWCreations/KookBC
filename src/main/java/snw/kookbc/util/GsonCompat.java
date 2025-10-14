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
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.NoSuchElementException;

/**
 * GSON 兼容层 - 提供与 GSON API 兼容的接口，但底层使用 Jackson
 *
 * <p>此类用于最小化从 GSON 迁移到 Jackson 的代码改动。
 * 所有方法都委托给 Jackson 实现。
 *
 * @since 0.52.0
 */
public final class GsonCompat {

    private GsonCompat() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * 检查 JsonNode 是否包含指定字段且不为 null
     */
    public static boolean has(JsonNode node, String key) {
        if (node == null || !node.isObject()) {
            return false;
        }
        JsonNode field = node.get(key);
        return field != null && !field.isNull();
    }

    /**
     * 获取 JsonNode 中的字段，如果不存在或为 null 则抛出异常
     */
    public static JsonNode get(JsonNode node, String key) {
        if (node == null || !node.isObject()) {
            throw new NoSuchElementException("Node is not an object or is null");
        }
        JsonNode result = node.get(key);
        if (result == null || result.isNull()) {
            throw new NoSuchElementException("There is no valid value mapped to requested key '" + key + "'.");
        }
        return result;
    }

    /**
     * 获取字符串值
     */
    public static String getAsString(JsonNode node, String key) {
        return get(node, key).asText();
    }

    /**
     * 获取整数值
     */
    public static int getAsInt(JsonNode node, String key) {
        return get(node, key).asInt();
    }

    /**
     * 获取长整数值
     */
    public static long getAsLong(JsonNode node, String key) {
        return get(node, key).asLong();
    }

    /**
     * 获取双精度浮点数值
     */
    public static double getAsDouble(JsonNode node, String key) {
        return get(node, key).asDouble();
    }

    /**
     * 获取布尔值
     */
    public static boolean getAsBoolean(JsonNode node, String key) {
        return get(node, key).asBoolean();
    }

    /**
     * 获取 JSON 对象
     */
    public static JsonNode getAsJsonObject(JsonNode node, String key) {
        JsonNode result = get(node, key);
        if (!result.isObject()) {
            throw new IllegalStateException("Field '" + key + "' is not a JSON object");
        }
        return result;
    }

    /**
     * 获取 JSON 数组
     */
    public static JsonNode getAsJsonArray(JsonNode node, String key) {
        JsonNode result = get(node, key);
        if (!result.isArray()) {
            throw new IllegalStateException("Field '" + key + "' is not a JSON array");
        }
        return result;
    }

    /**
     * 获取基本类型值（字符串、数字、布尔）
     */
    public static JsonNode getAsJsonPrimitive(JsonNode node, String key) {
        JsonNode result = get(node, key);
        if (!result.isValueNode()) {
            throw new IllegalStateException("Field '" + key + "' is not a primitive value");
        }
        return result;
    }

    /**
     * 解析 JSON 字符串为 JsonNode（替代 JsonParser）
     */
    public static JsonNode parseString(String json) {
        return JacksonUtil.parse(json);
    }

    /**
     * 将对象序列化为 JSON 字符串
     */
    public static String toJson(Object obj) {
        return JacksonUtil.toJson(obj);
    }

    /**
     * 从 JSON 字符串反序列化对象
     */
    public static <T> T fromJson(String json, Class<T> classOfT) {
        return JacksonUtil.fromJson(json, classOfT);
    }

    /**
     * 创建空的 JSON 对象
     */
    public static ObjectNode createObject() {
        return JacksonUtil.createObjectNode();
    }

    /**
     * 创建空的 JSON 数组
     */
    public static ArrayNode createArray() {
        return JacksonUtil.getMapper().createArrayNode();
    }
}