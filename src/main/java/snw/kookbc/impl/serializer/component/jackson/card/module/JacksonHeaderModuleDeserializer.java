/*
 *     KookBC -- The Kook Bot Client & JKook API standard implementation for Java.
 *     Copyright (C) 2022 - 2023 KookBC contributors
 */

package snw.kookbc.impl.serializer.component.jackson.card.module;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;

import snw.jkook.message.component.card.element.PlainTextElement;
import snw.jkook.message.component.card.module.HeaderModule;
import snw.kookbc.util.JacksonCardUtil;

public class JacksonHeaderModuleDeserializer extends JsonDeserializer<HeaderModule> {
    @Override
    public HeaderModule deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonNode node = p.getCodec().readTree(p);

        // HeaderModule构造器需要PlainTextElement参数
        PlainTextElement textElement;

        if (JacksonCardUtil.has(node, "text")) {
            JsonNode textNode = node.get("text");
            String content = JacksonCardUtil.getStringOrDefault(textNode, "content", "");
            textElement = new PlainTextElement(content);
        } else {
            // 如果没有text字段，使用空内容创建PlainTextElement
            textElement = new PlainTextElement("");
        }

        return new HeaderModule(textElement);
    }
}
