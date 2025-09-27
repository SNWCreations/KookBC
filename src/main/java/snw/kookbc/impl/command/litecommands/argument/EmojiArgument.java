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
import snw.jkook.command.CommandException;
import snw.jkook.command.CommandSender;
import snw.jkook.command.ConsoleCommandSender;
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
    public static final MessageKey<String> EMOJI_NOT_FOUND = MessageKey.of("emoji_not_found", "Emoji not found");
    public static final MessageKey<Message> NOT_CHANNEL = MessageKey.of("emoji_not_channel", "Not supporting finding emoji");
    public static final MessageKey<CommandSender> SENDER_UNSUPPORTED = MessageKey.of("emoji_sender_unsupported", sender -> {
        if (sender instanceof ConsoleCommandSender) {
            return "Unsupported console command";
        }
        return "Unsupported command";
    });
    public static final MessageKey<CommandException> EMOJI_FOUND_FAILURE = MessageKey.of("emoji_found_failure", "Emoji found failure");

    private final KBCClient client;
    private final MessageRegistry<CommandSender> messageRegistry;

    public EmojiArgument(KBCClient client, MessageRegistry<CommandSender> messageRegistry) {
        this.client = client;
        this.messageRegistry = messageRegistry;
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
                return ParseResult.failure(messageRegistry.getInvoked(SENDER_UNSUPPORTED, invocation, invocation.sender()));
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
                    return ParseResult.failure(messageRegistry.getInvoked(EMOJI_NOT_FOUND, invocation, argument));
                }
                return ParseResult.success(emoji);
            }
            return ParseResult.failure(messageRegistry.getInvoked(NOT_CHANNEL, invocation, message));
        } catch (final Exception e) {
            return ParseResult.failure(messageRegistry.getInvoked(EMOJI_FOUND_FAILURE, invocation, new CommandException("CustomEmoji not found", e)));
        }
    }

    @Override
    public SuggestionResult suggest(Invocation<CommandSender> invocation, Argument<CustomEmoji> argument, SuggestionContext context) {
        return super.suggest(invocation, argument, context);
    }
}
