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

import snw.jkook.message.component.card.module.CountdownModule;
import snw.kookbc.util.JacksonCardUtil;

public class JacksonCountdownModuleDeserializer extends JsonDeserializer<CountdownModule> {
    @Override
    public CountdownModule deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonNode node = p.getCodec().readTree(p);

        // CountdownModule构造器需要(CountdownModule.Type, long startTime, long endTime)
        String modeStr = JacksonCardUtil.getStringOrDefault(node, "mode", "day");
        long startTime = JacksonCardUtil.getLongOrDefault(node, "startTime", 0L);
        long endTime = JacksonCardUtil.getLongOrDefault(node, "endTime", System.currentTimeMillis() + 86400000L); // 默认24小时后

        CountdownModule.Type type = CountdownModule.Type.value(modeStr);
        return new CountdownModule(type, startTime, endTime);
    }
}
