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
import snw.jkook.entity.User;
import snw.jkook.entity.channel.NonCategoryChannel;
import snw.jkook.event.user.UserClickButtonEvent;
import snw.kookbc.impl.KBCClient;
import snw.kookbc.impl.serializer.event.jackson.BaseJacksonEventDeserializer;

import java.util.Objects;

/**
 * UserClickButtonEvent 的 Jackson 反序列化器
 *
 * @since KookBC 0.32.2
 */
public class UserClickButtonEventJacksonDeserializer extends BaseJacksonEventDeserializer<UserClickButtonEvent> {

    public UserClickButtonEventJacksonDeserializer(KBCClient client) {
        super(client);
    }

    @Override
    protected UserClickButtonEvent deserialize(JsonNode node) {
        final long timeStamp = extractTimeStamp(node);
        final JsonNode body = extractBody(node);

        final String userId = body.get("user_id").asText();
        final String targetId = body.get("target_id").asText();
        final Boolean needChannel = Objects.equals(userId, targetId);

        final User user = client.getStorage().getUser(userId);
        final String messageId = body.get("msg_id").asText();
        final String value = body.get("value").asText();
        final NonCategoryChannel channel = needChannel ? null
                : (NonCategoryChannel) client.getStorage().getChannel(targetId);
        return new UserClickButtonEvent(timeStamp, user, messageId, value, channel);
    }
}
