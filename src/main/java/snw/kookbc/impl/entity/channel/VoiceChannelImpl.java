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

import static snw.kookbc.util.JacksonUtil.getMapper;
import static snw.kookbc.util.JacksonUtil.get;
import static snw.kookbc.util.JacksonUtil.has;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

import snw.jkook.entity.Guild;
import snw.jkook.entity.User;
import snw.jkook.entity.channel.Category;
import snw.jkook.entity.channel.VoiceChannel;
import snw.kookbc.impl.KBCClient;
import snw.kookbc.impl.network.HttpAPIRoute;
import snw.kookbc.util.MapBuilder;
import snw.kookbc.util.JacksonUtil;

public class VoiceChannelImpl extends NonCategoryChannelImpl implements VoiceChannel {
    private boolean passwordProtected;
    private int maxSize;
    @SuppressWarnings("unused")
    private int quality;
    @SuppressWarnings("unused")
    private int chatLimitTime;

    public VoiceChannelImpl(KBCClient client, String id) {
        super(client, id);
    }

    public VoiceChannelImpl(KBCClient client, String id, User master, Guild guild, boolean permSync, Category parent,
            String name, Collection<RolePermissionOverwrite> rpo, Collection<UserPermissionOverwrite> upo, int level,
            boolean passwordProtected, int maxSize, int quality, int chatLimitTime) {
        super(client, id, master, guild, permSync, parent, name, rpo, upo, level, chatLimitTime);
        this.passwordProtected = passwordProtected;
        this.maxSize = maxSize;
        this.quality = quality;
        this.chatLimitTime = chatLimitTime;
        this.completed = true;
    }

    @Override
    public String createInvite(int validSeconds, int validTimes) {
        Map<String, Object> body = new MapBuilder()
                .put("channel_id", getId())
                .put("duration", validSeconds)
                .put("setting_times", validTimes)
                .build();
        JsonNode object = client.getNetworkClient().post(HttpAPIRoute.INVITE_CREATE.toFullURL(), body);
        return snw.kookbc.util.JacksonUtil.get(object, "url").asText();
    }

    @Override
    public boolean hasPassword() {
        initIfNeeded();
        return passwordProtected;
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
    public int getMaxSize() {
        initIfNeeded();
        return maxSize;
    }

    public void setMaxSize(int maxSize) {
        this.maxSize = maxSize;
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
    public int getQuality() { // must query because we can't update this value by update(JsonObject) method
        final JsonNode self = client.getNetworkClient()
                .get(HttpAPIRoute.CHANNEL_INFO.toFullURL() + "?target_id=" + getId());
        return snw.kookbc.util.JacksonUtil.get(self, "voice_quality").asInt();
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

    @Override
    public Collection<User> getUsers() {
        String rawContent = client.getNetworkClient()
                .getRawContent(HttpAPIRoute.CHANNEL_USER_LIST.toFullURL() + "?channel_id=" + getId());
        JsonNode rootNode = JacksonUtil.parse(rawContent);
        JsonNode array = JacksonUtil.get(rootNode, "data");
        Set<User> users = new HashSet<>();
        for (JsonNode element : array) {
            users.add(client.getStorage().getUser(element.get("id").asText(), element));
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
    public synchronized void update(JsonNode data) {
        super.update(data);
        boolean hasPassword = data.has("has_password") && data.get("has_password").asBoolean();
        int size = data.has("limit_amount") ? data.get("limit_amount").asInt() : 0;
        // KOOK does not provide voice quality value here!
        this.passwordProtected = hasPassword;
        this.maxSize = size;
    }

    @Override
    public StreamingInfo requestStreamingInfo(@Nullable String password) {
        final Map<String, Object> body = new MapBuilder()
                .put("channel_id", getId())
                .putIfNotNull("password", password)
                .build();
        final JsonNode res = client.getNetworkClient().post(HttpAPIRoute.VOICE_JOIN.toFullURL(), body);
        try {
            return getMapper().readValue(res.toString(), StreamingInfoImpl.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse StreamingInfo", e);
        }
    }

    @Override
    public StreamingInfo requestStreamingInfo(@Nullable String password, boolean rtcpMux) {
        final Map<String, ?> body = new MapBuilder()
                .put("channel_id", getId())
                .putIfNotNull("password", password)
                .put("rtcp_mux", rtcpMux)
                .build();
        final JsonNode res = client.getNetworkClient().post(HttpAPIRoute.VOICE_JOIN.toFullURL(), body);
        try {
            return getMapper().readValue(res.toString(), StreamingInfoImpl.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse StreamingInfo", e);
        }
    }

    @Override
    public StreamingInfo requestStreamingInfo(@Nullable String password, String audioSSRC, String audioPayloadType,
            boolean rtcpMux) {
        final Map<String, ?> body = new MapBuilder()
                .put("channel_id", getId())
                .putIfNotNull("password", password)
                .put("audio_ssrc", audioSSRC)
                .put("audio_pt", audioPayloadType)
                .put("rtcp_mux", rtcpMux)
                .build();
        final JsonNode res = client.getNetworkClient().post(HttpAPIRoute.VOICE_JOIN.toFullURL(), body);
        try {
            return getMapper().readValue(res.toString(), StreamingInfoImpl.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse StreamingInfo", e);
        }
    }

    @Override
    public void stopStreaming() {
        final Map<String, Object> body = new MapBuilder()
                .put("channel_id", getId())
                .build();
        client.getNetworkClient().postContent(HttpAPIRoute.VOICE_LEAVE.toFullURL(), body);
    }

    public static final class StreamingInfoImpl implements StreamingInfo {

        private final String ip;
        private final int port;
        private final int rtcp_port;
        private final int bitrate;
        private final String audio_ssrc;
        private final String audio_pt;

        @JsonCreator
        public StreamingInfoImpl(
                @JsonProperty("ip") String ip,
                @JsonProperty("port") int port,
                @JsonProperty("rtcp_port") int rtcp_port,
                @JsonProperty("bitrate") int bitrate,
                @JsonProperty("audio_ssrc") String audioSsrc,
                @JsonProperty("audio_pt") String audioPt) {
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
    public void keepStreaming() {
        final Map<String, Object> body = new MapBuilder()
                .put("channel_id", getId())
                .build();
        client.getNetworkClient().postContent(HttpAPIRoute.VOICE_KEEP_ALIVE.toFullURL(), body);
    }

}
