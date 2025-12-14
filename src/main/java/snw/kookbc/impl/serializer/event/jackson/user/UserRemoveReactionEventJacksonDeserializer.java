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
import snw.jkook.entity.CustomEmoji;
import snw.jkook.entity.Reaction;
import snw.jkook.entity.User;
import snw.jkook.event.user.UserRemoveReactionEvent;
import snw.kookbc.impl.KBCClient;
import snw.kookbc.impl.entity.ReactionImpl;
import snw.kookbc.impl.serializer.event.jackson.BaseJacksonEventDeserializer;

/**
 * UserRemoveReactionEvent 的 Jackson 反序列化器
 *
 * @since KookBC 0.33.0
 */
public class UserRemoveReactionEventJacksonDeserializer extends BaseJacksonEventDeserializer<UserRemoveReactionEvent> {

    public UserRemoveReactionEventJacksonDeserializer(KBCClient client) {
        super(client);
    }

    @Override
    protected UserRemoveReactionEvent deserialize(JsonNode node) {
        final long timeStamp = extractTimeStamp(node);
        final JsonNode body = extractBody(node);

        final JsonNode emojiObject = body.get("emoji");
        final CustomEmoji customEmoji = client.getStorage().getEmoji(emojiObject.get("id").asText(), emojiObject);
        final User user = client.getStorage().getUser(body.get("user_id").asText());
        final String messageId = body.get("msg_id").asText();
        Reaction reaction = client.getStorage().getReaction(messageId, customEmoji, user);
        if (reaction != null) {
            client.getStorage().removeReaction(reaction);
        } else {
            reaction = new ReactionImpl(client, messageId, customEmoji, user, -1);
        }
        return new UserRemoveReactionEvent(timeStamp, user, messageId, reaction);
    }
}
