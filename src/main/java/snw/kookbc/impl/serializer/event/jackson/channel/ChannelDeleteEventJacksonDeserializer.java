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
import snw.jkook.entity.Guild;
import snw.jkook.event.channel.ChannelDeleteEvent;
import snw.kookbc.impl.KBCClient;
import snw.kookbc.impl.serializer.event.jackson.BaseJacksonEventDeserializer;

/**
 * ChannelDeleteEvent 的 Jackson 反序列化器
 *
 * @since KookBC 0.32.2
 */
public class ChannelDeleteEventJacksonDeserializer extends BaseJacksonEventDeserializer<ChannelDeleteEvent> {

    public ChannelDeleteEventJacksonDeserializer(KBCClient client) {
        super(client);
    }

    @Override
    protected ChannelDeleteEvent deserialize(JsonNode node) {
        final long timeStamp = extractTimeStamp(node);
        final JsonNode body = extractBody(node);

        final String id = body.get("id").asText();
        final Guild guild = client.getStorage().getGuild(node.get("target_id").asText());
        return new ChannelDeleteEvent(timeStamp, id, guild);
    }

    @Override
    protected void beforeReturn(ChannelDeleteEvent event) {
        client.getStorage().removeChannel(event.getChannelId());
    }
}
