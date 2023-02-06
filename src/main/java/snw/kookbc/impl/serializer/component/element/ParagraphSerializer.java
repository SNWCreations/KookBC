package snw.kookbc.impl.serializer.component.element;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
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
