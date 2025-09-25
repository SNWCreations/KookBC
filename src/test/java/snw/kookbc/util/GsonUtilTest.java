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

import com.google.gson.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import snw.kookbc.test.BaseTest;

import java.lang.reflect.Type;
import java.util.List;
import java.util.NoSuchElementException;

import static org.assertj.core.api.Assertions.*;

/**
 * GsonUtil 工具类测试
 * 测试JSON处理相关的工具方法
 */
@DisplayName("GsonUtil JSON工具测试")
class GsonUtilTest extends BaseTest {

    @Test
    @DisplayName("CARD_GSON 和 NORMAL_GSON 应该正确初始化")
    void testGsonInstances() {
        assertThat(GsonUtil.CARD_GSON).isNotNull();
        assertThat(GsonUtil.NORMAL_GSON).isNotNull();
        assertThat(GsonUtil.CARD_GSON).isNotSameAs(GsonUtil.NORMAL_GSON);
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
    @DisplayName("has 应该正确检查JSON对象是否包含键")
    void testHas() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("key1", "value1");
        jsonObject.addProperty("key2", 123);
        jsonObject.add("nullKey", JsonNull.INSTANCE);

        // 存在且非null的键
        assertThat(GsonUtil.has(jsonObject, "key1")).isTrue();
        assertThat(GsonUtil.has(jsonObject, "key2")).isTrue();

        // null值的键
        assertThat(GsonUtil.has(jsonObject, "nullKey")).isFalse();

        // 不存在的键
        assertThat(GsonUtil.has(jsonObject, "nonExistentKey")).isFalse();
    }

    @Test
    @DisplayName("get 应该正确获取JSON元素")
    void testGet() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("stringKey", "testValue");
        jsonObject.addProperty("numberKey", 42);

        JsonElement stringElement = GsonUtil.get(jsonObject, "stringKey");
        JsonElement numberElement = GsonUtil.get(jsonObject, "numberKey");

        assertThat(stringElement.getAsString()).isEqualTo("testValue");
        assertThat(numberElement.getAsInt()).isEqualTo(42);
    }

    @Test
    @DisplayName("get 应该在键不存在时抛出异常")
    void testGetWithNonExistentKey() {
        JsonObject jsonObject = new JsonObject();

        assertThatThrownBy(() -> GsonUtil.get(jsonObject, "nonExistentKey"))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("There is no valid value mapped to requested key 'nonExistentKey'");
    }

    @Test
    @DisplayName("get 应该在值为null时抛出异常")
    void testGetWithNullValue() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.add("nullKey", JsonNull.INSTANCE);

        assertThatThrownBy(() -> GsonUtil.get(jsonObject, "nullKey"))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("There is no valid value mapped to requested key 'nullKey'");
    }

    @Test
    @DisplayName("getAsString 应该正确获取字符串值")
    void testGetAsString() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("stringKey", "hello world");

        String result = GsonUtil.getAsString(jsonObject, "stringKey");
        assertThat(result).isEqualTo("hello world");
    }

    @Test
    @DisplayName("getAsInt 应该正确获取整数值")
    void testGetAsInt() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("intKey", 12345);

        int result = GsonUtil.getAsInt(jsonObject, "intKey");
        assertThat(result).isEqualTo(12345);
    }

    @Test
    @DisplayName("getAsLong 应该正确获取长整数值")
    void testGetAsLong() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("longKey", 9876543210L);

        long result = GsonUtil.getAsLong(jsonObject, "longKey");
        assertThat(result).isEqualTo(9876543210L);
    }

    @Test
    @DisplayName("getAsDouble 应该正确获取浮点数值")
    void testGetAsDouble() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("doubleKey", 3.14159);

        double result = GsonUtil.getAsDouble(jsonObject, "doubleKey");
        assertThat(result).isCloseTo(3.14159, within(0.00001));
    }

    @Test
    @DisplayName("getAsBoolean 应该正确获取布尔值")
    void testGetAsBoolean() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("booleanKey", true);

        boolean result = GsonUtil.getAsBoolean(jsonObject, "booleanKey");
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("getAsJsonObject 应该正确获取JSON对象")
    void testGetAsJsonObject() {
        JsonObject jsonObject = new JsonObject();
        JsonObject nestedObject = new JsonObject();
        nestedObject.addProperty("nestedKey", "nestedValue");
        jsonObject.add("objectKey", nestedObject);

        JsonObject result = GsonUtil.getAsJsonObject(jsonObject, "objectKey");
        assertThat(result.get("nestedKey").getAsString()).isEqualTo("nestedValue");
    }

    @Test
    @DisplayName("getAsJsonArray 应该正确获取JSON数组")
    void testGetAsJsonArray() {
        JsonObject jsonObject = new JsonObject();
        JsonArray jsonArray = new JsonArray();
        jsonArray.add("item1");
        jsonArray.add("item2");
        jsonObject.add("arrayKey", jsonArray);

        JsonArray result = GsonUtil.getAsJsonArray(jsonObject, "arrayKey");
        assertThat(result.size()).isEqualTo(2);
        assertThat(result.get(0).getAsString()).isEqualTo("item1");
        assertThat(result.get(1).getAsString()).isEqualTo("item2");
    }

    @Test
    @DisplayName("getAsJsonPrimitive 应该正确获取JSON基本类型")
    void testGetAsJsonPrimitive() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("primitiveKey", "primitiveValue");

        JsonPrimitive result = GsonUtil.getAsJsonPrimitive(jsonObject, "primitiveKey");
        assertThat(result.getAsString()).isEqualTo("primitiveValue");
    }

    @Test
    @DisplayName("类型转换方法应该在类型不匹配时抛出异常")
    void testTypeConversionExceptions() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("stringValue", "notANumber");

        // 尝试将字符串转换为数字应该抛出异常
        assertThatThrownBy(() -> GsonUtil.getAsInt(jsonObject, "stringValue"))
                .isInstanceOf(NumberFormatException.class);
    }

    @Test
    @DisplayName("CARD_GSON 应该支持HTML转义禁用")
    void testCardGsonHtmlEscaping() {
        JsonObject testObject = new JsonObject();
        testObject.addProperty("html", "<b>Bold Text</b>");

        String result = GsonUtil.CARD_GSON.toJson(testObject);

        // CARD_GSON应该不转义HTML字符
        assertThat(result).contains("<b>Bold Text</b>");
    }

    @Test
    @DisplayName("NORMAL_GSON 应该正常工作")
    void testNormalGson() {
        JsonObject testObject = new JsonObject();
        testObject.addProperty("key", "value");

        String json = GsonUtil.NORMAL_GSON.toJson(testObject);
        JsonObject parsed = GsonUtil.NORMAL_GSON.fromJson(json, JsonObject.class);

        assertThat(parsed.get("key").getAsString()).isEqualTo("value");
    }
}