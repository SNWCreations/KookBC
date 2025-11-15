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

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import snw.kookbc.util.JacksonCardUtil;

import java.io.IOException;
import java.util.function.Function;

/**
 * 泛型内容元素序列化器
 * 用于处理只包含 type 和 content 字段的简单元素，如 PlainTextElement 和 MarkdownElement
 * 
 * @param <T> 元素类型
 */
public class JacksonContentElementDeserializer<T> extends JsonDeserializer<T> {

    private final Function<String, T> constructor;

    /**
     * 创建内容元素反序列化器
     * @param constructor 构造函数，接受字符串内容并返回对应的元素对象
     */
    public JacksonContentElementDeserializer(Function<String, T> constructor) {
        this.constructor = constructor;
        if (constructor == null) {
            throw new IllegalArgumentException("Constructor function cannot be null");
        }
    }

    @Override
    public T deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonNode node = p.getCodec().readTree(p);
        
        // 验证必需字段
        if (!node.has("type")) {
            throw new IllegalArgumentException("Missing required 'type' field in content element JSON");
        }
        
        if (!node.has("content")) {
            throw new IllegalArgumentException("Missing required 'content' field in content element JSON");
        }

        // 获取内容并创建对象
        String content = JacksonCardUtil.getStringOrDefault(node, "content", "");
        
        try {
            return constructor.apply(content);
        } catch (Exception e) {
            String type = JacksonCardUtil.getStringOrDefault(node, "type", "unknown");
            throw new IOException("Failed to create content element of type '" + type + "' with content: " + content, e);
        }
    }

    /**
     * 创建一个新的内容元素序列化器（静态工厂方法）
     * @param constructor 构造函数
     * @param <U> 元素类型
     * @return 新的序列化器实例
     */
    public static <U> JacksonContentElementDeserializer<U> create(Function<String, U> constructor) {
        return new JacksonContentElementDeserializer<>(constructor);
    }

    /**
     * 内容元素序列化器（支持序列化和反序列化）
     * @param <T> 元素类型
     */
    public static class ContentElementSerializer<T> extends JsonSerializer<T> {
        
        private final String type;
        private final Function<T, String> contentExtractor;
        
        public ContentElementSerializer(String type, Function<T, String> contentExtractor) {
            this.type = type;
            this.contentExtractor = contentExtractor;
            if (type == null || type.trim().isEmpty()) {
                throw new IllegalArgumentException("Type cannot be null or empty");
            }
            if (contentExtractor == null) {
                throw new IllegalArgumentException("Content extractor cannot be null");
            }
        }
        
        @Override
        public void serialize(T value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            gen.writeStartObject();
            gen.writeStringField("type", type);
            gen.writeStringField("content", contentExtractor.apply(value));
            gen.writeEndObject();
        }
    }
}