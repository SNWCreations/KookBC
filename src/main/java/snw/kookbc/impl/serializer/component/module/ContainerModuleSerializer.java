package snw.kookbc.impl.serializer.component.module;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import snw.jkook.message.component.card.module.ContainerModule;

import java.lang.reflect.Type;

public class ContainerModuleSerializer implements JsonSerializer<ContainerModule> {
    @Override
    public JsonElement serialize(ContainerModule module, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject moduleObj = new JsonObject();
        /*JsonArray elements = new JsonArray();
        for (ImageElement image : module.getImages()) {
            JsonObject element = new JsonObject();
            element.addProperty("type", "image");
            element.addProperty("src", image.getSource());
            elements.add(element);
        }*/
        JsonElement elements = context.serialize(module.getImages());
        moduleObj.addProperty("type", "container");
        moduleObj.add("elements", elements);
        return moduleObj;
    }
}
