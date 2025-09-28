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

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import snw.jkook.message.component.card.module.DividerModule;

import java.io.IOException;

/**
 * DividerModule Jackson 反序列化器
 * 处理分割线模块的反序列化（简单模块，无额外参数）
 */
public class JacksonDividerModuleDeserializer extends JsonDeserializer<DividerModule> {

    @Override
    public DividerModule deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        // DividerModule 没有额外的参数，使用静态实例
        // 还是要读取 JSON 以保持解析器的一致性
        p.getCodec().readTree(p);
        return DividerModule.INSTANCE;
    }
}