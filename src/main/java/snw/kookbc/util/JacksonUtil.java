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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import snw.jkook.util.Validate;

import java.lang.reflect.Type;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Jackson JSON工具类 - 高性能JSON处理
 * 提供与GsonUtil兼容的API，但使用Jackson实现更高性能
 */
public final class JacksonUtil {

    // 兼容性字段，供现有代码使用 - 移除以避免类型不匹配
    // public static final com.google.gson.Gson NORMAL_GSON = GsonUtil.NORMAL_GSON;

    // Jackson核心对象
    private static final ObjectMapper MAPPER;

    static {
        MAPPER = new ObjectMapper();
        MAPPER.registerModule(new JavaTimeModule());
        // 配置Jackson以处理缺失字段和null值
        MAPPER.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        MAPPER.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, false);
    }

    private JacksonUtil() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    // ===== 基础JSON操作 =====

    public static JsonNode parse(String json) {
        try {
            return MAPPER.readTree(json);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to parse JSON: " + json, e);
        }
    }

    public static String toJson(Object obj) {
        try {
            return MAPPER.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize object to JSON", e);
        }
    }

    public static <T> T fromJson(String json, Class<T> classOfT) {
        try {
            return MAPPER.readValue(json, classOfT);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to deserialize JSON to " + classOfT.getName(), e);
        }
    }

    public static <T> T fromJson(String json, TypeReference<T> typeRef) {
        try {
            return MAPPER.readValue(json, typeRef);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to deserialize JSON", e);
        }
    }

    // ===== 节点访问方法 =====

    public static JsonNode get(JsonNode node, String fieldName) {
        Validate.notNull(node, "JsonNode cannot be null");
        JsonNode result = node.get(fieldName);
        if (result == null || result.isNull()) {
            throw new NoSuchElementException("Field '" + fieldName + "' not found or is null in JSON node");
        }
        return result;
    }

    public static boolean has(JsonNode node, String fieldName) {
        if (node == null) {
            return false;
        }
        JsonNode field = node.get(fieldName);
        return field != null && !field.isNull();
    }

    public static String getAsString(JsonNode node, String fieldName) {
        return get(node, fieldName).asText();
    }

    public static int getAsInt(JsonNode node, String fieldName) {
        return get(node, fieldName).asInt();
    }

    public static long getAsLong(JsonNode node, String fieldName) {
        return get(node, fieldName).asLong();
    }

    public static boolean getAsBoolean(JsonNode node, String fieldName) {
        return get(node, fieldName).asBoolean();
    }

    public static double getAsDouble(JsonNode node, String fieldName) {
        return get(node, fieldName).asDouble();
    }

    // ===== 兼容性方法 (用于GsonUtil替换) =====

    public static JsonNode getAsJsonObject(JsonNode node, String fieldName) {
        JsonNode result = get(node, fieldName);
        if (!result.isObject()) {
            throw new IllegalStateException("Field '" + fieldName + "' is not a JSON object");
        }
        return result;
    }

    public static JsonNode getAsJsonArray(JsonNode node, String fieldName) {
        JsonNode result = get(node, fieldName);
        if (!result.isArray()) {
            throw new IllegalStateException("Field '" + fieldName + "' is not a JSON array");
        }
        return result;
    }

    // ===== 类型转换辅助方法 =====

    public static <T> List<T> toList(JsonNode arrayNode, Class<T> elementType) {
        try {
            return MAPPER.convertValue(arrayNode,
                MAPPER.getTypeFactory().constructCollectionType(List.class, elementType));
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Failed to convert JsonNode to List<" + elementType.getName() + ">", e);
        }
    }

    // 获取ObjectMapper实例，供高级用途使用
    public static ObjectMapper getMapper() {
        return MAPPER;
    }

    // ===== Null-Safe字段访问方法（EntityBuilder专用）=====

    /**
     * 获取必需的字符串字段，如果不存在或为null则抛出异常
     */
    public static String getRequiredString(JsonNode node, String fieldName) {
        JsonNode field = node.get(fieldName);
        if (field == null || field.isNull()) {
            throw new NoSuchElementException("Required field '" + fieldName + "' not found or is null");
        }
        return field.asText();
    }

    /**
     * 获取字符串字段，支持默认值
     */
    public static String getStringOrDefault(JsonNode node, String fieldName, String defaultValue) {
        JsonNode field = node.get(fieldName);
        if (field == null || field.isNull()) {
            return defaultValue;
        }
        return field.asText();
    }

    /**
     * 获取必需的整数字段，如果不存在或为null则抛出异常
     */
    public static int getRequiredInt(JsonNode node, String fieldName) {
        JsonNode field = node.get(fieldName);
        if (field == null || field.isNull()) {
            throw new NoSuchElementException("Required field '" + fieldName + "' not found or is null");
        }
        return field.asInt();
    }

    /**
     * 获取整数字段，支持默认值
     */
    public static int getIntOrDefault(JsonNode node, String fieldName, int defaultValue) {
        JsonNode field = node.get(fieldName);
        if (field == null || field.isNull()) {
            return defaultValue;
        }
        return field.asInt();
    }

    /**
     * 获取长整数字段，支持默认值
     */
    public static long getLongOrDefault(JsonNode node, String fieldName, long defaultValue) {
        JsonNode field = node.get(fieldName);
        if (field == null || field.isNull()) {
            return defaultValue;
        }
        return field.asLong();
    }

    /**
     * 获取布尔字段，支持默认值
     */
    public static boolean getBooleanOrDefault(JsonNode node, String fieldName, boolean defaultValue) {
        JsonNode field = node.get(fieldName);
        if (field == null || field.isNull()) {
            return defaultValue;
        }
        return field.asBoolean();
    }

    /**
     * 获取双精度字段，支持默认值
     */
    public static double getDoubleOrDefault(JsonNode node, String fieldName, double defaultValue) {
        JsonNode field = node.get(fieldName);
        if (field == null || field.isNull()) {
            return defaultValue;
        }
        return field.asDouble();
    }

    /**
     * 安全获取嵌套对象
     */
    public static JsonNode getObjectOrNull(JsonNode node, String fieldName) {
        JsonNode field = node.get(fieldName);
        if (field == null || field.isNull() || !field.isObject()) {
            return null;
        }
        return field;
    }

    /**
     * 安全获取数组
     */
    public static JsonNode getArrayOrNull(JsonNode node, String fieldName) {
        JsonNode field = node.get(fieldName);
        if (field == null || field.isNull() || !field.isArray()) {
            return null;
        }
        return field;
    }

    /**
     * 检查字段是否存在且不为null
     */
    public static boolean hasNonNull(JsonNode node, String fieldName) {
        JsonNode field = node.get(fieldName);
        return field != null && !field.isNull();
    }

    // ===== 其他工具方法 =====

    public static ObjectNode createObjectNode() {
        return MAPPER.createObjectNode();
    }

    public static String toJsonString(Object obj) {
        return toJson(obj);
    }

    public static Type createListType(Class<?> elementType) {
        // 为兼容性提供Type支持，返回List<elementType>的Type
        // 注意：序列化器应该使用GsonUtil.createListType()避免静态初始化循环依赖
        return MAPPER.getTypeFactory().constructCollectionType(List.class, elementType);
    }

    public static com.google.gson.JsonObject convertToGsonJsonObject(JsonNode jacksonNode) {
        if (jacksonNode == null) {
            return null;
        }
        try {
            // 优化：直接使用JsonParser而不是NORMAL_GSON，减少运行时Gson依赖
            // 将JsonNode转换为GSON JsonObject
            String jsonString = jacksonNode.toString();
            return new com.google.gson.JsonParser().parse(jsonString).getAsJsonObject();
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert Jackson JsonNode to Gson JsonObject", e);
        }
    }

    // ===== ObjectMapper 工厂方法 =====

    /**
     * 创建一个格式化输出的 ObjectMapper (Pretty Print)
     * 适用于配置文件、日志输出等需要可读性的场景
     */
    public static ObjectMapper createPrettyMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, false);
        // 启用格式化输出
        mapper.enable(com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT);
        // 禁用 HTML 转义 (与 GSON 的 disableHtmlEscaping 对应)
        mapper.getFactory().disable(com.fasterxml.jackson.core.JsonGenerator.Feature.ESCAPE_NON_ASCII);
        return mapper;
    }
}