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

package snw.kookbc.impl.serializer.component.card.module;

import com.google.gson.*;
import snw.jkook.entity.abilities.Accessory;
import snw.jkook.message.component.card.CardScopeElement;
import snw.jkook.message.component.card.element.ButtonElement;
import snw.jkook.message.component.card.element.ImageElement;
import snw.jkook.message.component.card.element.MarkdownElement;
import snw.jkook.message.component.card.element.PlainTextElement;
import snw.jkook.message.component.card.module.SectionModule;
import snw.jkook.message.component.card.structure.Paragraph;

import java.lang.reflect.Type;

import static snw.kookbc.util.GsonUtil.get;
import static snw.kookbc.util.GsonUtil.has;

public class SectionModuleSerializer implements JsonSerializer<SectionModule>, JsonDeserializer<SectionModule> {
    @Override
    public JsonElement serialize(SectionModule sectionModule, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject moduleObj = new JsonObject();
        JsonElement rawText = context.serialize(sectionModule.getText());
        moduleObj.addProperty("type", "section");
        moduleObj.add("text", rawText);
        Accessory.Mode mode = sectionModule.getMode();
        if (mode != null) {
            moduleObj.addProperty("mode", mode.getValue());
        }
        if (sectionModule.getAccessory() != null) {
            moduleObj.add("accessory", context.serialize(sectionModule.getAccessory()));
        }
        return moduleObj;
    }

    @Override
    public SectionModule deserialize(JsonElement element, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject jsonObject = element.getAsJsonObject();
        JsonObject text = jsonObject.get("text").getAsJsonObject();
        boolean hasModeField = has(jsonObject, "mode");
        Accessory.Mode mode = hasModeField ? Accessory.Mode.value(get(jsonObject, "mode").getAsString()) : null;
        Accessory accessory = null;
        if (has(jsonObject, "accessory")) {
            JsonObject accessoryJson = jsonObject.get("accessory").getAsJsonObject();
            String accessoryType = accessoryJson.getAsJsonPrimitive("type").getAsString();
            if (accessoryType.equals("image")) {
                accessory = context.deserialize(accessoryJson, ImageElement.class);
            } else if (accessoryType.equals("button")) {
                accessory = context.deserialize(accessoryJson, ButtonElement.class);
            }
        }
        CardScopeElement cardElement = null;
        String type = text.getAsJsonPrimitive("type").getAsString();
        switch (type) {
            case "plain-text": {
                cardElement = context.deserialize(text, PlainTextElement.class);
                break;
            }
            case "kmarkdown": {
                cardElement = context.deserialize(text, MarkdownElement.class);
                break;
            }
            case "paragraph": {
                cardElement = context.deserialize(text, Paragraph.class);
                break;
            }
        }
        return new SectionModule(cardElement, accessory, mode);
    }
}
