package snw.kookbc.impl.serializer.component.module;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import snw.jkook.message.component.card.module.InviteModule;

import java.lang.reflect.Type;

public class InviteModuleSerializer implements JsonSerializer<InviteModule> {
    @Override
    public JsonElement serialize(InviteModule module, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject moduleObj = new JsonObject();
        moduleObj.addProperty("type", "invite");
        moduleObj.addProperty("code", module.getCode());
        return moduleObj;
    }
}
