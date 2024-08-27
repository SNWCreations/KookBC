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

import static snw.kookbc.util.GsonUtil.getAsString;

import java.lang.reflect.Type;
import java.util.Objects;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import snw.jkook.entity.User;
import snw.jkook.entity.channel.NonCategoryChannel;
import snw.jkook.event.user.UserClickButtonEvent;
import snw.kookbc.impl.KBCClient;
import snw.kookbc.impl.serializer.event.NormalEventDeserializer;

public class UserClickButtonEventDeserializer extends NormalEventDeserializer<UserClickButtonEvent> {

    public UserClickButtonEventDeserializer(KBCClient client) {
        super(client);
    }

    @Override
    protected UserClickButtonEvent deserialize(JsonObject object, Type type, JsonDeserializationContext ctx,
            long timeStamp, JsonObject body) throws JsonParseException {
        final String userId = getAsString(body, "user_id");
        final String targetId = getAsString(body, "target_id");
        final Boolean needChannel = Objects.equals(userId, targetId);

        final User user = client.getStorage().getUser(userId);
        final String messageId = getAsString(body, "msg_id");
        final String value = getAsString(body, "value");
        final NonCategoryChannel channel = needChannel ? null
                : (NonCategoryChannel) client.getStorage().getChannel(targetId);
        return new UserClickButtonEvent(timeStamp, user, messageId, value, channel);
    }

}
