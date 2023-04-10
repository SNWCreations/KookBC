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

import org.jetbrains.annotations.Nullable;
import snw.jkook.command.ConsoleCommandExecutor;
import snw.jkook.command.ConsoleCommandSender;
import snw.jkook.command.JKookCommand;
import snw.jkook.command.UserCommandExecutor;
import snw.jkook.entity.User;
import snw.jkook.message.Message;
import snw.kookbc.SharedConstants;
import snw.kookbc.impl.KBCClient;
import snw.kookbc.impl.command.CommandManagerImpl;
import snw.kookbc.impl.command.WrappedCommand;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public final class HelpCommand implements UserCommandExecutor, ConsoleCommandExecutor {
    private static final String[] EMPTY_STRING_ARRAY = new String[]{};
    private final KBCClient client;

    public HelpCommand(KBCClient client) {
        this.client = client;
    }

    @Override
    public void onCommand(ConsoleCommandSender sender, Object[] arguments) {
        List<String> content = buildHelpContent(arguments.length > 0 ? (String) arguments[0] : null);
        if (content.isEmpty()) {
            client.getCore().getLogger().info("Unknown command.");
        } else {
            for (String line : content) {
                client.getCore().getLogger().info(line);
            }
        }
    }

    @Override
    public void onCommand(User sender, Object[] arguments, @Nullable Message message) {
        if (message == null) {
            // executed by CommandManager#executeCommand?
            return;
        }

        List<String> content = buildHelpContent(arguments.length > 0 ? (String) arguments[0] : null);
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
    }

    private List<String> buildHelpContent(@Nullable String target) {
        JKookCommand[] commands;
        CommandManagerImpl commandManager = (CommandManagerImpl) client.getCore().getCommandManager();
        if (target != null && !target.isEmpty()) {
            WrappedCommand command = commandManager.getCommand(target);
            if (command == null) {
                return Collections.emptyList();
            }
            commands = new JKookCommand[]{command.getCommand()};
        } else {
            commands = commandManager.getCommandSet().toArray(new JKookCommand[0]);
        }
        List<String> result = new LinkedList<>();
        result.add("-------- 命令帮助 --------");
        if (commands.length > 1) {
            for (JKookCommand command : commands) {
                result.add(
                        String.format("(%s)%s: %s",
                                String.join(" ",
                                        command.getPrefixes()),
                                command.getRootName(),
                                (command.getDescription() == null) ? "此命令没有简介。" : command.getDescription()
                        )
                );
            }
            result.add(""); // the blank line as the separator
            result.add("注: 在每条命令帮助的开头，括号中用空格隔开的字符为此命令的前缀。");
            result.add("如 \"(/ .)blah\" 即 \"/blah\", \".blah\" 为同一条命令。");
        } else {
            JKookCommand command = commands[0];
            result.add(String.format("命令: %s", command.getRootName()));
            result.add(String.format("别称: %s", String.join(" ", command.getAliases())));
            result.add(String.format("可用前缀: %s", String.join(" ", command.getPrefixes())));
            result.add(
                    String.format("简介: %s",
                            (command.getDescription() == null)
                                    ? "此命令没有简介。"
                                    : command.getDescription()
                    )
            );
            if (command.getHelpContent() != null && !command.getHelpContent().isEmpty()) {
                result.add("详细帮助信息:");
                result.add(command.getHelpContent());
            }
        }
        result.add("-------------------------");
        return result;
    }
}
