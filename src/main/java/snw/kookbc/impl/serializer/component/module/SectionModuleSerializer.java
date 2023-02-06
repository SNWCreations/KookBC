package snw.kookbc.impl.serializer.component.module;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import snw.jkook.entity.abilities.Accessory;
import snw.jkook.message.component.card.module.SectionModule;

import java.lang.reflect.Type;

public class SectionModuleSerializer implements JsonSerializer<SectionModule> {
    @Override
    public JsonElement serialize(SectionModule sectionModule, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject moduleObj = new JsonObject();
        JsonElement rawText = context.serialize(sectionModule.getText());
        moduleObj.addProperty("type", "section");
        moduleObj.add("text", rawText);
        Accessory.Mode mode = sectionModule.getMode();
        if (mode != null) {
            moduleObj.addProperty("mode", mode.getValue());
        }
        moduleObj.add("accessory", context.serialize(sectionModule.getAccessory()));
        return moduleObj;
    }
}
