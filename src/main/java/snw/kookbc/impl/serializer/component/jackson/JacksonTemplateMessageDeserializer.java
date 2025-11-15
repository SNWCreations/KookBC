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

package snw.kookbc.impl.serializer.component.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import snw.jkook.message.component.TemplateMessage;

import java.io.IOException;

/**
 * TemplateMessage Jackson 序列化器
 * 处理模板消息的序列化和反序列化
 * TemplateMessage 通常只是一个字符串内容
 */
public class JacksonTemplateMessageDeserializer extends JsonDeserializer<TemplateMessage> {

    @Override
    public TemplateMessage deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String content = p.getValueAsString();
        if (content == null) {
            content = "";
        }
        // 根据代码分析，TemplateMessage构造器需要3个参数 (long id, String content, int type)
        // 由于我们从字符串反序列化，使用默认值
        return new TemplateMessage(0L, content, 1);
    }

    /**
     * TemplateMessage 序列化器
     */
    public static class TemplateMessageSerializer extends JsonSerializer<TemplateMessage> {
        
        @Override
        public void serialize(TemplateMessage value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            String content = value.getContent();
            gen.writeString(content != null ? content : "");
        }
    }
}