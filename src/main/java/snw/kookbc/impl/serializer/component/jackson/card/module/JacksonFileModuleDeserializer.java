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

import snw.jkook.message.component.FileComponent;
import snw.jkook.message.component.card.module.FileModule;
import snw.kookbc.util.JacksonCardUtil;

public class JacksonFileModuleDeserializer extends JsonDeserializer<FileModule> {
    @Override
    public FileModule deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonNode node = p.getCodec().readTree(p);

        // FileModule构造器需要(FileComponent.Type, String src, String title, String cover)
        String typeStr = JacksonCardUtil.getStringOrDefault(node, "type", "file");
        String src = JacksonCardUtil.getStringOrDefault(node, "src", "");
        String title = JacksonCardUtil.getStringOrDefault(node, "title", "");
        String cover = JacksonCardUtil.getStringOrDefault(node, "cover", null);

        FileComponent.Type type = FileComponent.Type.value(typeStr);
        return new FileModule(type, src, title, cover);
    }
}
