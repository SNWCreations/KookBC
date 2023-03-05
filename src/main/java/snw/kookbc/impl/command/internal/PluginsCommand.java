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

package snw.kookbc.impl.command.internal;

import org.jetbrains.annotations.Nullable;
import snw.jkook.command.CommandExecutor;
import snw.jkook.command.CommandSender;
import snw.jkook.command.ConsoleCommandSender;
import snw.jkook.entity.User;
import snw.jkook.message.Message;
import snw.jkook.plugin.Plugin;
import snw.kookbc.impl.KBCClient;
import java.util.Arrays;
import java.util.stream.Collectors;

public final class PluginsCommand implements CommandExecutor {
    private final KBCClient client;

    public PluginsCommand(KBCClient client) {
        this.client = client;
    }

    @Override
    public void onCommand(CommandSender sender, Object[] arguments, @Nullable Message message) {
        if (sender instanceof User && message == null) {
            // executed by CommandManager#executeCommand?
            return;
        }
        Plugin[] plugins = client.getCore().getPluginManager().getPlugins();
        String result = String.format("%s (%d): %s",
                (sender instanceof ConsoleCommandSender) ? "Installed and running plugins" : "已安装并正在运行的插件",
                plugins.length,
                String.join(", ",
                    Arrays.stream(plugins)
                            .map(plugin -> plugin.getDescription().getName())
                            .collect(Collectors.toSet())
                )
        );

        if (sender instanceof User) {
            message.sendToSource(result);
        } else {
            client.getCore().getLogger().info(result);
        }
    }
}
