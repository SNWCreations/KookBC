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

package snw.kookbc.impl.component;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import snw.jkook.message.component.card.Size;
import snw.jkook.message.component.card.Theme;

import java.util.function.Consumer;

/**
 * @author huanmeng_qwq
 */
public class BaseCardComponentTest extends ComponentTest {
    protected final JsonObject simpleCardObject(Theme theme, Size size, Consumer<? super JsonArray> modules) {
        return jsonObject(json -> {
            json.addProperty("type", "card");
            json.addProperty("theme", theme.getValue());
            json.addProperty("size", size.getValue());
            json.add("modules", jsonArray(modules));
        });
    }

    protected final JsonObject simpleContextModuleObject(Consumer<? super JsonArray> elements) {
        return jsonObject(json -> {
            json.addProperty("type", "context");
            json.add("elements", jsonArray(elements));
        });
    }

    protected final JsonObject markdownElementObject(String content) {
        return jsonObject(
                jsonObject -> {
                    jsonObject.addProperty("type", "kmarkdown");
                    jsonObject.addProperty("content", content);
                }
        );
    }
}
