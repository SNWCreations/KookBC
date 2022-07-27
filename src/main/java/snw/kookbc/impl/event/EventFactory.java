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

package snw.kookbc.impl.event;

import com.google.gson.JsonObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import snw.jkook.entity.*;
import snw.jkook.entity.channel.Channel;
import snw.jkook.entity.channel.TextChannel;
import snw.jkook.entity.channel.VoiceChannel;
import snw.jkook.event.Event;
import snw.jkook.event.channel.*;
import snw.jkook.event.guild.*;
import snw.jkook.event.pm.PrivateMessageDeleteEvent;
import snw.jkook.event.pm.PrivateMessageReceivedEvent;
import snw.jkook.event.pm.PrivateMessageUpdateEvent;
import snw.jkook.event.role.RoleCreateEvent;
import snw.jkook.event.role.RoleDeleteEvent;
import snw.jkook.event.role.RoleInfoUpdateEvent;
import snw.jkook.event.user.*;
import snw.jkook.message.PrivateMessage;
import snw.jkook.message.TextChannelMessage;
import snw.kookbc.impl.KBCClient;
import snw.kookbc.impl.entity.ReactionImpl;
import snw.kookbc.impl.entity.UserImpl;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

// A basic enum-based event factory, designed for Network message processor.
public class EventFactory {

    // the object should be provided from snw.kbc.impl.network.Frame#getData.
    public static @Nullable Event getEvent(@NotNull JsonObject object) {
        long msgTimeStamp = object.get("msg_timestamp").getAsLong();

        JsonObject extra = object.getAsJsonObject("extra");
        String type = extra.get("type").getAsString();
        Integer messageType;
        try {
            messageType = Integer.parseInt(type);
        } catch (NumberFormatException e) {
            messageType = null;
        }
        if (messageType != null) {
            if (Objects.equals(object.get("channel_type").getAsString(), "PERSON")) {
                PrivateMessage pm = KBCClient.getInstance().getMessageBuilder().buildPrivateMessage(object);
                if (pm.getSender() == KBCClient.getInstance().getBot().getUser()) {
                    return null; // prevent self-process.
                }
                KBCClient.getInstance().getStorage().addMessage(pm);
                return new PrivateMessageReceivedEvent(pm.getTimeStamp(), pm.getSender(), pm);
            } else {
                TextChannelMessage message = KBCClient.getInstance().getMessageBuilder().buildTextChannelMessage(object);
                KBCClient.getInstance().getStorage().addMessage(message);
                return new ChannelMessageEvent(message.getTimeStamp(), message.getChannel(), message);
            }
        } else {
            JsonObject body = extra.getAsJsonObject("body");
            switch (EventType.value(type)) {
                case CHANNEL_USER_ADD_REACTION:
                case PM_ADD_REACTION:
                    String messageId = body.get("msg_id").getAsString();
                    User user = KBCClient.getInstance().getStorage().getUser(
                            body.get("user_id").getAsString()
                    );
                    JsonObject rawEmoji = body.getAsJsonObject("emoji");
                    CustomEmoji emoji = KBCClient.getInstance().getStorage().getEmoji(rawEmoji.get("id").getAsString(), rawEmoji);
                    ReactionImpl reaction = new ReactionImpl(messageId, emoji, user, msgTimeStamp);
                    KBCClient.getInstance().getStorage().addReaction(reaction);
                    return new UserAddReactionEvent(
                            msgTimeStamp,
                            user,
                            body.get("msg_id").getAsString(),
                            emoji
                    );
                case PM_REMOVE_REACTION:
                case CHANNEL_USER_REMOVE_REACTION:
                    JsonObject re = body.getAsJsonObject("emoji");
                    CustomEmoji em = KBCClient.getInstance().getStorage().getEmoji(re.get("id").getAsString(), re);
                    Reaction reaction1 = KBCClient.getInstance().getStorage().getReaction(
                            body.get("msg_id").getAsString(), em,
                            KBCClient.getInstance().getStorage().getUser(body.get("user_id").getAsString()));
                    if (reaction1 != null) {
                        KBCClient.getInstance().getStorage().removeReaction(reaction1);
                    }
                    return new UserRemoveReactionEvent(
                            msgTimeStamp,
                            KBCClient.getInstance().getStorage().getUser(body.get("user_id").getAsString()),
                            body.get("msg_id").getAsString(),
                            reaction1 == null ? new ReactionImpl(
                                    body.get("msg_id").getAsString(),
                                    em,
                                    KBCClient.getInstance().getStorage().getUser(body.get("user_id").getAsString()),
                                    -1
                            ) : reaction1
                    );
                case CHANNEL_MESSAGE_UPDATE:
                    return new ChannelMessageUpdateEvent(
                            msgTimeStamp,
                            KBCClient.getInstance().getStorage().getChannel(body.get("channel_id").getAsString()),
                            body.get("msg_id").getAsString(),
                            body.get("content").getAsString()
                    );
                case CHANNEL_MESSAGE_DELETE:
                    return new ChannelMessageDeleteEvent(
                            msgTimeStamp,
                            (TextChannel) KBCClient.getInstance().getStorage().getChannel(body.get("channel_id").getAsString()), // if this error, we can regard it as internal error
                            body.get("msg_id").getAsString()
                    );
                case CHANNEL_CREATE:
                    Channel newChannel = KBCClient.getInstance().getEntityBuilder().buildChannel(body);
                    KBCClient.getInstance().getStorage().addChannel(newChannel);
                    return new ChannelCreateEvent(msgTimeStamp, newChannel);
                case CHANNEL_UPDATE:
                    Channel channel = KBCClient.getInstance().getStorage().getChannel(body.get("id").getAsString());
                    KBCClient.getInstance().getEntityUpdater().updateChannel(body, channel);
                    return new ChannelInfoUpdateEvent(msgTimeStamp, channel);
                case CHANNEL_DELETE:
                    return new ChannelDeleteEvent(msgTimeStamp, body.get("id").getAsString());
                case CHANNEL_MESSAGE_PINNED:
                    return new ChannelMessagePinEvent(
                            msgTimeStamp,
                            KBCClient.getInstance().getStorage().getChannel(body.get("channel_id").getAsString()),
                            body.get("msg_id").getAsString(),
                            KBCClient.getInstance().getStorage().getUser(body.get("operator_id").getAsString())
                    );
                case CHANNEL_MESSAGE_UNPINNED:
                    return new ChannelMessageUnpinEvent(
                            msgTimeStamp,
                            KBCClient.getInstance().getStorage().getChannel(body.get("channel_id").getAsString()),
                            body.get("msg_id").getAsString(),
                            KBCClient.getInstance().getStorage().getUser(body.get("operator_id").getAsString())
                    );
                case GUILD_USER_UPDATE:
                    return new GuildUserNickNameUpdateEvent(
                            msgTimeStamp,
                            KBCClient.getInstance().getStorage().getGuild(object.get("target_id").getAsString()),
                            KBCClient.getInstance().getStorage().getUser(body.get("user_id").getAsString()),
                            body.get("nickname").getAsString());
                case GUILD_USER_ONLINE:
                    return new UserOnlineEvent(
                            msgTimeStamp,
                            KBCClient.getInstance().getStorage().getUser(body.get("user_id").getAsString())
                    );
                case GUILD_USER_OFFLINE:
                    return new UserOfflineEvent(
                            msgTimeStamp,
                            KBCClient.getInstance().getStorage().getUser(body.get("user_id").getAsString())
                    );
                case GUILD_ADD_ROLE:
                    Role role = KBCClient.getInstance().getEntityBuilder().buildRole(
                            KBCClient.getInstance().getStorage().getGuild(body.get("target_id").getAsString()),
                            body
                    );
                    return new RoleCreateEvent(msgTimeStamp, role);
                case GUILD_REMOVE_ROLE:
                    Role deletedRole = KBCClient.getInstance().getEntityBuilder().buildRole(
                            KBCClient.getInstance().getStorage().getGuild(body.get("target_id").getAsString()),
                            body);
                    return new RoleDeleteEvent(msgTimeStamp, deletedRole);
                case GUILD_UPDATE_ROLE:
                    Guild guild = KBCClient.getInstance().getStorage().getGuild(object.get("target_id").getAsString());
                    KBCClient.getInstance().getEntityUpdater().updateRole(
                            body,
                            KBCClient.getInstance().getStorage().getRole(guild, body.get("role_id").getAsInt(), body)
                    );
                    return new RoleInfoUpdateEvent(msgTimeStamp, KBCClient.getInstance().getStorage().getRole(guild, body.get("role_id").getAsInt()));
                case GUILD_UPDATE:
                    Guild guild1 = KBCClient.getInstance().getStorage().getGuild(body.get("id").getAsString());
                    KBCClient.getInstance().getEntityUpdater().updateGuild(body, guild1);
                    return new GuildInfoUpdateEvent(msgTimeStamp, guild1);
                case GUILD_DELETE:
                    return new GuildDeleteEvent(msgTimeStamp, body.get("id").getAsString());
                case GUILD_BAN:
                    List<User> banned = new ArrayList<>();
                    body.getAsJsonArray("user_id").forEach(
                            IT -> banned.add(KBCClient.getInstance().getStorage().getUser(IT.getAsString()))
                    );
                    return new GuildBanUserEvent(msgTimeStamp, KBCClient.getInstance().getStorage().getGuild(object.get("target_id").getAsString()), banned, KBCClient.getInstance().getStorage().getUser(body.get("operator_id").getAsString()), body.get("remark").getAsString());
                case GUILD_UNBAN:
                    List<User> unbanned = new ArrayList<>();
                    body.getAsJsonArray("user_id").forEach(
                            IT -> unbanned.add(KBCClient.getInstance().getStorage().getUser(IT.getAsString()))
                    );
                    return new GuildUnbanUserEvent(msgTimeStamp, KBCClient.getInstance().getStorage().getGuild(object.get("target_id").getAsString()), unbanned, KBCClient.getInstance().getStorage().getUser(body.get("operator_id").getAsString()));
                case PM_UPDATE:
                    return new PrivateMessageUpdateEvent(msgTimeStamp, body.get("msg_id").getAsString(), body.get("content").getAsString());
                case PM_DELETE:
                    return new PrivateMessageDeleteEvent(msgTimeStamp, body.get("msg_id").getAsString());
                case USER_JOINED_GUILD:
                    return new UserJoinGuildEvent(
                            msgTimeStamp,
                            KBCClient.getInstance().getStorage().getUser(body.get("user_id").getAsString()),
                            KBCClient.getInstance().getStorage().getGuild(object.get("target_id").getAsString())
                    );
                case USER_LEFT_GUILD:
                    return new UserLeaveGuildEvent(
                            msgTimeStamp,
                            KBCClient.getInstance().getStorage().getUser(body.get("user_id").getAsString()),
                            KBCClient.getInstance().getStorage().getGuild(object.get("target_id").getAsString())
                    );
                case USER_JOINED_VOICE_CHANNEL:
                    return new UserJoinVoiceChannelEvent(
                            msgTimeStamp,
                            KBCClient.getInstance().getStorage().getUser(body.get("user_id").getAsString()),
                            (VoiceChannel) KBCClient.getInstance().getStorage().getChannel(body.get("channel_id").getAsString())
                    );
                case USER_LEFT_VOICE_CHANNEL:
                    return new UserLeaveVoiceChannelEvent(
                            msgTimeStamp,
                            KBCClient.getInstance().getStorage().getUser(body.get("user_id").getAsString()),
                            (VoiceChannel) KBCClient.getInstance().getStorage().getChannel(body.get("channel_id").getAsString())
                    );
                case USER_CLICK_BUTTON:
                    return new UserClickButtonEvent(
                            msgTimeStamp,
                            KBCClient.getInstance().getStorage().getUser(body.get("user_id").getAsString()),
                            body.get("msg_id").getAsString(),
                            body.get("value").getAsString(),
                            Objects.equals(
                                    body.get("user_id").getAsString(),
                                    body.get("target_id").getAsString()
                            ) ? null : KBCClient.getInstance().getStorage().getChannel(body.get("target_id").getAsString())
                    );
                case USER_UPDATE:
                    UserImpl updatedUser = ((UserImpl) KBCClient.getInstance().getStorage().getUser(body.get("body_id").getAsString()));
                    updatedUser.setName(body.get("username").getAsString());
                    updatedUser.setAvatarUrl(body.get("avatar").getAsString());
                    return new UserInfoUpdateEvent(msgTimeStamp, updatedUser);
                case SELF_JOINED_GUILD:
                    return new UserJoinGuildEvent(msgTimeStamp, KBCClient.getInstance().getBot().getUser(), KBCClient.getInstance().getStorage().getGuild(body.get("guild_id").getAsString()));
                case SELF_LEFT_GUILD:
                    return new UserLeaveGuildEvent(msgTimeStamp, KBCClient.getInstance().getBot().getUser(), KBCClient.getInstance().getStorage().getGuild(body.get("guild_id").getAsString()));
            }
        }
        throw new RuntimeException("Unexpected event type");
    }
}
