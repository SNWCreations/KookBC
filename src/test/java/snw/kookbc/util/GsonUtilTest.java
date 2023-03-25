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

package snw.kookbc.util;

import static org.junit.jupiter.api.Assertions.*;
import static snw.kookbc.util.GsonUtil.*;

import java.lang.reflect.Type;
import java.util.List;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.Test;
import com.google.gson.JsonArray;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;

public class GsonUtilTest {
    @Test
    public void testCreateListType() {
        Type listString = createListType(String.class);
        JsonArray array = new JsonArray();
        array.add("a");
        array.add("b");

        List<String> deserializedList = NORMAL_GSON.fromJson(array, listString);

        assertEquals(array.size(), deserializedList.size());
        assertEquals(array.get(0).getAsString(), deserializedList.get(0));
        assertEquals(array.get(1).getAsString(), deserializedList.get(1));
    }

    @Test
    public void testHas() {
        // JsonObject.has will fail because the target object is null.
        assertThrows(NullPointerException.class, () -> has(null, "key"), "We should fail if there is no target object.");

        JsonObject obj = new JsonObject();

        assertFalse(has(obj, "test"), "There is no mapped value, but result was true.");

        obj.addProperty("test", "value");

        assertTrue(has(obj, "test"), "There is a mapped value, but result was false.");
    }

    @Test
    public void testGet() {
        JsonObject obj = new JsonObject();

        assertThrows(NoSuchElementException.class, () -> get(obj, "test"), "There is no mapped value, but NoSuchElementException has not thrown.");

        obj.addProperty("test", "value");

        assertTrue(get(obj, "test").isJsonPrimitive(), "String is a kind of JsomPrimitive, but result was false.");
        assertEquals(get(obj, "test").getAsString(), "value");

        obj.add("jsonNull", JsonNull.INSTANCE);

        // JsonNull should be converted to Java null,
        // and the metbod call should fail.
        assertThrows(NoSuchElementException.class, () -> get(obj, "jsonNull"), "JsonNull should be filtered out.");
    }
}
