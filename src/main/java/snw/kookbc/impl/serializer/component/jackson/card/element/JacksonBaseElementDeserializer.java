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

package snw.kookbc.impl.serializer.component.jackson.card.element;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import snw.jkook.message.component.card.element.*;
import snw.kookbc.util.JacksonCardUtil;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * BaseElement 多态反序列化器
 * 根据 JSON 中的 type 字段选择正确的 Element 实现类进行反序列化
 */
public class JacksonBaseElementDeserializer extends JsonDeserializer<BaseElement> {

    private static final Map<String, Class<? extends BaseElement>> ELEMENT_TYPE_MAP = new HashMap<>();

    static {
        // 注册各种元素类型映射
        // Paragraph不是BaseElement的子类，应该移除
        ELEMENT_TYPE_MAP.put("button", ButtonElement.class);
        ELEMENT_TYPE_MAP.put("image", ImageElement.class);
        ELEMENT_TYPE_MAP.put("plain-text", PlainTextElement.class);
        ELEMENT_TYPE_MAP.put("kmarkdown", MarkdownElement.class);
        // paragraph已移除，因为Paragraph是BaseStructure而不是BaseElement
    }

    @Override
    public BaseElement deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonNode node = p.getCodec().readTree(p);

        // 获取 type 字段，确定具体的元素类型
        if (!node.has("type")) {
            throw new IllegalArgumentException("Missing required 'type' field in element JSON");
        }

        String type = node.get("type").asText();
        if (type == null || type.trim().isEmpty()) {
            throw new IllegalArgumentException("Element type cannot be null or empty");
        }

        Class<? extends BaseElement> elementClass = ELEMENT_TYPE_MAP.get(type);
        if (elementClass == null) {
            throw new IllegalArgumentException("Unknown element type: " + type);
        }

        try {
            // 使用 JacksonCardUtil 的 mapper 进行反序列化，确保使用正确的序列化器
            return JacksonCardUtil.fromJson(node, elementClass);
        } catch (Exception e) {
            throw new IOException("Failed to deserialize element of type: " + type, e);
        }
    }

    /**
     * 获取支持的元素类型列表
     * @return 元素类型到类的映射
     */
    public static Map<String, Class<? extends BaseElement>> getSupportedTypes() {
        return new HashMap<>(ELEMENT_TYPE_MAP);
    }

    /**
     * 注册新的元素类型（用于扩展）
     * @param type 元素类型字符串
     * @param elementClass 对应的元素类
     */
    public static void registerElementType(String type, Class<? extends BaseElement> elementClass) {
        if (type == null || type.trim().isEmpty()) {
            throw new IllegalArgumentException("Element type cannot be null or empty");
        }
        if (elementClass == null) {
            throw new IllegalArgumentException("Element class cannot be null");
        }
        ELEMENT_TYPE_MAP.put(type, elementClass);
    }
}