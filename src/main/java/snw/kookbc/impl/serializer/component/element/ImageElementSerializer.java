package snw.kookbc.impl.serializer.component.element;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import snw.jkook.message.component.card.element.ImageElement;

import java.lang.reflect.Type;

public class ImageElementSerializer implements JsonSerializer<ImageElement> {
    @Override
    public JsonElement serialize(ImageElement element, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject accessoryJson = new JsonObject();
        accessoryJson.addProperty("type", "image");
        accessoryJson.addProperty("src", element.getSource());
        accessoryJson.addProperty("size", element.getSize().getValue());
        return accessoryJson;
    }
}
