package snw.kookbc.impl.serializer.component;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import snw.jkook.message.component.card.CardComponent;
import snw.jkook.message.component.card.MultipleCardComponent;

import java.lang.reflect.Type;
import java.util.List;

/**
 * 2023/3/1<br>
 * KookBC<br>
 *
 * @author huanmeng_qwq
 */
public class MultipleCardComponentSerializer implements JsonSerializer<MultipleCardComponent>, JsonDeserializer<MultipleCardComponent> {
    @Override
    public JsonElement serialize(MultipleCardComponent src, Type typeOfSrc, JsonSerializationContext context) {
        return context.serialize(src.getComponents());
    }

    @Override
    public MultipleCardComponent deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonArray array = json.getAsJsonArray();
        return new MultipleCardComponent(context.deserialize(array, TypeToken.getParameterized(List.class, CardComponent.class).getType()));
    }
}
