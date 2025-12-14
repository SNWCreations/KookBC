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
import snw.jkook.event.guild.GuildUnbanUserEvent;
import snw.kookbc.impl.KBCClient;
import snw.kookbc.impl.serializer.event.jackson.BaseJacksonEventDeserializer;
import snw.kookbc.impl.storage.EntityStorage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * GuildUnbanUserEvent 的 Jackson 反序列化器
 *
 * @since KookBC 0.33.0
 */
public class GuildUnbanUserEventJacksonDeserializer extends BaseJacksonEventDeserializer<GuildUnbanUserEvent> {

    public GuildUnbanUserEventJacksonDeserializer(KBCClient client) {
        super(client);
    }

    @Override
    protected GuildUnbanUserEvent deserialize(JsonNode node) {
        final long timeStamp = extractTimeStamp(node);
        final JsonNode body = extractBody(node);

        final EntityStorage storage = client.getStorage();
        final Guild guild = storage.getGuild(node.get("target_id").asText());

        // 转换 JsonNode 数组为 List<User>
        List<User> unbannedList = new ArrayList<>();
        JsonNode userIdArray = body.get("user_id");
        if (userIdArray != null && userIdArray.isArray()) {
            for (JsonNode userIdNode : userIdArray) {
                unbannedList.add(storage.getUser(userIdNode.asText()));
            }
        }
        final List<User> unbanned = Collections.unmodifiableList(unbannedList);

        final User operator = storage.getUser(body.get("operator_id").asText());
        return new GuildUnbanUserEvent(timeStamp, guild, unbanned, operator);
    }
}
