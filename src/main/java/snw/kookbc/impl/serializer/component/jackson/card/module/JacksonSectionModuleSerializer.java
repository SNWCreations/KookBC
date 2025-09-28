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
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import snw.jkook.message.component.card.module.SectionModule;

import java.io.IOException;

/**
 * SectionModule Jackson 序列化器
 * 将节模块序列化为标准的Kook卡片JSON格式
 */
public class JacksonSectionModuleSerializer extends JsonSerializer<SectionModule> {

    @Override
    public void serialize(SectionModule value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeStartObject();
        gen.writeStringField("type", "section");

        // 序列化text字段
        if (value.getText() != null) {
            gen.writeFieldName("text");
            serializers.defaultSerializeValue(value.getText(), gen);
        }

        // 序列化accessory字段（如果存在）
        if (value.getAccessory() != null) {
            gen.writeFieldName("accessory");
            serializers.defaultSerializeValue(value.getAccessory(), gen);
        }

        // 序列化mode字段
        if (value.getMode() != null) {
            gen.writeStringField("mode", value.getMode().getValue());
        }

        gen.writeEndObject();
    }
}