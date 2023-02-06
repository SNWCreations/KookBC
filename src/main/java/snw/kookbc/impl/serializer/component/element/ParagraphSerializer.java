package snw.kookbc.impl.serializer.component.element;

import com.google.gson.*;
import snw.jkook.message.component.card.element.BaseElement;
import snw.jkook.message.component.card.structure.Paragraph;

import java.lang.reflect.Type;

public class ParagraphSerializer implements JsonSerializer<Paragraph> {
    @Override
    public JsonElement serialize(Paragraph element, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject rawText = new JsonObject();
        rawText.addProperty("type", "paragraph");
        rawText.addProperty("cols", element.getColumns());
        rawText.add("fields", context.serialize(element.getFields()));
        return rawText;
    }
}
