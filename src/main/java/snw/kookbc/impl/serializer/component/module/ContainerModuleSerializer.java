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

import com.google.gson.*;
import snw.jkook.message.component.card.element.ImageElement;
import snw.jkook.message.component.card.module.ContainerModule;

import java.lang.reflect.Type;
import java.util.List;

import static snw.kookbc.util.GsonUtil.createListType;

public class ContainerModuleSerializer implements JsonSerializer<ContainerModule>, JsonDeserializer<ContainerModule> {
    static Type LIST_IMAGEELEMENT = createListType(ImageElement.class);

    @Override
    public JsonElement serialize(ContainerModule module, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject moduleObj = new JsonObject();
        // This will include the size attribute
        JsonElement elements = context.serialize(module.getImages());
        moduleObj.addProperty("type", "container");
        moduleObj.add("elements", elements);
        return moduleObj;
    }

    @Override
    public ContainerModule deserialize(JsonElement element, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject jsonObject = element.getAsJsonObject();
        List<ImageElement> list = context.deserialize(jsonObject.get("elements"), LIST_IMAGEELEMENT);
        return new ContainerModule(list);
    }
}
