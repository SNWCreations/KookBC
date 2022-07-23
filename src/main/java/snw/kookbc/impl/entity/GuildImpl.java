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

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;
import snw.jkook.entity.*;
import snw.jkook.entity.channel.Category;
import snw.jkook.entity.channel.Channel;
import snw.jkook.entity.channel.TextChannel;
import snw.jkook.entity.channel.VoiceChannel;
import snw.jkook.entity.mute.MuteResult;
import snw.jkook.util.PageIterator;
import snw.kookbc.impl.KBCClient;
import snw.kookbc.impl.entity.mute.MuteDataImpl;
import snw.kookbc.impl.entity.mute.MuteResultImpl;
import snw.kookbc.impl.network.HttpAPIRoute;
import snw.kookbc.impl.pageiter.*;
import snw.kookbc.util.MapBuilder;
import snw.kookbc.util.Validate;

import java.util.Map;
import java.util.Set;

public class GuildImpl implements Guild {
    private final String id;
    private final NotifyType notifyType;
    private final User master;
    private String name;
    private boolean public_; // I know Guild owner can turn this to false,
    // but I don't have internal events to listen for that!
    private String region;
    private String avatarUrl; // no vipAvatar here!

    public GuildImpl(String id,
                     String name,
                     boolean isPublic,
                     String region,
                     User master,
                     NotifyType notifyType,
                     String avatarUrl
    ) {
        this.id = id;
        this.name = name;
        this.public_ = isPublic;
        this.region = region;
        this.master = master;
        this.notifyType = notifyType;
        this.avatarUrl = avatarUrl;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public PageIterator<Set<User>> getUsers() {
        return new GuildUserListIterator(getId());
    }

    @Override
    public PageIterator<Set<User>> getUsers(String keyword, int roleId, boolean isMobileVerified, boolean isActiveTimeFirst, boolean isJoinedTimeFirst) {
        return new GuildUserListIterator(getId(), keyword, roleId, isMobileVerified, isActiveTimeFirst, isJoinedTimeFirst);
    }

    @Override
    public PageIterator<Set<Channel>> getChannels() {
        return new GuildChannelListIterator(getId());
    }

    @Override
    public String getVoiceChannelServerRegion() {
        return region;
    }

    @Override
    public PageIterator<Set<CustomEmoji>> getCustomEmojis() {
        return new GuildEmojiListIterator(this);
    }

    @Override
    public int getOnlineUserCount() {
        JsonObject userStatus = KBCClient.getInstance().getConnector().getClient().get(String.format("%s?guild_id=%s", HttpAPIRoute.GUILD_USERS.toFullURL(), id));
        return userStatus.get("online_count").getAsInt();
    }

    @Override
    public int getUserCount() {
        JsonObject userStatus = KBCClient.getInstance().getConnector().getClient().get(String.format("%s?guild_id=%s", HttpAPIRoute.GUILD_USERS.toFullURL(), id));
        return userStatus.get("user_count").getAsInt();
    }

    @Override
    public boolean isPublic() {
        return public_;
    }

    public void setPublic(boolean value) {
        this.public_ = value;
    }

    @Override
    public MuteResult getMuteStatus() {
        String url = String.format("%s?guild_id=%s", HttpAPIRoute.MUTE_LIST, getId());
        JsonObject object = KBCClient.getInstance().getConnector().getClient().get(url);

        MuteResultImpl result = new MuteResultImpl();
        for (JsonElement element : object.getAsJsonObject("mic").getAsJsonObject("user_ids").getAsJsonArray()) {
            String id = element.getAsString();
            MuteDataImpl data = new MuteDataImpl(id);
            data.setInputDisabled(true);
            result.add(data);
        }
        for (JsonElement element : object.getAsJsonObject("headset").getAsJsonObject("user_ids").getAsJsonArray()) {
            String id = element.getAsString();
            MuteDataImpl resDef = (MuteDataImpl) result.getByUser(id);
            if (resDef == null) {
                resDef = new MuteDataImpl(id);
                result.add(resDef);
            }
            resDef.setOutputDisabled(true);
        }
        return result;
    }

    @Override
    public void leave() {
        Map<String, Object> body = new MapBuilder()
                .put("guild_id", getId())
                .build();
        KBCClient.getInstance().getConnector().getClient().post(HttpAPIRoute.GUILD_LEAVE.toFullURL(), body);
    }

    @Override
    public void ban(User user, @Nullable String s, int i) {
        Map<String, Object> body = new MapBuilder()
                .put("guild_id", getId())
                .put("target_id", user.getId())
                .put("del_msg_days", i)
                .build();
        KBCClient.getInstance().getConnector().getClient().post(HttpAPIRoute.BLACKLIST_CREATE.toFullURL(), body);
    }

    @Override
    public void unban(User user) {
        Map<String, Object> body = new MapBuilder()
                .put("guild_id", getId())
                .put("target_id", user.getId())
                .build();
        KBCClient.getInstance().getConnector().getClient().post(HttpAPIRoute.BLACKLIST_DELETE.toFullURL(), body);
    }

    @Override
    public void kick(User user) {
        Map<String, Object> body = new MapBuilder()
                .put("guild_id", getId())
                .put("target_id", user.getId())
                .build();
        KBCClient.getInstance().getConnector().getClient().post(HttpAPIRoute.GUILD_KICK.toFullURL(), body);
    }

    @Override
    public TextChannel createTextChannel(String s, @Nullable Category category) {
        MapBuilder builder = new MapBuilder()
                .put("guild_id", getId())
                .put("name", s)
                .put("type", 1);
        if (category != null) {
            builder.put("parent_id", category.getId());
        }
        Map<String, Object> body = builder.build();
        TextChannel channel = (TextChannel) KBCClient.getInstance().getEntityBuilder().buildChannel(KBCClient.getInstance().getConnector().getClient().post(HttpAPIRoute.GUILD_KICK.toFullURL(), body));
        KBCClient.getInstance().getStorage().addChannel(channel);
        return channel;
    }

    @Override
    public VoiceChannel createVoiceChannel(String s, @Nullable Category category, @Range(from = 1L, to = 99L) int i, @Range(from = 1L, to = 3L) int i1) {
        MapBuilder builder = new MapBuilder()
                .put("guild_id", getId())
                .put("name", s)
                .put("type", 2)
                .put("limit_amount", i)
                .put("voice_quality", i1);
        if (category != null) {
            builder.put("parent_id", category.getId());
        }
        Map<String, Object> body = builder.build();
        VoiceChannel channel = (VoiceChannel) KBCClient.getInstance().getEntityBuilder().buildChannel(KBCClient.getInstance().getConnector().getClient().post(HttpAPIRoute.GUILD_KICK.toFullURL(), body));
        KBCClient.getInstance().getStorage().addChannel(channel);
        return channel;
    }

    @Override
    public Category createCategory(String s) {
        MapBuilder builder = new MapBuilder()
                .put("guild_id", getId())
                .put("name", s)
                .put("is_category", 1);
        Map<String, Object> body = builder.build();
        Category result = (Category) KBCClient.getInstance().getEntityBuilder().buildChannel(KBCClient.getInstance().getConnector().getClient().post(HttpAPIRoute.GUILD_KICK.toFullURL(), body));
        KBCClient.getInstance().getStorage().addChannel(result);
        return result;
    }

    @Override
    public Role createRole(String s) {
        Map<String, Object> body = new MapBuilder()
                .put("guild_id", getId())
                .put("name", s)
                .build();
        JsonObject res = KBCClient.getInstance().getConnector().getClient().post(HttpAPIRoute.ROLE_CREATE.toFullURL(), body);
        Role result = KBCClient.getInstance().getEntityBuilder().buildRole(this, res);
        KBCClient.getInstance().getStorage().addRole(this, result);
        return result;
    }

    @Override
    public CustomEmoji uploadEmoji(String s, @Nullable String s1) {
        MapBuilder builder = new MapBuilder()
                .put("guild_id", getId())
                .put("emoji", s);
        if (s1 != null) {
            builder.put("name", s1);
        }
        JsonObject object = KBCClient.getInstance().getConnector().getClient().post(HttpAPIRoute.GUILD_EMOJI_CREATE.toFullURL(), builder.build());
        CustomEmoji emoji = KBCClient.getInstance().getEntityBuilder().buildEmoji(object);
        KBCClient.getInstance().getStorage().addEmoji(emoji);
        return emoji;
    }

    @Override
    public PageIterator<Set<User>> getBannedUsers() {
        return new GuildBannedUserIterator(this);
    }

    @Override
    public NotifyType getNotifyType() {
        return notifyType;
    }

    @Override
    public @Nullable String getAvatarUrl(boolean b) {
        Validate.isTrue(!b, "KOOK official does not provide \"vip_avatar\" field for Guild.");
        return avatarUrl;
    }

    @Override
    public PageIterator<Set<Invitation>> getInvitations() {
        return new GuildInvitationsIterator(this);
    }

    @Override
    public String createInvite(int validSeconds, int validTimes) {
        Map<String, Object> body = new MapBuilder()
                .put("guild_id", getId())
                .put("duration", validSeconds)
                .put("setting_times", validTimes)
                .build();
        JsonObject object = KBCClient.getInstance().getConnector().getClient().post(HttpAPIRoute.INVITE_CREATE.toFullURL(), body);
        return object.get("url").getAsString();
    }

    @Override
    public User getMaster() {
        return master;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public void setAvatar(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }
}
