package snw.kookbc.impl.serializer.component.module;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import snw.jkook.message.component.card.module.CountdownModule;

import java.lang.reflect.Type;

public class CountdownModuleSerializer implements JsonSerializer<CountdownModule> {
    @Override
    public JsonElement serialize(CountdownModule module, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject moduleObj = new JsonObject();
        moduleObj.addProperty("type", "countdown");
        moduleObj.addProperty("mode", module.getType().getValue());
        moduleObj.addProperty("endTime", module.getEndTime());
        return moduleObj;
    }
}
