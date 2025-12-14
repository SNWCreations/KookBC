/*
 *     KookBC -- The Kook Bot Client & JKook API standard implementation for Java.
 *     Copyright (C) 2022 - 2023 KookBC contributors
 */

package snw.kookbc.impl.serializer.component.jackson.card.module;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import snw.jkook.message.component.card.element.ImageElement;
import snw.jkook.message.component.card.module.ImageGroupModule;
import snw.kookbc.util.JacksonCardUtil;

public class JacksonImageGroupModuleDeserializer extends JsonDeserializer<ImageGroupModule> {
    @Override
    public ImageGroupModule deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonNode node = p.getCodec().readTree(p);

        // ImageGroupModule构造器需要List<ImageElement>参数
        List<ImageElement> images = new ArrayList<>();

        // 尝试解析elements字段
        if (JacksonCardUtil.has(node, "elements")) {
            JsonNode elementsNode = node.get("elements");
            if (elementsNode.isArray()) {
                for (JsonNode elementNode : elementsNode) {
                    try {
                        ImageElement imageElement = JacksonCardUtil.fromJson(elementNode, ImageElement.class);
                        images.add(imageElement);
                    } catch (Exception e) {
                        // 忽略无法解析的元素，继续处理其他元素
                    }
                }
            }
        }

        return new ImageGroupModule(images);
    }
}
