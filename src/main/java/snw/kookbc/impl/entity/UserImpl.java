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

import org.jetbrains.annotations.Nullable;
import snw.jkook.entity.Guild;
import snw.jkook.entity.Role;
import snw.jkook.entity.User;
import snw.jkook.entity.channel.VoiceChannel;
import snw.jkook.message.component.BaseComponent;
import snw.kookbc.impl.KBCClient;
import snw.kookbc.impl.entity.builder.MessageBuilder;
import snw.kookbc.impl.network.HttpAPIRoute;
import snw.kookbc.util.MapBuilder;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;

public class UserImpl implements User {
    private final String id;
    private final boolean bot;
    private Collection<Integer> roles;
    private String name;
    private int identify;
    private Integer intimacy = null;
    private boolean online;
    private boolean ban;
    private boolean vip;
    private String avatarUrl;
    private String vipAvatarUrl;
    private VoiceChannel joinedChannel; // set by using setJoinedChannel()

    public UserImpl(String id,
                    boolean bot,
                    String name,
                    String avatarUrl,
                    String vipAvatarUrl,
                    int identify,
                    boolean online,
                    boolean ban,
                    boolean vip,
                    Collection<Integer> roles) {
        this.id = id;
        this.bot = bot;
        this.name = name;
        this.avatarUrl = avatarUrl;
        this.vipAvatarUrl = vipAvatarUrl;
        this.identify = identify;
        this.online = online;
        this.ban = ban;
        this.vip = vip;
        this.roles = roles;
    }


    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getNickName(Guild guild) {
        return KBCClient.getInstance().getNetworkClient()
                .get(String.format("%s?user_id=%s&guild_id=%s",
                        HttpAPIRoute.USER_WHO.toFullURL(),
                        id,
                        guild.getId()))
                .get("nickname")
                .getAsString();
    }

    @Override
    public void setNickName(Guild guild, String s) {
        Map<String, Object> body = new MapBuilder()
                .put("guild_id", guild.getId())
                .put("nickname", (s != null ? s : ""))
                .build();
        KBCClient.getInstance().getNetworkClient().post(HttpAPIRoute.GUILD_CHANGE_OTHERS_NICKNAME.toFullURL(), body);
    }

    @Override
    public int getIdentifyNumber() {
        return identify;
    }

    @Override
    public boolean isVip() {
        return vip;
    }

    public void setVip(boolean vip) {
        this.vip = vip;
    }

    @Override
    public boolean isBot() {
        return bot;
    }

    @Override
    public boolean isOnline() {
        return online;
    }

    public void setOnline(boolean online) {
        this.online = online;
    }

    @Override
    public boolean isBanned() {
        return ban;
    }

    @Override
    public Collection<Integer> getRoles() {
        return roles;
    }

    @Override
    public void sendPrivateMessage(BaseComponent baseComponent) {
        Object[] serialize = MessageBuilder.serialize(baseComponent);
        int type = (int) serialize[0];
        String json = (String) serialize[1];
        Map<String, Object> body = new MapBuilder()
                .put("type", type)
                .put("target_id", getId())
                .put("content", json)
                .build();
        KBCClient.getInstance().getNetworkClient().post(HttpAPIRoute.USER_CHAT_MESSAGE_CREATE.toFullURL(), body);
    }

    @Override
    public @Nullable VoiceChannel getJoinedVoiceChannel() {
        return joinedChannel;
    }

    @Override
    public int getIntimacy() {
        if (intimacy == null) {
            intimacy = KBCClient.getInstance().getNetworkClient().get(String.format("%s?user_id=%s", HttpAPIRoute.INTIMACY_INFO.toFullURL(), getId())).get("score").getAsInt();
        }
        return intimacy;
    }

    @Override
    public void setIntimacy(int i) {
        if (!((i > 0) && (i < 2200)))
            throw new IllegalArgumentException("Invalid score. 0--2200 is allowed.");
        Map<String, Object> body = new MapBuilder()
                .put("user_id", getId())
                .put("score", i)
                .build();
        KBCClient.getInstance().getNetworkClient().post(HttpAPIRoute.INTIMACY_UPDATE.toFullURL(), body);
        this.intimacy = i;
    }

    @Override
    public void grantRole(Role role) {
        Map<String, Object> body = new MapBuilder()
                .put("guild_id", role.getGuild().getId())
                .put("user_id", getId())
                .put("role_id", role.getId())
                .build();
        KBCClient.getInstance().getNetworkClient().post(HttpAPIRoute.ROLE_GRANT.toFullURL(), body);
        roles.add(role.getId());
    }

    @Override
    public void revokeRole(Role role) {
        Map<String, Object> body = new MapBuilder()
                .put("guild_id", role.getGuild().getId())
                .put("user_id", getId())
                .put("role_id", role.getId())
                .build();
        KBCClient.getInstance().getNetworkClient().post(HttpAPIRoute.ROLE_REVOKE.toFullURL(), body);
        roles.remove(role.getId());
    }

    @Override
    public void grantRole(Guild guild, int roleId) {
        Map<String, Object> body = new MapBuilder()
                .put("guild_id", guild.getId())
                .put("user_id", getId())
                .put("role_id", roleId)
                .build();
        KBCClient.getInstance().getNetworkClient().post(HttpAPIRoute.ROLE_GRANT.toFullURL(), body);
        roles.add(roleId);
    }

    @Override
    public void revokeRole(Guild guild, int roleId) {
        Map<String, Object> body = new MapBuilder()
                .put("guild_id", guild.getId())
                .put("user_id", getId())
                .put("role_id", roleId)
                .build();
        KBCClient.getInstance().getNetworkClient().post(HttpAPIRoute.ROLE_REVOKE.toFullURL(), body);
        roles.remove(roleId);
    }

    @Override
    public @Nullable String getAvatarUrl(boolean b) {
        return b ? vipAvatarUrl : avatarUrl;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setJoinedChannel(VoiceChannel joinedChannel) {
        this.joinedChannel = joinedChannel;
    }

    public void setIdentify(int identify) {
        this.identify = identify;
    }

    public void setBan(boolean ban) {
        this.ban = ban;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public void setVipAvatarUrl(String vipAvatarUrl) {
        this.vipAvatarUrl = vipAvatarUrl;
    }

    public void setRoles(Collection<Integer> roles) {
        this.roles = roles;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserImpl user = (UserImpl) o;
        return bot == user.bot && id.equals(user.id) && name.equals(user.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, bot);
    }
}
