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

import dev.rollczi.litecommands.LiteCommandsBuilder;
import dev.rollczi.litecommands.LiteCommandsFactory;
import snw.jkook.Core;
import snw.jkook.HttpAPI;
import snw.jkook.command.CommandSender;
import snw.jkook.command.ConsoleCommandSender;
import snw.jkook.entity.CustomEmoji;
import snw.jkook.entity.Guild;
import snw.jkook.entity.Role;
import snw.jkook.entity.User;
import snw.jkook.entity.channel.Channel;
import snw.jkook.entity.channel.NonCategoryChannel;
import snw.jkook.entity.channel.TextChannel;
import snw.jkook.entity.channel.VoiceChannel;
import snw.jkook.message.*;
import snw.jkook.message.component.FileComponent;
import snw.jkook.message.component.MarkdownComponent;
import snw.jkook.message.component.TextComponent;
import snw.jkook.message.component.card.CardComponent;
import snw.jkook.message.component.card.MultipleCardComponent;
import snw.jkook.plugin.Plugin;
import snw.jkook.util.KMarkdownHelper;
import snw.kookbc.impl.CoreImpl;
import snw.kookbc.impl.KBCClient;
import snw.kookbc.impl.command.CommandManagerImpl;
import snw.kookbc.impl.command.litecommands.argument.*;
import snw.kookbc.impl.command.litecommands.tools.KookMessageContextual;
import snw.kookbc.impl.command.litecommands.tools.KookOnlyConsoleContextual;
import snw.kookbc.impl.command.litecommands.tools.KookOnlyUserContextual;

public class LiteKookFactory {
    private LiteKookFactory() {
    }

    public static <B extends LiteCommandsBuilder<CommandSender, LiteKookSettings, B>> B builder(Plugin plugin) {
        return builder(plugin, new LiteKookSettings());
    }

    @SuppressWarnings("unchecked")
    public static <B extends LiteCommandsBuilder<CommandSender, LiteKookSettings, B>> B builder(Plugin plugin, LiteKookSettings liteBungeeSettings) {
        KBCClient client = ((CoreImpl) plugin.getCore()).getClient();
        HttpAPI httpAPI = plugin.getCore().getHttpAPI();
        return (B) new WrappedLiteCommandsBuilder<>(
                LiteCommandsFactory.builder(CommandSender.class, new KookLitePlatform(liteBungeeSettings, plugin, ((CommandManagerImpl) plugin.getCore().getCommandManager()).getCommandMap()))
                        .bind(Core.class, plugin::getCore)
                        .context(Message.class, new KookMessageContextual())
                        .context(User.class, new KookOnlyUserContextual<>("只有用户才能执行该命令"))
                        .context(ConsoleCommandSender.class, new KookOnlyConsoleContextual<>("只有后台才能执行该命令"))

                        .argument(User.class, new UserArgument(httpAPI))
                        .argument(Guild.class, new GuildArgument(httpAPI))
                        .argument(Channel.class, new ChannelArgument<>(httpAPI))
                        .argument(NonCategoryChannel.class, new ChannelArgument<>(httpAPI))
                        .argument(TextChannel.class, new ChannelArgument<>(httpAPI))
                        .argument(VoiceChannel.class, new ChannelArgument<>(httpAPI))

                        .argument(Role.class, new RoleArgument(client))
                        .argument(CustomEmoji.class, new EmojiArgument(client))

                        .result(String.class, new SimpleReplayResultHandler<>((sender, data) -> data.getFirst().reply(data.getSecond())))
                        .result(CardComponent.class, new SimpleReplayResultHandler<>((sender, data) -> data.getFirst().reply(data.getSecond())))
                        .result(MultipleCardComponent.class, new SimpleReplayResultHandler<>((sender, data) -> data.getFirst().reply(data.getSecond())))
                        .result(MarkdownComponent.class, new SimpleReplayResultHandler<>((sender, data) -> data.getFirst().reply(data.getSecond())))
                        .result(FileComponent.class, new SimpleReplayResultHandler<>((sender, data) -> data.getFirst().reply(data.getSecond())))
                        .result(TextComponent.class, new SimpleReplayResultHandler<>((sender, data) -> data.getFirst().reply(data.getSecond())))

                        .result(ChannelMessage.class, new SimpleReplayResultHandler<>((sender, data) -> data.getFirst().reply(data.getSecond().getComponent())))
                        .result(PrivateMessage.class, new SimpleReplayResultHandler<>((sender, data) -> data.getFirst().reply(data.getSecond().getComponent())))
                        .result(TextChannelMessage.class, new SimpleReplayResultHandler<>((sender, data) -> data.getFirst().reply(data.getSecond().getComponent())))
                        .result(VoiceChannelMessage.class, new SimpleReplayResultHandler<>((sender, data) -> data.getFirst().reply(data.getSecond().getComponent())))
        );
    }
}
