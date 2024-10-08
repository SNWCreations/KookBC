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
import snw.jkook.entity.channel.Channel;

public class ChannelArgument<T extends Channel> extends ArgumentResolver<CommandSender, T> {
    public static final MessageKey<String> CHANNEL_NOT_FOUND = MessageKey.of("channel_not_found", "Channel not found");
    public static final MessageKey<CommandException> CHANNEL_FOUND_FAILURE = MessageKey.of("channel_found_failure", "Channel found failure");

    private final HttpAPI httpAPI;
    private final MessageRegistry<CommandSender> messageRegistry;

    public ChannelArgument(HttpAPI httpAPI, MessageRegistry<CommandSender> messageRegistry) {
        this.httpAPI = httpAPI;
        this.messageRegistry = messageRegistry;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected ParseResult<T> parse(Invocation<CommandSender> invocation, Argument<T> context, String argument) {
        String input = argument;
        if (input.startsWith("(chn)")) {
            input = input.substring(5);
        }
        if (input.endsWith("(chn)")) {
            input = input.substring(0, input.length() - 5);
        }
        try {
            Channel channel = httpAPI.getChannel(input);
            if (channel == null) {
                return ParseResult.failure(messageRegistry.getInvoked(CHANNEL_NOT_FOUND, invocation, argument));
            }
            return ParseResult.success((T) channel);
        } catch (final Exception e) {
            return ParseResult.failure(messageRegistry.getInvoked(CHANNEL_FOUND_FAILURE, invocation, new CommandException("Channel not found", e)));
        }
    }

    @Override
    public SuggestionResult suggest(Invocation<CommandSender> invocation, Argument<T> argument,
            SuggestionContext context) {
        return super.suggest(invocation, argument, context);
    }
}
