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
import snw.jkook.message.component.FileComponent;
import snw.jkook.message.component.card.module.FileModule;

import java.lang.reflect.Type;

public class FileModuleSerializer implements JsonSerializer<FileModule> {
    @Override
    public JsonElement serialize(FileModule module, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject moduleObj = new JsonObject();
        moduleObj.addProperty("type", module.getType().getValue());
        moduleObj.addProperty("title", (module.getTitle()));
        moduleObj.addProperty("src", module.getSource());
        if (module.getType() == FileComponent.Type.AUDIO) {
            moduleObj.addProperty("cover", module.getCover());
        }
        return moduleObj;
    }
}
