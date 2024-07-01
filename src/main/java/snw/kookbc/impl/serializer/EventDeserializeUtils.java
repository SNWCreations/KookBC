package snw.kookbc.impl.serializer;

import com.google.gson.JsonObject;

import static snw.kookbc.util.GsonUtil.get;

public class EventDeserializeUtils {
    public static JsonObject getBody(JsonObject object) {
        return get(get(object, "extra").getAsJsonObject(), "body").getAsJsonObject();
    }
}
