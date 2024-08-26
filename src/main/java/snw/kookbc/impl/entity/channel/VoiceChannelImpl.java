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

package snw.kookbc.impl.entity.channel;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import snw.jkook.entity.Guild;
import snw.jkook.entity.User;
import snw.jkook.entity.channel.Category;
import snw.jkook.entity.channel.VoiceChannel;
import snw.kookbc.impl.KBCClient;
import snw.kookbc.impl.network.HttpAPIRoute;
import snw.kookbc.util.MapBuilder;

import java.util.*;

import static snw.kookbc.util.GsonUtil.*;

public class VoiceChannelImpl extends NonCategoryChannelImpl implements VoiceChannel {
    private boolean passwordProtected;
    private int maxSize;
    private int quality;
    private int chatLimitTime;

    public VoiceChannelImpl(KBCClient client, String id, User master, Guild guild, boolean permSync, Category parent, String name, Collection<RolePermissionOverwrite> rpo, Collection<UserPermissionOverwrite> upo, int level, boolean passwordProtected, int maxSize, int quality, int chatLimitTime) {
        super(client, id, master, guild, permSync, parent, name, rpo, upo, level, chatLimitTime);
        this.passwordProtected = passwordProtected;
        this.maxSize = maxSize;
        this.quality = quality;
        this.chatLimitTime = chatLimitTime;
    }

    @Override
    public String createInvite(int validSeconds, int validTimes) {
        Map<String, Object> body = new MapBuilder()
                .put("channel_id", getId())
                .put("duration", validSeconds)
                .put("setting_times", validTimes)
                .build();
        JsonObject object = client.getNetworkClient().post(HttpAPIRoute.INVITE_CREATE.toFullURL(), body);
        return get(object, "url").getAsString();
    }

    @Override
    public boolean hasPassword() {
        return passwordProtected;
    }

    @Override
    public int getMaxSize() {
        return maxSize;
    }

    public void setMaxSize(int maxSize) {
        this.maxSize = maxSize;
    }

    @Override
    public Collection<User> getUsers() {
        String rawContent = client.getNetworkClient().getRawContent(HttpAPIRoute.CHANNEL_USER_LIST.toFullURL() + "?channel_id=" + getId());
        JsonArray array = JsonParser.parseString(rawContent).getAsJsonObject().getAsJsonArray("data");
        Set<User> users = new HashSet<>();
        for (JsonElement element : array) {
            JsonObject obj = element.getAsJsonObject();
            users.add(client.getStorage().getUser(obj.get("id").getAsString(), obj));
        }
        return Collections.unmodifiableCollection(users);
    }

    @Override
    public void moveToHere(Collection<User> users) {
        Map<String, Object> body = new MapBuilder()
                .put("target_id", getId())
                .put("user_ids", users.stream().map(User::getId).toArray(String[]::new))
                .build();
        client.getNetworkClient().post(HttpAPIRoute.MOVE_USER.toFullURL(), body);
    }

    public void setPasswordProtected(boolean passwordProtected) {
        this.passwordProtected = passwordProtected;
    }

    @Override
    public void update(JsonObject data) {
        synchronized (this) {
            super.update(data);
            boolean hasPassword = has(data, "has_password") && get(data, "has_password").getAsBoolean();
            int size = has(data, "limit_amount") ? get(data, "limit_amount").getAsInt() : 0;
            // KOOK does not provide voice quality value here!
            this.passwordProtected = hasPassword;
            this.maxSize = size;
        }
    }

    @Override
    public int getQuality() { // must query because we can't update this value by update(JsonObject) method
        final JsonObject self = client.getNetworkClient()
                .get(HttpAPIRoute.CHANNEL_INFO.toFullURL() + "?target_id=" + getId());
        return get(self, "voice_quality").getAsInt();
    }

    @Override
    public void setPassword(@NotNull String password) {
        final Map<String, Object> body = new MapBuilder()
                .put("channel_id", getId())
                .put("password", password)
                .build();
        client.getNetworkClient().post(HttpAPIRoute.CHANNEL_UPDATE.toFullURL(), body);
        this.passwordProtected = !password.isEmpty();
    }

    @Override
    public void setSize(int size) {
        final Map<String, Object> body = new MapBuilder()
                .put("channel_id", getId())
                .put("limit_amount", size)
                .build();
        client.getNetworkClient().post(HttpAPIRoute.CHANNEL_UPDATE.toFullURL(), body);
        setMaxSize(size);
    }

    @Override
    public void setQuality(int i) {
        final Map<String, Object> body = new MapBuilder()
                .put("channel_id", getId())
                .put("voice_quality", i)
                .build();
        client.getNetworkClient().post(HttpAPIRoute.CHANNEL_UPDATE.toFullURL(), body);
        this.quality = i;
    }

    public static final class StreamingInfoImpl implements StreamingInfo {

        private final String ip;
        private final int port;
        private final int rtcp_port;
        private final int bitrate;
        private final String audio_ssrc;
        private final String audio_pt;

        public StreamingInfoImpl(String ip, int port, int rtcp_port, int bitrate, String audioSsrc, String audioPt) {
            this.ip = ip;
            this.port = port;
            this.rtcp_port = rtcp_port;
            this.bitrate = bitrate;
            audio_ssrc = audioSsrc;
            audio_pt = audioPt;
        }


        @Override
        public String getIp() {
            return ip;
        }

        @Override
        public int getPort() {
            return port;
        }

        @Override
        public int getRTCPPort() {
            return rtcp_port;
        }

        @Override
        public int getBitRate() {
            return bitrate;
        }

        @Override
        public String getAudioSSRC() {
            return audio_ssrc;
        }

        @Override
        public String getAudioPayloadType() {
            return audio_pt;
        }
    }

    @Override
    public StreamingInfo requestStreamingInfo(@Nullable String password) {
        final Map<String, Object> body = new MapBuilder()
                .put("channel_id", getId())
                .putIfNotNull("password", password)
                .build();
        final JsonObject res = client.getNetworkClient().post(HttpAPIRoute.VOICE_JOIN.toFullURL(), body);
        return NORMAL_GSON.fromJson(res, StreamingInfoImpl.class);
    }

    @Override
    public StreamingInfo requestStreamingInfo(@Nullable String password, boolean rtcpMux) {
        final Map<String, ?> body = new MapBuilder()
                .put("channel_id", getId())
                .putIfNotNull("password", password)
                .put("rtcp_mux", rtcpMux)
                .build();
        final JsonObject res = client.getNetworkClient().post(HttpAPIRoute.VOICE_JOIN.toFullURL(), body);
        return NORMAL_GSON.fromJson(res, StreamingInfoImpl.class);
    }

    @Override
    public StreamingInfo requestStreamingInfo(@Nullable String password, String audioSSRC, String audioPayloadType, boolean rtcpMux) {
        final Map<String, ?> body = new MapBuilder()
                .put("channel_id", getId())
                .putIfNotNull("password", password)
                .put("audio_ssrc", audioSSRC)
                .put("audio_pt", audioPayloadType)
                .put("rtcp_mux", rtcpMux)
                .build();
        final JsonObject res = client.getNetworkClient().post(HttpAPIRoute.VOICE_JOIN.toFullURL(), body);
        return NORMAL_GSON.fromJson(res, StreamingInfoImpl.class);
    }

    @Override
    public void keepStreaming() {
        final Map<String, Object> body = new MapBuilder()
                .put("channel_id", getId())
                .build();
        client.getNetworkClient().postContent(HttpAPIRoute.VOICE_KEEP_ALIVE.toFullURL(), body);
    }

    @Override
    public void stopStreaming() {
        final Map<String, Object> body = new MapBuilder()
                .put("channel_id", getId())
                .build();
        client.getNetworkClient().postContent(HttpAPIRoute.VOICE_LEAVE.toFullURL(), body);
    }

}
