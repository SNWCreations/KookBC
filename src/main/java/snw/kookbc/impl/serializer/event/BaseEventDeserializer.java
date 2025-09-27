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

import com.fasterxml.jackson.databind.JsonNode;
import com.google.gson.*;
import snw.jkook.event.Event;
import snw.kookbc.impl.KBCClient;
import snw.kookbc.util.JacksonUtil;

import java.lang.reflect.Type;

public abstract class BaseEventDeserializer<T extends Event> implements JsonDeserializer<T> {
    protected final KBCClient client;

    protected BaseEventDeserializer(KBCClient client) {
        this.client = client;
    }

    @Override
    public final T deserialize(JsonElement element, Type type, JsonDeserializationContext ctx) throws JsonParseException {
        final JsonObject object = element.getAsJsonObject();

        // 优先使用Jackson版本（性能更好且更安全）
        if (useJacksonDeserialization()) {
            try {
                JsonNode node = JacksonUtil.parse(object.toString());
                return deserializeFromNode(node);
            } catch (Exception e) {
                // Jackson反序列化失败时回退到GSON（兼容性保证）
                client.getCore().getLogger().debug("Jackson deserialization failed, falling back to GSON", e);
            }
        }

        // GSON兼容模式
        T t = deserialize(object, type, ctx);
        beforeReturn(t);
        return t;
    }

    protected abstract T deserialize(JsonObject object, Type type, JsonDeserializationContext ctx) throws JsonParseException;

    // ===== Jackson Migration Support =====

    /**
     * Jackson版本的反序列化方法 - 推荐使用
     * 提供更好的null-safe处理，适合处理Kook API的不完整JSON
     *
     * @param node Jackson JsonNode对象
     * @return 反序列化后的事件对象
     */
    protected T deserializeFromNode(JsonNode node) {
        // 子类应该重写此方法提供原生Jackson实现
        // 默认实现提供向后兼容
        try {
            JsonObject object = JacksonUtil.convertToGsonJsonObject(node);
            T event = deserialize(object, null, null);
            beforeReturn(event);
            return event;
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize event from JsonNode: " + e.getMessage(), e);
        }
    }


    /**
     * 检查是否应该使用Jackson进行反序列化
     * 子类可以重写此方法来控制迁移进度
     */
    protected boolean useJacksonDeserialization() {
        return true; // 默认启用Jackson以获得更好性能
    }

    // override it if you want to do something before we returning the final result.
    protected void beforeReturn(T event) {
    }

}
