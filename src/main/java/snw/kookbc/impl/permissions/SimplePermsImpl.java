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
import snw.jkook.permissions.*;
import snw.jkook.plugin.Plugin;
import snw.jkook.util.Pair;
import snw.kookbc.impl.plugin.InternalPlugin;

import java.util.*;

public class SimplePermsImpl implements Permissible {
    private final Permissible own;
    private final Set<PermissionContext> initializeContexts = new HashSet<>();
    private final Map<Pair<PermissionContext, String>, PermissionAttachmentInfo> permissions;
    private final List<PermissionAttachment> attachments = new LinkedList<>();

    public SimplePermsImpl(Permissible own) {
        this.own = own;
        this.permissions = new HashMap<>();
    }

    @Override
    public boolean hasPermission(PermissionContext context, @Nullable String permission) {
        if (permission == null) return true;
        String name = permission.toLowerCase(Locale.ENGLISH);
        if (this.isPermissionSet(context, name)) {
            return this.permissions.get(Pair.of(context, name)).getValue();
        } else {
            return false;
        }
    }

    @Override
    public boolean hasPermission(PermissionContext context, @NotNull PermissionNode perm) {
        String name = perm.getName().toLowerCase(Locale.ENGLISH);
        return this.isPermissionSet(context, name) ? this.permissions.get(Pair.of(context, name)).getValue() : perm.getDefault().getValue();
    }

    @Override
    public boolean isPermissionSet(PermissionContext context, @NotNull String name) {
        if (!initializeContexts.contains(context)) {
            this.own.recalculatePermissions(context);
            this.initializeContexts.add(context);
        }
        return this.permissions.containsKey(Pair.of(context, name.toLowerCase(Locale.ENGLISH)));
    }

    @Override
    public boolean isPermissionSet(PermissionContext context, @NotNull PermissionNode perm) {
        return this.isPermissionSet(context, perm.getName());
    }

    @Override
    public void recalculatePermissions(PermissionContext context) {
        this.permissions.clear();
        for (PermissionAttachment permissionAttachment : this.attachments) {
            for (Map.Entry<Pair<PermissionContext, String>, Boolean> entry : permissionAttachment.getPermissions().entrySet()) {
                Pair<PermissionContext, String> pair = entry.getKey();
                String name = pair.second().toLowerCase(Locale.ENGLISH);
                this.permissions.put(Pair.of(pair.first(), name), new PermissionAttachmentInfo(pair.first(), this.own, name, permissionAttachment, entry.getValue()));
            }
        }
    }

    @Override
    public void removeAttachment(PermissionContext context, PermissionAttachment permissionAttachment) {
        if (permissionAttachment == null) {
            throw new IllegalArgumentException("Attachment cannot be null");
        }

        if (this.attachments.remove(permissionAttachment)) {
            recalculatePermissions(context);
        } else {
            throw new IllegalArgumentException("Given attachment is not part of Permissible object " + this.own);
        }
    }

    @Override
    public @NotNull PermissionAttachment addAttachment(PermissionContext context, @NotNull Plugin plugin, @NotNull String name, boolean value) {
        if (!plugin.isEnabled()) {
            throw new IllegalArgumentException("Plugin " + plugin.getDescription().getName() + " v" + plugin.getDescription().getVersion() + " is disabled");
        } else if (!(plugin instanceof InternalPlugin) && name.startsWith("kook.")) {
            throw new IllegalArgumentException("Cannot add this permission: " + name);
        } else {
            PermissionAttachment result = new PermissionAttachment(plugin, this);
            result.setPermission(context, name, value);
            this.attachments.add(result);
            this.recalculatePermissions(context);
            return result;
        }
    }

    @Override
    public @NotNull Set<PermissionAttachmentInfo> getEffectivePermissions(PermissionContext context) {
        return Collections.unmodifiableSet(new HashSet<>(this.permissions.values()));
    }
}
