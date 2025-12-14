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
import snw.jkook.message.component.card.module.ActionGroupModule;

import java.io.IOException;

/**
 * ActionGroupModule Jackson 序列化器
 * 将操作组模块序列化为标准的Kook卡片JSON格式
 */
public class JacksonActionGroupModuleSerializer extends JsonSerializer<ActionGroupModule> {

    @Override
    public void serialize(ActionGroupModule value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeStartObject();
        gen.writeStringField("type", "action-group");

        // 序列化elements字段
        if (value.getButtons() != null && !value.getButtons().isEmpty()) {
            gen.writeFieldName("elements");
            serializers.defaultSerializeValue(value.getButtons(), gen);
        }

        gen.writeEndObject();
    }
}