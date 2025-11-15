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
package snw.kookbc.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import snw.jkook.message.component.TemplateMessage;
import snw.jkook.message.component.card.CardComponent;
import snw.jkook.message.component.card.MultipleCardComponent;
import snw.jkook.message.component.card.element.BaseElement;
import snw.jkook.message.component.card.element.ButtonElement;
import snw.jkook.message.component.card.element.ImageElement;
import snw.jkook.message.component.card.element.MarkdownElement;
import snw.jkook.message.component.card.element.PlainTextElement;
import snw.jkook.message.component.card.module.BaseModule;
import snw.jkook.message.component.card.module.ActionGroupModule;
import snw.jkook.message.component.card.module.ContainerModule;
import snw.jkook.message.component.card.module.ContextModule;
import snw.jkook.message.component.card.module.CountdownModule;
import snw.jkook.message.component.card.module.DividerModule;
import snw.jkook.message.component.card.module.FileModule;
import snw.jkook.message.component.card.module.HeaderModule;
import snw.jkook.message.component.card.module.ImageGroupModule;
import snw.jkook.message.component.card.module.InviteModule;
import snw.jkook.message.component.card.module.SectionModule;
import snw.jkook.message.component.card.structure.Paragraph;
import snw.kookbc.impl.serializer.component.jackson.JacksonTemplateMessageDeserializer;
import snw.kookbc.impl.serializer.component.jackson.card.JacksonCardComponentDeserializer;
import snw.kookbc.impl.serializer.component.jackson.card.JacksonMultipleCardComponentDeserializer;
import snw.kookbc.impl.serializer.component.jackson.card.element.JacksonBaseElementDeserializer;
import snw.kookbc.impl.serializer.component.jackson.card.element.JacksonButtonElementDeserializer;
import snw.kookbc.impl.serializer.component.jackson.card.element.JacksonButtonElementSerializer;
import snw.kookbc.impl.serializer.component.jackson.card.element.JacksonImageElementDeserializer;
import snw.kookbc.impl.serializer.component.jackson.card.element.JacksonContentElementDeserializer;
import snw.kookbc.impl.serializer.component.jackson.card.element.JacksonPlainTextElementSerializer;
import snw.kookbc.impl.serializer.component.jackson.card.element.JacksonMarkdownElementSerializer;
import snw.kookbc.impl.serializer.component.jackson.card.module.JacksonBaseModuleDeserializer;
import snw.kookbc.impl.serializer.component.jackson.card.module.JacksonActionGroupModuleDeserializer;
import snw.kookbc.impl.serializer.component.jackson.card.module.JacksonActionGroupModuleSerializer;
import snw.kookbc.impl.serializer.component.jackson.card.module.JacksonContainerModuleDeserializer;
import snw.kookbc.impl.serializer.component.jackson.card.module.JacksonContextModuleDeserializer;
import snw.kookbc.impl.serializer.component.jackson.card.module.JacksonContextModuleSerializer;
import snw.kookbc.impl.serializer.component.jackson.card.module.JacksonCountdownModuleDeserializer;
import snw.kookbc.impl.serializer.component.jackson.card.module.JacksonDividerModuleDeserializer;
import snw.kookbc.impl.serializer.component.jackson.card.module.JacksonDividerModuleSerializer;
import snw.kookbc.impl.serializer.component.jackson.card.module.JacksonFileModuleDeserializer;
import snw.kookbc.impl.serializer.component.jackson.card.module.JacksonHeaderModuleDeserializer;
import snw.kookbc.impl.serializer.component.jackson.card.module.JacksonHeaderModuleSerializer;
import snw.kookbc.impl.serializer.component.jackson.card.module.JacksonImageGroupModuleDeserializer;
import snw.kookbc.impl.serializer.component.jackson.card.module.JacksonInviteModuleDeserializer;
import snw.kookbc.impl.serializer.component.jackson.card.module.JacksonSectionModuleDeserializer;
import snw.kookbc.impl.serializer.component.jackson.card.module.JacksonSectionModuleSerializer;
import snw.kookbc.impl.serializer.component.jackson.card.structure.JacksonParagraphDeserializer;

/**
 * Jackson卡片消息处理工具类
 * 提供高性能、null-safe的卡片消息序列化/反序列化功能
 */
public final class JacksonCardUtil {

    private static final ObjectMapper CARD_MAPPER;

    static {
        CARD_MAPPER = new ObjectMapper();

        // 配置JSON处理选项
        CARD_MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        CARD_MAPPER.configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, false);
        // 关键修复：允许序列化空Bean对象（如DividerModule）
        CARD_MAPPER.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        CARD_MAPPER.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        // 创建自定义序列化器模块
        SimpleModule cardModule = new SimpleModule("KookCardModule");

        // 注册顶层组件序列化器
        cardModule.addDeserializer(TemplateMessage.class, new JacksonTemplateMessageDeserializer());
        cardModule.addDeserializer(CardComponent.class, new JacksonCardComponentDeserializer());
        cardModule.addSerializer(CardComponent.class, new JacksonCardComponentDeserializer.CardComponentSerializer());
        cardModule.addDeserializer(MultipleCardComponent.class, new JacksonMultipleCardComponentDeserializer());
        cardModule.addSerializer(MultipleCardComponent.class, new JacksonMultipleCardComponentDeserializer.MultipleCardComponentSerializer());

        // 注册元素序列化器（多态处理）
        cardModule.addDeserializer(BaseElement.class, new JacksonBaseElementDeserializer());
        cardModule.addDeserializer(ButtonElement.class, new JacksonButtonElementDeserializer());
        cardModule.addSerializer(ButtonElement.class, new JacksonButtonElementSerializer());
        cardModule.addDeserializer(ImageElement.class, new JacksonImageElementDeserializer());
        cardModule.addSerializer(ImageElement.class, new JacksonImageElementDeserializer.ImageElementSerializer());
        cardModule.addDeserializer(MarkdownElement.class, new JacksonContentElementDeserializer<>(MarkdownElement::new));
        cardModule.addSerializer(MarkdownElement.class, new JacksonMarkdownElementSerializer());
        cardModule.addDeserializer(PlainTextElement.class, new JacksonContentElementDeserializer<>(PlainTextElement::new));
        cardModule.addSerializer(PlainTextElement.class, new JacksonPlainTextElementSerializer());

        // 注册模块序列化器（多态处理）
        cardModule.addDeserializer(BaseModule.class, new JacksonBaseModuleDeserializer());
        cardModule.addDeserializer(ActionGroupModule.class, new JacksonActionGroupModuleDeserializer());
        cardModule.addSerializer(ActionGroupModule.class, new JacksonActionGroupModuleSerializer());
        cardModule.addDeserializer(ContainerModule.class, new JacksonContainerModuleDeserializer());
        cardModule.addDeserializer(ContextModule.class, new JacksonContextModuleDeserializer());
        cardModule.addSerializer(ContextModule.class, new JacksonContextModuleSerializer());
        cardModule.addDeserializer(CountdownModule.class, new JacksonCountdownModuleDeserializer());
        cardModule.addDeserializer(DividerModule.class, new JacksonDividerModuleDeserializer());
        // 注册DividerModule的Jackson序列化器
        cardModule.addSerializer(DividerModule.class, new JacksonDividerModuleSerializer());
        cardModule.addDeserializer(FileModule.class, new JacksonFileModuleDeserializer());
        cardModule.addDeserializer(HeaderModule.class, new JacksonHeaderModuleDeserializer());
        cardModule.addSerializer(HeaderModule.class, new JacksonHeaderModuleSerializer());
        cardModule.addDeserializer(ImageGroupModule.class, new JacksonImageGroupModuleDeserializer());
        cardModule.addDeserializer(InviteModule.class, new JacksonInviteModuleDeserializer());
        cardModule.addDeserializer(SectionModule.class, new JacksonSectionModuleDeserializer());
        cardModule.addSerializer(SectionModule.class, new JacksonSectionModuleSerializer());

        // 注册结构序列化器
        cardModule.addDeserializer(Paragraph.class, new JacksonParagraphDeserializer());

        CARD_MAPPER.registerModule(cardModule);
    }

    private JacksonCardUtil() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    // ===== 卡片消息专用序列化方法 =====

    public static <T> T fromJson(String json, Class<T> classOfT) {
        try {
            return CARD_MAPPER.readValue(json, classOfT);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to deserialize card JSON to " + classOfT.getName(), e);
        }
    }

    public static <T> T fromJson(JsonNode node, Class<T> classOfT) {
        try {
            return CARD_MAPPER.treeToValue(node, classOfT);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to deserialize card JsonNode to " + classOfT.getName(), e);
        }
    }

    public static String toJson(Object obj) {
        try {
            return CARD_MAPPER.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize card object to JSON", e);
        }
    }

    public static JsonNode toJsonNode(Object obj) {
        return CARD_MAPPER.valueToTree(obj);
    }

    public static JsonNode parse(String json) {
        try {
            return CARD_MAPPER.readTree(json);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to parse card JSON: " + json, e);
        }
    }

    // ===== null-safe字段访问方法 =====

    public static boolean has(JsonNode node, String fieldName) {
        JsonNode field = node.get(fieldName);
        return field != null && !field.isNull();
    }

    public static String getRequiredString(JsonNode node, String fieldName) {
        JsonNode field = node.get(fieldName);
        if (field == null || field.isNull()) {
            throw new IllegalArgumentException("Required field '" + fieldName + "' is missing or null");
        }
        return field.asText();
    }

    public static String getStringOrDefault(JsonNode node, String fieldName, String defaultValue) {
        JsonNode field = node.get(fieldName);
        return (field != null && !field.isNull()) ? field.asText() : defaultValue;
    }

    public static int getIntOrDefault(JsonNode node, String fieldName, int defaultValue) {
        JsonNode field = node.get(fieldName);
        return (field != null && !field.isNull()) ? field.asInt() : defaultValue;
    }

    public static long getLongOrDefault(JsonNode node, String fieldName, long defaultValue) {
        JsonNode field = node.get(fieldName);
        return (field != null && !field.isNull()) ? field.asLong() : defaultValue;
    }

    public static boolean getBooleanOrDefault(JsonNode node, String fieldName, boolean defaultValue) {
        JsonNode field = node.get(fieldName);
        return (field != null && !field.isNull()) ? field.asBoolean() : defaultValue;
    }

    public static double getDoubleOrDefault(JsonNode node, String fieldName, double defaultValue) {
        JsonNode field = node.get(fieldName);
        return (field != null && !field.isNull()) ? field.asDouble() : defaultValue;
    }

    // 获取配置好的ObjectMapper实例
    public static ObjectMapper getMapper() {
        return CARD_MAPPER;
    }
}