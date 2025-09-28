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

package snw.kookbc.impl.serializer.event.channel;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

// Jackson Migration Support
import com.fasterxml.jackson.databind.JsonNode;
import snw.jkook.event.channel.ChannelInfoUpdateEvent;
import snw.kookbc.util.JacksonUtil;
import snw.kookbc.impl.KBCClient;
import snw.kookbc.impl.entity.channel.ChannelImpl;
import snw.kookbc.impl.serializer.event.NormalEventDeserializer;

import java.lang.reflect.Type;
import java.util.Objects;

import static snw.kookbc.impl.entity.builder.EntityBuildUtil.parseChannel;

public class ChannelInfoUpdateEventDeserializer extends NormalEventDeserializer<ChannelInfoUpdateEvent> {

    public ChannelInfoUpdateEventDeserializer(KBCClient client) {
        super(client);
    }

    @Override
    protected ChannelInfoUpdateEvent deserialize(JsonObject object, Type type, JsonDeserializationContext ctx, long timeStamp, JsonObject body) throws JsonParseException {
        // Null-safe字段获取
        final String id = body.has("id") ? body.get("id").getAsString() : null;
        final int channelType = body.has("type") ? body.get("type").getAsInt() : 0;

        if (id == null) {
            throw new JsonParseException("Missing required field 'id' in channel update event");
        }

        final ChannelImpl channel = (ChannelImpl) parseChannel(client, id, channelType);
        if (channel == null) {
            throw new JsonParseException("Unable to parse channel with id: " + id + ", type: " + channelType);
        }

        channel.update(body);
        return new ChannelInfoUpdateEvent(timeStamp, channel);
    }

    // ===== Jackson Migration Support =====

    /**
     * Jackson版本的反序列化方法 - 处理Kook不完整JSON数据
     * 提供更好的null-safe处理
     */
    @Override
    protected ChannelInfoUpdateEvent deserializeFromNode(JsonNode node, long timeStamp, JsonNode body) {
        // 使用 JacksonUtil 的空值处理机制
        final String id = JacksonUtil.getStringOrDefault(body, "id", null);
        final int channelType = JacksonUtil.getIntOrDefault(body, "type", 0);

        if (id == null) {
            throw new RuntimeException("Missing required field 'id' in channel update event");
        }

        final ChannelImpl channel = (ChannelImpl) parseChannel(client, id, channelType);
        if (channel == null) {
            throw new RuntimeException("Unable to parse channel with id: " + id + ", type: " + channelType);
        }

        channel.update(body);
        return new ChannelInfoUpdateEvent(timeStamp, channel);
    }

    /**
     * 启用Jackson反序列化
     */
    @Override
    protected boolean useJacksonDeserialization() {
        // ChannelImpl已支持Jackson update方法，可以启用
        return true;
    }

}
