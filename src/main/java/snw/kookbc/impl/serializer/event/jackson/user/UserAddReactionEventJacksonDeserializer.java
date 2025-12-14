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
import snw.jkook.entity.User;
import snw.jkook.event.user.UserAddReactionEvent;
import snw.kookbc.impl.KBCClient;
import snw.kookbc.impl.entity.ReactionImpl;
import snw.kookbc.impl.serializer.event.jackson.BaseJacksonEventDeserializer;

/**
 * UserAddReactionEvent 的 Jackson 反序列化器
 *
 * @since KookBC 0.33.0
 */
public class UserAddReactionEventJacksonDeserializer extends BaseJacksonEventDeserializer<UserAddReactionEvent> {

    public UserAddReactionEventJacksonDeserializer(KBCClient client) {
        super(client);
    }

    @Override
    protected UserAddReactionEvent deserialize(JsonNode node) {
        final long timeStamp = extractTimeStamp(node);
        final JsonNode body = extractBody(node);

        final String messageId = body.get("msg_id").asText();
        final User user = client.getStorage().getUser(body.get("user_id").asText());
        final JsonNode rawEmoji = body.get("emoji");
        final CustomEmoji emoji = client.getStorage().getEmoji(rawEmoji.get("id").asText(), rawEmoji);
        final ReactionImpl reaction = new ReactionImpl(client, messageId, emoji, user, timeStamp);

        return new UserAddReactionEvent(timeStamp, user, messageId, reaction);
    }

    @Override
    protected void beforeReturn(UserAddReactionEvent event) {
        client.getStorage().addReaction(event.getReaction());
    }
}
