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

package snw.kookbc.impl.serializer.event.guild;


import java.lang.reflect.Type;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

// Jackson Migration Support
import com.fasterxml.jackson.databind.JsonNode;

import snw.jkook.entity.Guild;
import snw.jkook.entity.User;
import snw.jkook.event.guild.GuildUserNickNameUpdateEvent;
import snw.kookbc.impl.KBCClient;
import snw.kookbc.impl.serializer.event.NormalEventDeserializer;
import snw.kookbc.impl.storage.EntityStorage;
import snw.kookbc.util.JacksonUtil;

public class GuildUserNickNameUpdateEventDeserializer extends NormalEventDeserializer<GuildUserNickNameUpdateEvent> {

    public GuildUserNickNameUpdateEventDeserializer(KBCClient client) {
        super(client);
    }

    @Override
    protected GuildUserNickNameUpdateEvent deserialize(JsonObject object, Type type, JsonDeserializationContext ctx,
            long timeStamp, JsonObject body) throws JsonParseException {
        String guildId;
        User user;
        String nickname;
        final EntityStorage entityStorage = client.getStorage();

        if (body.has("my_nickname")) {
            // GuildInfoUpdateEvent 中的昵称更新
            guildId = body.get("id").getAsString();
            user = client.getCore().getUser();
            nickname = body.get("my_nickname").getAsString();
        } else {
            // 普通用户昵称更新事件
            guildId = object.get("target_id").getAsString();

            // 处理 user_id 字段缺失的情况
            if (!body.has("user_id")) {
                throw new JsonParseException("Missing required field 'user_id' in guild member update event");
            }
            user = entityStorage.getUser(body.get("user_id").getAsString());

            // 处理 nickname 字段缺失的情况，使用空字符串作为默认值
            nickname = body.has("nickname") ? body.get("nickname").getAsString() : "";
        }

        final Guild guild = entityStorage.getGuild(guildId);
        return new GuildUserNickNameUpdateEvent(timeStamp, guild, user, nickname);
    }

    // ===== Jackson Migration Support =====

    /**
     * Jackson版本的反序列化方法 - 处理Kook不完整JSON数据
     * 提供更好的null-safe处理
     */
    @Override
    protected GuildUserNickNameUpdateEvent deserializeFromNode(JsonNode node, long timeStamp, JsonNode body) {
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

            // 处理 user_id 字段缺失的情况
            String userId = JacksonUtil.getStringOrDefault(body, "user_id", null);
            if (userId == null) {
                throw new RuntimeException("Missing required field 'user_id' in guild member update event");
            }
            user = entityStorage.getUser(userId);

            // 使用 JacksonUtil 的空值处理，默认值为空字符串
            nickname = JacksonUtil.getStringOrDefault(body, "nickname", "");
        }

        final Guild guild = entityStorage.getGuild(guildId);
        return new GuildUserNickNameUpdateEvent(timeStamp, guild, user, nickname);
    }

    /**
     * 启用Jackson反序列化
     */
    @Override
    protected boolean useJacksonDeserialization() {
        // 启用 Jackson 反序列化以更好地处理空值
        return true;
    }

}
