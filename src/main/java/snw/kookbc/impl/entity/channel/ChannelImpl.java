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

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.jetbrains.annotations.Nullable;
import snw.jkook.Permission;
import snw.jkook.entity.Guild;
import snw.jkook.entity.Role;
import snw.jkook.entity.User;
import snw.jkook.entity.channel.Channel;
import snw.jkook.util.Validate;
import snw.kookbc.impl.KBCClient;
import snw.kookbc.impl.network.HttpAPIRoute;
import snw.kookbc.interfaces.LazyLoadable;
import snw.kookbc.interfaces.Updatable;
import snw.kookbc.util.MapBuilder;

import java.util.*;

import static snw.kookbc.util.GsonUtil.get;

public abstract class ChannelImpl implements Channel, Updatable, LazyLoadable {
    protected final KBCClient client;
    private final String id;
    private User master;
    private Guild guild;
    private Collection<RolePermissionOverwrite> rpo;
    private Collection<UserPermissionOverwrite> upo;
    private boolean permSync;
    private String name;
    private int level;
    private boolean completed;

    public ChannelImpl(KBCClient client, String id) {
        this.client = client;
        this.id = id;
    }

    public ChannelImpl(KBCClient client, String id, User master, Guild guild, boolean permSync, String name, Collection<RolePermissionOverwrite> rpo, Collection<UserPermissionOverwrite> upo, int level) {
        this.client = client;
        this.id = id;
        this.master = master;
        this.guild = guild;
        this.permSync = permSync;
        this.name = name;
        this.rpo = rpo;
        this.upo = upo;
        this.level = level;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public Guild getGuild() {
        return guild;
    }

    @Override
    public boolean isPermissionSync() {
        return permSync;
    }

    public void setPermissionSync(boolean permSync) {
        this.permSync = permSync;
    }


    @Override
    public void delete() {
        client.getNetworkClient().post(HttpAPIRoute.CHANNEL_DELETE.toFullURL(),
                Collections.singletonMap("channel_id", getId())
        );
    }

    @Override
    public int getLevel() {
        return level;
    }

    @Override
    public void setLevel(int level) {
        Map<String, Object> body = new MapBuilder()
                .put("channel_id", getId())
                .put("level", level)
                .build();
        client.getNetworkClient().post(HttpAPIRoute.CHANNEL_UPDATE.toFullURL(), body);
        this.level = level;
    }

    @Override
    public void updatePermission(Role role, int rawAllow, int rawDeny) {
        updatePermission(role.getId(), rawAllow, rawDeny);
    }

    @Override
    public void updatePermission(int role, int rawAllow, int rawDeny) {
        Map<String, Object> body = new MapBuilder()
                .put("channel_id", getId())
                .put("type", "role_id")
                .put("value", String.valueOf(role))
                .put("allow", rawAllow)
                .put("deny", rawDeny)
                .build();
        client.getNetworkClient().post(HttpAPIRoute.CHANNEL_ROLE_UPDATE.toFullURL(), body);
    }

    @Override
    public void updatePermission(User user, int rawAllow, int rawDeny) {
        Map<String, Object> body = new MapBuilder()
                .put("channel_id", getId())
                .put("type", "user_id")
                .put("value", user.getId())
                .put("allow", rawAllow)
                .put("deny", rawDeny)
                .build();
        client.getNetworkClient().post(HttpAPIRoute.CHANNEL_ROLE_UPDATE.toFullURL(), body);
    }

    @Override
    public void addPermission(User user, Permission... perms) {
        if (perms.length == 0) {
            return;
        }
        int origin = 0;
        int deny = 0;
        UserPermissionOverwrite o = getUserPermissionOverwriteByUser(user);
        if (o != null) {
            origin = o.getRawAllow();
            deny = o.getRawDeny();
        }
        origin = Permission.sum(origin, perms);
        updatePermission(user, origin, deny);
    }

    @Override
    public void removePermission(User user, Permission... perms) {
        if (perms.length == 0) {
            return;
        }
        int origin = 0;
        int deny = 0;
        UserPermissionOverwrite o = getUserPermissionOverwriteByUser(user);
        if (o != null) {
            origin = o.getRawAllow();
            deny = o.getRawDeny();
        }
        origin = Permission.removeFrom(origin, perms);
        updatePermission(user, origin, deny);
    }

    @Override
    public void addPermission(Role role, Permission... perms) {
        addPermission(role.getId(), perms);
    }

    @Override
    public void removePermission(Role role, Permission... perms) {
        removePermission(role.getId(), perms);
    }

    @Override
    public void addPermission(int roleId, Permission... perms) {
        if (perms.length == 0) {
            return;
        }
        int origin = 0;
        int deny = 0;
        RolePermissionOverwrite o = getRolePermissionOverwriteByRole(roleId);
        if (o != null) {
            origin = o.getRawAllow();
            deny = o.getRawDeny();
        }
        origin = Permission.sum(origin, perms);
        updatePermission(roleId, origin, deny);
    }

    @Override
    public void removePermission(int roleId, Permission... perms) {
        if (perms.length == 0) {
            return;
        }
        int origin = 0;
        int deny = 0;
        RolePermissionOverwrite o = getRolePermissionOverwriteByRole(roleId);
        if (o != null) {
            origin = o.getRawAllow();
            deny = o.getRawDeny();
        }
        origin = Permission.removeFrom(origin, perms);
        updatePermission(roleId, origin, deny);
    }

    @Nullable
    public UserPermissionOverwrite getUserPermissionOverwriteByUser(User user) {
        for (UserPermissionOverwrite o : getOverwrittenUserPermissions()) {
            if (o.getUser() == user) {
                return o;
            }
        }
        return null;
    }

    @Nullable
    public RolePermissionOverwrite getRolePermissionOverwriteByRole(Role role) {
        for (RolePermissionOverwrite o : getOverwrittenRolePermissions()) {
            if (o.getRoleId() == role.getId()) {
                return o;
            }
        }
        return null;
    }

    @Override
    @Nullable
    public RolePermissionOverwrite getRolePermissionOverwriteByRole(int roleId) {
        for (RolePermissionOverwrite o : getOverwrittenRolePermissions()) {
            if (o.getRoleId() == roleId) {
                return o;
            }
        }
        return null;
    }

    @Override
    public void deletePermission(Role role) {
        Map<String, Object> body = new MapBuilder()
                .put("channel_id", getId())
                .put("type", "role_id")
                .put("value", String.valueOf(role.getId()))
                .build();
        client.getNetworkClient().post(HttpAPIRoute.CHANNEL_ROLE_DELETE.toFullURL(), body);
    }

    @Override
    public void deletePermission(User user) {
        Map<String, Object> body = new MapBuilder()
                .put("channel_id", getId())
                .put("type", "user_id")
                .put("value", String.valueOf(user.getId()))
                .build();
        client.getNetworkClient().post(HttpAPIRoute.CHANNEL_ROLE_DELETE.toFullURL(), body);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        Map<String, Object> body = new MapBuilder()
                .put("channel_id", getId())
                .put("name", name)
                .build();
        client.getNetworkClient().post(HttpAPIRoute.CHANNEL_UPDATE.toFullURL(), body);
        setName0(name);
    }

    public void setName0(String name) {
        this.name = name;
    }

    @Override
    public Collection<RolePermissionOverwrite> getOverwrittenRolePermissions() {
        return Collections.unmodifiableCollection(rpo);
    }

    public void setOverwrittenRolePermissions(Collection<RolePermissionOverwrite> rpo) {
        this.rpo = rpo;
    }

    @Override
    public Collection<UserPermissionOverwrite> getOverwrittenUserPermissions() {
        return Collections.unmodifiableCollection(upo);
    }

    public Collection<UserPermissionOverwrite> getOverwrittenUserPermissions0() {
        return upo;
    }

    public void setOverwrittenUserPermissions(Collection<UserPermissionOverwrite> upo) {
        this.upo = upo;
    }

    @Override
    public User getMaster() {
        return master;
    }

    @Override
    public void update(JsonObject data) {
        Validate.isTrue(Objects.equals(getId(), get(data, "id").getAsString()), "You can't update channel by using different data");
        synchronized (this) {
            // basic information
            String name = get(data, "name").getAsString();
            boolean isPermSync = get(data, "permission_sync").getAsInt() != 0;
            // rpo parse
            Collection<RolePermissionOverwrite> rpo = new ArrayList<>();
            for (JsonElement element : get(data, "permission_overwrites").getAsJsonArray()) {
                JsonObject orpo = element.getAsJsonObject();
                rpo.add(
                        new RolePermissionOverwrite(
                                orpo.get("role_id").getAsInt(),
                                orpo.get("allow").getAsInt(),
                                orpo.get("deny").getAsInt()
                        )
                );
            }

            // upo parse
            Collection<UserPermissionOverwrite> upo = new ArrayList<>();
            for (JsonElement element : get(data, "permission_users").getAsJsonArray()) {
                JsonObject oupo = element.getAsJsonObject();
                JsonObject rawUser = oupo.getAsJsonObject("user");
                upo.add(
                        new UserPermissionOverwrite(
                                client.getStorage().getUser(rawUser.get("id").getAsString(), rawUser),
                                oupo.get("allow").getAsInt(),
                                oupo.get("deny").getAsInt()
                        )
                );
            }

            this.name = name;
            this.permSync = isPermSync;
            this.rpo = rpo;
            this.upo = upo;

            // Why we delay the add operation?
            // We may construct the channel object at any time,
            // but sometimes they are just garbage object,
            // to prevent them affecting the cache, we won't add it
            // when constructing. When this method is called,
            // we think this object will be actually used.
            client.getStorage().addChannel(this);
        }
    }

    @Override
    public boolean isCompleted() {
        return completed;
    }

    @Override
    public void initialize() {
        final JsonObject data = client.getNetworkClient().get(
                String.format("%s?target_id=%s", HttpAPIRoute.CHANNEL_INFO.toFullURL(), getId())
        );
        update(data);
        completed = true;
    }
}
