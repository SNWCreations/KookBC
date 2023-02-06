package snw.kookbc.impl.serializer.component;

import com.google.gson.*;
import snw.jkook.message.component.card.CardComponent;

import java.lang.reflect.Type;

public class CardComponentSerializer implements JsonSerializer<CardComponent> {

    @Override
    public JsonElement serialize(CardComponent component, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject object = new JsonObject();
        object.addProperty("type", "card");
        object.addProperty("theme", component.getTheme().getValue());
        object.addProperty("size", component.getSize().getValue());
        if (component.getColor() != null && component.getColor().isEmpty()) {
            object.addProperty("color", component.getColor());
        }
        JsonArray modules = context.serialize(component.getModules()).getAsJsonArray();
        object.add("modules", modules);
        return object;
    }
}
