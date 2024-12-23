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

import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

// An easy builder for building POST body.
public class MapBuilder {
    private final Map<String, Object> result = new HashMap<>();

    public MapBuilder put(String key, Object value) {
        result.put(key, value);
        return this;
    }

    public MapBuilder putIfNotNull(String key, @Nullable Object value) {
        if (value != null) {
            result.put(key, value);
        }
        return this;
    }

    public <T> MapBuilder putIfNotNull(String key, T source, Function<T, Object> behavior) {
        if (source != null) {
            result.put(key, behavior.apply(source));
        }
        return this;
    }

    @SuppressWarnings("unchecked")
    public <T> MapBuilder putIfInstance(String key, Object source, Class<T> type, Function<T, Object> behavior) {
        if (type.isInstance(source)) {
            result.put(key, behavior.apply((T) source));
        }
        return this;
    }

    public Map<String, Object> build() {
        return result;
    }
}
