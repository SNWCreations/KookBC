/*
 *     KookBC -- The Kook Bot Client & JKook API standard implementation for Java.
 *     Copyright (C) 2022 - 2023 KookBC contributors
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as published
 *     by the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 *
 *     You should have received a copy of the GNU Affero General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package snw.kookbc.impl.serializer.component.element;

import com.google.gson.*;
import snw.jkook.message.component.card.Theme;
import snw.jkook.message.component.card.element.BaseElement;
import snw.jkook.message.component.card.element.ButtonElement;
import snw.jkook.message.component.card.element.MarkdownElement;
import snw.jkook.message.component.card.element.PlainTextElement;

import java.lang.reflect.Type;
import java.net.MalformedURLException;
import java.net.URL;

public class ButtonElementSerializer implements JsonSerializer<ButtonElement>, JsonDeserializer<ButtonElement> {
    @Override
    public JsonElement serialize(ButtonElement element, Type typeOfSrc, JsonSerializationContext context) {
        if (element.getEventType() == ButtonElement.EventType.LINK) {
            try {
                new URL(element.getValue());
            } catch (MalformedURLException e) {
                throw new RuntimeException("Invalid URL for the button", e);
            }
        }
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

    @Override
    public ButtonElement deserialize(JsonElement element, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject jsonObject = element.getAsJsonObject();
        String theme = jsonObject.getAsJsonPrimitive("theme").getAsString();

        JsonObject textObj = jsonObject.getAsJsonObject("text");
        String type = "", content = null;
        if (textObj != null) {
            type = textObj.getAsJsonPrimitive("type").getAsString();
            content = textObj.getAsJsonPrimitive("content").getAsString();
        }
        String click = jsonObject.has("click") ? jsonObject.getAsJsonPrimitive("click").getAsString() : null;
        String value = jsonObject.has("value") ? jsonObject.getAsJsonPrimitive("value").getAsString() : null;
        BaseElement text = (type.equals("kmarkdown")) ? new MarkdownElement(content) : new PlainTextElement(content);
        return new ButtonElement(Theme.value(theme), value, ButtonElement.EventType.value(click), text);
    }
}
