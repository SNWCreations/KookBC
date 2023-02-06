package snw.kookbc.impl.serializer.component.element;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import snw.jkook.message.component.card.element.PlainTextElement;

import java.lang.reflect.Type;

public class PlainTextElementSerializer implements JsonSerializer<PlainTextElement> {
    @Override
    public JsonElement serialize(PlainTextElement element, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject rawText = new JsonObject();
        rawText.addProperty("type", "plain-text");
        rawText.addProperty("content", element.getContent());
        return rawText;
    }
}
