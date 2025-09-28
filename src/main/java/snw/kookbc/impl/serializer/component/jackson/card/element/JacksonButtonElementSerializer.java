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
import snw.jkook.message.component.card.element.ButtonElement;

import java.io.IOException;

/**
 * ButtonElement Jackson 序列化器
 * 将按钮元素序列化为标准的Kook卡片JSON格式
 */
public class JacksonButtonElementSerializer extends JsonSerializer<ButtonElement> {

    @Override
    public void serialize(ButtonElement value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeStartObject();
        gen.writeStringField("type", "button");

        // 序列化theme字段
        if (value.getTheme() != null) {
            gen.writeStringField("theme", value.getTheme().getValue());
        }

        // 序列化value字段
        if (value.getValue() != null) {
            gen.writeStringField("value", value.getValue());
        }

        // 序列化click字段
        if (value.getEventType() != null) {
            gen.writeStringField("click", value.getEventType().getValue());
        }

        // 序列化text字段
        if (value.getText() != null) {
            gen.writeFieldName("text");
            serializers.defaultSerializeValue(value.getText(), gen);
        }

        gen.writeEndObject();
    }
}