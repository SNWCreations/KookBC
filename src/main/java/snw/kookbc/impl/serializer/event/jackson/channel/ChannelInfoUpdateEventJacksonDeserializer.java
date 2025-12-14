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
import snw.jkook.event.channel.ChannelInfoUpdateEvent;
import snw.kookbc.impl.KBCClient;
import snw.kookbc.impl.entity.channel.ChannelImpl;
import snw.kookbc.impl.serializer.event.jackson.BaseJacksonEventDeserializer;
import snw.kookbc.util.JacksonUtil;

import static snw.kookbc.impl.entity.builder.EntityBuildUtil.parseChannel;

/**
 * ChannelInfoUpdateEvent 的 Jackson 反序列化器
 *
 * @since KookBC 0.33.0
 */
public class ChannelInfoUpdateEventJacksonDeserializer extends BaseJacksonEventDeserializer<ChannelInfoUpdateEvent> {

    public ChannelInfoUpdateEventJacksonDeserializer(KBCClient client) {
        super(client);
    }

    @Override
    protected ChannelInfoUpdateEvent deserialize(JsonNode node) {
        final long timeStamp = extractTimeStamp(node);
        final JsonNode body = extractBody(node);

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
}
