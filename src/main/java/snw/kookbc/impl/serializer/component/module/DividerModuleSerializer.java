package snw.kookbc.impl.serializer.component.module;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import snw.jkook.message.component.card.module.DividerModule;

import java.lang.reflect.Type;

/**
 * 2023/2/1<br>
 * KookBC<br>
 *
 * @author huanmeng_qwq
 */
public class DividerModuleSerializer implements JsonSerializer<DividerModule> {
    @Override
    public JsonElement serialize(DividerModule src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject moduleObj = new JsonObject();
        moduleObj.addProperty("type", "divider");
        return moduleObj;
    }
}
