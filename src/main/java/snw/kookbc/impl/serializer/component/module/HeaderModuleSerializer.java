package snw.kookbc.impl.serializer.component.module;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import snw.jkook.message.component.card.module.HeaderModule;

import java.lang.reflect.Type;

/**
 * 2023/2/1<br>
 * KookBC<br>
 *
 * @author huanmeng_qwq
 */
public class HeaderModuleSerializer implements JsonSerializer<HeaderModule> {
    @Override
    public JsonElement serialize(HeaderModule module, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject moduleObj = new JsonObject();
        JsonObject textObj = new JsonObject();
        textObj.addProperty("type", "plain-text");
        textObj.addProperty("content", module.getElement().getContent());
        moduleObj.addProperty("type", "header");
        moduleObj.add("text", textObj);
        return moduleObj;
    }
}
