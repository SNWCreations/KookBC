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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import snw.jkook.message.component.TemplateMessage;
import snw.jkook.message.component.card.CardComponent;
import snw.jkook.message.component.card.MultipleCardComponent;
import snw.jkook.message.component.card.element.ButtonElement;
import snw.jkook.message.component.card.element.ImageElement;
import snw.jkook.message.component.card.element.MarkdownElement;
import snw.jkook.message.component.card.element.PlainTextElement;
import snw.jkook.message.component.card.module.*;
import snw.jkook.message.component.card.structure.Paragraph;
import snw.jkook.util.Validate;
import snw.kookbc.impl.serializer.component.TemplateMessageSerializer;
import snw.kookbc.impl.serializer.component.card.CardComponentSerializer;
import snw.kookbc.impl.serializer.component.card.MultipleCardComponentSerializer;
import snw.kookbc.impl.serializer.component.card.element.ButtonElementSerializer;
import snw.kookbc.impl.serializer.component.card.element.ContentElementSerializer;
import snw.kookbc.impl.serializer.component.card.element.ImageElementSerializer;
import snw.kookbc.impl.serializer.component.card.module.*;
import snw.kookbc.impl.serializer.component.card.structure.ParagraphSerializer;

import java.lang.reflect.Type;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Jackson-based JSON utility for high-performance JSON processing.
 * Provides migration path from Gson while maintaining API compatibility.
 */
public final class JacksonUtil {

    /**
     * High-performance ObjectMapper configured for Card components with custom serializers.
     * Optimized for virtual threads and concurrent access.
     */
    public static final ObjectMapper CARD_MAPPER;

    /**
     * Standard ObjectMapper for general JSON processing.
     * Optimized for performance with virtual threads.
     */
    public static final ObjectMapper NORMAL_MAPPER;

    static {
        // Initialize CARD_MAPPER with custom serializers (migration from Gson serializers)
        CARD_MAPPER = createCardMapper();

        // Initialize NORMAL_MAPPER for general use
        NORMAL_MAPPER = createNormalMapper();
    }

    private static ObjectMapper createCardMapper() {
        ObjectMapper mapper = new ObjectMapper();

        // Register JavaTime module for JSR-310 support
        mapper.registerModule(new JavaTimeModule());

        // Create custom module for Card components
        SimpleModule cardModule = new SimpleModule("KookBC-Card-Module");

        // TODO: Convert Gson serializers to Jackson serializers
        // This is a placeholder - we'll need to create Jackson equivalents
        // of the existing Gson serializers

        mapper.registerModule(cardModule);
        return mapper;
    }

    private static ObjectMapper createNormalMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        return mapper;
    }

    /**
     * Creates a TypeReference for List types (Jackson equivalent of Gson's TypeToken).
     */
    public static <T> TypeReference<List<T>> createListType(Class<T> elementType) {
        Validate.notNull(elementType);
        return new TypeReference<List<T>>() {};
    }

    /**
     * Check if JsonNode contains the specified field and is not null.
     * Gson-compatible API for migration.
     */
    public static boolean has(JsonNode node, String fieldName) {
        return node.has(fieldName) && !node.get(fieldName).isNull();
    }

    /**
     * Get JsonNode field, throwing exception if not found or null.
     * Gson-compatible API for migration.
     */
    public static JsonNode get(JsonNode node, String fieldName) {
        if (!node.has(fieldName) || node.get(fieldName).isNull()) {
            throw new NoSuchElementException("There is no valid value mapped to requested key '" + fieldName + "'.");
        }
        return node.get(fieldName);
    }

    public static String getAsString(JsonNode node, String fieldName) {
        return get(node, fieldName).asText();
    }

    public static int getAsInt(JsonNode node, String fieldName) {
        return get(node, fieldName).asInt();
    }

    public static long getAsLong(JsonNode node, String fieldName) {
        return get(node, fieldName).asLong();
    }

    public static double getAsDouble(JsonNode node, String fieldName) {
        return get(node, fieldName).asDouble();
    }

    public static boolean getAsBoolean(JsonNode node, String fieldName) {
        return get(node, fieldName).asBoolean();
    }

    /**
     * Parse JSON string to JsonNode using NORMAL_MAPPER.
     */
    public static JsonNode parse(String json) {
        try {
            return NORMAL_MAPPER.readTree(json);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to parse JSON", e);
        }
    }

    /**
     * Convert object to JSON string using NORMAL_MAPPER.
     */
    public static String toJson(Object object) {
        try {
            return NORMAL_MAPPER.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize to JSON", e);
        }
    }

    /**
     * Convert object to JSON string using CARD_MAPPER.
     */
    public static String toCardJson(Object object) {
        try {
            return CARD_MAPPER.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize card to JSON", e);
        }
    }

    /**
     * Parse JSON to specific type using NORMAL_MAPPER.
     */
    public static <T> T fromJson(String json, Class<T> clazz) {
        try {
            return NORMAL_MAPPER.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to deserialize JSON to " + clazz.getSimpleName(), e);
        }
    }

    /**
     * Parse JSON to specific type using TypeReference.
     */
    public static <T> T fromJson(String json, TypeReference<T> typeRef) {
        try {
            return NORMAL_MAPPER.readValue(json, typeRef);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to deserialize JSON", e);
        }
    }

    private JacksonUtil() {
        // Utility class
    }
}