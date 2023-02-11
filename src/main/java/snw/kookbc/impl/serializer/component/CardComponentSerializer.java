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

package snw.kookbc.impl.serializer.component;

import com.google.gson.*;
import snw.jkook.message.component.card.CardComponent;

import java.lang.reflect.Type;

public class CardComponentSerializer implements JsonSerializer<CardComponent> {

    @Override
    public JsonElement serialize(CardComponent component, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject object = new JsonObject();
        object.addProperty("type", "card");
        object.addProperty("theme", component.getTheme().getValue());
        object.addProperty("size", component.getSize().getValue());
        if (component.getColor() != null && component.getColor().isEmpty()) {
            object.addProperty("color", component.getColor());
        }
        JsonArray modules = context.serialize(component.getModules()).getAsJsonArray();
        object.add("modules", modules);
        return object;
    }
}
