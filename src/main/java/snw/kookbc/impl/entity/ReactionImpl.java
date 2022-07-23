/*
 *     KookBC -- The Kook Bot Client & JKook API standard implementation for Java.
 *     Copyright (C) 2022 KookBC contributors
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

package snw.kookbc.impl.entity;

import snw.jkook.entity.CustomEmoji;
import snw.jkook.entity.Reaction;
import snw.jkook.entity.User;
import snw.kookbc.impl.KBCClient;
import snw.kookbc.impl.network.HttpAPIRoute;
import snw.kookbc.util.MapBuilder;

import java.util.Map;
import java.util.Objects;

public class ReactionImpl implements Reaction {
    private final String messageId;
    private final CustomEmoji emoji;
    private final User sender;
    private final long timeStamp;

    public ReactionImpl(String messageId, CustomEmoji emoji, User sender, long timeStamp) {
        this.messageId = messageId;
        this.emoji = emoji;
        this.sender = sender;
        this.timeStamp = timeStamp;
    }

    @Override
    public String getMessageId() {
        return messageId;
    }

    @Override
    public CustomEmoji getEmoji() {
        return emoji;
    }

    @Override
    public void delete() {
        Map<String, Object> body = new MapBuilder()
                .put("msg_id", getMessageId())
                .put("emoji_id", emoji.getId())
                .build();
        KBCClient.getInstance().getConnector().getClient().post(HttpAPIRoute.CHANNEL_MESSAGE_REACTION_REMOVE.toFullURL(), body);
    }

    @Override
    public User getSender() {
        return sender;
    }

    @Override
    public long getTimeStamp() {
        return timeStamp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ReactionImpl)) return false;
        ReactionImpl reaction = (ReactionImpl) o;
        return messageId.equals(reaction.messageId) && emoji.equals(reaction.emoji) && sender.equals(reaction.sender);
    }

    @Override
    public int hashCode() {
        return Objects.hash(messageId, emoji, sender);
    }
}
