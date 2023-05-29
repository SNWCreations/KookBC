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
import snw.jkook.message.component.card.Size;
import snw.jkook.message.component.card.element.ImageElement;

import java.lang.reflect.Type;

import static snw.kookbc.util.GsonUtil.get;
import static snw.kookbc.util.GsonUtil.has;

public class ImageElementSerializer implements JsonSerializer<ImageElement>, JsonDeserializer<ImageElement> {
    @Override
    public JsonElement serialize(ImageElement element, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject accessoryJson = new JsonObject();
        accessoryJson.addProperty("type", "image");
        accessoryJson.addProperty("src", element.getSource());
        accessoryJson.addProperty("size", element.getSize().getValue());
        accessoryJson.addProperty("alt", element.getAlt());
        accessoryJson.addProperty("circle", element.isCircled());
        return accessoryJson;
    }

    @Override
    public ImageElement deserialize(JsonElement element, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject jsonObject = element.getAsJsonObject();
        String src = get(jsonObject, "src").getAsString();
        String size = has(jsonObject, "size") ? get(jsonObject, "size").getAsString() : Size.LG.getValue();
        String alt = has(jsonObject, "alt") ? get(jsonObject, "alt").getAsString() : "";
        boolean circle = has(jsonObject, "circle") && get(jsonObject, "circle").getAsBoolean();
        return new ImageElement(src, alt, Size.value(size), circle);
    }
}
