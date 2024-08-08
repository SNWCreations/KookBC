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
import snw.jkook.entity.CustomEmoji;
import snw.jkook.entity.Guild;
import snw.jkook.message.ChannelMessage;
import snw.jkook.message.Message;
import snw.jkook.util.PageIterator;
import snw.kookbc.impl.KBCClient;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public class EmojiArgument extends ArgumentResolver<CommandSender, CustomEmoji> {
    private final KBCClient client;

    public EmojiArgument(KBCClient client) {
        this.client = client;
    }

    @Override
    protected ParseResult<CustomEmoji> parse(Invocation<CommandSender> invocation, Argument<CustomEmoji> context, String argument) {
        String emojiName = argument;
        String emojiId = argument;
        if (emojiName.startsWith("(emj)")) {
            emojiName = emojiName.substring(5);
        }
        int indexOf = emojiName.indexOf("(emj)");
        if (indexOf >= 0) {
            emojiId = emojiName.substring(indexOf + 5);
            emojiName = emojiName.substring(0, indexOf);
        }
        indexOf = emojiId.indexOf(']');
        if (indexOf >= 0) {
            emojiId = emojiId.substring(1, indexOf);
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

                CustomEmoji emoji = client.getStorage().getEmoji(emojiId);
                if (emoji == null) {
                    PageIterator<Set<CustomEmoji>> emojis = guild.getCustomEmojis();
                    FIND:
                    while (emojis.hasNext()) {
                        Set<CustomEmoji> set = emojis.next();
                        for (CustomEmoji r : set) {
                            if (Objects.equals(r.getId(), emojiId) && Objects.equals(r.getName(), emojiName)) {
                                emoji = r;
                                break FIND;
                            }
                        }
                    }
                }
                if (emoji == null) {
                    return ParseResult.failure(new CommandException("CustomEmoji not found"));
                }
                return ParseResult.success(emoji);
            }
            return ParseResult.failure(new CommandException("Unsupported argument: " + argument));
        } catch (final Exception e) {
            return ParseResult.failure(new CommandException("CustomEmoji not found"));
        }
    }

    @Override
    public SuggestionResult suggest(Invocation<CommandSender> invocation, Argument<CustomEmoji> argument, SuggestionContext context) {
        return super.suggest(invocation, argument, context);
    }
}
