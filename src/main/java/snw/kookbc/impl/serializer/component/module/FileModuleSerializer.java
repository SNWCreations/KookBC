package snw.kookbc.impl.serializer.component.module;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import snw.jkook.message.component.FileComponent;
import snw.jkook.message.component.card.module.FileModule;

import java.lang.reflect.Type;

public class FileModuleSerializer implements JsonSerializer<FileModule> {
    @Override
    public JsonElement serialize(FileModule module, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject moduleObj = new JsonObject();
        moduleObj.addProperty("type", module.getType().getValue());
        moduleObj.addProperty("title", (module.getTitle()));
        moduleObj.addProperty("src", module.getSource());
        if (module.getType() == FileComponent.Type.AUDIO) {
            moduleObj.addProperty("cover", module.getCover());
        }
        return moduleObj;
    }
}
