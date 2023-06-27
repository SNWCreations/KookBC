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
package snw.kookbc.impl.command.cloud.parser;

import cloud.commandframework.arguments.parser.ArgumentParseResult;
import cloud.commandframework.arguments.parser.ArgumentParser;
import cloud.commandframework.context.CommandContext;
import cloud.commandframework.exceptions.parsing.NoInputProvidedException;
import org.checkerframework.checker.nullness.qual.NonNull;
import snw.jkook.command.CommandException;
import snw.jkook.command.CommandSender;
import snw.jkook.plugin.Plugin;
import snw.kookbc.impl.KBCClient;

import java.util.Arrays;
import java.util.List;
import java.util.Queue;
import java.util.stream.Collectors;

/**
 * @author huanmeng_qwq
 */
public class PluginArgumentParser implements ArgumentParser<CommandSender, Plugin> {
    private final KBCClient client;

    public PluginArgumentParser(KBCClient client) {
        this.client = client;
    }

    @Override
    public @NonNull ArgumentParseResult<@NonNull Plugin> parse(@NonNull CommandContext<@NonNull CommandSender> commandContext, @NonNull Queue<@NonNull String> inputQueue) {
        final String input = inputQueue.peek();
        if (input == null) {
            return ArgumentParseResult.failure(new NoInputProvidedException(PluginArgumentParser.class, commandContext));
        }
        try {
            Plugin plugin = Arrays.stream(client.getCore().getPluginManager().getPlugins())
                    .filter(p -> p.getDescription().getName().equalsIgnoreCase(input))
                    .findFirst()
                    .orElse(null);
            if (plugin == null) {
                return ArgumentParseResult.failure(new CommandException("Plugin not found"));
            }
            inputQueue.remove();
            return ArgumentParseResult.success(plugin);
        } catch (final Exception e) {
            return ArgumentParseResult.failure(e);
        }
    }

    @Override
    public @NonNull List<@NonNull String> suggestions(@NonNull CommandContext<CommandSender> commandContext, @NonNull String input) {
        return Arrays.stream(client.getCore().getPluginManager().getPlugins()).map(plugin -> plugin.getDescription().getName()).collect(Collectors.toList());
    }
}