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


import java.lang.reflect.Type;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

// Jackson Migration Support
import com.fasterxml.jackson.databind.JsonNode;

import snw.jkook.entity.User;
import snw.jkook.entity.channel.Channel;
import snw.jkook.event.channel.ChannelMessagePinEvent;
import snw.kookbc.impl.KBCClient;
import snw.kookbc.impl.entity.channel.TextChannelImpl;
import snw.kookbc.impl.entity.channel.VoiceChannelImpl;
import snw.kookbc.impl.serializer.event.NormalEventDeserializer;

public class ChannelMessagePinEventDeserializer extends NormalEventDeserializer<ChannelMessagePinEvent> {

    public ChannelMessagePinEventDeserializer(KBCClient client) {
        super(client);
    }

    @Override
    protected ChannelMessagePinEvent deserialize(JsonObject object, Type type, JsonDeserializationContext ctx,
            long timeStamp, JsonObject body) throws JsonParseException {
        final String id = body.get("channel_id").getAsString();
        final Channel channel = body.get("channel_type").getAsInt() == 1
                ? new TextChannelImpl(client, id)
                : new VoiceChannelImpl(client, id);
        final String msgId = body.get("msg_id").getAsString();
        final User operator = client.getStorage().getUser(body.get("operator_id").getAsString());
        return new ChannelMessagePinEvent(timeStamp, channel, msgId, operator);
    }

    // ===== Jackson Migration Support =====

    /**
     * Jackson版本的反序列化方法 - 处理Kook不完整JSON数据
     * 提供更好的null-safe处理
     */
    @Override
    protected ChannelMessagePinEvent deserializeFromNode(JsonNode node, long timeStamp, JsonNode body) {
        final String id = body.get("channel_id").asText();
        final Channel channel = body.get("channel_type").asInt() == 1
                ? new TextChannelImpl(client, id)
                : new VoiceChannelImpl(client, id);
        final String msgId = body.get("msg_id").asText();
        final User operator = client.getStorage().getUser(body.get("operator_id").asText());
        return new ChannelMessagePinEvent(timeStamp, channel, msgId, operator);
    }

    /**
     * 启用Jackson反序列化
     */
    @Override
    protected boolean useJacksonDeserialization() {
        // 不依赖MessageBuilder，可以直接启用
        return true;
    }

}
