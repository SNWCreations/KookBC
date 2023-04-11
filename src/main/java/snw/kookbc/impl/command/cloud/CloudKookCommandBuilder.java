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
package snw.kookbc.impl.command.cloud;

import cloud.commandframework.Command;
import cloud.commandframework.CommandManager;
import cloud.commandframework.arguments.CommandArgument;
import cloud.commandframework.arguments.StaticArgument;
import cloud.commandframework.meta.CommandMeta;
import org.jetbrains.annotations.Nullable;
import snw.jkook.command.CommandSender;
import snw.jkook.command.JKookCommand;
import snw.jkook.plugin.Plugin;
import snw.kookbc.impl.command.CommandMap;
import snw.kookbc.impl.command.SimpleCommandMap;
import snw.kookbc.impl.command.WrappedCommand;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author huanmeng_qwq
 */
public class CloudKookCommandBuilder {
    private final Plugin plugin;
    private final CloudCommandManagerImpl manager;
    private final Command<CommandSender> command;
    private final CommandMap commandMap;

    public CloudKookCommandBuilder(Plugin plugin, CloudCommandManagerImpl manager, Command<CommandSender> command, CommandMap commandMap) {
        this.plugin = plugin;
        this.manager = manager;
        this.command = command;
        this.commandMap = commandMap;
    }

    @SuppressWarnings("unchecked")
    @Nullable
    public JKookCommand build() {
        final CommandArgument<?, ?> commandArgument = command.getArguments().get(0);
        boolean overwrite = manager.getSetting(CommandManager.ManagerSettings.OVERRIDE_EXISTING_COMMANDS);
        Map<String, WrappedCommand> wrappedCommandMap = commandMap.getView(false);

        String rootName = commandArgument.getName();
        List<String> names = new ArrayList<>();
        names.add(rootName);
        names.addAll(
                ((StaticArgument<CommandSender>) commandArgument).getAlternativeAliases()
        );

        names.removeIf(name -> wrappedCommandMap.containsKey(name) && !overwrite);

        if (names.isEmpty()) {
            return null;
        }

        JKookCommand kookCommand = new JKookCommand(names.remove(0));
        for (String name : names) {
            kookCommand.addAlias(name);
        }
        kookCommand.setExecutor(new CloudCommandExecutor(plugin, manager, kookCommand, rootName));
        kookCommand.setDescription(command.getCommandMeta().get(CommandMeta.DESCRIPTION).orElse(null));

        return kookCommand;
    }
}
