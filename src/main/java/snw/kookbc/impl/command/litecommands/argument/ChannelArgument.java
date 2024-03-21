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
import dev.rollczi.litecommands.suggestion.SuggestionContext;
import dev.rollczi.litecommands.suggestion.SuggestionResult;
import snw.jkook.HttpAPI;
import snw.jkook.command.CommandException;
import snw.jkook.command.CommandSender;
import snw.jkook.entity.User;
import snw.jkook.entity.channel.Channel;

public class ChannelArgument<T extends Channel> extends ArgumentResolver<CommandSender, T> {
    private final HttpAPI httpAPI;

    public ChannelArgument(HttpAPI httpAPI) {
        this.httpAPI = httpAPI;
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
                return ParseResult.failure(new CommandException("Channel not found"));
            }
            return ParseResult.success((T) channel);
        } catch (final Exception e) {
            return ParseResult.failure(new CommandException("Channel not found"));
        }
    }

    @Override
    public SuggestionResult suggest(Invocation<CommandSender> invocation, Argument<T> argument, SuggestionContext context) {
        return super.suggest(invocation, argument, context);
    }
}
