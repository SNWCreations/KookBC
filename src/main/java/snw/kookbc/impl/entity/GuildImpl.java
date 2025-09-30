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

import com.fasterxml.jackson.databind.JsonNode;
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
import snw.kookbc.interfaces.LazyLoadable;
import snw.kookbc.interfaces.Updatable;
import snw.kookbc.util.MapBuilder;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;
import static snw.jkook.util.Validate.isTrue;
import static snw.kookbc.util.JacksonUtil.*;

public class GuildImpl implements Guild, Updatable, LazyLoadable {
    private final KBCClient client;
    private final String id;
    private NotifyType notifyType;
    private User master;
    private String name;
    private boolean public_; // I know Guild owner can turn this to false, but I don't have internal events
    // to listen for that!
    private String region;
    private String avatarUrl; // no vipAvatar here!
    private boolean completed;

    public GuildImpl(KBCClient client, String id) {
        this.client = requireNonNull(client);
        this.id = requireNonNull(id);
    }

    public GuildImpl(KBCClient client, String id, NotifyType notifyType, User master, String name, boolean public_,
                     String region, String avatarUrl) {
        this(client, id);
        this.notifyType = notifyType;
        this.master = master;
        this.name = name;
        this.public_ = public_;
        this.region = region;
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
    public PageIterator<Set<User>> getUsers(String keyword, @Nullable Integer roleId,
                                            @Nullable Boolean isMobileVerified, @Nullable Boolean isActiveTimeFirst,
                                            @Nullable Boolean isJoinedTimeFirst) {
        return new GuildUserListIterator(client, getId(), keyword, roleId, isMobileVerified, isActiveTimeFirst,
                isJoinedTimeFirst);
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
        initIfNeeded();
        return region;
    }

    @Override
    public PageIterator<Set<CustomEmoji>> getCustomEmojis() {
        return new GuildEmojiListIterator(client, this);
    }

    @Override
    public int getOnlineUserCount() {
        JsonNode userStatus = client.getNetworkClient()
                .get(String.format("%s?guild_id=%s", HttpAPIRoute.GUILD_USERS.toFullURL(), id));
        return userStatus.get("online_count").asInt();
    }

    @Override
    public int getUserCount() {
        JsonNode userStatus = client.getNetworkClient()
                .get(String.format("%s?guild_id=%s", HttpAPIRoute.GUILD_USERS.toFullURL(), id));
        return userStatus.get("user_count").asInt();
    }

    @Override
    public boolean isPublic() {
        initIfNeeded();
        return public_;
    }

    public void setPublic(boolean value) {
        this.public_ = value;
    }

    @Override
    public MuteResult getMuteStatus() {
        String url = String.format("%s?guild_id=%s", HttpAPIRoute.MUTE_LIST, getId());
        JsonNode object = client.getNetworkClient().get(url);

        MuteResultImpl result = new MuteResultImpl();
        for (JsonNode element : object.get("mic").get("user_ids")) {
            String id = element.asText();
            MuteDataImpl data = new MuteDataImpl(client.getStorage().getUser(id));
            data.setInputDisabled(true);
            result.add(data);
        }
        for (JsonNode element : object.get("headset").get("user_ids")) {
            String id = element.asText();
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
        client.getNetworkClient().postContent(HttpAPIRoute.GUILD_LEAVE.toFullURL(),
                Collections.singletonMap("guild_id", getId()));
    }

    @Override
    public void ban(User user, @Nullable String s, int i) {
        Map<String, Object> body = new MapBuilder()
                .put("guild_id", getId())
                .put("target_id", user.getId())
                .put("del_msg_days", i)
                .putIfNotNull("remarks", s)
                .build();
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
        Map<String, Object> body = new MapBuilder()
                .put("guild_id", getId())
                .put("name", s)
                .put("type", 1)
                .putIfNotNull("parent_id", category, Channel::getId)
                .build();
        TextChannel channel = (TextChannel) client.getEntityBuilder()
                .buildChannel(client.getNetworkClient().post(HttpAPIRoute.CHANNEL_CREATE.toFullURL(), body));
        client.getStorage().addChannel(channel);
        return channel;
    }

    @Override
    public VoiceChannel createVoiceChannel(String s, @Nullable Category parent, @Range(from = 1L, to = 99L) int size,
                                           @Range(from = 1L, to = 3L) int quality) {
        Map<String, Object> body = new MapBuilder()
                .put("guild_id", getId())
                .put("name", s)
                .put("type", 2)
                .put("limit_amount", size)
                .put("voice_quality", String.valueOf(quality))
                .putIfNotNull("parent_id", parent, Channel::getId)
                .build();
        VoiceChannel channel = (VoiceChannel) client.getEntityBuilder()
                .buildChannel(client.getNetworkClient().post(HttpAPIRoute.CHANNEL_CREATE.toFullURL(), body));
        client.getStorage().addChannel(channel);
        return channel;
    }

    @Override
    public Category createCategory(String s) {
        Map<String, Object> body = new MapBuilder()
                .put("guild_id", getId())
                .put("name", s)
                .put("is_category", 1)
                .build();
        Category result = (Category) client.getEntityBuilder()
                .buildChannel(client.getNetworkClient().post(HttpAPIRoute.GUILD_KICK.toFullURL(), body));
        client.getStorage().addChannel(result);
        return result;
    }

    @Override
    public Role createRole(String s) {
        Map<String, Object> body = new MapBuilder()
                .put("guild_id", getId())
                .put("name", s)
                .build();
        JsonNode res = client.getNetworkClient().post(HttpAPIRoute.ROLE_CREATE.toFullURL(), body);
        Role result = client.getEntityBuilder().buildRole(this, snw.kookbc.util.JacksonUtil.convertToGsonJsonObject(res));
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
                        RequestBody.create(content, MediaType.parse(type)));
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
        JsonNode object = snw.kookbc.util.JacksonUtil.parse(client.getNetworkClient().call(request)).get("data");
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
        initIfNeeded();
        return notifyType;
    }

    @Override
    public @Nullable String getAvatarUrl(boolean b) {
        Validate.isTrue(!b, "KOOK official does not provide \"vip_avatar\" field for Guild.");
        initIfNeeded();
        return avatarUrl;
    }

    @Override
    public Collection<BoostInfo> getBoostInfo(int start, int end) throws IllegalArgumentException {
        Validate.isTrue(start >= 0, "The paramater 'start' cannot be negative");
        Validate.isTrue(end > 0, "The parameter 'end' cannot be negative");
        Validate.isTrue(start < end, "The parameter 'start' cannot be greater than the parameter 'end'");
        JsonNode object = client.getNetworkClient().get(
                String.format("%s?guild_id=%s&start_time=%s&end_time=%s", HttpAPIRoute.GUILD_BOOST_HISTORY.toFullURL(),
                        getId(), start, end));
        Collection<BoostInfo> result = new HashSet<>();
        for (JsonNode item : object.get("items")) {
            result.add(
                    new BoostInfoImpl(
                            client.getStorage().getUser(item.get("user_id").asText()),
                            item.get("start_time").asInt(),
                            item.get("end_time").asInt()));
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
        JsonNode object = client.getNetworkClient().post(HttpAPIRoute.INVITE_CREATE.toFullURL(), body);
        return snw.kookbc.util.JacksonUtil.get(object, "url").asText();
    }

    @Override
    public User getMaster() {
        initIfNeeded();
        return master;
    }

    @Override
    public String getName() {
        initIfNeeded();
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

    // GSON compatibility method - marked for deprecation
    public synchronized void updateFromGson(com.google.gson.JsonObject data) {
        update(snw.kookbc.util.JacksonUtil.parse(data.toString()));
    }

    @Override
    public synchronized void update(JsonNode data) {
        final String id = data.get("id").asText();
        final int notifyTypeId = data.get("notify_type").asInt();
        final Supplier<String> notifyErr = () -> "Unexpected NotifyType, got " + notifyTypeId;
        isTrue(Objects.equals(getId(), id), "You can't update guild by using different data");
        this.name = data.get("name").asText();
        this.public_ = data.get("enable_open").asBoolean();
        this.region = data.get("region").asText();
        this.notifyType = requireNonNull(NotifyType.value(notifyTypeId), notifyErr);
        this.avatarUrl = data.get("icon").asText();
        this.master = new UserImpl(client, data.get("user_id").asText());
    }

    @Override
    public boolean isCompleted() {
        return completed;
    }

    @Override
    public void initialize() {
        final JsonNode data = client.getNetworkClient()
                .get(String.format("%s?guild_id=%s", HttpAPIRoute.GUILD_INFO.toFullURL(), id));
        update(data);
        completed = true;
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