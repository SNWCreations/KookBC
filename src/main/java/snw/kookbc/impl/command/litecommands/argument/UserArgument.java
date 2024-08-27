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

package snw.kookbc.impl.command.litecommands.argument;

import dev.rollczi.litecommands.argument.Argument;
import dev.rollczi.litecommands.argument.parser.ParseResult;
import dev.rollczi.litecommands.argument.resolver.ArgumentResolver;
import dev.rollczi.litecommands.invocation.Invocation;
import dev.rollczi.litecommands.message.MessageKey;
import dev.rollczi.litecommands.message.MessageRegistry;
import dev.rollczi.litecommands.suggestion.SuggestionContext;
import dev.rollczi.litecommands.suggestion.SuggestionResult;
import snw.jkook.HttpAPI;
import snw.jkook.command.CommandException;
import snw.jkook.command.CommandSender;
import snw.jkook.entity.User;

public class UserArgument extends ArgumentResolver<CommandSender, User> {
    public static final MessageKey<String> USER_NOT_FOUND = MessageKey.of("user_not_found", "User not found");
    public static final MessageKey<CommandException> USER_FOUND_FAILURE = MessageKey.of("user_found_failure", "User found failure");

    private final HttpAPI httpAPI;
    private final MessageRegistry<CommandSender> messageRegistry;

    public UserArgument(HttpAPI httpAPI, MessageRegistry<CommandSender> messageRegistry) {
        this.httpAPI = httpAPI;
        this.messageRegistry = messageRegistry;
    }

    @Override
    protected ParseResult<User> parse(Invocation<CommandSender> invocation, Argument<User> context, String argument) {
        String input = argument;
        if (input.startsWith("(met)")) {
            input = input.substring(5);
        }
        if (input.endsWith("(met)")) {
            input = input.substring(0, input.length() - 5);
        }
        try {
            User user = httpAPI.getUser(input);
            if (user == null) {
                return ParseResult.failure(messageRegistry.getInvoked(USER_NOT_FOUND, invocation, argument));
            }
            return ParseResult.success(user);
        } catch (final Exception e) {
            return ParseResult.failure(messageRegistry.getInvoked(USER_FOUND_FAILURE, invocation, new CommandException("User not found", e)));
        }
    }

    @Override
    public SuggestionResult suggest(Invocation<CommandSender> invocation, Argument<User> argument, SuggestionContext context) {
        return super.suggest(invocation, argument, context);
    }
}
