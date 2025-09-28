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

package snw.kookbc.impl.serializer.component.jackson.card.structure;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import snw.jkook.message.component.card.element.BaseElement;
import snw.jkook.message.component.card.element.MarkdownElement;
import snw.jkook.message.component.card.element.PlainTextElement;
import snw.jkook.message.component.card.structure.Paragraph;
import snw.kookbc.util.JacksonCardUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Paragraph Jackson 反序列化器
 * 处理段落结构的反序列化
 */
public class JacksonParagraphDeserializer extends JsonDeserializer<Paragraph> {

    @Override
    public Paragraph deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonNode node = p.getCodec().readTree(p);

        // 解析字段列表
        List<BaseElement> fields = new ArrayList<>();
        
        if (JacksonCardUtil.has(node, "fields")) {
            JsonNode fieldsNode = node.get("fields");
            if (fieldsNode.isArray()) {
                for (JsonNode fieldNode : fieldsNode) {
                    try {
                        String type = JacksonCardUtil.getStringOrDefault(fieldNode, "type", "plain-text");
                        BaseElement field = "kmarkdown".equals(type) ?
                            JacksonCardUtil.fromJson(fieldNode, MarkdownElement.class) :
                            JacksonCardUtil.fromJson(fieldNode, PlainTextElement.class);
                        if (field != null) {
                            fields.add(field);
                        }
                    } catch (Exception e) {
                        // 跳过无效字段
                    }
                }
            }
        }

        // 解析列数
        int cols = JacksonCardUtil.getIntOrDefault(node, "cols", 1);

        return new Paragraph(cols, fields);
    }
}