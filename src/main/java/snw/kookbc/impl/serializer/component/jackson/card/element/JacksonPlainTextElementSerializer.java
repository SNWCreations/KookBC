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
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import snw.jkook.message.component.card.element.PlainTextElement;

import java.io.IOException;

/**
 * PlainTextElement Jackson 序列化器
 * 将纯文本元素序列化为标准的Kook卡片JSON格式
 */
public class JacksonPlainTextElementSerializer extends JsonSerializer<PlainTextElement> {

    @Override
    public void serialize(PlainTextElement value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeStartObject();
        gen.writeStringField("type", "plain-text");

        // 序列化content字段
        if (value.getContent() != null) {
            gen.writeStringField("content", value.getContent());
        }

        // PlainTextElement默认支持emoji
        gen.writeBooleanField("emoji", true);

        gen.writeEndObject();
    }
}