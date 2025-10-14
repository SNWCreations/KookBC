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
import snw.jkook.entity.CustomEmoji;
import snw.jkook.entity.Guild;
import snw.jkook.event.guild.GuildUpdateEmojiEvent;
import snw.kookbc.impl.KBCClient;
import snw.kookbc.impl.entity.CustomEmojiImpl;
import snw.kookbc.impl.serializer.event.jackson.BaseJacksonEventDeserializer;

/**
 * GuildUpdateEmojiEvent 的 Jackson 反序列化器
 *
 * @since KookBC 0.32.2
 */
public class GuildUpdateEmojiEventJacksonDeserializer extends BaseJacksonEventDeserializer<GuildUpdateEmojiEvent> {

    public GuildUpdateEmojiEventJacksonDeserializer(KBCClient client) {
        super(client);
    }

    @Override
    protected GuildUpdateEmojiEvent deserialize(JsonNode node) {
        final long timeStamp = extractTimeStamp(node);
        final JsonNode body = extractBody(node);

        final CustomEmoji customEmoji = client.getStorage().getEmoji(body.get("id").asText(), body);
        final Guild guild = customEmoji.getGuild();
        ((CustomEmojiImpl) customEmoji).update(body);
        return new GuildUpdateEmojiEvent(timeStamp, guild, customEmoji);
    }
}
