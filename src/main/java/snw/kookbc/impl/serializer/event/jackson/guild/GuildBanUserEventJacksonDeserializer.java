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
import snw.jkook.entity.Guild;
import snw.jkook.entity.User;
import snw.jkook.event.guild.GuildBanUserEvent;
import snw.kookbc.impl.KBCClient;
import snw.kookbc.impl.serializer.event.jackson.BaseJacksonEventDeserializer;
import snw.kookbc.impl.storage.EntityStorage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * GuildBanUserEvent 的 Jackson 反序列化器
 *
 * @since KookBC 0.33.0
 */
public class GuildBanUserEventJacksonDeserializer extends BaseJacksonEventDeserializer<GuildBanUserEvent> {

    public GuildBanUserEventJacksonDeserializer(KBCClient client) {
        super(client);
    }

    @Override
    protected GuildBanUserEvent deserialize(JsonNode node) {
        final long timeStamp = extractTimeStamp(node);
        final JsonNode body = extractBody(node);

        final EntityStorage entityStorage = client.getStorage();
        final Guild guild = entityStorage.getGuild(node.get("target_id").asText());
        final User operator = entityStorage.getUser(body.get("operator_id").asText());

        // 转换 JsonNode 数组为 List<User>
        List<User> bannedList = new ArrayList<>();
        JsonNode userIdArray = body.get("user_id");
        if (userIdArray != null && userIdArray.isArray()) {
            for (JsonNode userIdNode : userIdArray) {
                bannedList.add(entityStorage.getUser(userIdNode.asText()));
            }
        }
        final List<User> banned = Collections.unmodifiableList(bannedList);

        final String reason = body.get("remark").asText();
        return new GuildBanUserEvent(timeStamp, guild, banned, operator, reason);
    }
}
