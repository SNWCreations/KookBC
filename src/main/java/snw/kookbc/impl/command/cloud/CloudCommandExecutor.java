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

import cloud.commandframework.exceptions.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import snw.jkook.command.CommandExecutor;
import snw.jkook.command.CommandSender;
import snw.jkook.command.JKookCommand;
import snw.jkook.message.Message;
import snw.jkook.plugin.Plugin;

import java.util.concurrent.CompletionException;

/**
 * @author huanmeng_qwq
 */
public class CloudCommandExecutor implements CommandExecutor {
    private final Plugin plugin;
    private final CloudBasedCommandManager manager;
    private final JKookCommand root;
    private final String rootName;

    public CloudCommandExecutor(@NotNull Plugin plugin, @NotNull CloudBasedCommandManager manager, @NotNull JKookCommand root, @NotNull String rootName) {
        this.plugin = plugin;
        this.manager = manager;
        this.root = root;
        this.rootName = rootName;
    }

    @Override
    public void onCommand(@NotNull CommandSender sender, @NotNull Object[] objects, @Nullable Message message) {
        final StringBuilder builder = new StringBuilder(rootName);
        for (final Object obj : objects) {
            builder.append(" ").append(obj);
        }
        manager.executeCommand(sender, builder.toString(), message)
                .whenComplete((commandResult, throwable) -> {
                    if (throwable instanceof CompletionException) {
                        throwable = throwable.getCause();
                    }
                    final Throwable finalThrowable = throwable;
                    if (throwable instanceof InvalidSyntaxException) {
                        manager.handleException(sender,
                                InvalidSyntaxException.class,
                                (InvalidSyntaxException) throwable, (c, e) ->
                                        replay(message, "指令语法错误。正确的用法: "
                                                + root.getPrefixes().iterator().next()
                                                + ((InvalidSyntaxException) finalThrowable)
                                                .getCorrectSyntax())
                        );
                    } else if (throwable instanceof InvalidCommandSenderException) {
                        manager.handleException(sender,
                                InvalidCommandSenderException.class,
                                (InvalidCommandSenderException) throwable, (c, e) ->
                                        replay(message, e.getRequiredSender().equals(CommandSender.class)
                                                ? "该指令只能由KOOK 用户执行"
                                                : "该指令只能由控制台执行")
                        );
                    } else if (throwable instanceof NoPermissionException) {
                        manager.handleException(sender,
                                NoPermissionException.class,
                                (NoPermissionException) throwable, (c, e) ->
                                        replay(message, "你没有权限执行该指令")
                        );
                    }/* else if (throwable instanceof NoSuchCommandException) {
                                manager.handleException(sender,
                                        NoSuchCommandException.class,
                                        (NoSuchCommandException) throwable, (c, e) ->
                                                replay(message, "Unknown command. Type \"/help\" for help.")
                                );
                            }*/ else if (throwable instanceof ArgumentParseException) {
                        manager.handleException(sender,
                                ArgumentParseException.class,
                                (ArgumentParseException) throwable, (c, e) ->
                                        replay(message, "无效的命令参数: "
                                                + finalThrowable.getCause().getMessage())
                        );
                    } else if (throwable instanceof CommandExecutionException) {
                        manager.handleException(sender,
                                CommandExecutionException.class,
                                (CommandExecutionException) throwable, (c, e) -> {
                                    replay(message, "尝试执行此命令时发生内部错误");
                                    plugin.getLogger().error(
                                            "Exception executing command handler", finalThrowable.getCause()
                                    );
                                }
                        );
                    } else if (throwable != null) {
                        replay(message, "尝试执行此命令时发生内部错误");
                        plugin.getLogger().error("An unhandled exception was thrown during command execution",
                                throwable
                        );
                    }
                }).join();
    }

    public void replay(@Nullable Message message, String content) {
        if (message != null) {
            message.reply(content);
        } else {
            plugin.getLogger().info(content);
        }
    }
}
