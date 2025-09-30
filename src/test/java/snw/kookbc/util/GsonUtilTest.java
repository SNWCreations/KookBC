/*
 *     KookBC -- The Kook Bot Client & JKook API standard implementation for Java.
 *     Copyright (C) 2022 - 2023 SNWCreations and contributors
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as
 *     published by the Free Software Foundation, either version 3 of the
 *     License, or (at your option) any later version.
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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import snw.kookbc.test.BaseTest;

import java.lang.reflect.Type;
import java.util.List;
import java.util.NoSuchElementException;

import static org.assertj.core.api.Assertions.*;

/**
 * GsonUtil 工具类测试（已迁移到 Jackson）
 * 测试 JSON 处理相关的工具方法
 */
@DisplayName("GsonUtil JSON 工具测试（Jackson 版本）")
class GsonUtilTest extends BaseTest {

    @Test
    @DisplayName("CARD_GSON 和 NORMAL_GSON 应该正确初始化为 Jackson ObjectMapper")
    void testGsonInstances() {
        assertThat(GsonUtil.CARD_GSON).isNotNull();
        assertThat(GsonUtil.NORMAL_GSON).isNotNull();
        // 现在两者都指向 JacksonCardUtil 和 JacksonUtil 的 mapper
    }

    @Test
    @DisplayName("createListType 应该创建正确的List类型")
    void testCreateListType() {
        Type stringListType = GsonUtil.createListType(String.class);
        Type integerListType = GsonUtil.createListType(Integer.class);

        assertThat(stringListType).isNotNull();
        assertThat(integerListType).isNotNull();
        assertThat(stringListType).isNotEqualTo(integerListType);
    }

    @Test
    @DisplayName("createListType 应该验证参数不为null")
    void testCreateListTypeWithNull() {
        assertThatThrownBy(() -> GsonUtil.createListType(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("has 应该正确检查 JSON 对象是否包含键")
    void testHas() {
        ObjectNode jsonObject = JacksonUtil.getMapper().createObjectNode();
        jsonObject.put("key1", "value1");
        jsonObject.put("key2", 123);
        jsonObject.putNull("nullKey");

        // 存在且非null的键
        assertThat(GsonUtil.has(jsonObject, "key1")).isTrue();
        assertThat(GsonUtil.has(jsonObject, "key2")).isTrue();

        // null值的键
        assertThat(GsonUtil.has(jsonObject, "nullKey")).isFalse();

        // 不存在的键
        assertThat(GsonUtil.has(jsonObject, "nonExistentKey")).isFalse();
    }

    @Test
    @DisplayName("get 应该正确获取 JSON 元素")
    void testGet() {
        ObjectNode jsonObject = JacksonUtil.getMapper().createObjectNode();
        jsonObject.put("stringKey", "testValue");
        jsonObject.put("numberKey", 42);

        JsonNode stringElement = GsonUtil.get(jsonObject, "stringKey");
        JsonNode numberElement = GsonUtil.get(jsonObject, "numberKey");

        assertThat(stringElement.asText()).isEqualTo("testValue");
        assertThat(numberElement.asInt()).isEqualTo(42);
    }

    @Test
    @DisplayName("get 应该在键不存在时抛出异常")
    void testGetWithNonExistentKey() {
        ObjectNode jsonObject = JacksonUtil.getMapper().createObjectNode();

        assertThatThrownBy(() -> GsonUtil.get(jsonObject, "nonExistentKey"))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("There is no valid value mapped to requested key 'nonExistentKey'");
    }

    @Test
    @DisplayName("get 应该在值为null时抛出异常")
    void testGetWithNullValue() {
        ObjectNode jsonObject = JacksonUtil.getMapper().createObjectNode();
        jsonObject.putNull("nullKey");

        assertThatThrownBy(() -> GsonUtil.get(jsonObject, "nullKey"))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("There is no valid value mapped to requested key 'nullKey'");
    }

    @Test
    @DisplayName("getAsString 应该正确获取字符串值")
    void testGetAsString() {
        ObjectNode jsonObject = JacksonUtil.getMapper().createObjectNode();
        jsonObject.put("stringKey", "hello world");

        String result = GsonUtil.getAsString(jsonObject, "stringKey");
        assertThat(result).isEqualTo("hello world");
    }

    @Test
    @DisplayName("getAsInt 应该正确获取整数值")
    void testGetAsInt() {
        ObjectNode jsonObject = JacksonUtil.getMapper().createObjectNode();
        jsonObject.put("intKey", 12345);

        int result = GsonUtil.getAsInt(jsonObject, "intKey");
        assertThat(result).isEqualTo(12345);
    }

    @Test
    @DisplayName("getAsLong 应该正确获取长整数值")
    void testGetAsLong() {
        ObjectNode jsonObject = JacksonUtil.getMapper().createObjectNode();
        jsonObject.put("longKey", 9876543210L);

        long result = GsonUtil.getAsLong(jsonObject, "longKey");
        assertThat(result).isEqualTo(9876543210L);
    }

    @Test
    @DisplayName("getAsDouble 应该正确获取浮点数值")
    void testGetAsDouble() {
        ObjectNode jsonObject = JacksonUtil.getMapper().createObjectNode();
        jsonObject.put("doubleKey", 3.14159);

        double result = GsonUtil.getAsDouble(jsonObject, "doubleKey");
        assertThat(result).isCloseTo(3.14159, within(0.00001));
    }

    @Test
    @DisplayName("getAsBoolean 应该正确获取布尔值")
    void testGetAsBoolean() {
        ObjectNode jsonObject = JacksonUtil.getMapper().createObjectNode();
        jsonObject.put("booleanKey", true);

        boolean result = GsonUtil.getAsBoolean(jsonObject, "booleanKey");
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("getAsJsonObject 应该正确获取JSON对象")
    void testGetAsJsonObject() {
        ObjectNode jsonObject = JacksonUtil.getMapper().createObjectNode();
        ObjectNode nestedObject = JacksonUtil.getMapper().createObjectNode();
        nestedObject.put("nestedKey", "nestedValue");
        jsonObject.set("objectKey", nestedObject);

        JsonNode result = GsonUtil.getAsJsonObject(jsonObject, "objectKey");
        assertThat(result.get("nestedKey").asText()).isEqualTo("nestedValue");
    }

    @Test
    @DisplayName("getAsJsonArray 应该正确获取JSON数组")
    void testGetAsJsonArray() {
        ObjectNode jsonObject = JacksonUtil.getMapper().createObjectNode();
        ArrayNode jsonArray = JacksonUtil.getMapper().createArrayNode();
        jsonArray.add("item1");
        jsonArray.add("item2");
        jsonObject.set("arrayKey", jsonArray);

        JsonNode result = GsonUtil.getAsJsonArray(jsonObject, "arrayKey");
        assertThat(result.size()).isEqualTo(2);
        assertThat(result.get(0).asText()).isEqualTo("item1");
        assertThat(result.get(1).asText()).isEqualTo("item2");
    }

    @Test
    @DisplayName("getAsJsonPrimitive 应该正确获取JSON基本类型")
    void testGetAsJsonPrimitive() {
        ObjectNode jsonObject = JacksonUtil.getMapper().createObjectNode();
        jsonObject.put("primitiveKey", "primitiveValue");

        JsonNode result = GsonUtil.getAsJsonPrimitive(jsonObject, "primitiveKey");
        assertThat(result.asText()).isEqualTo("primitiveValue");
    }

    @Test
    @DisplayName("类型转换方法应该处理类型不匹配的情况")
    void testTypeConversionExceptions() {
        ObjectNode jsonObject = JacksonUtil.getMapper().createObjectNode();
        jsonObject.put("stringValue", "notANumber");

        // Jackson 的 asInt() 在遇到非数字字符串时返回 0（默认行为）
        // 这与 GSON 的行为不同，GSON 会抛出 NumberFormatException
        int result = GsonUtil.getAsInt(jsonObject, "stringValue");
        assertThat(result).isEqualTo(0);
    }

    @Test
    @DisplayName("CARD_GSON 应该支持HTML转义禁用")
    void testCardGsonHtmlEscaping() throws Exception {
        ObjectNode testObject = JacksonCardUtil.getMapper().createObjectNode();
        testObject.put("html", "<b>Bold Text</b>");

        String result = JacksonCardUtil.toJson(testObject);

        // CARD_GSON应该不转义HTML字符
        assertThat(result).contains("<b>Bold Text</b>");
    }

    @Test
    @DisplayName("NORMAL_GSON 应该正常工作")
    void testNormalGson() throws Exception {
        ObjectNode testObject = JacksonUtil.getMapper().createObjectNode();
        testObject.put("key", "value");

        String json = JacksonUtil.toJson(testObject);
        JsonNode parsed = JacksonUtil.parse(json);

        assertThat(parsed.get("key").asText()).isEqualTo("value");
    }
}