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

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import snw.jkook.Permission;
import snw.jkook.entity.Guild;
import snw.jkook.entity.Role;
import snw.jkook.entity.User;
import snw.jkook.entity.channel.Channel;
import snw.jkook.entity.channel.VoiceChannel;
import snw.jkook.exceptions.BadResponseException;
import snw.jkook.message.Message;
import snw.jkook.message.PrivateMessage;
import snw.jkook.message.component.BaseComponent;
import snw.jkook.message.component.MarkdownComponent;
import snw.jkook.permissions.PermissionAttachment;
import snw.jkook.permissions.PermissionAttachmentInfo;
import snw.jkook.permissions.PermissionNode;
import snw.jkook.plugin.Plugin;
import snw.jkook.util.PageIterator;
import snw.jkook.util.Validate;
import snw.kookbc.impl.KBCClient;
import snw.kookbc.impl.entity.builder.MessageBuilder;
import snw.kookbc.impl.network.HttpAPIRoute;
import snw.kookbc.impl.pageiter.UserJoinedVoiceChannelIterator;
import snw.kookbc.impl.permissions.SimplePermsImpl;
import snw.kookbc.impl.permissions.UserPermissionSaved;
import snw.kookbc.interfaces.LazyLoadable;
import snw.kookbc.interfaces.Updatable;
import snw.kookbc.util.MapBuilder;

import java.util.*;
import java.util.concurrent.TimeUnit;

import static java.util.Objects.requireNonNull;
import static snw.kookbc.util.GsonUtil.get;

public class UserImpl implements User, Updatable, LazyLoadable {
    private final KBCClient client;
    private final String id;
    private boolean bot;
    private String name;
    private int identify;
    private boolean ban;
    private boolean vip;
    private String avatarUrl;
    private String vipAvatarUrl;
    private boolean completed;

    private final SimplePermsImpl perms;
    private final Cache<String, Collection<Integer>> cacheRoleIds = Caffeine.newBuilder()
            .weakKeys()
            .expireAfterAccess(20, TimeUnit.SECONDS).build();

    public UserImpl(KBCClient client, String id) {
        this.client = requireNonNull(client);
        this.id = requireNonNull(id);
        this.perms = new SimplePermsImpl(this.client, this);
        this.perms.load();
    }

    public UserImpl(KBCClient client, String id, boolean bot, String name, int identify, boolean ban, boolean vip,
                    String avatarUrl, String vipAvatarUrl) {
        this(client, id);
        this.bot = bot;
        this.name = name;
        this.identify = identify;
        this.ban = ban;
        this.vip = vip;
        this.avatarUrl = avatarUrl;
        this.vipAvatarUrl = vipAvatarUrl;
        this.completed = true;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getNickName(Guild guild) {
        return client.getNetworkClient()
                .get(String.format("%s?user_id=%s&guild_id=%s", HttpAPIRoute.USER_WHO.toFullURL(), id, guild.getId()))
                .get("nickname")
                .getAsString();
    }

    @Override
    public String getFullName(@Nullable Guild guild) {
        return (guild != null ? getNickName(guild) : getName()) + "#" + getIdentifyNumber();
    }

    @Override
    public void setNickName(Guild guild, String s) {
        Map<String, Object> body = new MapBuilder()
                .put("guild_id", guild.getId())
                .put("nickname", (s != null ? s : ""))
                .put("user_id", getId())
                .build();
        client.getNetworkClient().post(HttpAPIRoute.GUILD_CHANGE_OTHERS_NICKNAME.toFullURL(), body);
    }

    @Override
    public int getIdentifyNumber() {
        initIfNeeded();
        return identify;
    }

    @Override
    public boolean isVip() {
        initIfNeeded();
        return vip;
    }

    public void setVip(boolean vip) {
        this.vip = vip;
    }

    @Override
    public boolean isBot() {
        initIfNeeded();
        return bot;
    }

    @Override
    public boolean isOnline() {
        return client.getNetworkClient().get(String.format("%s?user_id=%s", HttpAPIRoute.USER_WHO.toFullURL(), id))
                .get("online").getAsBoolean();
    }

    @Override
    public boolean isBanned() {
        initIfNeeded();
        return ban;
    }

    @Override
    public Collection<Integer> getRoles(Guild guild) {
        JsonArray array = client.getNetworkClient()
                .get(String.format("%s?user_id=%s&guild_id=%s",
                        HttpAPIRoute.USER_WHO.toFullURL(),
                        id,
                        guild.getId()))
                .getAsJsonArray("roles");
        HashSet<Integer> result = new HashSet<>();
        for (JsonElement element : array) {
            result.add(element.getAsInt());
        }
        return Collections.unmodifiableSet(result);
    }

    @Override
    public String sendPrivateMessage(String s) {
        return sendPrivateMessage(new MarkdownComponent(s));
    }

    @Override
    public String sendPrivateMessage(String s, PrivateMessage privateMessage) {
        return sendPrivateMessage(new MarkdownComponent(s), privateMessage);
    }

    @Override
    public String sendPrivateMessage(BaseComponent baseComponent) {
        return sendPrivateMessage(baseComponent, null);
    }

    @Override
    public String sendPrivateMessage(BaseComponent component, PrivateMessage quote) {
        Object[] serialize = MessageBuilder.serialize(component);
        int type = (int) serialize[0];
        String json = (String) serialize[1];
        Map<String, Object> body = new MapBuilder()
                .put("type", type)
                .put("target_id", getId())
                .put("content", json)
                .putIfNotNull("quote", quote, Message::getId)
                .build();
        return client.getNetworkClient().post(HttpAPIRoute.USER_CHAT_MESSAGE_CREATE.toFullURL(), body).get("msg_id")
                .getAsString();
    }

    @Override
    public PageIterator<Collection<VoiceChannel>> getJoinedVoiceChannel(Guild guild) {
        return new UserJoinedVoiceChannelIterator(client, this, guild);
    }

    @Override
    public int getIntimacy() {
        return client.getNetworkClient()
                .get(String.format("%s?user_id=%s", HttpAPIRoute.INTIMACY_INFO.toFullURL(), getId())).get("score")
                .getAsInt();
    }

    @Override
    public IntimacyInfo getIntimacyInfo() {
        JsonObject object = client.getNetworkClient()
                .get(String.format("%s?user_id=%s", HttpAPIRoute.INTIMACY_INFO.toFullURL(), getId()));
        String socialImage = get(object, "img_url").getAsString();
        String socialInfo = get(object, "social_info").getAsString();
        int lastRead = get(object, "last_read").getAsInt();
        int score = get(object, "score").getAsInt();
        JsonArray socialImageListRaw = get(object, "img_list").getAsJsonArray();
        Collection<IntimacyInfo.SocialImage> socialImages = new ArrayList<>(socialImageListRaw.size());
        for (JsonElement element : socialImageListRaw) {
            JsonObject obj = element.getAsJsonObject();
            String id = obj.get("id").getAsString();
            String url = obj.get("url").getAsString();
            socialImages.add(
                    new SocialImageImpl(id, url));
        }
        return new IntimacyInfoImpl(socialImage, socialInfo, lastRead, score, socialImages);
    }

    @Override
    public void setIntimacy(int i) {
        if (!((i > 0) && (i < 2200)))
            throw new IllegalArgumentException("Invalid score. 0--2200 is allowed.");
        Map<String, Object> body = new MapBuilder()
                .put("user_id", getId())
                .put("score", i)
                .build();
        client.getNetworkClient().post(HttpAPIRoute.INTIMACY_UPDATE.toFullURL(), body);
    }

    @Override
    public void setIntimacy(int i, String s, @Nullable Integer imageId) {
        Map<String, Object> body = new MapBuilder()
                .put("user_id", getId())
                .put("score", i)
                .putIfNotNull("social_info", s)
                .putIfNotNull("img_id", imageId)
                .build();
        client.getNetworkClient().post(HttpAPIRoute.INTIMACY_UPDATE.toFullURL(), body);
    }

    @Override
    public void grantRole(Role role) {
        Map<String, Object> body = new MapBuilder()
                .put("guild_id", role.getGuild().getId())
                .put("user_id", getId())
                .put("role_id", role.getId())
                .build();
        client.getNetworkClient().post(HttpAPIRoute.ROLE_GRANT.toFullURL(), body);
    }

    @Override
    public void revokeRole(Role role) {
        Map<String, Object> body = new MapBuilder()
                .put("guild_id", role.getGuild().getId())
                .put("user_id", getId())
                .put("role_id", role.getId())
                .build();
        client.getNetworkClient().post(HttpAPIRoute.ROLE_REVOKE.toFullURL(), body);
    }

    @Override
    public void grantRole(Guild guild, int roleId) {
        Map<String, Object> body = new MapBuilder()
                .put("guild_id", guild.getId())
                .put("user_id", getId())
                .put("role_id", roleId)
                .build();
        client.getNetworkClient().post(HttpAPIRoute.ROLE_GRANT.toFullURL(), body);
    }

    @Override
    public void revokeRole(Guild guild, int roleId) {
        Map<String, Object> body = new MapBuilder()
                .put("guild_id", guild.getId())
                .put("user_id", getId())
                .put("role_id", roleId)
                .build();
        client.getNetworkClient().post(HttpAPIRoute.ROLE_REVOKE.toFullURL(), body);
    }

    @Override
    public void block() {
        Map<String, Object> body = new MapBuilder()
                .put("user_id", getId())
                .build();
        client.getNetworkClient().post(HttpAPIRoute.FRIEND_BLOCK.toFullURL(), body);
    }

    @Override
    public void unblock() {
        Map<String, Object> body = new MapBuilder()
                .put("user_id", getId())
                .build();
        client.getNetworkClient().post(HttpAPIRoute.FRIEND_UNBLOCK.toFullURL(), body);
    }

    @Override
    public @Nullable String getAvatarUrl(boolean b) {
        initIfNeeded();
        return b ? vipAvatarUrl : avatarUrl;
    }

    @Override
    public String getName() {
        initIfNeeded();
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    @Override
    public void update(JsonObject data) {
        Validate.isTrue(Objects.equals(getId(), get(data, "id").getAsString()),
                "You can't update user by using different data");
        synchronized (this) {
            name = get(data, "username").getAsString();
            bot = get(data, "bot").getAsBoolean();
            avatarUrl = get(data, "avatar").getAsString();
            vipAvatarUrl = get(data, "vip_avatar").getAsString();
            identify = get(data, "identify_num").getAsInt();
            ban = get(data, "status").getAsInt() == 10;
            vip = get(data, "is_vip").getAsBoolean();
        }
    }

    @Override
    public boolean isCompleted() {
        return completed;
    }

    @Override
    public void initialize() {
        final JsonObject data = client.getNetworkClient().get(
                String.format("%s?user_id=%s", HttpAPIRoute.USER_WHO.toFullURL(), id));
        update(data);
        completed = true;
    }

    @Override
    public boolean hasPermission(@Nullable Channel context, @Nullable String permission) {
        return this.perms.hasPermission(context, permission);
    }

    @Override
    public boolean hasPermission(@Nullable Channel context, @NotNull PermissionNode perm) {
        return this.perms.hasPermission(context, perm);
    }

    @Override
    public boolean isPermissionSet(@Nullable Channel context, @NotNull String name) {
        return this.perms.isPermissionSet(context, name);
    }

    @Override
    public boolean isPermissionSet(@Nullable Channel context, @NotNull PermissionNode perm) {
        return this.perms.isPermissionSet(context, perm);
    }

    @Override
    public void recalculatePermissions() {
        this.perms.recalculatePermissions();
        if (this.perms.isLoading()) {
            return;
        }
        client.getUserPermissions().put(getId(), new UserPermissionSaved(getId(), this.perms.getEffectivePermissions(null)));
        client.savePermissions();
    }

    public Map<Permission, Boolean> calculateChannel(Channel channel) {
        Map<Permission, Boolean> result = new HashMap<>();
        Collection<Integer> cached = cacheRoleIds.asMap().get(id);
        if (cached == null) {
            cacheRoleIds.put(id, cached = getRoles(channel.getGuild()));
        }
        Collection<Integer> userRoleIds = new HashSet<>(cached);
        HashSet<Role> guildRoles = new HashSet<>();
        List<Role> cachedGuildRoles = client.getStorage().getRoles(channel.getGuild());
        if (!cachedGuildRoles.isEmpty()) {
            guildRoles.addAll(cachedGuildRoles);
        }
        for (Permission value : Permission.values()) {
            boolean calculated = false;
            try {
                calculated = calculateDefaultPerms(value, channel, userRoleIds, guildRoles);
            } catch (BadResponseException e) {
                this.client.getCore().getLogger().error("Error occurred while calculating built-in permissions", e);
                break;
            } catch (Exception e) {
                this.client.getCore().getLogger().error("Error occurred while calculating built-in permissions", e);
            }
            result.put(value, calculated);
        }
        return result;
    }

    public boolean calculateDefaultPerms(Permission permission, Channel channel, Collection<Integer> userRoleIds, Collection<Role> guildRoles) {
        Channel.UserPermissionOverwrite userPermissionOverwriteByUser = channel.getUserPermissionOverwriteByUser(this);
        if (userPermissionOverwriteByUser != null && permission.isIncludedIn(userPermissionOverwriteByUser.getRawAllow())) {
            return true;
        }

        Guild guild = channel.getGuild();
        if (guildRoles.isEmpty()) {
            PageIterator<Set<Role>> iterator = guild.getRoles();
            while (iterator.hasNext()) {
                Set<Role> batchRoles = iterator.next();
                guildRoles.addAll(batchRoles);
            }
        }
        for (Integer roleId : userRoleIds) {
            if (roleId == null) continue;
            Channel.RolePermissionOverwrite rolePermissionOverwriteByRole = channel.getRolePermissionOverwriteByRole(roleId);
            if (rolePermissionOverwriteByRole != null && permission.isIncludedIn(rolePermissionOverwriteByRole.getRawAllow())) {
                return true;
            }
        }

        if (Objects.equals(guild.getMaster().getId(), id)) {
            return true;
        }
        if (userRoleIds.isEmpty() || guildRoles.isEmpty()) {
            return false;
        }
        HashSet<Integer> cachedRoleIds = new HashSet<>(userRoleIds);
        for (Role role : guildRoles) {
            if (cachedRoleIds.contains(role.getId())) {
                cachedRoleIds.remove(role.getId());
                if (calculateDefaultPerms(permission, role)) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean calculateDefaultPerms(Permission permission, Role role) {
        return role.isPermissionSet(permission);
    }


    @Override
    public void removeAttachment(PermissionAttachment permissionAttachment) {
        this.perms.removeAttachment(permissionAttachment);
    }

    @Override
    public @NotNull PermissionAttachment addAttachment(@Nullable Channel context, @NotNull Plugin plugin, @NotNull String name, boolean value) {
        return this.perms.addAttachment(context, plugin, name, value);
    }

    @Override
    public @NotNull Set<PermissionAttachmentInfo> getEffectivePermissions(@Nullable Channel channel) {
        return this.perms.getEffectivePermissions(channel);
    }
}

class IntimacyInfoImpl implements User.IntimacyInfo {
    private final String socialImage;
    private final String socialInfo;
    private final int lastRead;
    private final int score;
    private final Collection<SocialImage> socialImages;

    IntimacyInfoImpl(String socialImage, String socialInfo, int lastRead, int score,
                     Collection<SocialImage> socialImages) {
        this.socialImage = socialImage;
        this.socialInfo = socialInfo;
        this.lastRead = lastRead;
        this.score = score;
        this.socialImages = Collections.unmodifiableCollection(socialImages);
    }

    @Override
    public String getSocialImage() {
        return socialImage;
    }

    @Override
    public String getSocialInfo() {
        return socialInfo;
    }

    @Override
    public int getLastRead() {
        return lastRead;
    }

    @Override
    public int getScore() {
        return score;
    }

    @Override
    public Collection<SocialImage> getSocialImages() {
        return socialImages;
    }
}

class SocialImageImpl implements User.IntimacyInfo.SocialImage {
    private final String id;
    private final String url;

    SocialImageImpl(String id, String url) {
        this.id = id;
        this.url = url;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getUrl() {
        return url;
    }
}
