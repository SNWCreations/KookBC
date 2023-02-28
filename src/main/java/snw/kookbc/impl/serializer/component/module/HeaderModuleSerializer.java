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
import snw.jkook.message.component.card.element.PlainTextElement;
import snw.jkook.message.component.card.module.HeaderModule;

import java.lang.reflect.Type;

public class HeaderModuleSerializer implements JsonSerializer<HeaderModule>, JsonDeserializer<HeaderModule> {
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

    @Override
    public HeaderModule deserialize(JsonElement element, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject jsonObject = element.getAsJsonObject();
        JsonObject text = jsonObject.getAsJsonObject("text");
        String content = text.getAsJsonPrimitive("content").getAsString();
        return new HeaderModule(new PlainTextElement(content));
    }
}
