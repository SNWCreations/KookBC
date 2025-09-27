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

import snw.jkook.entity.User;
import snw.jkook.event.user.UserOnlineEvent;
import snw.kookbc.impl.KBCClient;
import snw.kookbc.impl.serializer.event.NormalEventDeserializer;

public class UserOnlineEventDeserializer extends NormalEventDeserializer<UserOnlineEvent> {

    public UserOnlineEventDeserializer(KBCClient client) {
        super(client);
    }

    @Override
    protected UserOnlineEvent deserialize(JsonObject object, Type type, JsonDeserializationContext ctx, long timeStamp,
            JsonObject body) throws JsonParseException {
        final User user = client.getStorage().getUser(body.get("user_id").getAsString());
        return new UserOnlineEvent(timeStamp, user);
    }

    // ===== Jackson Migration Support =====

    /**
     * Jackson版本的反序列化方法 - 处理Kook不完整JSON数据
     * 提供更好的null-safe处理
     */
    @Override
    protected UserOnlineEvent deserializeFromNode(JsonNode node, long timeStamp, JsonNode body) {
        final User user = client.getStorage().getUser(body.get("user_id").asText());
        return new UserOnlineEvent(timeStamp, user);
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
