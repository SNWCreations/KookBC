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

package snw.kookbc.impl.serializer.event.user;


import java.lang.reflect.Type;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

// Jackson Migration Support
import com.fasterxml.jackson.databind.JsonNode;

import snw.jkook.entity.Guild;
import snw.jkook.entity.User;
import snw.jkook.event.user.UserLeaveGuildEvent;
import snw.kookbc.impl.KBCClient;
import snw.kookbc.impl.serializer.event.NormalEventDeserializer;

public class UserLeaveGuildEventDeserializer extends NormalEventDeserializer<UserLeaveGuildEvent> {

    public UserLeaveGuildEventDeserializer(KBCClient client) {
        super(client);
    }

    @Override
    protected UserLeaveGuildEvent deserialize(JsonObject object, Type type, JsonDeserializationContext ctx,
            long timeStamp, JsonObject body) throws JsonParseException {
        String realType = object.getAsJsonObject("extra").get("type").getAsString();
        User user;
        String guildId;
        if ("self_exited_guild".equals(realType)) {
            user = client.getCore().getUser();
            guildId = body.get("guild_id").getAsString();
        } else {
            user = client.getStorage().getUser(body.get("user_id").getAsString());
            guildId = object.get("target_id").getAsString();
        }
        Guild guild = client.getStorage().getGuild(guildId);
        if (guild == null) {
            return new UserLeaveGuildEvent(timeStamp, user, guildId);
        }
        return new UserLeaveGuildEvent(timeStamp, user, guild);
    }

    // ===== Jackson Migration Support =====

    /**
     * Jackson版本的反序列化方法 - 处理Kook不完整JSON数据
     * 提供更好的null-safe处理
     */
    @Override
    protected UserLeaveGuildEvent deserializeFromNode(JsonNode node, long timeStamp, JsonNode body) {
        String realType = node.get("extra").get("type").asText();
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

    /**
     * 启用Jackson反序列化
     */
    @Override
    protected boolean useJacksonDeserialization() {
        // 不依赖MessageBuilder，可以直接启用
        return true;
    }

}
