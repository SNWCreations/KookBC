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

import snw.jkook.message.component.card.module.InviteModule;
import snw.kookbc.util.JacksonCardUtil;

public class JacksonInviteModuleDeserializer extends JsonDeserializer<InviteModule> {
    @Override
    public InviteModule deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonNode node = p.getCodec().readTree(p);

        // InviteModule构造器需要String code参数
        String code = JacksonCardUtil.getStringOrDefault(node, "code", "");
        return new InviteModule(code);
    }
}
