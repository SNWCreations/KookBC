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
import snw.jkook.JKook;
import snw.jkook.command.CommandSender;
import snw.jkook.command.ConsoleCommandSender;
import snw.jkook.command.JKookCommand;
import snw.jkook.entity.User;
import snw.jkook.message.Message;
import snw.kookbc.impl.command.CommandManagerImpl;
import snw.kookbc.impl.command.UnknownArgumentException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;

import static snw.kookbc.impl.command.cloud.CloudBasedCommandManager.KOOK_MESSAGE_KEY;
import static snw.kookbc.util.Util.toEnglishNumOrder;

/**
 * @author huanmeng_qwq
 */
public class CloudWrappedCommandExecutionHandler implements CommandExecutionHandler<CommandSender> {
    private final CommandManagerImpl parent;
    private final JKookCommand commandObject;

    public CloudWrappedCommandExecutionHandler(CommandManagerImpl parent, JKookCommand commandObject) {
        this.parent = parent;
        this.commandObject = commandObject;
    }

    @Override
    public void execute(@NonNull CommandContext<CommandSender> commandContext) {
        @SuppressWarnings("unchecked")
        String[] rawInput = ((List<String>) commandContext.get("__raw_input__")).toArray(new String[0]);

        CommandSender sender = commandContext.getSender();
        Message message = commandContext.get(KOOK_MESSAGE_KEY);
        List<String> list = new ArrayList<>(Arrays.asList(rawInput));
        if (!list.isEmpty()) {
            list.remove(0); // remove head
        }

        Object[] arguments;
        try {
            arguments = parent.processArguments(commandObject, list);
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

        if (sender instanceof User && commandObject.getUserCommandExecutor() != null) {
            commandObject.getUserCommandExecutor().onCommand((User) sender, arguments, message);
        } else if (sender instanceof ConsoleCommandSender && commandObject.getConsoleCommandExecutor() != null) {
            commandObject.getConsoleCommandExecutor().onCommand((ConsoleCommandSender) sender, arguments);
        } else {
            commandObject.getExecutor().onCommand(sender, arguments, message);
        }
    }

    private void reply(String content, String contentForConsole, CommandSender sender, @Nullable Message message) {
        if (sender instanceof ConsoleCommandSender) {
            JKook.getCore().getLogger().info(contentForConsole);
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
