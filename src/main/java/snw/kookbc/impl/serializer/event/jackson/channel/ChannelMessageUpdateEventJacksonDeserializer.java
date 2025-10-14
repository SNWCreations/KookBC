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

package snw.kookbc.impl.serializer.event.jackson.channel;

import com.fasterxml.jackson.databind.JsonNode;
import snw.jkook.entity.channel.Channel;
import snw.jkook.event.channel.ChannelMessageUpdateEvent;
import snw.jkook.message.Message;
import snw.jkook.message.component.MarkdownComponent;
import snw.kookbc.impl.KBCClient;
import snw.kookbc.impl.entity.channel.TextChannelImpl;
import snw.kookbc.impl.entity.channel.VoiceChannelImpl;
import snw.kookbc.impl.message.MessageImpl;
import snw.kookbc.impl.serializer.event.jackson.BaseJacksonEventDeserializer;

/**
 * ChannelMessageUpdateEvent 的 Jackson 反序列化器
 *
 * @since KookBC 0.32.2
 */
public class ChannelMessageUpdateEventJacksonDeserializer extends BaseJacksonEventDeserializer<ChannelMessageUpdateEvent> {

    public ChannelMessageUpdateEventJacksonDeserializer(KBCClient client) {
        super(client);
    }

    @Override
    protected ChannelMessageUpdateEvent deserialize(JsonNode node) {
        final long timeStamp = extractTimeStamp(node);
        final JsonNode body = extractBody(node);

        final String id = body.get("channel_id").asText();
        final Channel channel = body.get("channel_type").asInt() == 1
                ? new TextChannelImpl(client, id)
                : new VoiceChannelImpl(client, id);
        final String msgId = body.get("msg_id").asText();
        final String content = node.get("content").asText();
        return new ChannelMessageUpdateEvent(timeStamp, channel, msgId, content);
    }

    @Override
    protected void beforeReturn(ChannelMessageUpdateEvent event) {
        final Message message = client.getStorage().getMessage(event.getMessageId());
        if (message != null) {
            ((MessageImpl) message).setComponent0(new MarkdownComponent(event.getContent()));
        }
    }
}
