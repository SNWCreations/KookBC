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

package snw.kookbc.impl.serializer.component.module;

import com.google.gson.*;
import snw.jkook.message.component.card.element.BaseElement;
import snw.jkook.message.component.card.element.ImageElement;
import snw.jkook.message.component.card.element.MarkdownElement;
import snw.jkook.message.component.card.element.PlainTextElement;
import snw.jkook.message.component.card.module.ContextModule;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class ContextModuleSerializer implements JsonSerializer<ContextModule>, JsonDeserializer<ContextModule> {
    @Override
    public JsonElement serialize(ContextModule module, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject moduleObj = new JsonObject();
        JsonArray elements = new JsonArray();
        for (BaseElement base : module.getModules()) {
            if (base instanceof PlainTextElement || base instanceof MarkdownElement || base instanceof ImageElement) {
                JsonElement raw = context.serialize(base);
                elements.add(raw);
            } else {
                throw new IllegalArgumentException("Invalid element in context module");
            }
        }
        moduleObj.addProperty("type", "context");
        moduleObj.add("elements", elements);
        return moduleObj;
    }

    @Override
    public ContextModule deserialize(JsonElement element, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject jsonObject = element.getAsJsonObject();
        JsonArray elements = jsonObject.getAsJsonArray("elements");
        List<BaseElement> list = new ArrayList<>();
        for (JsonElement element1 : elements) {
            JsonObject obj = element1.getAsJsonObject();
            String type = obj.getAsJsonPrimitive("type").getAsString();
            switch (type) {
                case "plain-text": {
                    list.add(context.deserialize(obj, PlainTextElement.class));
                    break;
                }
                case "kmarkdown": {
                    list.add(context.deserialize(obj, MarkdownElement.class));
                    break;
                }
                case "image":
                    list.add(context.deserialize(obj, ImageElement.class));
                    break;
            }
        }
        return new ContextModule(list);
    }
}
