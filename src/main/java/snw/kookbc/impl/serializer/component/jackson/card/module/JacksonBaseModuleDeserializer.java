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
import snw.jkook.message.component.card.module.*;
import snw.kookbc.util.JacksonCardUtil;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * BaseModule 多态反序列化器
 * 根据 JSON 中的 type 字段选择正确的 Module 实现类进行反序列化
 */
public class JacksonBaseModuleDeserializer extends JsonDeserializer<BaseModule> {

    private static final Map<String, Class<? extends BaseModule>> MODULE_TYPE_MAP = new HashMap<>();

    static {
        // 注册各种模块类型映射
        MODULE_TYPE_MAP.put("section", SectionModule.class);
        MODULE_TYPE_MAP.put("action-group", ActionGroupModule.class);
        MODULE_TYPE_MAP.put("container", ContainerModule.class);
        MODULE_TYPE_MAP.put("context", ContextModule.class);
        MODULE_TYPE_MAP.put("countdown", CountdownModule.class);
        MODULE_TYPE_MAP.put("divider", DividerModule.class);
        MODULE_TYPE_MAP.put("file", FileModule.class);
        MODULE_TYPE_MAP.put("header", HeaderModule.class);
        MODULE_TYPE_MAP.put("image-group", ImageGroupModule.class);
        MODULE_TYPE_MAP.put("invite", InviteModule.class);
    }

    @Override
    public BaseModule deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonNode node = p.getCodec().readTree(p);

        // 获取 type 字段，确定具体的模块类型
        if (!node.has("type")) {
            throw new IllegalArgumentException("Missing required 'type' field in module JSON");
        }

        String type = node.get("type").asText();
        if (type == null || type.trim().isEmpty()) {
            throw new IllegalArgumentException("Module type cannot be null or empty");
        }

        Class<? extends BaseModule> moduleClass = MODULE_TYPE_MAP.get(type);
        if (moduleClass == null) {
            throw new IllegalArgumentException("Unknown module type: " + type + ". Supported types: " 
                + String.join(", ", MODULE_TYPE_MAP.keySet()));
        }

        try {
            // 使用 JacksonCardUtil 的 mapper 进行反序列化，确保使用正确的序列化器
            return JacksonCardUtil.fromJson(node, moduleClass);
        } catch (Exception e) {
            throw new IOException("Failed to deserialize module of type '" + type + "': " + e.getMessage(), e);
        }
    }

    /**
     * 获取支持的模块类型列表
     * @return 模块类型到类的映射
     */
    public static Map<String, Class<? extends BaseModule>> getSupportedTypes() {
        return new HashMap<>(MODULE_TYPE_MAP);
    }

    /**
     * 注册新的模块类型（用于扩展）
     * @param type 模块类型字符串
     * @param moduleClass 对应的模块类
     */
    public static void registerModuleType(String type, Class<? extends BaseModule> moduleClass) {
        if (type == null || type.trim().isEmpty()) {
            throw new IllegalArgumentException("Module type cannot be null or empty");
        }
        if (moduleClass == null) {
            throw new IllegalArgumentException("Module class cannot be null");
        }
        MODULE_TYPE_MAP.put(type, moduleClass);
    }

    /**
     * 检查是否支持指定的模块类型
     * @param type 模块类型
     * @return true 如果支持
     */
    public static boolean isSupported(String type) {
        return type != null && MODULE_TYPE_MAP.containsKey(type);
    }
}