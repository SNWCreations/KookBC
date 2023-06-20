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
import cloud.commandframework.annotations.CommandMethod;
import cloud.commandframework.annotations.processing.CommandContainer;
import org.jetbrains.annotations.Nullable;
import snw.jkook.command.CommandSender;
import snw.jkook.entity.User;
import snw.jkook.message.Message;
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

/**
 * @author huanmeng_qwq
 */
@SuppressWarnings("unused")
@CommandContainer
@CommandPrefix({"-", "#"})
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
    public void consoleHelp(CommandSender sender, Message message, @Argument("target") @Nullable String target) {
        List<String> content = buildHelpContent(target);
        if (sender instanceof User) {
            String finalResult;
            if (content.isEmpty()) {
                finalResult = "找不到命令。";
            } else {
                content.removeIf(IT -> IT.startsWith("(/)stop:"));

                if (client.getConfig().getBoolean("allow-help-ad", true)) {
                    content.add(
                            String.format(
                                    "由 [%s](%s) v%s 驱动 - %s API %s",
                                    SharedConstants.IMPL_NAME,
                                    SharedConstants.REPO_URL,
                                    SharedConstants.IMPL_VERSION,
                                    SharedConstants.SPEC_NAME,
                                    client.getCore().getAPIVersion()
                            )
                    );
                } else {
                    content.remove(content.size() - 1);
                }
                finalResult = String.join("\n", content.toArray(EMPTY_STRING_ARRAY));
            }
            message.sendToSource(finalResult);
        } else {
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
            result.add(String.format("别称: %s", String.join(" ", command.aliases())));
            result.add(String.format("可用前缀: %s", String.join(" ", command.prefixes())));
            result.add(
                    String.format("简介: %s",
                            (Util.isBlank(command.description()))
                                    ? "此命令没有简介。"
                                    : command.description()
                    )
            );
            //todo: add help content
            /*if (command.getHelpContent() != null && !command.getHelpContent().isEmpty()) {
                result.add("详细帮助信息:");
                result.add(command.getHelpContent());
            }*/
        } else {
            return Collections.emptyList();
        }
        result.add("-------------------------");
        return result;
    }
}
