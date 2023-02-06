package snw.kookbc.impl.serializer.component.module;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import snw.jkook.message.component.card.module.ImageGroupModule;

import java.lang.reflect.Type;

public class ImageGroupModuleSerializer implements JsonSerializer<ImageGroupModule> {
    @Override
    public JsonElement serialize(ImageGroupModule module, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject moduleObj = new JsonObject();
        /*JsonArray elements = new JsonArray();
        for (ImageElement image : module.getImages()) {
            JsonObject element = new JsonObject();
            element.addProperty("type", "image");
            element.addProperty("src", image.getSource());
            elements.add(element);
        }*/
        JsonElement elements = context.serialize(module.getImages());
        moduleObj.addProperty("type", "image-group");
        moduleObj.add("elements", elements);
        return moduleObj;
    }
}
