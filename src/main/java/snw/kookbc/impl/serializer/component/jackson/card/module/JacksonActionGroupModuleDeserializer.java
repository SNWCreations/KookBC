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
import com.fasterxml.jackson.databind.JsonNode;
import snw.jkook.message.component.card.element.ButtonElement;
import snw.jkook.message.component.card.element.InteractElement;
import snw.jkook.message.component.card.module.ActionGroupModule;
import snw.kookbc.util.JacksonCardUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * ActionGroupModule Jackson 反序列化器
 * 处理动作组模块的反序列化
 */
public class JacksonActionGroupModuleDeserializer extends JsonDeserializer<ActionGroupModule> {

    @Override
    public ActionGroupModule deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonNode node = p.getCodec().readTree(p);

        // 解析按钮元素列表，ActionGroupModule需要List<InteractElement>
        List<InteractElement> elements = new ArrayList<>();

        if (JacksonCardUtil.has(node, "elements")) {
            JsonNode elementsNode = node.get("elements");
            if (elementsNode.isArray()) {
                for (JsonNode elementNode : elementsNode) {
                    try {
                        // ButtonElement实现了InteractElement接口
                        ButtonElement button = JacksonCardUtil.fromJson(elementNode, ButtonElement.class);
                        if (button != null) {
                            elements.add(button);
                        }
                    } catch (Exception e) {
                        // 日志记录但不中断处理，跳过无效的按钮
                    }
                }
            }
        }

        return new ActionGroupModule(elements);
    }
}