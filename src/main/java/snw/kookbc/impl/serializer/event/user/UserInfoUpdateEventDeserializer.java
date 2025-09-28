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

import snw.jkook.event.user.UserInfoUpdateEvent;
import snw.kookbc.impl.KBCClient;
import snw.kookbc.impl.entity.UserImpl;
import snw.kookbc.impl.serializer.event.NormalEventDeserializer;
import snw.kookbc.util.JacksonUtil;

public class UserInfoUpdateEventDeserializer extends NormalEventDeserializer<UserInfoUpdateEvent> {

    public UserInfoUpdateEventDeserializer(KBCClient client) {
        super(client);
    }

    @Override
    protected UserInfoUpdateEvent deserialize(JsonObject object, Type type, JsonDeserializationContext ctx,
            long timeStamp, JsonObject body) throws JsonParseException {
        // 处理Kook API不一致的字段命名，按优先级顺序查找
        String userId = null;
        if (body.has("user_id")) {
            userId = body.get("user_id").getAsString();
        } else if (body.has("body_id")) {
            userId = body.get("body_id").getAsString();
        }

        if (userId == null) {
            throw new JsonParseException("Missing required field 'user_id' or 'body_id' in user update event");
        }

        UserImpl user = ((UserImpl) client.getStorage().getUser(userId));

        // Null-safe字段更新
        if (body.has("username")) {
            user.setName(body.get("username").getAsString());
        }
        if (body.has("avatar")) {
            user.setAvatarUrl(body.get("avatar").getAsString());
        }

        return new UserInfoUpdateEvent(timeStamp, user);
    }

    // ===== Jackson Migration Support =====

    /**
     * Jackson版本的反序列化方法 - 处理Kook不完整JSON数据
     * 提供更好的null-safe处理
     */
    @Override
    protected UserInfoUpdateEvent deserializeFromNode(JsonNode node, long timeStamp, JsonNode body) {
        // 使用 JacksonUtil 的空值处理机制
        String userId = JacksonUtil.getStringOrDefault(body, "user_id", null);
        if (userId == null) {
            userId = JacksonUtil.getStringOrDefault(body, "body_id", null);
        }

        if (userId == null) {
            throw new RuntimeException("Missing required field 'user_id' or 'body_id' in user update event");
        }

        UserImpl user = ((UserImpl) client.getStorage().getUser(userId));

        // 使用 null-safe 字段更新
        if (JacksonUtil.hasNonNull(body, "username")) {
            user.setName(JacksonUtil.getStringOrDefault(body, "username", user.getName()));
        }
        if (JacksonUtil.hasNonNull(body, "avatar")) {
            user.setAvatarUrl(JacksonUtil.getStringOrDefault(body, "avatar", user.getAvatarUrl(false)));
        }

        return new UserInfoUpdateEvent(timeStamp, user);
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
