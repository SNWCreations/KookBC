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
import snw.jkook.message.component.card.module.CountdownModule;

import java.lang.reflect.Type;

import static snw.kookbc.util.GsonUtil.get;

public class CountdownModuleSerializer implements JsonSerializer<CountdownModule>, JsonDeserializer<CountdownModule> {
    @Override
    public JsonElement serialize(CountdownModule module, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject moduleObj = new JsonObject();
        moduleObj.addProperty("type", "countdown");
        moduleObj.addProperty("mode", module.getType().getValue());
        if (module.getType() == CountdownModule.Type.SECOND) {
            moduleObj.addProperty("startTime", module.getStartTime());
        }
        moduleObj.addProperty("endTime", module.getEndTime());
        return moduleObj;
    }

    @Override
    public CountdownModule deserialize(JsonElement element, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject jsonObject = element.getAsJsonObject();
        String mode = get(jsonObject, "mode").getAsString();
        long startTime = get(jsonObject, "startTime").getAsLong();
        long endTime = get(jsonObject, "endTime").getAsLong();
        return new CountdownModule(CountdownModule.Type.value(mode), startTime, endTime);
    }
}
