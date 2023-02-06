package snw.kookbc.impl.serializer.component.module;

import com.google.gson.*;
import snw.jkook.message.component.card.element.BaseElement;
import snw.jkook.message.component.card.element.ImageElement;
import snw.jkook.message.component.card.element.MarkdownElement;
import snw.jkook.message.component.card.element.PlainTextElement;
import snw.jkook.message.component.card.module.ContextModule;

import java.lang.reflect.Type;

public class ContextModuleSerializer implements JsonSerializer<ContextModule> {
    @Override
    public JsonElement serialize(ContextModule module, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject moduleObj = new JsonObject();
        JsonArray elements = new JsonArray();
        for (BaseElement base : module.getModules()) {
            JsonObject rawObj = new JsonObject();
            if (base instanceof PlainTextElement || base instanceof MarkdownElement) {
                String content;
                String type;
                if (base instanceof PlainTextElement) {
                    type = "plain-text";
                    content = ((PlainTextElement) base).getContent();
                } else {
                    type = "kmarkdown";
                    content = ((MarkdownElement) base).getContent();
                }
                rawObj.addProperty("type", type);
                rawObj.addProperty("content", content);
                elements.add(rawObj);
            } else if (base instanceof ImageElement) {
                ImageElement image = (ImageElement) base;
                rawObj.addProperty("type", "image");
                rawObj.addProperty("src", image.getSource());
                elements.add(rawObj);
            }
        }
        moduleObj.addProperty("type", "context");
        moduleObj.add("elements", elements);
        return moduleObj;
    }
}
