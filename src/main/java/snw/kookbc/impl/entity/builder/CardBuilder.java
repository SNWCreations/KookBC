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

package snw.kookbc.impl.entity.builder;

import com.fasterxml.jackson.databind.JsonNode;
import snw.jkook.message.component.card.CardComponent;
import snw.jkook.message.component.card.MultipleCardComponent;
import snw.jkook.util.Validate;
import snw.kookbc.util.JacksonCardUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Jackson-based高性能卡片消息构建器
 * 提供null-safe的卡片消息序列化/反序列化功能
 */
public class CardBuilder {

    // ===== Jackson版本方法（推荐使用）=====

    /**
     * 从JsonNode数组构建多卡片组件
     * @param arrayNode Jackson JsonNode数组
     * @return MultipleCardComponent
     */
    public static MultipleCardComponent buildCardArray(JsonNode arrayNode) {
        Validate.notNull(arrayNode, "JsonNode array cannot be null");
        Validate.isTrue(arrayNode.isArray(), "JsonNode must be an array");

        List<CardComponent> components = new ArrayList<>();
        for (JsonNode jsonElement : arrayNode) {
            if (jsonElement.isObject()) {
                components.add(buildCardObject(jsonElement));
            }
        }
        return new MultipleCardComponent(components);
    }

    /**
     * 从JsonNode对象构建单个卡片组件
     * @param objectNode Jackson JsonNode对象
     * @return CardComponent
     */
    public static CardComponent buildCardObject(JsonNode objectNode) {
        Validate.notNull(objectNode, "JsonNode object cannot be null");
        Validate.isTrue(objectNode.isObject(), "JsonNode must be an object");
        Validate.isTrue(Objects.equals(JacksonCardUtil.getStringOrDefault(objectNode, "type", ""), "card"),
                       "The provided element is not a card.");

        return JacksonCardUtil.fromJson(objectNode, CardComponent.class);
    }

    /**
     * 从JSON字符串构建卡片组件
     * @param jsonString JSON字符串
     * @return CardComponent或MultipleCardComponent
     */
    public static Object buildCard(String jsonString) {
        JsonNode root = JacksonCardUtil.parse(jsonString);
        if (root.isArray()) {
            return buildCardArray(root);
        } else if (root.isObject()) {
            return buildCardObject(root);
        } else {
            throw new IllegalArgumentException("JSON must be object or array");
        }
    }

    // ===== 序列化方法 =====

    /**
     * 序列化单个卡片组件为JsonNode
     * @param component CardComponent
     * @return JsonNode
     */
    public static JsonNode serializeToNode(CardComponent component) {
        return JacksonCardUtil.toJsonNode(component);
    }

    /**
     * 序列化多卡片组件为JsonNode
     * @param component MultipleCardComponent
     * @return JsonNode（数组类型）
     */
    public static JsonNode serializeToNode(MultipleCardComponent component) {
        return JacksonCardUtil.toJsonNode(component);
    }

}
