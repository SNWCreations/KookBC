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
import snw.jkook.message.component.card.Size;
import snw.jkook.message.component.card.Theme;
import snw.jkook.message.component.card.module.*;
import snw.kookbc.util.JacksonCardUtil;

import java.io.IOException;
import java.util.*;

/**
 * CardComponent Jackson 序列化器
 * 处理卡片组件的序列化和反序列化
 */
public class JacksonCardComponentDeserializer extends JsonDeserializer<CardComponent> {

    public static final Map<String, Class<? extends BaseModule>> MODULE_MAP;

    static {
        Map<String, Class<? extends BaseModule>> mutableMap = new HashMap<>();
        mutableMap.put("action-group", ActionGroupModule.class);
        mutableMap.put("container", ContainerModule.class);
        mutableMap.put("context", ContextModule.class);
        mutableMap.put("countdown", CountdownModule.class);
        mutableMap.put("divider", DividerModule.class);
        mutableMap.put("file", FileModule.class);
        mutableMap.put("audio", FileModule.class);
        mutableMap.put("video", FileModule.class);
        mutableMap.put("header", HeaderModule.class);
        mutableMap.put("image-group", ImageGroupModule.class);
        mutableMap.put("invite", InviteModule.class);
        mutableMap.put("section", SectionModule.class);
        MODULE_MAP = Collections.unmodifiableMap(mutableMap);
    }

    @Override
    public CardComponent deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonNode node = p.getCodec().readTree(p);

        // 解析基本属性
        String size = JacksonCardUtil.getRequiredString(node, "size");
        String theme = JacksonCardUtil.getStringOrDefault(node, "theme", null);
        String color = JacksonCardUtil.getStringOrDefault(node, "color", null);
        
        // 如果 color 是空字符串，设置为 null
        if (color != null && color.trim().isEmpty()) {
            color = null;
        }

        // 解析模块列表
        List<BaseModule> modules = new ArrayList<>();
        if (JacksonCardUtil.has(node, "modules")) {
            JsonNode modulesNode = node.get("modules");
            if (modulesNode.isArray()) {
                for (JsonNode moduleNode : modulesNode) {
                    try {
                        String moduleType = JacksonCardUtil.getRequiredString(moduleNode, "type");
                        BaseModule module = processModule(moduleNode, moduleType);
                        if (module != null) {
                            modules.add(module);
                        }
                    } catch (Exception e) {
                        // 日志记录但不中断处理，跳过无效模块
                    }
                }
            }
        }

        try {
            Size cardSize = Size.value(size);
            Theme cardTheme = theme != null ? Theme.value(theme) : null;
            return new CardComponent(modules, cardSize, cardTheme, color);
        } catch (Exception e) {
            throw new IOException("Failed to create CardComponent: " + e.getMessage(), e);
        }
    }

    private BaseModule processModule(JsonNode moduleNode, String type) {
        Class<? extends BaseModule> moduleClass = MODULE_MAP.get(type);
        if (moduleClass == null) {
            throw new IllegalArgumentException("Unsupported module type: " + type);
        }
        return JacksonCardUtil.fromJson(moduleNode, moduleClass);
    }

    /**
     * CardComponent 序列化器
     */
    public static class CardComponentSerializer extends JsonSerializer<CardComponent> {
        
        @Override
        public void serialize(CardComponent component, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            gen.writeStartObject();
            
            gen.writeStringField("type", "card");
            
            // 序列化尺寸
            Size size = component.getSize();
            if (size != null) {
                gen.writeStringField("size", size.getValue());
            }
            
            // 序列化主题或颜色
            String color = component.getColor();
            if (color != null && !color.isEmpty()) {
                gen.writeStringField("color", color);
            } else {
                Theme theme = component.getTheme();
                if (theme != null) {
                    gen.writeStringField("theme", theme.getValue());
                }
            }
            
            // 序列化模块列表
            gen.writeObjectField("modules", component.getModules());
            
            gen.writeEndObject();
        }
    }
}