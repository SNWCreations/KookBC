package snw.kookbc.impl.serializer.component.element;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import snw.jkook.message.component.card.element.BaseElement;
import snw.jkook.message.component.card.element.ButtonElement;
import snw.jkook.message.component.card.element.MarkdownElement;
import snw.jkook.message.component.card.element.PlainTextElement;

import java.lang.reflect.Type;

public class ButtonElementSerializer implements JsonSerializer<ButtonElement> {
    @Override
    public JsonElement serialize(ButtonElement element, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject accessoryJson = new JsonObject();
        accessoryJson.addProperty("type", "button");
        accessoryJson.addProperty("theme", element.getTheme().getValue());
        JsonObject textObj = new JsonObject();
        BaseElement textModule = element.getText();
        textObj.addProperty("type", (textModule instanceof MarkdownElement) ? "kmarkdown" : "plain-text");
        textObj.addProperty("content",
                (textModule instanceof MarkdownElement) ?
                        ((MarkdownElement) textModule).getContent() :
                        ((PlainTextElement) textModule).getContent()
        );
        accessoryJson.add("text", textObj);
        accessoryJson.addProperty("click", element.getEventType().getValue());
        accessoryJson.addProperty("value", element.getValue());
        return accessoryJson;
    }
}
