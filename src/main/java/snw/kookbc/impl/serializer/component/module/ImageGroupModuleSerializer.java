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
import snw.jkook.message.component.card.module.ImageGroupModule;

import java.lang.reflect.Type;

public class ImageGroupModuleSerializer implements JsonSerializer<ImageGroupModule> {
    @Override
    public JsonElement serialize(ImageGroupModule module, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject moduleObj = new JsonObject();
        /*JsonArray elements = new JsonArray();
        for (ImageElement image : module.getImages()) {
            JsonObject element = new JsonObject();
            element.addProperty("type", "image");
            element.addProperty("src", image.getSource());
            elements.add(element);
        }*/
        JsonElement elements = context.serialize(module.getImages());
        moduleObj.addProperty("type", "image-group");
        moduleObj.add("elements", elements);
        return moduleObj;
    }
}
