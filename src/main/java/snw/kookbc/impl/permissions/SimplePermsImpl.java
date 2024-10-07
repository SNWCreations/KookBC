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

package snw.kookbc.impl.permissions;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import snw.jkook.Permission;
import snw.jkook.entity.channel.Channel;
import snw.jkook.permissions.*;
import snw.jkook.plugin.Plugin;
import snw.kookbc.impl.entity.UserImpl;

import java.util.*;

public class SimplePermsImpl implements Permissible {
    private final UserImpl own;
    private final Map<String, PermissionAttachmentInfo> permissions;
    private final List<PermissionAttachment> attachments = new LinkedList<>();

    public SimplePermsImpl(UserImpl own) {
        this.own = own;
        this.permissions = new HashMap<>();
    }

    @Override
    public boolean hasPermission(@Nullable Channel context, @Nullable String permission) {
        if (permission == null) return true;
        String name = permission.toLowerCase(Locale.ENGLISH);
        if (context != null && name.startsWith("kook.")) {
            Map<Permission, Boolean> map = this.own.calculateChannel(context);
            Permissions perms = Permissions.values().stream().filter(e -> e.getPermission().equalsIgnoreCase(name)).findFirst().orElse(null);
            if (perms == null) {
                return false;
            }
            return map.containsKey(perms.getPermissionEnum()) && map.get(perms.getPermissionEnum());
        }
        if (this.isPermissionSet(context, name)) {
            return this.permissions.get(name).getValue();
        } else {
            return false;
        }
    }

    @Override
    public boolean hasPermission(@Nullable Channel context, @NotNull PermissionNode perm) {
        String name = perm.getName().toLowerCase(Locale.ENGLISH);
        if (context != null && name.startsWith("kook.")) {
            Map<Permission, Boolean> map = this.own.calculateChannel(context);
            Permissions perms = Permissions.values().stream().filter(e -> e.getPermission().equalsIgnoreCase(name)).findFirst().orElse(null);
            if (perms == null) {
                return false;
            }
            return map.containsKey(perms.getPermissionEnum()) && map.get(perms.getPermissionEnum());
        }
        return this.isPermissionSet(context, name) ? this.permissions.get(name).getValue() : perm.getDefault().getValue();
    }

    @Override
    public boolean isPermissionSet(@Nullable Channel context, @NotNull String name) {
        return this.permissions.containsKey(name.toLowerCase(Locale.ENGLISH));
    }

    @Override
    public boolean isPermissionSet(@Nullable Channel context, @NotNull PermissionNode perm) {
        return this.isPermissionSet(context, perm.getName());
    }

    @Override
    public void recalculatePermissions() {
        this.permissions.clear();
        for (PermissionAttachment permissionAttachment : this.attachments) {
            for (Map.Entry<String, Boolean> entry : permissionAttachment.getPermissions().entrySet()) {
                String name = entry.getKey().toLowerCase(Locale.ENGLISH);
                if (name.startsWith("kook.")) {
                    continue;
                }
                this.permissions.put(name, new PermissionAttachmentInfo(this.own, name, permissionAttachment, entry.getValue()));
            }
        }
    }

    @Override
    public void removeAttachment(PermissionAttachment permissionAttachment) {
        if (permissionAttachment == null) {
            throw new IllegalArgumentException("Attachment cannot be null");
        }

        if (this.attachments.remove(permissionAttachment)) {
            recalculatePermissions();
        } else {
            throw new IllegalArgumentException("Given attachment is not part of Permissible object " + this.own);
        }
    }

    @Override
    public @NotNull PermissionAttachment addAttachment(@Nullable Channel context, @NotNull Plugin plugin, @NotNull String name, boolean value) {
        if (!plugin.isEnabled()) {
            throw new IllegalArgumentException("Plugin " + plugin.getDescription().getName() + " v" + plugin.getDescription().getVersion() + " is disabled");
        } else if (name.startsWith("kook.")) {
            if (context == null)
                throw new IllegalArgumentException("Cannot add this permission: " + name);
            Permissions pemrs = Permissions.values().stream().filter(e -> e.getPermission().equalsIgnoreCase(name)).findFirst().orElse(null);
            if (pemrs == null)
                throw new IllegalArgumentException("Cannot add this permission: " + name);
            context.addPermission(this.own, pemrs.getPermissionEnum());
            return new PermissionAttachment(plugin, this);
        } else {
            PermissionAttachment result = new PermissionAttachment(plugin, this);
            result.setPermission(name, value);
            this.attachments.add(result);
            this.recalculatePermissions();
            return result;
        }
    }

    @Override
    public @NotNull Set<PermissionAttachmentInfo> getEffectivePermissions(@Nullable Channel context) {
        Set<PermissionAttachmentInfo> result = new HashSet<>();
        if (context != null) {
            Map<Permission, Boolean> map = this.own.calculateChannel(context);
            for (Map.Entry<Permission, Boolean> entry : map.entrySet()) {
                result.add(new PermissionAttachmentInfo(this.own, Permissions.getPermission(entry.getKey()), null, entry.getValue()));
            }
        }
        result.addAll(this.permissions.values());
        return result;
    }
}
