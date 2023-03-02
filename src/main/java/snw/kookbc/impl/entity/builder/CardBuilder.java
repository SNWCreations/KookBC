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
import snw.jkook.entity.abilities.Accessory;
import snw.jkook.message.component.card.CardComponent;
import snw.jkook.message.component.card.MultipleCardComponent;
import snw.jkook.message.component.card.element.*;
import snw.jkook.util.Validate;
import snw.kookbc.util.GsonUtil;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

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
        Validate.isTrue(Objects.equals(object.get("type").getAsString(), "card"), "The provided element is not a card.");
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

    private static void addAccessory(Accessory accessory, JsonObject moduleObj) {
        if (accessory == null) return;
        JsonObject accessoryJson = new JsonObject();
        if (accessory instanceof ImageElement) {
            accessoryJson.addProperty("type", "image");
            accessoryJson.addProperty("src", ((ImageElement) accessory).getSource());
            accessoryJson.addProperty("size", ((ImageElement) accessory).getSize().getValue());
        } else if (accessory instanceof ButtonElement) {
            accessoryJson.addProperty("type", "button");
            accessoryJson.addProperty("theme", ((ButtonElement) accessory).getTheme().getValue());
            JsonObject textObj = new JsonObject();
            BaseElement textModule = ((ButtonElement) accessory).getText();
            textObj.addProperty("type", (textModule instanceof MarkdownElement) ? "kmarkdown" : "plain-text");
            textObj.addProperty("content",
                    (textModule instanceof MarkdownElement) ?
                            ((MarkdownElement) textModule).getContent() :
                            ((PlainTextElement) textModule).getContent()
            );
            accessoryJson.add("text", textObj);
            accessoryJson.addProperty("click", ((ButtonElement) accessory).getEventType().getValue());
            accessoryJson.addProperty("value", ((ButtonElement) accessory).getValue());
        }
        moduleObj.add("accessory", accessoryJson);
    }

    private static BaseElement createButtonText(JsonObject rawButtonText, String buttonContent) {
        BaseElement buttonText;
        switch (rawButtonText.get("type").getAsString()) {
            case "plain-text":
                buttonText = new PlainTextElement(buttonContent, false);
                break;
            case "kmarkdown":
                buttonText = new MarkdownElement(buttonContent);
                break;
            default:
                throw new IllegalArgumentException("Unknown button text type.");
        }
        return buttonText;
    }

}
