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

import java.lang.reflect.Type;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

// Jackson Migration Support
import com.fasterxml.jackson.databind.JsonNode;

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
        final JsonObject content = new com.google.gson.JsonParser()
            .parse(object.get("content").getAsString())
            .getAsJsonObject();
        final JsonObject data = content.getAsJsonObject("data");
        final long timeStamp = object.get("msg_timestamp").getAsLong();
        final User consumer = storage.getUser(data.get("user_id").getAsString());
        final User affected = storage.getUser(data.get("target_id").getAsString());
        final int itemId = data.get("item_id").getAsInt();
        return new ItemConsumedEvent(timeStamp, consumer, affected, itemId);
    }

    // ===== Jackson Migration Support =====

    /**
     * Jackson版本的反序列化方法 - 处理Kook不完整JSON数据
     * 提供更好的null-safe处理
     */
    @Override
    protected ItemConsumedEvent deserializeFromNode(JsonNode node) {
        final EntityStorage storage = client.getStorage();
        // Jackson直接解析嵌套JSON字符串
        final JsonNode content = snw.kookbc.util.JacksonUtil.parse(node.get("content").asText());
        final JsonNode data = content.get("data");
        final long timeStamp = node.get("msg_timestamp").asLong();
        final User consumer = storage.getUser(data.get("user_id").asText());
        final User affected = storage.getUser(data.get("target_id").asText());
        final int itemId = data.get("item_id").asInt();
        return new ItemConsumedEvent(timeStamp, consumer, affected, itemId);
    }

    /**
     * 启用Jackson反序列化
     */
    @Override
    protected boolean useJacksonDeserialization() {
        // Storage已支持Jackson，可以启用
        return true;
    }

}
