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
import org.checkerframework.checker.nullness.qual.NonNull;
import snw.jkook.command.CommandException;
import snw.jkook.command.CommandSender;
import snw.jkook.entity.Guild;
import snw.kookbc.impl.KBCClient;

import java.util.Queue;

/**
 * @author huanmeng_qwq
 */
public class GuildArgumentParser implements @NonNull ArgumentParser<CommandSender, Guild> {
    private final KBCClient client;

    public GuildArgumentParser(KBCClient client) {
        this.client = client;
    }

    @Override
    public @NonNull ArgumentParseResult<@NonNull Guild> parse(@NonNull CommandContext<@NonNull CommandSender> commandContext, @NonNull Queue<@NonNull String> inputQueue) {
        final String input = inputQueue.peek();
        if (input == null) {
            return ArgumentParseResult.failure(new cloud.commandframework.exceptions.parsing.NoInputProvidedException(GuildArgumentParser.class, commandContext));
        }
        try {
            Guild guild = client.getCore().getHttpAPI().getGuild(input);
            if (guild == null) {
                return ArgumentParseResult.failure(new CommandException("Guild not found"));
            }
            inputQueue.remove();
            return ArgumentParseResult.success(guild);
        } catch (final Exception e) {
            return ArgumentParseResult.failure(new CommandException("Guild not found"));
        }
    }
}
