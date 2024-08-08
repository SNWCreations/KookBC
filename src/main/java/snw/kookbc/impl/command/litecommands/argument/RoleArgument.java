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
import snw.jkook.command.CommandException;
import snw.jkook.command.CommandSender;
import snw.jkook.entity.Guild;
import snw.jkook.entity.Role;
import snw.jkook.message.ChannelMessage;
import snw.jkook.message.Message;
import snw.jkook.util.PageIterator;
import snw.kookbc.impl.KBCClient;

import java.util.Optional;
import java.util.Set;

public class RoleArgument extends ArgumentResolver<CommandSender, Role> {
    private final KBCClient client;

    public RoleArgument(KBCClient client) {
        this.client = client;
    }

    @Override
    protected ParseResult<Role> parse(Invocation<CommandSender> invocation, Argument<Role> context, String argument) {
        String input = argument;
        int index = input.indexOf("(rol)");
        if (index >= 0) {
            input = input.substring(index + 5);
        }
        index = input.indexOf("(rol)");
        if (index >= 0) {
            input = input.substring(0, index);
        }
        try {
            Optional<Message> optional = invocation.context().get(Message.class);
            if (!optional.isPresent()) {
                return ParseResult.failure(new CommandException("Unsupported argument: " + argument));
            }
            Message message = optional.get();
            if (message instanceof ChannelMessage) {
                ChannelMessage channelMessage = (ChannelMessage) message;
                Guild guild = channelMessage.getChannel().getGuild();

                int roleId = Integer.parseInt(input);

                Role role = client.getStorage().getRole(guild, roleId);
                if (role == null) {
                    PageIterator<Set<Role>> roles = guild.getRoles();
                    FIND:
                    while (roles.hasNext()) {
                        Set<Role> set = roles.next();
                        for (Role r : set) {
                            if (r.getId() == roleId) {
                                role = r;
                                break FIND;
                            }
                        }
                    }
                }
                if (role == null) {
                    return ParseResult.failure(new CommandException("Role not found"));
                }
                return ParseResult.success(role);
            }
            return ParseResult.failure(new CommandException("Unsupported argument: " + argument));
        } catch (final Exception e) {
            return ParseResult.failure(new CommandException("Role not found", e));
        }
    }

    @Override
    public SuggestionResult suggest(Invocation<CommandSender> invocation, Argument<Role> argument, SuggestionContext context) {
        return super.suggest(invocation, argument, context);
    }
}
