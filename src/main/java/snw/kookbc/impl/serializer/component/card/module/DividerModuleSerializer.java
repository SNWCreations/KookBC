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
import snw.jkook.message.component.card.module.DividerModule;

import java.lang.reflect.Type;

import static snw.kookbc.util.GsonUtil.get;
import static snw.kookbc.util.GsonUtil.has;

public class DividerModuleSerializer implements JsonSerializer<DividerModule>, JsonDeserializer<DividerModule> {
    @Override
    public JsonElement serialize(DividerModule src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject moduleObj = new JsonObject();
        moduleObj.addProperty("type", "divider");
        return moduleObj;
    }

    @Override
    public DividerModule deserialize(JsonElement element, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject jsonObject = element.getAsJsonObject();
        if (has(jsonObject, "type") && get(jsonObject, "type").getAsString().equals("divider")) {
            return DividerModule.INSTANCE;
        }
        throw new JsonParseException("Invalid divider module");
    }
}
