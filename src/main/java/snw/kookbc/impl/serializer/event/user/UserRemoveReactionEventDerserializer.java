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

import snw.jkook.entity.CustomEmoji;
import snw.jkook.entity.Reaction;
import snw.jkook.event.user.UserRemoveReactionEvent;
import snw.kookbc.impl.KBCClient;
import snw.kookbc.impl.entity.ReactionImpl;
import snw.kookbc.impl.serializer.event.NormalEventDeserializer;

public class UserRemoveReactionEventDerserializer extends NormalEventDeserializer<UserRemoveReactionEvent> {
    public UserRemoveReactionEventDerserializer(KBCClient client) {
        super(client);
    }

    @Override
    protected UserRemoveReactionEvent deserialize(JsonObject object, Type type, JsonDeserializationContext ctx, long timeStamp, JsonObject body) throws JsonParseException {
        JsonObject re = body.getAsJsonObject("emoji");
        CustomEmoji em = client.getStorage().getEmoji(re.get("id").getAsString(), re);
        Reaction reaction1 = client.getStorage().getReaction(
                body.get("msg_id").getAsString(), em,
                client.getStorage().getUser(body.get("user_id").getAsString())
        );
        if (reaction1 != null) {
            client.getStorage().removeReaction(reaction1);
        }
        return new UserRemoveReactionEvent(
            timeStamp,
            client.getStorage().getUser(body.get("user_id").getAsString()),
            body.get("msg_id").getAsString(),
            reaction1 == null ? new ReactionImpl(
                client, body.get("msg_id").getAsString(),
                em,
                client.getStorage().getUser(body.get("user_id").getAsString()),
                -1
            ) : reaction1
        );
    }
}
