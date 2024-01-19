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

package snw.kookbc.impl.command.cloud;

import cloud.commandframework.context.CommandContext;
import cloud.commandframework.execution.CommandExecutionHandler;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.jetbrains.annotations.Nullable;
import snw.jkook.command.CommandSender;
import snw.jkook.command.ConsoleCommandSender;
import snw.jkook.command.JKookCommand;
import snw.jkook.entity.User;
import snw.jkook.message.Message;
import snw.jkook.plugin.Plugin;
import snw.kookbc.impl.KBCClient;
import snw.kookbc.impl.command.CommandManagerImpl;
import snw.kookbc.impl.command.UnknownArgumentException;
import snw.kookbc.impl.command.cloud.exception.CommandPluginDisabledException;

import java.util.*;

import static snw.kookbc.impl.command.cloud.CloudConstants.KOOK_MESSAGE_KEY;
import static snw.kookbc.impl.command.cloud.CloudConstants.PLUGIN_KEY;
import static snw.kookbc.util.Util.toEnglishNumOrder;

/**
 * @author huanmeng_qwq
 */
public class CloudWrappedCommandExecutionHandler implements CommandExecutionHandler<CommandSender> {

    protected final KBCClient client;

    private final CommandManagerImpl parent;
    private final JKookCommand commandObject;

    public CloudWrappedCommandExecutionHandler(KBCClient client, CommandManagerImpl parent, JKookCommand commandObject) {
        this.client = client;
        this.parent = parent;
        this.commandObject = commandObject;
    }

    @Override
    public void execute(@NonNull CommandContext<CommandSender> commandContext) {
        @SuppressWarnings("unchecked")
        String[] rawInput = ((List<String>) commandContext.get("__raw_input__")).toArray(new String[0]);

        JKookCommand command = commandObject;

        CommandSender sender = commandContext.getSender();
        Message message = commandContext.getOrDefault(KOOK_MESSAGE_KEY, null);
        List<String> list = new ArrayList<>(Arrays.asList(rawInput));
        if (!list.isEmpty()) {
            list.remove(0); // remove head
        }
        Collection<JKookCommand> sub = command.getSubcommands();
        if (!sub.isEmpty()) { // if the command have subcommand, expect true
            client.getCore().getLogger().debug("The subcommand does exists. Attempting to search the final command.");
            while (!list.isEmpty()) {
                // get the first argument, so we got "a"
                String subName = list.get(0);
                client.getCore().getLogger().debug("Got temp subcommand root name: {}", subName);

                boolean found = false; // expect true
                // then get the command
                for (JKookCommand s : sub) {
                    // if the root name equals to the sub name
                    if (Objects.equals(s.getRootName(), subName)) { // expect true
                        client.getCore().getLogger().debug("Got valid subcommand: {}", subName); // debug
                        // then remove the first argument
                        list.remove(0); // "a" was removed, so we have "b" in next round
                        command = s; // got "a" subcommand
                        found = true; // found
                        sub = command.getSubcommands(); // update the search target, so we can search deeply
                        break; // it's not necessary to continue
                    }
                }
                if (!found) { // if the subcommand is not found
                    client.getCore().getLogger().debug("No subcommand matching current command root name. We will attempt to execute the command currently found."); // debug
                    // then we can regard the actualCommand as the final result to be executed
                    break; // exit the while loop
                }
            }
        }
        Plugin plugin = commandContext.getOptional(PLUGIN_KEY).orElse(null);
        if (plugin == null || !plugin.isEnabled()) {
            throw new CommandPluginDisabledException(new RuntimeException("Plugin is disabled"), commandContext, plugin);
        }

        Object[] arguments;
        try {
            arguments = parent.processArguments(command, list);
        } catch (NoSuchElementException e) {
            reply("执行命令失败: 参数不足。", "Unable to execute command: No enough arguments.", sender, message);
            return;
        } catch (UnknownArgumentException e) {
            reply(
                    "执行命令失败: 无法解析第 " + e.argIndex() + " 个参数。",
                    "Unable to execute command: unable to parse the " + toEnglishNumOrder(e.argIndex()) + " argument.",
                    sender, message
            );
            return;
        }

        if (sender instanceof User && command.getUserCommandExecutor() != null) {
            command.getUserCommandExecutor().onCommand((User) sender, arguments, message);
        } else if (sender instanceof ConsoleCommandSender && command.getConsoleCommandExecutor() != null) {
            command.getConsoleCommandExecutor().onCommand((ConsoleCommandSender) sender, arguments);
        } else if (command.getExecutor() != null) {
            command.getExecutor().onCommand(sender, arguments, message);
        } else {
            reply(
                    "执行命令失败: 此命令已注册，但它是一个空壳，没有可用的命令逻辑。",
                    "No executor was registered for provided command line.",
                    sender, message);
        }
    }

    private void reply(String content, String contentForConsole, CommandSender sender, @Nullable Message message) {
        if (sender instanceof ConsoleCommandSender) {
            client.getCore().getLogger().info(contentForConsole);
        } else if (sender instanceof User) {
            // contentForConsole should be null at this time
            if (message != null) {
                message.reply(content);
            } else {
                ((User) sender).sendPrivateMessage(content);
            }
        }
    }

}
