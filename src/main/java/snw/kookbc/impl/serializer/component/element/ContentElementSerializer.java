package snw.kookbc.impl.serializer.component.element;

import com.google.gson.*;

import java.lang.reflect.Type;
import java.util.function.Function;

/**
 * 2023/3/12<br>
 * KookBC<br>
 *
 * @author huanmeng_qwq
 */
public class ContentElementSerializer<T> implements JsonSerializer<T>, JsonDeserializer<T> {
    private final String type;
    private final Function<T, String> contentFunc;
    private final Function<String, T> parseFunc;

    public ContentElementSerializer(String type, Function<T, String> contentFunc, Function<String, T> parseFunc) {
        this.type = type;
        this.contentFunc = contentFunc;
        this.parseFunc = parseFunc;
    }

    @Override
    public JsonElement serialize(T element, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject rawText = new JsonObject();
        rawText.addProperty("type", type);
        rawText.addProperty("content", contentFunc.apply(element));
        return rawText;
    }

    @Override
    public T deserialize(JsonElement element, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject jsonObject = element.getAsJsonObject();
        String content = jsonObject.getAsJsonPrimitive("content").getAsString();
        return parseFunc.apply(content);
    }
}
