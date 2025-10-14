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

package snw.kookbc.impl.serializer.event.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import snw.jkook.event.Event;
import snw.kookbc.impl.KBCClient;

import java.io.IOException;

import static snw.kookbc.util.JacksonUtil.get;

/**
 * Jackson 事件反序列化器基类
 *
 * <p>提供事件反序列化的通用逻辑和工具方法。
 *
 * @param <T> 事件类型
 * @since KookBC 0.32.2
 */
public abstract class BaseJacksonEventDeserializer<T extends Event> extends JsonDeserializer<T> {

    protected final KBCClient client;

    protected BaseJacksonEventDeserializer(KBCClient client) {
        this.client = client;
    }

    @Override
    public T deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonNode node = p.getCodec().readTree(p);
        T event = deserialize(node);

        // 调用后处理钩子
        if (event != null) {
            beforeReturn(event);
        }

        return event;
    }

    /**
     * 从 JsonNode 反序列化事件对象
     *
     * @param node JSON 数据节点
     * @return 事件对象
     */
    protected abstract T deserialize(JsonNode node);

    /**
     * 事件返回前的处理钩子
     * <p>子类可以重写此方法进行额外处理，例如更新缓存
     *
     * @param event 事件对象
     */
    protected void beforeReturn(T event) {
        // 默认不做处理，子类可以重写
    }

    /**
     * 提取时间戳
     *
     * @param node JSON 节点
     * @return 时间戳（毫秒）
     */
    protected long extractTimeStamp(JsonNode node) {
        return get(node, "msg_timestamp").asLong();
    }

    /**
     * 提取 extra.body 节点
     *
     * @param node JSON 节点
     * @return body 节点
     */
    protected JsonNode extractBody(JsonNode node) {
        return get(get(node, "extra"), "body");
    }
}
