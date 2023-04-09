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
import snw.jkook.entity.Reaction;
import snw.jkook.event.user.UserRemoveReactionEvent;
import snw.kookbc.impl.KBCClient;
import snw.kookbc.impl.entity.ReactionImpl;
import snw.kookbc.impl.serializer.event.NormalEventDeserializer;

import java.lang.reflect.Type;

import static snw.kookbc.util.GsonUtil.get;

public class UserRemoveReactionEventDeserializer extends NormalEventDeserializer<UserRemoveReactionEvent> {

    public UserRemoveReactionEventDeserializer(KBCClient client) {
        super(client);
    }

    @Override
    protected UserRemoveReactionEvent deserialize(JsonObject object, Type type, JsonDeserializationContext ctx, long timeStamp, JsonObject body) throws JsonParseException {
        JsonObject emojiObject = get(body, "emoji").getAsJsonObject();
        CustomEmoji customEmoji = client.getStorage().getEmoji(get(emojiObject, "id").getAsString(), emojiObject);
        Reaction reaction = client.getStorage().getReaction(
                get(object, "msg_id").getAsString(), customEmoji,
                client.getStorage().getUser(get(object, "user_id").getAsString())
        );
        if (reaction != null) {
            client.getStorage().removeReaction(reaction);
        } else {
            reaction = new ReactionImpl(
                    client,
                    get(object, "msg.id").getAsString(),
                    customEmoji,
                    client.getStorage().getUser(get(object, "user_id").getAsString()),
                    -1
            );
        }
        return new UserRemoveReactionEvent(
            timeStamp,
            client.getStorage().getUser(get(object, "user_id").getAsString()),
            get(object, "msg_id").getAsString(),
            reaction
        );
    }

}
