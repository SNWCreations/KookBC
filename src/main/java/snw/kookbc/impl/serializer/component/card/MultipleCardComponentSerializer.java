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
package snw.kookbc.impl.serializer.component.card;

import com.google.gson.*;
import snw.jkook.message.component.card.CardComponent;
import snw.jkook.message.component.card.MultipleCardComponent;

import java.lang.reflect.Type;

import static snw.kookbc.util.GsonUtil.createListType;

public class MultipleCardComponentSerializer implements JsonSerializer<MultipleCardComponent>, JsonDeserializer<MultipleCardComponent> {
    private static final Type LIST_CARDCOMPONENT = createListType(CardComponent.class);

    @Override
    public JsonElement serialize(MultipleCardComponent src, Type typeOfSrc, JsonSerializationContext context) {
        return context.serialize(src.getComponents());
    }

    @Override
    public MultipleCardComponent deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonArray array = json.getAsJsonArray();
        return new MultipleCardComponent(context.deserialize(array, LIST_CARDCOMPONENT));
    }
}
