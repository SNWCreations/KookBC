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

package snw.kookbc.impl.serializer.component.jackson.card.module;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import snw.jkook.entity.abilities.Accessory;
import snw.jkook.message.component.card.CardScopeElement;
import snw.jkook.message.component.card.element.*;
import snw.jkook.message.component.card.module.SectionModule;
import snw.jkook.message.component.card.structure.Paragraph;
import snw.kookbc.util.JacksonCardUtil;

import java.io.IOException;

/**
 * SectionModule Jackson 序列化器
 * 处理章节模块的序列化和反序列化
 */
public class JacksonSectionModuleDeserializer extends JsonDeserializer<SectionModule> {

    @Override
    public SectionModule deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonNode node = p.getCodec().readTree(p);

        // 解析文本内容
        CardScopeElement text = null;
        if (JacksonCardUtil.has(node, "text")) {
            JsonNode textNode = node.get("text");
            String textType = JacksonCardUtil.getStringOrDefault(textNode, "type", "plain-text");
            
            switch (textType) {
                case "plain-text":
                    text = JacksonCardUtil.fromJson(textNode, PlainTextElement.class);
                    break;
                case "kmarkdown":
                    text = JacksonCardUtil.fromJson(textNode, MarkdownElement.class);
                    break;
                case "paragraph":
                    text = JacksonCardUtil.fromJson(textNode, Paragraph.class);
                    break;
                default:
                    throw new IOException("Unsupported text type in SectionModule: " + textType);
            }
        }

        // 解析模式
        Accessory.Mode mode = null;
        if (JacksonCardUtil.has(node, "mode")) {
            String modeValue = node.get("mode").asText();
            try {
                mode = Accessory.Mode.value(modeValue);
            } catch (Exception e) {
                // 日志记录但不抛出异常，使用 null 值
            }
        }

        // 解析附件
        Accessory accessory = null;
        if (JacksonCardUtil.has(node, "accessory")) {
            JsonNode accessoryNode = node.get("accessory");
            String accessoryType = JacksonCardUtil.getStringOrDefault(accessoryNode, "type", "");
            
            switch (accessoryType) {
                case "image":
                    accessory = JacksonCardUtil.fromJson(accessoryNode, ImageElement.class);
                    break;
                case "button":
                    accessory = JacksonCardUtil.fromJson(accessoryNode, ButtonElement.class);
                    break;
                default:
                    // 日志记录但不抛出异常，忽略不支持的附件类型
                    break;
            }
        }

        return new SectionModule(text, accessory, mode);
    }

    /**
     * SectionModule 序列化器
     */
    public static class SectionModuleSerializer extends JsonSerializer<SectionModule> {
        
        @Override
        public void serialize(SectionModule module, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            gen.writeStartObject();
            
            gen.writeStringField("type", "section");
            
            // 序列化文本
            CardScopeElement text = module.getText();
            if (text != null) {
                gen.writeObjectField("text", text);
            }
            
            // 序列化模式
            Accessory.Mode mode = module.getMode();
            if (mode != null) {
                gen.writeStringField("mode", mode.getValue());
            }
            
            // 序列化附件
            Accessory accessory = module.getAccessory();
            if (accessory != null) {
                gen.writeObjectField("accessory", accessory);
            }
            
            gen.writeEndObject();
        }
    }
}