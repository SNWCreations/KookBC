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

package snw.kookbc.impl.event;

import com.google.gson.JsonObject;
import org.jetbrains.annotations.NotNull;
import snw.jkook.entity.*;
import snw.jkook.entity.channel.Channel;
import snw.jkook.entity.channel.TextChannel;
import snw.jkook.entity.channel.VoiceChannel;
import snw.jkook.event.Event;
import snw.jkook.event.channel.*;
import snw.jkook.event.guild.*;
import snw.jkook.event.item.ItemConsumedEvent;
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
import snw.kookbc.impl.network.Frame;
import snw.kookbc.impl.network.exceptions.BadResponseException;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static snw.kookbc.util.GsonUtil.get;

// A basic enum-based event factory, designed for Network message processor.
public class EventFactory {

    // the object should be provided from snw.kbc.impl.network.Frame#getData.
    public static Event getEvent(@NotNull KBCClient client, @NotNull Frame frame) {
        JsonObject object = frame.getData();
        long msgTimeStamp = get(object, "msg_timestamp").getAsLong();

        JsonObject extra = object.getAsJsonObject("extra");
        String type = extra.get("type").getAsString();
        Integer messageType;
        try {
            messageType = Integer.parseInt(type);
        } catch (NumberFormatException e) {
            messageType = null;
        }
        if (messageType != null) {
            if (messageType == 12) {
                JsonObject content = object.getAsJsonObject("content");
                User consumer = client.getStorage().getUser(content.get("user_id").getAsString());
                User affected = client.getStorage().getUser(content.get("target_id").getAsString());
                int itemId = get(object, "item_id").getAsInt();
                return new ItemConsumedEvent(msgTimeStamp, consumer, affected, itemId);
            }
            if (Objects.equals(get(object, "channel_type").getAsString(), "PERSON")) {
                PrivateMessage pm = client.getMessageBuilder().buildPrivateMessage(object);
                client.getStorage().addMessage(pm);
                return new PrivateMessageReceivedEvent(pm.getTimeStamp(), pm.getSender(), pm);
            } else {
                TextChannelMessage message = client.getMessageBuilder().buildTextChannelMessage(object);
                client.getStorage().addMessage(message);
                return new ChannelMessageEvent(message.getTimeStamp(), message.getChannel(), message);
            }
        } else {
            JsonObject body = extra.getAsJsonObject("body");
            switch (EventType.value(type)) {
                case CHANNEL_USER_ADD_REACTION:
                case PM_ADD_REACTION:
                    String messageId = body.get("msg_id").getAsString();
                    User user = client.getStorage().getUser(
                            body.get("user_id").getAsString()
                    );
                    JsonObject rawEmoji = body.getAsJsonObject("emoji");
                    CustomEmoji emoji = client.getStorage().getEmoji(rawEmoji.get("id").getAsString(), rawEmoji);
                    ReactionImpl reaction = new ReactionImpl(client, messageId, emoji, user, msgTimeStamp);
                    client.getStorage().addReaction(reaction);
                    return new UserAddReactionEvent(
                            msgTimeStamp,
                            user,
                            body.get("msg_id").getAsString(),
                            reaction
                    );
                case PM_REMOVE_REACTION:
                case CHANNEL_USER_REMOVE_REACTION:
                    JsonObject re = body.getAsJsonObject("emoji");
                    CustomEmoji em = client.getStorage().getEmoji(re.get("id").getAsString(), re);
                    Reaction reaction1 = client.getStorage().getReaction(
                            body.get("msg_id").getAsString(), em,
                            client.getStorage().getUser(body.get("user_id").getAsString()));
                    if (reaction1 != null) {
                        client.getStorage().removeReaction(reaction1);
                    }
                    return new UserRemoveReactionEvent(
                            msgTimeStamp,
                            client.getStorage().getUser(body.get("user_id").getAsString()),
                            body.get("msg_id").getAsString(),
                            reaction1 == null ? new ReactionImpl(
                                    client, body.get("msg_id").getAsString(),
                                    em,
                                    client.getStorage().getUser(body.get("user_id").getAsString()),
                                    -1
                            ) : reaction1
                    );
                case CHANNEL_MESSAGE_UPDATE:
                    return new ChannelMessageUpdateEvent(
                            msgTimeStamp,
                            client.getStorage().getChannel(body.get("channel_id").getAsString()),
                            body.get("msg_id").getAsString(),
                            body.get("content").getAsString()
                    );
                case CHANNEL_MESSAGE_DELETE:
                    client.getStorage().removeMessage(body.get("msg_id").getAsString());
                    return new ChannelMessageDeleteEvent(
                            msgTimeStamp,
                            (TextChannel) client.getStorage().getChannel(body.get("channel_id").getAsString()), // if this error, we can regard it as internal error
                            body.get("msg_id").getAsString()
                    );
                case CHANNEL_CREATE:
                    Channel newChannel = client.getEntityBuilder().buildChannel(body);
                    client.getStorage().addChannel(newChannel);
                    return new ChannelCreateEvent(msgTimeStamp, newChannel);
                case CHANNEL_UPDATE:
                    Channel channel;
                    try {
                        channel = client.getStorage().getChannel(body.get("id").getAsString());
                    } catch (BadResponseException e) {
                        client.getCore().getLogger().warn("Detected snw.jkook.event.channel.ChannelInfoUpdateEvent, but we are unable to fetch channel (id {}).", body.get("id").getAsString());
                        return null;
                    }
                    client.getEntityUpdater().updateChannel(body, channel);
                    return new ChannelInfoUpdateEvent(msgTimeStamp, channel);
                case CHANNEL_DELETE:
                    client.getStorage().removeChannel(body.get("id").getAsString());
                    return new ChannelDeleteEvent(msgTimeStamp, body.get("id").getAsString(), client.getStorage().getGuild(get(object, "target_id").getAsString()));
                case CHANNEL_MESSAGE_PINNED:
                    return new ChannelMessagePinEvent(
                            msgTimeStamp,
                            client.getStorage().getChannel(body.get("channel_id").getAsString()),
                            body.get("msg_id").getAsString(),
                            client.getStorage().getUser(body.get("operator_id").getAsString())
                    );
                case CHANNEL_MESSAGE_UNPINNED:
                    return new ChannelMessageUnpinEvent(
                            msgTimeStamp,
                            client.getStorage().getChannel(body.get("channel_id").getAsString()),
                            body.get("msg_id").getAsString(),
                            client.getStorage().getUser(body.get("operator_id").getAsString())
                    );
                case GUILD_USER_UPDATE:
                    return new GuildUserNickNameUpdateEvent(
                            msgTimeStamp,
                            client.getStorage().getGuild(get(object, "target_id").getAsString()),
                            client.getStorage().getUser(body.get("user_id").getAsString()),
                            body.get("nickname").getAsString());
                case GUILD_USER_ONLINE:
                    return new UserOnlineEvent(
                            msgTimeStamp,
                            client.getStorage().getUser(body.get("user_id").getAsString())
                    );
                case GUILD_USER_OFFLINE:
                    return new UserOfflineEvent(
                            msgTimeStamp,
                            client.getStorage().getUser(body.get("user_id").getAsString())
                    );
                case GUILD_ADD_ROLE:
                    Role role = client.getEntityBuilder().buildRole(
                            client.getStorage().getGuild(get(object, "target_id").getAsString()),
                            body
                    );
                    return new RoleCreateEvent(msgTimeStamp, role);
                case GUILD_REMOVE_ROLE:
                    Role deletedRole = client.getEntityBuilder().buildRole(
                            client.getStorage().getGuild(get(object, "target_id").getAsString()),
                            body);
                    return new RoleDeleteEvent(msgTimeStamp, deletedRole);
                case GUILD_UPDATE_ROLE:
                    Guild guild = client.getStorage().getGuild(get(object, "target_id").getAsString());
                    client.getEntityUpdater().updateRole(
                            body,
                            client.getStorage().getRole(guild, body.get("role_id").getAsInt(), body)
                    );
                    return new RoleInfoUpdateEvent(msgTimeStamp, client.getStorage().getRole(guild, body.get("role_id").getAsInt()));
                case GUILD_UPDATE:
                    Guild guild1 = client.getStorage().getGuild(body.get("id").getAsString());
                    client.getEntityUpdater().updateGuild(body, guild1);
                    return new GuildInfoUpdateEvent(msgTimeStamp, guild1);
                case GUILD_DELETE:
                    client.getStorage().removeGuild(body.get("id").getAsString());
                    return new GuildDeleteEvent(msgTimeStamp, body.get("id").getAsString());
                case GUILD_BAN:
                    List<User> banned = new ArrayList<>();
                    body.getAsJsonArray("user_id").forEach(
                            IT -> banned.add(client.getStorage().getUser(IT.getAsString()))
                    );
                    return new GuildBanUserEvent(msgTimeStamp, client.getStorage().getGuild(get(object, "target_id").getAsString()), banned, client.getStorage().getUser(body.get("operator_id").getAsString()), body.get("remark").getAsString());
                case GUILD_UNBAN:
                    List<User> unbanned = new ArrayList<>();
                    body.getAsJsonArray("user_id").forEach(
                            IT -> unbanned.add(client.getStorage().getUser(IT.getAsString()))
                    );
                    return new GuildUnbanUserEvent(msgTimeStamp, client.getStorage().getGuild(get(object, "target_id").getAsString()), unbanned, client.getStorage().getUser(body.get("operator_id").getAsString()));
                case ADD_EMOJI:
                    CustomEmoji emoji1 = client.getEntityBuilder().buildEmoji(body);
                    return new GuildAddEmojiEvent(msgTimeStamp, emoji1.getGuild(), emoji1);
                case DELETE_EMOJI:
                    CustomEmoji emoji2 = client.getStorage().getEmoji(body.get("id").getAsString(), body);
                    return new GuildRemoveEmojiEvent(msgTimeStamp, emoji2.getGuild(), emoji2);
                case UPDATE_EMOJI:
                    CustomEmoji emoji3 = client.getStorage().getEmoji(body.get("id").getAsString(), body);
                    client.getEntityUpdater().updateEmoji(body, emoji3);
                    return new GuildUpdateEmojiEvent(msgTimeStamp, emoji3.getGuild(), emoji3);
                case PM_UPDATE:
                    return new PrivateMessageUpdateEvent(msgTimeStamp, body.get("msg_id").getAsString(), body.get("content").getAsString());
                case PM_DELETE:
                    client.getStorage().removeMessage(body.get("msg_id").getAsString());
                    return new PrivateMessageDeleteEvent(msgTimeStamp, body.get("msg_id").getAsString());
                case USER_JOINED_GUILD:
                    return new UserJoinGuildEvent(
                            msgTimeStamp,
                            client.getStorage().getUser(body.get("user_id").getAsString()),
                            client.getStorage().getGuild(get(object, "target_id").getAsString())
                    );
                case USER_LEFT_GUILD:
                    return new UserLeaveGuildEvent(
                            msgTimeStamp,
                            client.getStorage().getUser(body.get("user_id").getAsString()),
                            client.getStorage().getGuild(get(object, "target_id").getAsString())
                    );
                case USER_JOINED_VOICE_CHANNEL:
                    return new UserJoinVoiceChannelEvent(
                            msgTimeStamp,
                            client.getStorage().getUser(body.get("user_id").getAsString()),
                            (VoiceChannel) client.getStorage().getChannel(body.get("channel_id").getAsString())
                    );
                case USER_LEFT_VOICE_CHANNEL:
                    return new UserLeaveVoiceChannelEvent(
                            msgTimeStamp,
                            client.getStorage().getUser(body.get("user_id").getAsString()),
                            (VoiceChannel) client.getStorage().getChannel(body.get("channel_id").getAsString())
                    );
                case USER_CLICK_BUTTON:
                    return new UserClickButtonEvent(
                            msgTimeStamp,
                            client.getStorage().getUser(body.get("user_id").getAsString()),
                            body.get("msg_id").getAsString(),
                            body.get("value").getAsString(),
                            Objects.equals(
                                    body.get("user_id").getAsString(),
                                    body.get("target_id").getAsString()
                            ) ? null : (TextChannel) client.getStorage().getChannel(body.get("target_id").getAsString())
                    );
                case USER_UPDATE:
                    UserImpl updatedUser = ((UserImpl) client.getStorage().getUser(body.get("body_id").getAsString()));
                    updatedUser.setName(body.get("username").getAsString());
                    updatedUser.setAvatarUrl(body.get("avatar").getAsString());
                    return new UserInfoUpdateEvent(msgTimeStamp, updatedUser);
                case SELF_JOINED_GUILD:
                    return new UserJoinGuildEvent(msgTimeStamp, client.getCore().getUser(), client.getStorage().getGuild(body.get("guild_id").getAsString()));
                case SELF_LEFT_GUILD:
                    String gId = body.get("guild_id").getAsString();
                    Guild g = client.getStorage().getGuild(gId);
                    if (g != null) { // cache hit
                        return new UserLeaveGuildEvent(msgTimeStamp, client.getCore().getUser(), g);
                    } else {
                        return new UserLeaveGuildEvent(msgTimeStamp, client.getCore().getUser(), gId);
                    }
            }
        }
        client.getCore().getLogger().error("We cannot understand the frame.");
        client.getCore().getLogger().error("Frame content: {}", frame);
        return null; // don't worry, the caller will handle null.
    }
}
