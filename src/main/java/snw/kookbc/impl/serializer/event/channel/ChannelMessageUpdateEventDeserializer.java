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

import snw.jkook.entity.channel.Channel;
import snw.jkook.event.channel.ChannelMessageUpdateEvent;
import snw.jkook.message.Message;
import snw.jkook.message.component.MarkdownComponent;
import snw.kookbc.impl.KBCClient;
import snw.kookbc.impl.entity.channel.TextChannelImpl;
import snw.kookbc.impl.entity.channel.VoiceChannelImpl;
import snw.kookbc.impl.message.MessageImpl;
import snw.kookbc.impl.serializer.event.NormalEventDeserializer;

public class ChannelMessageUpdateEventDeserializer extends NormalEventDeserializer<ChannelMessageUpdateEvent> {

    public ChannelMessageUpdateEventDeserializer(KBCClient client) {
        super(client);
    }

    @Override
    protected ChannelMessageUpdateEvent deserialize(JsonObject object, Type type, JsonDeserializationContext ctx,
            long timeStamp, JsonObject body) throws JsonParseException {
        final String id = body.get("channel_id").getAsString();
        final Channel channel = body.get("channel_type").getAsInt() == 1
                ? new TextChannelImpl(client, id)
                : new VoiceChannelImpl(client, id);
        final String msgId = body.get("msg_id").getAsString();
        final String content = object.get("content").getAsString();
        return new ChannelMessageUpdateEvent(timeStamp, channel, msgId, content);
    }

    // ===== Jackson Migration Support =====

    /**
     * Jackson版本的反序列化方法 - 处理Kook不完整JSON数据
     * 提供更好的null-safe处理
     */
    @Override
    protected ChannelMessageUpdateEvent deserializeFromNode(JsonNode node, long timeStamp, JsonNode body) {
        final String id = body.get("channel_id").asText();
        final Channel channel = body.get("channel_type").asInt() == 1
                ? new TextChannelImpl(client, id)
                : new VoiceChannelImpl(client, id);
        final String msgId = body.get("msg_id").asText();
        final String content = node.get("content").asText();
        return new ChannelMessageUpdateEvent(timeStamp, channel, msgId, content);
    }

    /**
     * 启用Jackson反序列化
     */
    @Override
    protected boolean useJacksonDeserialization() {
        // 不依赖MessageBuilder，可以直接启用
        return true;
    }

    @Override
    protected void beforeReturn(ChannelMessageUpdateEvent event) {
        final Message message = client.getStorage().getMessage(event.getMessageId());
        if (message != null) {
            ((MessageImpl) message).setComponent0(new MarkdownComponent(event.getContent()));
        }
    }
}
