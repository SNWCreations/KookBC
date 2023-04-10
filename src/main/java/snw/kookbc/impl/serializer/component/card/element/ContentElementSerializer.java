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

import java.lang.reflect.Type;
import java.util.function.Function;

import static snw.kookbc.util.GsonUtil.get;

public class ContentElementSerializer<T> implements JsonSerializer<T>, JsonDeserializer<T> {
    private final String type;
    private final Function<T, String> contentFunc;
    private final Function<String, T> parseFunc;

    public ContentElementSerializer(String type, Function<T, String> contentFunc, Function<String, T> parseFunc) {
        this.type = type;
        this.contentFunc = contentFunc;
        this.parseFunc = parseFunc;
    }

    @Override
    public JsonElement serialize(T element, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject rawText = new JsonObject();
        rawText.addProperty("type", type);
        rawText.addProperty("content", contentFunc.apply(element));
        return rawText;
    }

    @Override
    public T deserialize(JsonElement element, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject jsonObject = element.getAsJsonObject();
        String content = get(jsonObject, "content").getAsString();
        return parseFunc.apply(content);
    }
}
