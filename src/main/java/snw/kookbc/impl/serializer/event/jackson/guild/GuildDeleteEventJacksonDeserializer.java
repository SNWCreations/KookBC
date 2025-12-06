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

package snw.kookbc.impl.serializer.event.jackson.guild;

import com.fasterxml.jackson.databind.JsonNode;
import snw.jkook.event.guild.GuildDeleteEvent;
import snw.kookbc.impl.KBCClient;
import snw.kookbc.impl.serializer.event.jackson.BaseJacksonEventDeserializer;

/**
 * GuildDeleteEvent 的 Jackson 反序列化器
 *
 * @since KookBC 0.33.0
 */
public class GuildDeleteEventJacksonDeserializer extends BaseJacksonEventDeserializer<GuildDeleteEvent> {

    public GuildDeleteEventJacksonDeserializer(KBCClient client) {
        super(client);
    }

    @Override
    protected GuildDeleteEvent deserialize(JsonNode node) {
        final long timeStamp = extractTimeStamp(node);
        final JsonNode body = extractBody(node);

        final String id = body.get("id").asText();
        client.getStorage().removeGuild(id);
        return new GuildDeleteEvent(timeStamp, id);
    }
}
