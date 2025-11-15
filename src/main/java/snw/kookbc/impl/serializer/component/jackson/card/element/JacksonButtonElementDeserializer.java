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
import snw.jkook.message.component.card.Theme;
import snw.jkook.message.component.card.element.BaseElement;
import snw.jkook.message.component.card.element.ButtonElement;
import snw.jkook.message.component.card.element.MarkdownElement;
import snw.jkook.message.component.card.element.PlainTextElement;
import snw.kookbc.util.JacksonCardUtil;

import java.io.IOException;

/**
 * ButtonElement Jackson 序列化器
 * 处理按钮元素的序列化和反序列化
 */
public class JacksonButtonElementDeserializer extends JsonDeserializer<ButtonElement> {

    @Override
    public ButtonElement deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonNode node = p.getCodec().readTree(p);

        // 验证必需字段
        String theme = JacksonCardUtil.getRequiredString(node, "theme");
        
        // 解析按钮文本
        BaseElement text = null;
        if (JacksonCardUtil.has(node, "text")) {
            JsonNode textNode = node.get("text");
            if (textNode.isObject()) {
                String textType = JacksonCardUtil.getStringOrDefault(textNode, "type", "plain-text");
                if ("kmarkdown".equals(textType)) {
                    text = JacksonCardUtil.fromJson(textNode, MarkdownElement.class);
                } else {
                    text = JacksonCardUtil.fromJson(textNode, PlainTextElement.class);
                }
            } else if (textNode.isTextual()) {
                // 如果 text 是字符串，创建 PlainTextElement
                text = new PlainTextElement(textNode.asText());
            }
        }
        
        // 如果没有文本，使用空字符串
        if (text == null) {
            text = new PlainTextElement("");
        }

        // 解析事件类型和值
        String click = JacksonCardUtil.getStringOrDefault(node, "click", "");
        String value = JacksonCardUtil.getStringOrDefault(node, "value", "");

        try {
            Theme buttonTheme = Theme.value(theme);
            ButtonElement.EventType eventType = ButtonElement.EventType.value(click);
            return new ButtonElement(buttonTheme, value, eventType, text);
        } catch (Exception e) {
            throw new IOException("Failed to create ButtonElement: " + e.getMessage(), e);
        }
    }

    /**
     * ButtonElement 序列化器
     */
    public static class ButtonElementSerializer extends JsonSerializer<ButtonElement> {
        
        @Override
        public void serialize(ButtonElement element, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            gen.writeStartObject();
            
            gen.writeStringField("type", "button");
            gen.writeStringField("theme", element.getTheme().getValue());
            
            // 序列化按钮文本
            BaseElement textElement = element.getText();
            if (textElement != null) {
                gen.writeObjectField("text", textElement);
            } else {
                gen.writeStringField("text", "");
            }
            
            // 序列化事件类型
            ButtonElement.EventType eventType = element.getEventType();
            if (eventType != null) {
                gen.writeStringField("click", eventType.getValue());
            } else {
                gen.writeStringField("click", "");
            }
            
            // 序列化值
            String value = element.getValue();
            if (value != null) {
                gen.writeStringField("value", value);
            } else {
                gen.writeStringField("value", "");
            }
            
            gen.writeEndObject();
        }
    }
}