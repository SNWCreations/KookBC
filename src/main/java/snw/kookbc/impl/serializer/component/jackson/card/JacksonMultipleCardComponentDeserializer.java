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

package snw.kookbc.impl.serializer.component.jackson.card;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import snw.jkook.message.component.card.CardComponent;
import snw.jkook.message.component.card.MultipleCardComponent;
import snw.kookbc.util.JacksonCardUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * MultipleCardComponent Jackson 序列化器
 * 处理多卡片组件的序列化和反序列化
 */
public class JacksonMultipleCardComponentDeserializer extends JsonDeserializer<MultipleCardComponent> {

    @Override
    public MultipleCardComponent deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonNode node = p.getCodec().readTree(p);

        List<CardComponent> components = new ArrayList<>();
        
        if (node.isArray()) {
            for (JsonNode cardNode : node) {
                try {
                    CardComponent card = JacksonCardUtil.fromJson(cardNode, CardComponent.class);
                    if (card != null) {
                        components.add(card);
                    }
                } catch (Exception e) {
                    // 日志记录但不中断处理，跳过无效卡片
                }
            }
        } else {
            throw new IOException("MultipleCardComponent must be a JSON array");
        }

        return new MultipleCardComponent(components);
    }

    /**
     * MultipleCardComponent 序列化器
     */
    public static class MultipleCardComponentSerializer extends JsonSerializer<MultipleCardComponent> {
        
        @Override
        public void serialize(MultipleCardComponent value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            // 直接序列化为卡片数组
            gen.writeObject(value.getComponents());
        }
    }
}