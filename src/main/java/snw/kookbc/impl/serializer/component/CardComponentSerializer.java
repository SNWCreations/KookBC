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

package snw.kookbc.impl.serializer.component;

import com.google.gson.*;
import snw.jkook.message.component.card.CardComponent;
import snw.jkook.message.component.card.Size;
import snw.jkook.message.component.card.Theme;
import snw.jkook.message.component.card.module.*;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static snw.kookbc.util.GsonUtil.*;

public class CardComponentSerializer implements JsonSerializer<CardComponent>, JsonDeserializer<CardComponent> {
    public static final Map<String, Class<? extends BaseModule>> MODULE_MAP = new HashMap<>();

    static {
        MODULE_MAP.put("action-group", ActionGroupModule.class);
        MODULE_MAP.put("container", ContainerModule.class);
        MODULE_MAP.put("context", ContextModule.class);
        MODULE_MAP.put("countdown", CountdownModule.class);
        MODULE_MAP.put("divider", DividerModule.class);
        MODULE_MAP.put("file", FileModule.class);
        MODULE_MAP.put("audio", FileModule.class);
        MODULE_MAP.put("video", FileModule.class);
        MODULE_MAP.put("header", HeaderModule.class);
        MODULE_MAP.put("image-group", ImageGroupModule.class);
        MODULE_MAP.put("invite", InviteModule.class);
        MODULE_MAP.put("section", SectionModule.class);
    }

    @Override
    public JsonElement serialize(CardComponent component, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject object = new JsonObject();
        object.addProperty("type", "card");
        object.addProperty("theme", component.getTheme().getValue());
        object.addProperty("size", component.getSize().getValue());
        if (component.getColor() != null && component.getColor().isEmpty()) {
            object.addProperty("color", component.getColor());
        }
        JsonArray modules = context.serialize(component.getModules()).getAsJsonArray();
        object.add("modules", modules);
        return object;
    }

    @Override
    public CardComponent deserialize(JsonElement element, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject jsonObject = element.getAsJsonObject();
        String theme = get(jsonObject, "theme").getAsString();
        String size = get(jsonObject, "size").getAsString();
        String color = has(jsonObject, "color") ? get(jsonObject, "color").getAsString() : null;
        if (color != null && color.isEmpty()) {
            color = null;
        }
        JsonArray modules = jsonObject.getAsJsonArray("modules");
        List<BaseModule> list = new ArrayList<>(modules.size());
        modules.forEach(jsonElement -> {
            JsonObject json = jsonElement.getAsJsonObject();
            String type = json.getAsJsonPrimitive("type").getAsString();
            processModule(context, list, json, type);
        });
        return new CardComponent(list, Size.value(size), Theme.value(theme), color);
    }

    private static void processModule(JsonDeserializationContext context, List<BaseModule> list, JsonObject json, String type) {
        if (!MODULE_MAP.containsKey(type)) {
            throw new IllegalArgumentException("Unsupported module type: " + type);
        }
        list.add(context.deserialize(json, MODULE_MAP.get(type)));
    }
}
