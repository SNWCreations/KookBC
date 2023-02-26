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
import snw.jkook.message.component.card.element.ButtonElement;
import snw.jkook.message.component.card.module.ActionGroupModule;
import snw.jkook.util.Validate;
import snw.kookbc.SharedConstants;

import java.lang.reflect.Type;

public class ActionGroupModuleSerializer implements JsonSerializer<ActionGroupModule> {
    @Override
    public JsonElement serialize(ActionGroupModule module, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject moduleObj = new JsonObject();
        Validate.isTrue(
                module.getButtons().stream().allMatch(button -> button instanceof ButtonElement),
                "If this has error, please tell the author of " + SharedConstants.SPEC_NAME + "! Maybe Kook updated the action module?"
        );
        moduleObj.addProperty("type", "action-group");
        moduleObj.add("elements", context.serialize(module.getButtons()));
        return moduleObj;
    }
}
