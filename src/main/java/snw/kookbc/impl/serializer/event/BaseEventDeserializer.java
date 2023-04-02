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

package snw.kookbc.impl.serializer.event;

import com.google.gson.*;
import snw.jkook.event.Event;
import snw.kookbc.impl.KBCClient;

import java.lang.reflect.Type;

public abstract class BaseEventDeserializer<T extends Event> implements JsonDeserializer<T> {
    protected final KBCClient client;

    protected BaseEventDeserializer(KBCClient client) {
        this.client = client;
    }

    @Override
    public final T deserialize(JsonElement element, Type type, JsonDeserializationContext ctx) throws JsonParseException {
        final JsonObject object = element.getAsJsonObject();
        T t = deserialize(object, type, ctx);
        beforeReturn(t);
        return t;
    }

    protected abstract T deserialize(JsonObject object, Type type, JsonDeserializationContext ctx) throws JsonParseException;

    // override it if you want to do something before we returning the final result.
    protected void beforeReturn(T event) {
    }

}
