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
import snw.jkook.message.component.card.Size;
import snw.jkook.message.component.card.element.ImageElement;
import snw.kookbc.util.JacksonCardUtil;

import java.io.IOException;

/**
 * ImageElement Jackson 序列化器
 * 处理图片元素的序列化和反序列化
 */
public class JacksonImageElementDeserializer extends JsonDeserializer<ImageElement> {

    @Override
    public ImageElement deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonNode node = p.getCodec().readTree(p);

        // 获取必需字段
        String src = JacksonCardUtil.getRequiredString(node, "src");

        // 获取可选字段，使用默认值
        String size = JacksonCardUtil.getStringOrDefault(node, "size", Size.LG.getValue());
        String alt = JacksonCardUtil.getStringOrDefault(node, "alt", "");
        boolean circle = JacksonCardUtil.getBooleanOrDefault(node, "circle", false);

        try {
            Size imageSize = Size.value(size);
            return new ImageElement(src, alt, imageSize, circle);
        } catch (Exception e) {
            throw new IOException("Failed to create ImageElement with src='" + src + "', size='" + size + "': " + e.getMessage(), e);
        }
    }

    /**
     * ImageElement 序列化器
     */
    public static class ImageElementSerializer extends JsonSerializer<ImageElement> {
        
        @Override
        public void serialize(ImageElement element, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            gen.writeStartObject();
            
            gen.writeStringField("type", "image");
            gen.writeStringField("src", element.getSource());
            
            // 序列化尺寸
            Size size = element.getSize();
            if (size != null) {
                gen.writeStringField("size", size.getValue());
            } else {
                gen.writeStringField("size", Size.LG.getValue());
            }
            
            // 序列化 alt 文本
            String alt = element.getAlt();
            gen.writeStringField("alt", alt != null ? alt : "");
            
            // 序列化圆形标识
            gen.writeBooleanField("circle", element.isCircled());
            
            gen.writeEndObject();
        }
    }
}