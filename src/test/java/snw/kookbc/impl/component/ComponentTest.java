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

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import snw.jkook.message.component.BaseComponent;
import snw.kookbc.impl.SerializeTest;

import java.util.function.Consumer;

/**
 * @author huanmeng_qwq
 */
abstract class ComponentTest extends SerializeTest {
    void assertComponent(BaseComponent component, Consumer<? super JsonObject> consumer) {
        testObject(component, consumer);
    }

    void assertComponent(BaseComponent component, JsonElement element) {
        testObject(component, element);
    }
}
