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

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import snw.jkook.entity.channel.TextChannel;
import snw.jkook.event.user.UserClickButtonEvent;
import snw.kookbc.impl.KBCClient;
import snw.kookbc.impl.serializer.event.NormalEventDeserializer;

import java.lang.reflect.Type;
import java.util.Objects;

import static snw.kookbc.util.GsonUtil.get;

public class UserClickButtonEventDeserializer extends NormalEventDeserializer<UserClickButtonEvent> {

    public UserClickButtonEventDeserializer(KBCClient client) {
        super(client);
    }

    @Override
    protected UserClickButtonEvent deserialize(JsonObject object, Type type, JsonDeserializationContext ctx, long timeStamp, JsonObject body) throws JsonParseException {
        return new UserClickButtonEvent(
                timeStamp,
                client.getStorage().getUser(get(body, "user_id").getAsString()),
                get(body, "msg_id").getAsString(),
                get(body, "value").getAsString(),
                Objects.equals(
                        get(body, "user_id").getAsString(),
                        get(body, "target_id").getAsString()
                ) ? null : (TextChannel) client.getStorage().getChannel(get(body, "target_id").getAsString())
        );
    }

}
