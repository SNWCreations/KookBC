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

package snw.kookbc.impl.serializer;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import snw.kookbc.util.GsonUtil;

import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author huanmeng_qwq
 */
public abstract class SerializeTest {
    protected static Gson gson = GsonUtil.CARD_GSON;

    final void assertJson(JsonElement object, JsonElement object1) {
        assertEquals(object, object1);
    }

    final JsonElement serialize(Object object) {
        return gson.toJsonTree(object);
    }

    final <T> T deserialize(JsonElement jsonElement, Class<T> clazz) {
        return gson.fromJson(jsonElement, clazz);
    }

    final void testArray(Object obj, Consumer<? super JsonArray> consumer) {
        JsonArray array = jsonArray(consumer);
        JsonElement element = serialize(obj);
        assertJson(element, array);
    }

    protected final void testObject(Object obj, Consumer<? super JsonObject> consumer) {
        JsonObject object = jsonObject(consumer);
        JsonElement element = serialize(obj);
        assertJson(element, object);
    }

    public final void testObject(Object obj, JsonElement element) {
        JsonElement serialize = serialize(obj);
        assertJson(element, serialize);
    }

    protected static JsonObject jsonObject(Consumer<? super JsonObject> consumer) {
        JsonObject json = new JsonObject();
        consumer.accept(json);
        return json;
    }

    protected static JsonArray jsonArray(Consumer<? super JsonArray> consumer) {
        JsonArray json = new JsonArray();
        consumer.accept(json);
        return json;
    }

    protected static JsonArray jsonArray(JsonElement... elements) {
        JsonArray json = new JsonArray();
        for (JsonElement element : elements) {
            json.add(element);
        }
        return json;
    }

    @SafeVarargs
    protected static JsonArray jsonArray(Consumer<? super JsonObject>... consumers) {
        JsonArray json = new JsonArray();
        for (Consumer<? super JsonObject> consumer : consumers) {
            json.add(jsonObject(consumer));
        }
        return json;
    }
}
