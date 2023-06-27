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

package snw.kookbc.impl.serializer.component.card.structure;

import com.google.gson.*;
import snw.jkook.message.component.card.element.BaseElement;
import snw.jkook.message.component.card.element.MarkdownElement;
import snw.jkook.message.component.card.element.PlainTextElement;
import snw.jkook.message.component.card.structure.Paragraph;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import static snw.kookbc.util.GsonUtil.get;
import static snw.kookbc.util.GsonUtil.has;

public class ParagraphSerializer implements JsonSerializer<Paragraph>, JsonDeserializer<Paragraph> {
    @Override
    public JsonElement serialize(Paragraph element, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject rawText = new JsonObject();
        rawText.addProperty("type", "paragraph");
        rawText.addProperty("cols", element.getColumns());
        rawText.add("fields", context.serialize(element.getFields()));
        return rawText;
    }

    @Override
    public Paragraph deserialize(JsonElement element, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject jsonObject = element.getAsJsonObject();
        if (has(jsonObject, "type") && get(jsonObject, "type").getAsString().equals("paragraph")) {
            int cols = get(jsonObject, "cols").getAsInt();
            JsonArray fieldArray = jsonObject.getAsJsonArray("fields");

            List<BaseElement> fields = new ArrayList<>(fieldArray.size());
            fieldArray.forEach(json -> {
                JsonObject object = json.getAsJsonObject();
                String type = object.getAsJsonPrimitive("type").getAsString();
                String content = object.getAsJsonPrimitive("content").getAsString();
                if (type.equals("kmarkdown")) {
                    fields.add(new MarkdownElement(content));
                } else {
                    fields.add(new PlainTextElement(content));
                }
            });

            return new Paragraph(cols, fields);
        }
        throw new JsonParseException("Invalid paragraph");
    }
}
