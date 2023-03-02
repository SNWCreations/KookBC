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
import snw.jkook.message.component.card.element.MarkdownElement;

import java.lang.reflect.Type;

public class MarkdownElementSerializer implements JsonSerializer<MarkdownElement>, JsonDeserializer<MarkdownElement> {
    @Override
    public JsonElement serialize(MarkdownElement element, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject rawText = new JsonObject();
        rawText.addProperty("type", "kmarkdown");
        rawText.addProperty("content", element.getContent());
        return rawText;
    }

    @Override
    public MarkdownElement deserialize(JsonElement element, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject jsonObject = element.getAsJsonObject();
        String content = jsonObject.getAsJsonPrimitive("content").getAsString();
        return new MarkdownElement(content);
    }
}
