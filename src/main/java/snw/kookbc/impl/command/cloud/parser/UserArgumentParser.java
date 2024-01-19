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
import snw.jkook.entity.User;
import snw.kookbc.impl.KBCClient;

import java.util.Queue;

/**
 * @author huanmeng_qwq
 */
public class UserArgumentParser implements ArgumentParser<CommandSender, User> {
    private final KBCClient client;

    public UserArgumentParser(KBCClient client) {
        this.client = client;
    }

    @Override
    public @NonNull ArgumentParseResult<@NonNull User> parse(@NonNull CommandContext<@NonNull CommandSender> commandContext, @NonNull Queue<@NonNull String> inputQueue) {
        String input = inputQueue.peek();
        if (input == null) {
            return ArgumentParseResult.failure(new NoInputProvidedException(UserArgumentParser.class, commandContext));
        }
        if (input.startsWith("(met)")) {
            input = input.substring(5);
        }
        if (input.endsWith("(met)")) {
            input = input.substring(0, input.length() - 5);
        }
        try {
            User user = client.getCore().getHttpAPI().getUser(input);
            if (user == null) {
                return ArgumentParseResult.failure(new CommandException("User not found"));
            }
            inputQueue.remove();
            return ArgumentParseResult.success(user);
        } catch (final Exception e) {
            return ArgumentParseResult.failure(new CommandException("User not found"));
        }
    }
}
