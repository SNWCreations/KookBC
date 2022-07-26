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

package snw.kookbc.impl.entity.channel;

import org.jetbrains.annotations.Nullable;
import snw.jkook.entity.Invitation;
import snw.jkook.entity.Role;
import snw.jkook.entity.User;
import snw.jkook.entity.channel.Category;
import snw.jkook.entity.channel.Channel;
import snw.jkook.util.PageIterator;
import snw.kookbc.impl.KBCClient;
import snw.kookbc.impl.network.HttpAPIRoute;
import snw.kookbc.impl.pageiter.ChannelInvitationIterator;
import snw.kookbc.util.MapBuilder;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

public abstract class ChannelImpl implements Channel {
    private final String id;
    private final User master;
    private Collection<RolePermissionOverwrite> rpo;
    private Collection<UserPermissionOverwrite> upo;
    private boolean permSync;
    private Category parent;
    private String name;

    public ChannelImpl(String id, User master, boolean permSync, Category parent, String name, Collection<RolePermissionOverwrite> rpo, Collection<UserPermissionOverwrite> upo) {
        this.id = id;
        this.master = master;
        this.permSync = permSync;
        this.parent = parent;
        this.name = name;
        this.rpo = rpo;
        this.upo = upo;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public boolean isPermissionSync() {
        return permSync;
    }

    public void setPermissionSync(boolean permSync) {
        this.permSync = permSync;
    }

    @Override
    public @Nullable Category getParent() {
        return parent;
    }

    public void setParent(Category parent) {
        if (this.parent != null) {
            ((CategoryImpl) this.parent).getChannels0().remove(this);
        }
        this.parent = parent;
        ((CategoryImpl) parent.getChannels()).getChannels0().add(this);
    }

    @Override
    public void delete() {
        KBCClient.getInstance().getConnector().getClient().post(HttpAPIRoute.CHANNEL_DELETE.toFullURL(),
                new MapBuilder()
                        .put("channel_id", getId())
                        .build()
        );
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
        KBCClient.getInstance().getConnector().getClient().post(HttpAPIRoute.CHANNEL_ROLE_UPDATE.toFullURL(), body);
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
        KBCClient.getInstance().getConnector().getClient().post(HttpAPIRoute.CHANNEL_ROLE_UPDATE.toFullURL(), body);
    }

    @Override
    public void deletePermission(Role role) {
        Map<String, Object> body = new MapBuilder()
                .put("channel_id", getId())
                .put("type", "role_id")
                .put("value", String.valueOf(role.getId()))
                .build();
        KBCClient.getInstance().getConnector().getClient().post(HttpAPIRoute.CHANNEL_ROLE_DELETE.toFullURL(), body);
    }

    @Override
    public void deletePermission(User user) {
        Map<String, Object> body = new MapBuilder()
                .put("channel_id", getId())
                .put("type", "user_id")
                .put("value", String.valueOf(user.getId()))
                .build();
        KBCClient.getInstance().getConnector().getClient().post(HttpAPIRoute.CHANNEL_ROLE_DELETE.toFullURL(), body);
    }

    @Override
    public PageIterator<Set<Invitation>> getInvitations() {
        return new ChannelInvitationIterator(this);
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public Collection<RolePermissionOverwrite> getOverwrittenRolePermissions() {
        return Collections.unmodifiableCollection(rpo);
    }

    public void setOverwrittenRolePermissions(Collection<RolePermissionOverwrite> rpo) {
        this.rpo = rpo;
    }

    // setters following:

    @Override
    public Collection<UserPermissionOverwrite> getOverwrittenUserPermissions() {
        return Collections.unmodifiableCollection(upo);
    }

    public void setOverwrittenUserPermissions(Collection<UserPermissionOverwrite> upo) {
        this.upo = upo;
    }

    @Override
    public User getMaster() {
        return master;
    }
}
