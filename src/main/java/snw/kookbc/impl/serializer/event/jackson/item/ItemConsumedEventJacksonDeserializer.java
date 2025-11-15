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

package snw.kookbc.impl.serializer.event.jackson.item;

import com.fasterxml.jackson.databind.JsonNode;
import snw.jkook.entity.User;
import snw.jkook.event.item.ItemConsumedEvent;
import snw.kookbc.impl.KBCClient;
import snw.kookbc.impl.serializer.event.jackson.BaseJacksonEventDeserializer;
import snw.kookbc.impl.storage.EntityStorage;
import snw.kookbc.util.JacksonUtil;

/**
 * ItemConsumedEvent 的 Jackson 反序列化器
 *
 * @since KookBC 0.32.2
 */
public class ItemConsumedEventJacksonDeserializer extends BaseJacksonEventDeserializer<ItemConsumedEvent> {

    public ItemConsumedEventJacksonDeserializer(KBCClient client) {
        super(client);
    }

    @Override
    protected ItemConsumedEvent deserialize(JsonNode node) {
        final EntityStorage storage = client.getStorage();
        final JsonNode content = JacksonUtil.parse(node.get("content").asText());
        final JsonNode data = content.get("data");
        final long timeStamp = node.get("msg_timestamp").asLong();
        final User consumer = storage.getUser(data.get("user_id").asText());
        final User affected = storage.getUser(data.get("target_id").asText());
        final int itemId = data.get("item_id").asInt();
        return new ItemConsumedEvent(timeStamp, consumer, affected, itemId);
    }
}
