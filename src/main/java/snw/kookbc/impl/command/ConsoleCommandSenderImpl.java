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

package snw.kookbc.impl.command;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import snw.jkook.command.ConsoleCommandSender;
import snw.jkook.entity.channel.Channel;
import snw.jkook.permissions.PermissionAttachment;
import snw.jkook.permissions.PermissionAttachmentInfo;
import snw.jkook.permissions.PermissionNode;
import snw.jkook.plugin.Plugin;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

public class ConsoleCommandSenderImpl implements ConsoleCommandSender {
    private static final Map<Plugin, ConsoleCommandSenderImpl> INSTANCES = new WeakHashMap<>();
    private final Logger logger;

    protected ConsoleCommandSenderImpl(Logger logger) {
        this.logger = logger;
    }

    @Override
    public Logger getLogger() {
        return logger;
    }

    public static synchronized ConsoleCommandSender get(Plugin plugin) {
        return INSTANCES.computeIfAbsent(plugin, i -> new ConsoleCommandSenderImpl(i.getLogger()));
    }

    public static void removeFor(Plugin plugin) {
        INSTANCES.remove(plugin);
    }

    @Override
    public boolean hasPermission(@Nullable Channel channel, @Nullable String s) {
        return true;
    }

    @Override
    public boolean hasPermission(@Nullable Channel channel, @NotNull PermissionNode permissionNode) {
        return true;
    }

    @Override
    public boolean isPermissionSet(Channel channel, @NotNull String s) {
        return true;
    }

    @Override
    public boolean isPermissionSet(@Nullable Channel channel, @NotNull PermissionNode permissionNode) {
        return true;
    }

    @Override
    public void recalculatePermissions() {

    }

    @Override
    public void removeAttachment(PermissionAttachment permissionAttachment) {

    }

    @Override
    public @NotNull PermissionAttachment addAttachment(@Nullable Channel channel, @NotNull Plugin plugin, @NotNull String s, boolean b) {
        return new PermissionAttachment(plugin, this);
    }

    @Override
    public @NotNull Set<PermissionAttachmentInfo> getEffectivePermissions(@Nullable Channel channel) {
        return Collections.emptySet();
    }
}
