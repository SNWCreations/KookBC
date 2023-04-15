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
import cloud.commandframework.internal.CommandRegistrationHandler;
import org.checkerframework.checker.nullness.qual.NonNull;
import snw.jkook.command.CommandSender;
import snw.jkook.command.JKookCommand;
import snw.jkook.plugin.Plugin;
import snw.kookbc.impl.command.CommandManagerImpl;
import snw.kookbc.impl.command.CommandMap;
import snw.kookbc.impl.command.WrappedCommand;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author huanmeng_qwq
 */
public class CloudCommandRegistrationHandlerImpl implements CommandRegistrationHandler {
    private final Plugin plugin;
    private final CommandMap commandMap;
    private CloudBasedCommandManager manager;
    private final Map<CommandArgument<?, ?>, JKookCommand> registeredCommands = new ConcurrentHashMap<>();

    public CloudCommandRegistrationHandlerImpl(Plugin plugin) {
        this.plugin = plugin;
        this.commandMap = ((CommandManagerImpl) plugin.getCore().getCommandManager()).getCommandMap();
    }

    final void initialize(CloudBasedCommandManager manager) {
        this.manager = manager;
    }

    @Override
    public boolean registerCommand(@NonNull Command<?> command) {
        @SuppressWarnings("unchecked")
        JKookCommand register = CloudKookCommandBuilder.build(plugin, manager, (Command<CommandSender>) command, commandMap);
        if (register == null) {
            return false;
        }

        boolean overwrite = manager.getSetting(CommandManager.ManagerSettings.OVERRIDE_EXISTING_COMMANDS);
        if (overwrite) {
            Map<String, WrappedCommand> map = commandMap.getView(false);
            for (String name : register.getAliases()) {
                WrappedCommand wrappedCommand = map.get(name);
                if (wrappedCommand != null) {
                    JKookCommand origin = wrappedCommand.getCommand();
                    commandMap.unregister(origin);
                }
            }
            WrappedCommand wrappedCommand = map.get(register.getRootName());
            if (wrappedCommand != null) {
                JKookCommand origin = wrappedCommand.getCommand();
                commandMap.unregister(origin);
            }
        }

        registeredCommands.put(command.getArguments().get(0), register);

        register.register(plugin);
        return true;
    }

    @Override
    public void unregisterRootCommand(@NonNull StaticArgument<?> rootCommand) {
        if (!registeredCommands.containsKey(rootCommand)) {
            return;
        }
        Map<String, WrappedCommand> map = commandMap.getView(false);
        WrappedCommand wrappedCommand = map.get(rootCommand.getName());
        if (wrappedCommand != null) {
            JKookCommand origin = wrappedCommand.getCommand();
            commandMap.unregister(origin);
        }
    }
}
