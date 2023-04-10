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

package snw.kookbc.impl.serializer.component.card.element;

import com.google.gson.*;
import snw.jkook.message.component.card.Theme;
import snw.jkook.message.component.card.element.BaseElement;
import snw.jkook.message.component.card.element.ButtonElement;
import snw.jkook.message.component.card.element.MarkdownElement;
import snw.jkook.message.component.card.element.PlainTextElement;

import java.lang.reflect.Type;

import static snw.kookbc.util.GsonUtil.get;
import static snw.kookbc.util.GsonUtil.has;

public class ButtonElementSerializer implements JsonSerializer<ButtonElement>, JsonDeserializer<ButtonElement> {
    @Override
    public JsonElement serialize(ButtonElement element, Type typeOfSrc, JsonSerializationContext context) {
        ButtonElement.EventType eventType = element.getEventType();
        String value = element.getValue();
        JsonObject accessoryJson = new JsonObject();
        accessoryJson.addProperty("type", "button");
        accessoryJson.addProperty("theme", element.getTheme().getValue());

        BaseElement textModule = element.getText();
        if (textModule != null) {
            accessoryJson.add("text", context.serialize(textModule));
        } else {
            accessoryJson.addProperty("text", "");
        }
        if (eventType != null) {
            accessoryJson.addProperty("click", eventType.getValue());
        } else {
            accessoryJson.addProperty("click", "");
        }
        if (value != null) {
            accessoryJson.addProperty("value", value);
        } else {
            accessoryJson.addProperty("value", "");
        }
        return accessoryJson;
    }

    @Override
    public ButtonElement deserialize(JsonElement element, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject jsonObject = element.getAsJsonObject();
        String theme = get(jsonObject, "theme").getAsString();

        JsonObject textObj = jsonObject.getAsJsonObject("text");
        BaseElement text = context.deserialize(textObj,
                "kmarkdown".equals(textObj.getAsJsonPrimitive("type").getAsString()) ? MarkdownElement.class
                        : PlainTextElement.class);

        String click = has(jsonObject, "click") ? get(jsonObject, "click").getAsString() : "";
        String value = has(jsonObject, "value") ? get(jsonObject, "value").getAsString() : "";

        return new ButtonElement(Theme.value(theme), value, ButtonElement.EventType.value(click), text);
    }
}
