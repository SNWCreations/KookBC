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
import snw.jkook.entity.User;
import snw.jkook.entity.channel.Channel;
import snw.jkook.event.channel.ChannelMessagePinEvent;
import snw.kookbc.impl.KBCClient;
import snw.kookbc.impl.entity.channel.TextChannelImpl;
import snw.kookbc.impl.entity.channel.VoiceChannelImpl;
import snw.kookbc.impl.serializer.event.jackson.BaseJacksonEventDeserializer;

/**
 * ChannelMessagePinEvent 的 Jackson 反序列化器
 *
 * @since KookBC 0.33.0
 */
public class ChannelMessagePinEventJacksonDeserializer extends BaseJacksonEventDeserializer<ChannelMessagePinEvent> {

    public ChannelMessagePinEventJacksonDeserializer(KBCClient client) {
        super(client);
    }

    @Override
    protected ChannelMessagePinEvent deserialize(JsonNode node) {
        final long timeStamp = extractTimeStamp(node);
        final JsonNode body = extractBody(node);

        final String id = body.get("channel_id").asText();
        final Channel channel = body.get("channel_type").asInt() == 1
                ? new TextChannelImpl(client, id)
                : new VoiceChannelImpl(client, id);
        final String msgId = body.get("msg_id").asText();
        final User operator = client.getStorage().getUser(body.get("operator_id").asText());
        return new ChannelMessagePinEvent(timeStamp, channel, msgId, operator);
    }
}
