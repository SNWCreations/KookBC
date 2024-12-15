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

package snw.kookbc.impl.command.litecommands;

import dev.rollczi.litecommands.command.CommandRoute;
import dev.rollczi.litecommands.meta.Meta;
import dev.rollczi.litecommands.meta.MetaKey;
import dev.rollczi.litecommands.meta.MetaType;
import dev.rollczi.litecommands.platform.AbstractPlatform;
import dev.rollczi.litecommands.platform.PlatformInvocationListener;
import dev.rollczi.litecommands.platform.PlatformSuggestionListener;
import org.jetbrains.annotations.NotNull;
import snw.jkook.command.CommandExecutor;
import snw.jkook.command.CommandSender;
import snw.jkook.command.JKookCommand;
import snw.jkook.plugin.Plugin;
import snw.kookbc.impl.command.CommandMap;
import snw.kookbc.impl.command.WrappedCommand;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class KookLitePlatform extends AbstractPlatform<CommandSender, LiteKookSettings> {
    public static final MetaKey<Set<String>> PREFIX = MetaKey.of("kookbc_prefix", MetaType.set(), new HashSet<>(Collections.singletonList("/")));

    private final Plugin plugin;
    private final CommandMap commandMap;

    protected KookLitePlatform(@NotNull LiteKookSettings settings, Plugin plugin, CommandMap commandMap) {
        super(settings, (sender) -> new KookSender(sender, null));
        this.plugin = plugin;
        this.commandMap = commandMap;
    }

    @Override
    protected void hook(CommandRoute<CommandSender> commandRoute, PlatformInvocationListener<CommandSender> platformInvocationListener, PlatformSuggestionListener<CommandSender> platformSuggestionListener) {
        for (String label : commandRoute.names()) {
            List<String> desc = commandRoute.meta().get(Meta.DESCRIPTION);
            Set<String> prefixes = commandRoute.meta().get(PREFIX);
            // List<String> perms = commandRoute.meta().get(Meta.PERMISSIONS);
            JKookCommand command = new JKookCommand(label, prefixes)
                    .setDescription(String.join("\n", desc))
                    .setExecutor(new LiteKookCommandExecutor(plugin.getCore(), this, settings, commandRoute, label, platformInvocationListener, platformSuggestionListener));
            commandMap.register(plugin, command);
        }
    }

    @Override
    protected void unhook(CommandRoute<CommandSender> commandRoute) {
        for (WrappedCommand wrappedCommand : commandMap.getView(false).values()) {
            CommandExecutor executor = wrappedCommand.getCommand().getExecutor();
            if (executor instanceof LiteKookCommandExecutor) {
                if (commandRoute.isNameOrAlias(wrappedCommand.getCommand().getRootName())) {
                    commandMap.unregister(wrappedCommand.getCommand());
                }
            }
        }
    }
}
