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

package snw.kookbc.impl.serializer.event.item;

import static com.google.gson.JsonParser.parseString;
import static snw.kookbc.util.GsonUtil.getAsInt;
import static snw.kookbc.util.GsonUtil.getAsJsonObject;
import static snw.kookbc.util.GsonUtil.getAsLong;
import static snw.kookbc.util.GsonUtil.getAsString;

import java.lang.reflect.Type;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import snw.jkook.entity.User;
import snw.jkook.event.item.ItemConsumedEvent;
import snw.kookbc.impl.KBCClient;
import snw.kookbc.impl.serializer.event.BaseEventDeserializer;
import snw.kookbc.impl.storage.EntityStorage;

public class ItemConsumedEventDeserializer extends BaseEventDeserializer<ItemConsumedEvent> {

    public ItemConsumedEventDeserializer(KBCClient client) {
        super(client);
    }

    @Override
    protected ItemConsumedEvent deserialize(JsonObject object, Type type, JsonDeserializationContext ctx)
            throws JsonParseException {
        final EntityStorage storage = client.getStorage();
        final JsonObject content = parseString(getAsString(object, "content")).getAsJsonObject();
        final JsonObject data = getAsJsonObject(content, "data");
        final long timeStamp = getAsLong(object, "msg_timestamp");
        final User consumer = storage.getUser(getAsString(data, "user_id"));
        final User affected = storage.getUser(getAsString(data, "target_id"));
        final int itemId = getAsInt(data, "item_id");
        return new ItemConsumedEvent(timeStamp, consumer, affected, itemId);
    }

}
