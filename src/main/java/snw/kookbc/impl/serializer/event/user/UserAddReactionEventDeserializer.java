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
import snw.jkook.entity.CustomEmoji;
import snw.jkook.entity.User;
import snw.jkook.event.user.UserAddReactionEvent;
import snw.kookbc.impl.KBCClient;
import snw.kookbc.impl.entity.ReactionImpl;
import snw.kookbc.impl.serializer.event.NormalEventDeserializer;

import java.lang.reflect.Type;

import static snw.kookbc.util.GsonUtil.get;

public class UserAddReactionEventDeserializer extends NormalEventDeserializer<UserAddReactionEvent> {

    public UserAddReactionEventDeserializer(KBCClient client) {
        super(client);
    }

    @Override
    protected UserAddReactionEvent deserialize(JsonObject object, Type type, JsonDeserializationContext ctx, long timeStamp, JsonObject body) throws JsonParseException {
        String messageId = get(body, "msg_id").getAsString();
        User user = client.getStorage().getUser(get(body, "user_id").getAsString());
        JsonObject rawEmoji = get(body, "emoji").getAsJsonObject();
        CustomEmoji emoji = client.getStorage().getEmoji(get(rawEmoji, "id").getAsString(), rawEmoji);
        ReactionImpl reaction = new ReactionImpl(client, messageId, emoji, user, timeStamp);
        return new UserAddReactionEvent(
            timeStamp,
            user,
            messageId,
            reaction
        );
    }

    @Override
    public void beforeReturn(UserAddReactionEvent event) {
        client.getStorage().addReaction(event.getReaction());
    }

}
