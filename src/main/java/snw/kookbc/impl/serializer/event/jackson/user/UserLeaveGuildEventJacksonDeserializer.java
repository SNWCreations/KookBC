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

package snw.kookbc.impl.serializer.event.jackson.user;

import com.fasterxml.jackson.databind.JsonNode;
import snw.jkook.entity.Guild;
import snw.jkook.entity.User;
import snw.jkook.event.user.UserLeaveGuildEvent;
import snw.kookbc.impl.KBCClient;
import snw.kookbc.impl.serializer.event.jackson.BaseJacksonEventDeserializer;

import static snw.kookbc.util.JacksonUtil.get;

/**
 * UserLeaveGuildEvent 的 Jackson 反序列化器
 *
 * @since KookBC 0.32.2
 */
public class UserLeaveGuildEventJacksonDeserializer extends BaseJacksonEventDeserializer<UserLeaveGuildEvent> {

    public UserLeaveGuildEventJacksonDeserializer(KBCClient client) {
        super(client);
    }

    @Override
    protected UserLeaveGuildEvent deserialize(JsonNode node) {
        final long timeStamp = extractTimeStamp(node);
        final JsonNode body = extractBody(node);

        String realType = get(get(node, "extra"), "type").asText();
        User user;
        String guildId;
        if ("self_exited_guild".equals(realType)) {
            user = client.getCore().getUser();
            guildId = body.get("guild_id").asText();
        } else {
            user = client.getStorage().getUser(body.get("user_id").asText());
            guildId = node.get("target_id").asText();
        }
        Guild guild = client.getStorage().getGuild(guildId);
        if (guild == null) {
            return new UserLeaveGuildEvent(timeStamp, user, guildId);
        }
        return new UserLeaveGuildEvent(timeStamp, user, guild);
    }
}
