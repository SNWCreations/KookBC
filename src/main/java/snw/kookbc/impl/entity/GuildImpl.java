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

package snw.kookbc.impl.entity;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;
import snw.jkook.entity.*;
import snw.jkook.entity.channel.Category;
import snw.jkook.entity.channel.Channel;
import snw.jkook.entity.channel.TextChannel;
import snw.jkook.entity.channel.VoiceChannel;
import snw.jkook.entity.mute.MuteResult;
import snw.jkook.util.PageIterator;
import snw.jkook.util.Validate;
import snw.kookbc.impl.KBCClient;
import snw.kookbc.impl.entity.mute.MuteDataImpl;
import snw.kookbc.impl.entity.mute.MuteResultImpl;
import snw.kookbc.impl.network.HttpAPIRoute;
import snw.kookbc.impl.pageiter.*;
import snw.kookbc.util.MapBuilder;

import java.nio.charset.StandardCharsets;
import java.util.*;

import static snw.kookbc.util.GsonUtil.*;

public class GuildImpl implements Guild {
    private final KBCClient client;
    private final String id;
    private final NotifyType notifyType;
    private final User master;
    private String name;
    private boolean public_; // I know Guild owner can turn this to false,
    // but I don't have internal events to listen for that!
    private String region;
    private String avatarUrl; // no vipAvatar here!

    public GuildImpl(KBCClient client, String id,
                     String name,
                     boolean isPublic,
                     String region,
                     User master,
                     NotifyType notifyType,
                     String avatarUrl
    ) {
        this.client = client;
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
        return new GuildUserListIterator(client, getId());
    }

    @Override
    public PageIterator<Set<User>> getUsers(String keyword, @Nullable Integer roleId, @Nullable Boolean isMobileVerified, @Nullable Boolean isActiveTimeFirst, @Nullable Boolean isJoinedTimeFirst) {
        return new GuildUserListIterator(client, getId(), keyword, roleId, isMobileVerified, isActiveTimeFirst, isJoinedTimeFirst);
    }

    @Override
    public PageIterator<Set<Channel>> getChannels() {
        return new GuildChannelListIterator(client, getId());
    }

    @Override
    public PageIterator<Set<Role>> getRoles() {
        return new GuildRoleListIterator(client, this);
    }

    @Override
    public String getVoiceChannelServerRegion() {
        return region;
    }

    @Override
    public PageIterator<Set<CustomEmoji>> getCustomEmojis() {
        return new GuildEmojiListIterator(client, this);
    }

    @Override
    public int getOnlineUserCount() {
        JsonObject userStatus = client.getNetworkClient().get(String.format("%s?guild_id=%s", HttpAPIRoute.GUILD_USERS.toFullURL(), id));
        return userStatus.get("online_count").getAsInt();
    }

    @Override
    public int getUserCount() {
        JsonObject userStatus = client.getNetworkClient().get(String.format("%s?guild_id=%s", HttpAPIRoute.GUILD_USERS.toFullURL(), id));
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
        JsonObject object = client.getNetworkClient().get(url);

        MuteResultImpl result = new MuteResultImpl();
        for (JsonElement element : object.getAsJsonObject("mic").getAsJsonArray("user_ids")) {
            String id = element.getAsString();
            MuteDataImpl data = new MuteDataImpl(client.getStorage().getUser(id));
            data.setInputDisabled(true);
            result.add(data);
        }
        for (JsonElement element : object.getAsJsonObject("headset").getAsJsonArray("user_ids")) {
            String id = element.getAsString();
            MuteDataImpl resDef = (MuteDataImpl) result.getByUser(id);
            if (resDef == null) {
                resDef = new MuteDataImpl(client.getStorage().getUser(id));
                result.add(resDef);
            }
            resDef.setOutputDisabled(true);
        }
        return result;
    }

    @Override
    public void leave() {
        client.getNetworkClient().postContent(HttpAPIRoute.GUILD_LEAVE.toFullURL(), Collections.singletonMap("guild_id", getId()));
    }

    @Override
    public void ban(User user, @Nullable String s, int i) {
        MapBuilder builder = new MapBuilder()
                .put("guild_id", getId())
                .put("target_id", user.getId())
                .put("del_msg_days", i);
        if (s != null) {
            builder.put("remark", s);
        }
        Map<String, Object> body = builder.build();
        client.getNetworkClient().post(HttpAPIRoute.BLACKLIST_CREATE.toFullURL(), body);
    }

    @Override
    public void unban(User user) {
        Map<String, Object> body = new MapBuilder()
                .put("guild_id", getId())
                .put("target_id", user.getId())
                .build();
        client.getNetworkClient().post(HttpAPIRoute.BLACKLIST_DELETE.toFullURL(), body);
    }

    @Override
    public void kick(User user) {
        Map<String, Object> body = new MapBuilder()
                .put("guild_id", getId())
                .put("target_id", user.getId())
                .build();
        client.getNetworkClient().post(HttpAPIRoute.GUILD_KICK.toFullURL(), body);
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
        TextChannel channel = (TextChannel) client.getEntityBuilder().buildChannel(client.getNetworkClient().post(HttpAPIRoute.CHANNEL_CREATE.toFullURL(), body));
        client.getStorage().addChannel(channel);
        return channel;
    }

    @Override
    public VoiceChannel createVoiceChannel(String s, @Nullable Category category, @Range(from = 1L, to = 99L) int i, @Range(from = 1L, to = 3L) int i1) {
        MapBuilder builder = new MapBuilder()
                .put("guild_id", getId())
                .put("name", s)
                .put("type", 2)
                .put("limit_amount", i)
                .put("voice_quality", String.valueOf(i1));
        if (category != null) {
            builder.put("parent_id", category.getId());
        }
        Map<String, Object> body = builder.build();
        VoiceChannel channel = (VoiceChannel) client.getEntityBuilder().buildChannel(client.getNetworkClient().post(HttpAPIRoute.CHANNEL_CREATE.toFullURL(), body));
        client.getStorage().addChannel(channel);
        return channel;
    }

    @Override
    public Category createCategory(String s) {
        MapBuilder builder = new MapBuilder()
                .put("guild_id", getId())
                .put("name", s)
                .put("is_category", 1);
        Map<String, Object> body = builder.build();
        Category result = (Category) client.getEntityBuilder().buildChannel(client.getNetworkClient().post(HttpAPIRoute.GUILD_KICK.toFullURL(), body));
        client.getStorage().addChannel(result);
        return result;
    }

    @Override
    public Role createRole(String s) {
        Map<String, Object> body = new MapBuilder()
                .put("guild_id", getId())
                .put("name", s)
                .build();
        JsonObject res = client.getNetworkClient().post(HttpAPIRoute.ROLE_CREATE.toFullURL(), body);
        Role result = client.getEntityBuilder().buildRole(this, res);
        client.getStorage().addRole(this, result);
        return result;
    }

    @Override
    public CustomEmoji uploadEmoji(String s, @Nullable String name) {
        return uploadEmoji(s.getBytes(StandardCharsets.ISO_8859_1), "image/png", name);
    }

    @Override
    public CustomEmoji uploadEmoji(byte[] content, String type, @Nullable String name) {
        MultipartBody.Builder requestBodyBuilder = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("guild_id", getId())
                .addFormDataPart(
                        "emoji",
                        "114514",
                        RequestBody.create(content, MediaType.parse(type))
                );
        if (name != null) {
            if (name.length() < 2) {
                throw new IllegalArgumentException("The emoji name should be greater or equals 2.");
            }
            requestBodyBuilder.addFormDataPart("name", name);
        }
        MultipartBody requestBody = requestBodyBuilder.build();
        Request request = new Request.Builder()
                .url(HttpAPIRoute.GUILD_EMOJI_CREATE.toFullURL())
                .post(requestBody)
                .addHeader("Authorization", client.getNetworkClient().getTokenWithPrefix())
                .build();
        JsonObject object = JsonParser.parseString(client.getNetworkClient().call(request)).getAsJsonObject().getAsJsonObject("data");
        CustomEmoji emoji = client.getEntityBuilder().buildEmoji(object);
        client.getStorage().addEmoji(emoji);
        return emoji;
    }

    @Override
    public PageIterator<Set<User>> getBannedUsers() {
        return new GuildBannedUserIterator(client, this);
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
    public Collection<BoostInfo> getBoostInfo(int start, int end) throws IllegalArgumentException {
        Validate.isTrue(start >= 0, "The paramater 'start' cannot be negative");
        Validate.isTrue(end > 0, "The parameter 'end' cannot be negative");
        Validate.isTrue(start < end, "The parameter 'start' cannot be greater than the parameter 'end'");
        JsonObject object = client.getNetworkClient().get(
                String.format("%s?guild_id=%s&start_time=%s&end_time=%s", HttpAPIRoute.GUILD_BOOST_HISTORY.toFullURL(), getId(), start, end)
        );
        Collection<BoostInfo> result = new HashSet<>();
        for (JsonElement item : object.getAsJsonArray("items")) {
            JsonObject data = item.getAsJsonObject();
            result.add(
                    new BoostInfoImpl(
                            client.getStorage().getUser(data.get("user_id").getAsString()),
                            data.get("start_time").getAsInt(),
                            data.get("end_time").getAsInt()
                    )
            );
        }
        return Collections.unmodifiableCollection(result);
    }

    @Override
    public PageIterator<Set<Invitation>> getInvitations() {
        return new GuildInvitationsIterator(client, this);
    }

    @Override
    public String createInvite(int validSeconds, int validTimes) {
        Map<String, Object> body = new MapBuilder()
                .put("guild_id", getId())
                .put("duration", validSeconds)
                .put("setting_times", validTimes)
                .build();
        JsonObject object = client.getNetworkClient().post(HttpAPIRoute.INVITE_CREATE.toFullURL(), body);
        return get(object, "url").getAsString();
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

// Just a JavaBean that contains the boost information.
// See Guild#getBoostInfo.
final class BoostInfoImpl implements Guild.BoostInfo {
    private final User booster;
    private final int startTime;
    private final int endTime;

    BoostInfoImpl(User booster, int startTime, int endTime) {
        this.booster = booster;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    @Override
    public User getBooster() {
        return booster;
    }

    @Override
    public int getStartTime() {
        return startTime;
    }

    @Override
    public int getEndTime() {
        return endTime;
    }
}