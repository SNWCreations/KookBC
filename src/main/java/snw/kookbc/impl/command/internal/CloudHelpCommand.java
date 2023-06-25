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
package snw.kookbc.impl.command.internal;

import cloud.commandframework.annotations.Argument;
import cloud.commandframework.annotations.CommandDescription;
import cloud.commandframework.annotations.CommandMethod;
import cloud.commandframework.annotations.processing.CommandContainer;
import org.jetbrains.annotations.Nullable;
import snw.jkook.command.CommandSender;
import snw.jkook.entity.User;
import snw.jkook.message.Message;
import snw.jkook.message.component.card.CardBuilder;
import snw.jkook.message.component.card.Size;
import snw.jkook.message.component.card.Theme;
import snw.jkook.message.component.card.element.ButtonElement;
import snw.jkook.message.component.card.element.MarkdownElement;
import snw.jkook.message.component.card.element.PlainTextElement;
import snw.jkook.message.component.card.module.*;
import snw.kookbc.SharedConstants;
import snw.kookbc.impl.KBCClient;
import snw.kookbc.impl.command.cloud.CloudCommandInfo;
import snw.kookbc.impl.command.cloud.CloudCommandManagerImpl;
import snw.kookbc.impl.command.cloud.annotations.CommandPrefix;
import snw.kookbc.util.Util;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import static snw.kookbc.impl.command.internal.HelpCommand.EMPTY_PLAIN_TEXT_ELEMENT;
import static snw.kookbc.impl.command.internal.HelpCommand.HELP_VALUE_HEADER;

/**
 * @author huanmeng_qwq
 */
@SuppressWarnings("unused")
@CommandContainer
@CommandPrefix({"/", "-", "#"})
public class CloudHelpCommand {
    private static final String[] EMPTY_STRING_ARRAY = new String[]{};

    private KBCClient client;

    public CloudHelpCommand(KBCClient client) {
        this.client = client;
    }

    // 这里是因为构建时会判断是否会有空构造器，否则会报错。
    public CloudHelpCommand() {
    }

    @CommandPrefix("$")
    @CommandMethod("help [target]")
    @CommandDescription("获取此帮助列表。")
    public void consoleHelp(CommandSender sender, Message message, @Argument("target") @Nullable String target) {
        if (sender instanceof User) {
            List<String> content = Util.listCloudCommandsHelp(this.client);
            CloudCommandInfo specificCommand = Util.findSpecificCloudCommand(this.client, target);
            CardBuilder finalBuilder;
            if (content.isEmpty() && specificCommand == null) {
                finalBuilder = new CardBuilder()
                        .setTheme(Theme.DANGER)
                        .setSize(Size.LG)
                        .addModule(new HeaderModule("找不到命令"));
            } else if (specificCommand != null) {
                finalBuilder = new CardBuilder()
                        .setTheme(Theme.SUCCESS)
                        .setSize(Size.LG)
                        .addModule(new HeaderModule("命令帮助"))
                        .addModule(DividerModule.INSTANCE)
                        .addModule(new SectionModule(new MarkdownElement(
                                String.format("**命令**: %s", specificCommand.rootName())
                        )))
                        .addModule(new SectionModule(new MarkdownElement(
                                String.format("**别称**: %s", String.join(" ", specificCommand.aliases()))
                        )))
                        .addModule(new SectionModule(new MarkdownElement(
                                String.format("**可用前缀**: %s", String.join(" ", specificCommand.prefixes()))
                        )))
                        .addModule(new SectionModule(new MarkdownElement(
                                Util.limit(
                                        String.format("**简介**: %s",
                                                (Util.isBlank(specificCommand.description()))
                                                        ? "此命令没有简介。"
                                                        : specificCommand.description()
                                        ),
                                        4997
                                )
                        )));
                if (!Util.isBlank(specificCommand.helpContent())) {
                    finalBuilder.addModule(new SectionModule(new MarkdownElement(
                            Util.limit(String.format("**详细帮助信息**:\n%s", specificCommand.helpContent()), 4997)
                    )));
                }
            } else {
                int totalPages = content.size() % 5 == 0 ? content.size() / 5 : content.size() / 5 + 1;
                CardBuilder builder = new CardBuilder()
                        .setTheme(Theme.SUCCESS)
                        .setSize(Size.LG)
                        .addModule(new HeaderModule(String.format("命令帮助 (1/%d)", totalPages)))
                        .addModule(DividerModule.INSTANCE);
                content.removeIf(IT -> IT.startsWith("(/)stop:"));
                if (content.size() <= 5) {
                    content.stream()
                            .map(SectionModule::new)
                            .forEachOrdered(builder::addModule);
                } else {
                    content.stream()
                            .limit(5L)
                            .map(SectionModule::new)
                            .forEachOrdered(builder::addModule);
                    builder.addModule(DividerModule.INSTANCE)
                            .addModule(new ActionGroupModule(
                                    Arrays.asList(
                                            new ButtonElement(
                                                    Theme.PRIMARY,
                                                    HELP_VALUE_HEADER + "{\"page\": 0, \"current\": 1}", // Placeholder
                                                    ButtonElement.EventType.NO_ACTION,
                                                    new PlainTextElement("上一页")
                                            ),
                                            new ButtonElement(Theme.SECONDARY, "", EMPTY_PLAIN_TEXT_ELEMENT), // Placeholder
                                            new ButtonElement(Theme.SECONDARY, "", EMPTY_PLAIN_TEXT_ELEMENT), // Placeholder
                                            new ButtonElement(
                                                    Theme.PRIMARY,
                                                    HELP_VALUE_HEADER + "{\"page\": 2, \"current\": 1}",
                                                    ButtonElement.EventType.RETURN_VAL,
                                                    new PlainTextElement("下一页")
                                            )
                                    )

                            ));
                }
                finalBuilder = builder;
            }
            if (client.getConfig().getBoolean("allow-help-ad", true)) {
                finalBuilder.addModule(DividerModule.INSTANCE)
                        .addModule(new ContextModule(
                                Collections.singletonList(
                                        new MarkdownElement(
                                                String.format(
                                                        "由 [%s](%s) v%s 驱动 - %s API %s",
                                                        SharedConstants.IMPL_NAME,
                                                        SharedConstants.REPO_URL,
                                                        SharedConstants.IMPL_VERSION,
                                                        SharedConstants.SPEC_NAME,
                                                        client.getCore().getAPIVersion()
                                                )
                                        )
                                )
                        ));
            }
            message.sendToSource(finalBuilder.build());
        } else {
            List<String> content = buildHelpContent(target);
            if (content.isEmpty()) {
                client.getCore().getLogger().info("Commands is empty.");
            } else {
                for (String line : content) {
                    client.getCore().getLogger().info(line);
                }
            }
        }
    }

    private List<String> buildHelpContent(@Nullable String target) {
        List<CloudCommandInfo> commands = ((CloudCommandManagerImpl) client.getCore().getCommandManager()).getCommandsInfo();
        if (target != null && !target.isEmpty()) {
            commands = commands.stream()
                    .filter(command -> command.rootName().equalsIgnoreCase(target) ||
                            Arrays.stream(command.aliases())
                                    .anyMatch(alias -> alias.equalsIgnoreCase(target))
                    )
                    .collect(Collectors.toList());
        }
        List<String> result = new LinkedList<>();
        result.add("-------- 命令帮助 --------");
        if (commands.size() > 1) {
            for (CloudCommandInfo command : commands) {
                result.add(
                        String.format("(%s)%s: %s",
                                String.join(" ",
                                        command.prefixes()),
                                command.rootName(),
                                (Util.isBlank(command.description())) ? "此命令没有简介。" : command.description()
                        )
                );
            }
            result.add(""); // the blank line as the separator
            result.add("注: 在每条命令帮助的开头，括号中用空格隔开的字符为此命令的前缀。");
            result.add("如 \"(/ .)blah\" 即 \"/blah\", \".blah\" 为同一条命令。");
        } else if (commands.size() == 1) {
            CloudCommandInfo command = commands.get(0);
            result.add(String.format("命令: %s", command.rootName()));
            if (command.aliases().length > 0) {
                result.add(String.format("别称: %s", String.join(" ", command.aliases())));
            }
            result.add(String.format("可用前缀: %s", String.join(" ", command.prefixes())));
            result.add(
                    String.format("简介: %s",
                            (Util.isBlank(command.description()))
                                    ? "此命令没有简介。"
                                    : command.description()
                    )
            );
            if (!Util.isBlank(command.helpContent())) {
                result.add("详细帮助信息:");
                result.add(command.helpContent());
            }
        } else {
            return Collections.emptyList();
        }
        result.add("-------------------------");
        return result;
    }
}
