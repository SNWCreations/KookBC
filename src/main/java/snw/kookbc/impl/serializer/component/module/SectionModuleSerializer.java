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

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import snw.jkook.entity.abilities.Accessory;
import snw.jkook.message.component.card.module.SectionModule;

import java.lang.reflect.Type;

public class SectionModuleSerializer implements JsonSerializer<SectionModule> {
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
        moduleObj.add("accessory", context.serialize(sectionModule.getAccessory()));
        return moduleObj;
    }
}
