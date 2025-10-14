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
import snw.jkook.event.guild.GuildUserNickNameUpdateEvent;
import snw.kookbc.impl.KBCClient;
import snw.kookbc.impl.serializer.event.jackson.BaseJacksonEventDeserializer;
import snw.kookbc.impl.storage.EntityStorage;
import snw.kookbc.util.JacksonUtil;

/**
 * GuildUserNickNameUpdateEvent 的 Jackson 反序列化器
 *
 * @since KookBC 0.32.2
 */
public class GuildUserNickNameUpdateEventJacksonDeserializer extends BaseJacksonEventDeserializer<GuildUserNickNameUpdateEvent> {

    public GuildUserNickNameUpdateEventJacksonDeserializer(KBCClient client) {
        super(client);
    }

    @Override
    protected GuildUserNickNameUpdateEvent deserialize(JsonNode node) {
        final long timeStamp = extractTimeStamp(node);
        final JsonNode body = extractBody(node);

        String guildId;
        User user;
        String nickname;
        final EntityStorage entityStorage = client.getStorage();

        if (JacksonUtil.hasNonNull(body, "my_nickname")) {
            // GuildInfoUpdateEvent 中的昵称更新
            guildId = JacksonUtil.getRequiredString(body, "id");
            user = client.getCore().getUser();
            nickname = JacksonUtil.getStringOrDefault(body, "my_nickname", "");
        } else {
            // 普通用户昵称更新事件
            guildId = JacksonUtil.getRequiredString(node, "target_id");

            String userId = JacksonUtil.getStringOrDefault(body, "user_id", null);
            if (userId == null) {
                throw new RuntimeException("Missing required field 'user_id' in guild member update event");
            }
            user = entityStorage.getUser(userId);

            nickname = JacksonUtil.getStringOrDefault(body, "nickname", "");
        }

        final Guild guild = entityStorage.getGuild(guildId);
        return new GuildUserNickNameUpdateEvent(timeStamp, guild, user, nickname);
    }
}
