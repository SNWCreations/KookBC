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
import snw.jkook.event.user.UserInfoUpdateEvent;
import snw.kookbc.impl.KBCClient;
import snw.kookbc.impl.entity.UserImpl;
import snw.kookbc.impl.serializer.event.jackson.BaseJacksonEventDeserializer;
import snw.kookbc.util.JacksonUtil;

/**
 * UserInfoUpdateEvent 的 Jackson 反序列化器
 *
 * @since KookBC 0.32.2
 */
public class UserInfoUpdateEventJacksonDeserializer extends BaseJacksonEventDeserializer<UserInfoUpdateEvent> {

    public UserInfoUpdateEventJacksonDeserializer(KBCClient client) {
        super(client);
    }

    @Override
    protected UserInfoUpdateEvent deserialize(JsonNode node) {
        final long timeStamp = extractTimeStamp(node);
        final JsonNode body = extractBody(node);

        String userId = JacksonUtil.getStringOrDefault(body, "user_id", null);
        if (userId == null) {
            userId = JacksonUtil.getStringOrDefault(body, "body_id", null);
        }

        if (userId == null) {
            throw new RuntimeException("Missing required field 'user_id' or 'body_id' in user update event");
        }

        UserImpl user = ((UserImpl) client.getStorage().getUser(userId));

        if (JacksonUtil.hasNonNull(body, "username")) {
            user.setName(JacksonUtil.getStringOrDefault(body, "username", user.getName()));
        }
        if (JacksonUtil.hasNonNull(body, "avatar")) {
            user.setAvatarUrl(JacksonUtil.getStringOrDefault(body, "avatar", user.getAvatarUrl(false)));
        }

        return new UserInfoUpdateEvent(timeStamp, user);
    }
}
