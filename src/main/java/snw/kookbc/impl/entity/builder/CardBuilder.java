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

package snw.kookbc.impl.entity.builder;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import snw.jkook.message.component.card.CardComponent;
import snw.jkook.message.component.card.MultipleCardComponent;
import snw.jkook.util.Validate;
import snw.kookbc.util.GsonUtil;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import static snw.kookbc.util.GsonUtil.get;

// Just for (de)serialize the CardMessages.
public class CardBuilder {

    public static MultipleCardComponent buildCard(JsonArray array) {
        List<CardComponent> components = new LinkedList<>();
        for (JsonElement jsonElement : array) {
            components.add(buildCard(jsonElement.getAsJsonObject()));
        }
        return new MultipleCardComponent(components);
    }

    public static CardComponent buildCard(JsonObject object) {
        Validate.isTrue(Objects.equals(get(object, "type").getAsString(), "card"), "The provided element is not a card.");
        return GsonUtil.CARD_GSON.fromJson(object, CardComponent.class);
    }

    public static JsonArray serialize(CardComponent component) {
        JsonArray result = new JsonArray();
        result.add(serialize0(component));
        return result;
    }

    public static JsonArray serialize(MultipleCardComponent component) {
        JsonArray array = new JsonArray();
        for (CardComponent card : component.getComponents()) {
            array.add(serialize0(card));
        }
        return array;
    }

    public static JsonObject serialize0(CardComponent component) {
        return GsonUtil.CARD_GSON.toJsonTree(component).getAsJsonObject();
    }

}
