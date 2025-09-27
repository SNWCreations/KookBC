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


import java.lang.reflect.Type;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import snw.jkook.event.Event;
import snw.kookbc.impl.KBCClient;
import snw.kookbc.util.JacksonUtil;

import static snw.kookbc.util.JacksonUtil.get;

public abstract class NormalEventDeserializer<T extends Event> extends BaseEventDeserializer<T> {

    protected NormalEventDeserializer(KBCClient client) {
        super(client);
    }

    @Override
    protected T deserialize(JsonObject object, Type type, JsonDeserializationContext ctx) throws JsonParseException {
        final long timeStamp = object.get("msg_timestamp").getAsLong();
        final JsonObject body = object.getAsJsonObject("extra").getAsJsonObject("body");
        return deserialize(object, type, ctx, timeStamp, body);
    }

    protected abstract T deserialize(JsonObject object, Type type, JsonDeserializationContext ctx, long timeStamp,
            JsonObject body) throws JsonParseException;

    // ===== Jackson Migration Support =====

    /**
     * Jackson版本的反序列化方法 - 提供更好的null-safe处理
     */
    @Override
    protected T deserializeFromNode(JsonNode node) {
        try {
            final long timeStamp = get(node, "msg_timestamp").asLong();
            final JsonNode body = get(get(node, "extra"), "body");
            return deserializeFromNode(node, timeStamp, body);
        } catch (Exception e) {
            // 如果Jackson反序列化失败，回退到基类的桥接实现
            client.getCore().getLogger().debug("Jackson specific deserialization failed for {}, using fallback",
                    getClass().getSimpleName(), e);
            return super.deserializeFromNode(node);
        }
    }

    /**
     * Jackson版本的抽象反序列化方法 - 子类可以选择性实现以获得更好性能
     * 默认实现：委托给基类的桥接实现以保持向后兼容性
     */
    protected T deserializeFromNode(JsonNode node, long timeStamp, JsonNode body) {
        // 默认实现：使用基类的桥接实现
        // 子类重写此方法可获得更好的性能和null-safe处理
        return super.deserializeFromNode(node);
    }

}
