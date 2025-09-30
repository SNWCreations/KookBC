package snw.kookbc.impl.serializer.component;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import snw.jkook.message.component.TemplateMessage;

import java.lang.reflect.Type;

import com.fasterxml.jackson.databind.JsonNode;
public class TemplateMessageSerializer implements JsonSerializer<TemplateMessage> {

    @Override
    public JsonElement serialize(TemplateMessage templateMessage, Type type, JsonSerializationContext jsonSerializationContext) {
        return new JsonPrimitive(templateMessage.getContent());
    }
}
