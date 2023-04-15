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

import cloud.commandframework.CloudCapability;
import cloud.commandframework.CommandManager;
import cloud.commandframework.arguments.standard.StringArrayArgument;
import cloud.commandframework.context.CommandContext;
import cloud.commandframework.context.CommandContextFactory;
import cloud.commandframework.context.StandardCommandContextFactory;
import cloud.commandframework.exceptions.*;
import cloud.commandframework.execution.CommandExecutionCoordinator;
import cloud.commandframework.execution.CommandExecutionHandler;
import cloud.commandframework.execution.CommandResult;
import cloud.commandframework.internal.CommandInputTokenizer;
import cloud.commandframework.internal.CommandRegistrationHandler;
import cloud.commandframework.keys.CloudKey;
import cloud.commandframework.keys.SimpleCloudKey;
import cloud.commandframework.meta.CommandMeta;
import cloud.commandframework.meta.SimpleCommandMeta;
import cloud.commandframework.services.State;
import io.leangen.geantyref.TypeToken;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import snw.jkook.JKook;
import snw.jkook.command.CommandSender;
import snw.jkook.command.ConsoleCommandSender;
import snw.jkook.command.JKookCommand;
import snw.jkook.entity.User;
import snw.jkook.message.Message;
import snw.jkook.plugin.Plugin;
import snw.kookbc.impl.command.CommandManagerImpl;
import snw.kookbc.impl.command.UnknownArgumentException;
import snw.kookbc.impl.message.NullMessage;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static snw.kookbc.util.Util.toEnglishNumOrder;

/**
 * @author huanmeng_qwq
 */
public class CloudBasedCommandManager extends CommandManager<CommandSender> {
    public static final CloudKey<Message> KOOK_MESSAGE_KEY = SimpleCloudKey.of("kook_message", TypeToken.get(Message.class));
    private final Plugin plugin;
    private final CommandContextFactory<CommandSender> commandContextFactory = new StandardCommandContextFactory<>();

    // JKook
    // Map<Fullname, Prefix>
    private final Map<String, String> jKookCommandFullnameList = new ConcurrentHashMap<>();
    private final Map<JKookCommand, Plugin> jKookCommandPluginMap = new ConcurrentHashMap<>();
    private final Map<String, JKookCommand> jKookCommandMap = new ConcurrentHashMap<>();

    CloudBasedCommandManager(@NonNull Plugin plugin) {
        super(CommandExecutionCoordinator.simpleCoordinator(), /*new CloudCommandRegistrationHandlerImpl(plugin)*/CommandRegistrationHandler.nullCommandRegistrationHandler());
        /*((CloudCommandRegistrationHandlerImpl) commandRegistrationHandler()).initialize(this);*/
        this.plugin = plugin;
        parameterInjectorRegistry().registerInjector(Message.class,
                (context, annotationAccessor) -> context.getOrDefault(KOOK_MESSAGE_KEY, NullMessage.INSTANCE)
        );
        registerCapability(CloudCapability.StandardCapabilities.ROOT_COMMAND_DELETION);
    }

    @Override
    public boolean hasPermission(@NonNull CommandSender sender, @NonNull String permission) {
        return true;
    }

    @Override
    public @NonNull CommandMeta createDefaultCommandMeta() {
        return SimpleCommandMeta.empty();
    }

    public @NotNull Plugin plugin() {
        return plugin;
    }

    public void registerJKook(JKookCommand jKookCommand, Plugin registerPlugin, CommandManagerImpl jkookManager) {
        Collection<String> prefixes = jKookCommand.getPrefixes();
        List<String> commandsList = new ArrayList<>();
        commandsList.add(jKookCommand.getRootName());
        commandsList.addAll(jKookCommand.getAliases());
        for (String s : commandsList) {
            prefixes.forEach(prefix -> jKookCommandFullnameList.put(prefix + s, prefix));
            prefixes.forEach(prefix -> jKookCommandMap.put(prefix + s, jKookCommand));
        }

        jKookCommandPluginMap.put(jKookCommand, registerPlugin);

        StringArrayArgument<CommandSender> args = StringArrayArgument.optional("args", (commandSenderCommandContext, s) -> Collections.emptyList());
        command(
                commandBuilder(jKookCommand.getRootName(), jKookCommand.getAliases(), SimpleCommandMeta.builder().with(CommandMeta.DESCRIPTION, jKookCommand.getDescription()).build())
                        .handler(new CommandExecutionHandler<CommandSender>() {
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

                            @Override
                            public void execute(@NonNull CommandContext<CommandSender> commandContext) {
                                @SuppressWarnings("unchecked")
                                String[] rawInput = ((List<String>) commandContext.get("__raw_input__")).toArray(new String[0]);

                                CommandSender sender = commandContext.getSender();
                                Message message = commandContext.get(KOOK_MESSAGE_KEY);
                                List<String> list = new ArrayList<>(Arrays.asList(rawInput));
                                if (!list.isEmpty()) {
                                    list.remove(0);
                                }
                                Object[] arguments;
                                try {
                                    arguments = jkookManager.processArguments(jKookCommand, list);
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
                                if (sender instanceof User && jKookCommand.getUserCommandExecutor() != null) {
                                    jKookCommand.getUserCommandExecutor().onCommand((User) sender, arguments, message);
                                } else if (sender instanceof ConsoleCommandSender && jKookCommand.getConsoleCommandExecutor() != null) {
                                    jKookCommand.getConsoleCommandExecutor().onCommand((ConsoleCommandSender) sender, arguments);
                                } else {
                                    jKookCommand.getExecutor().onCommand(sender, arguments, message);
                                }
                            }
                        })
                        .argument(args)
                        .build()
        );
    }

    public void unregisterJKookCommand(JKookCommand jKookCommand) {
        jKookCommandFullnameList.entrySet().removeIf(entry -> {
            List<String> list = collectJKookCommandNames(jKookCommand, true);
            for (String name : list) {
                if (entry.getKey().equals(name)) {
                    deleteRootCommand(entry.getKey().substring(entry.getValue().length()));
                    jKookCommandMap.remove(name);
                    return true;
                }
            }
            return false;
        });
        jKookCommandPluginMap.remove(jKookCommand);

    }

    public void unregisterJKookCommands(Plugin plugin) {
        List<Map.Entry<JKookCommand, Plugin>> list = jKookCommandPluginMap.entrySet().stream().filter(entry -> entry.getValue().equals(plugin)).collect(Collectors.toList());
        list.forEach(e -> unregisterJKookCommand(e.getKey()));
    }

    private List<String> collectJKookCommandNames(JKookCommand command, boolean withPrefix) {
        List<String> list = new ArrayList<>();
        if (withPrefix) {
            for (String prefix : command.getPrefixes()) {
                list.add(prefix + command.getRootName());
                command.getAliases().forEach(alias -> list.add(prefix + alias));
            }
        } else {
            list.add(command.getRootName());
            list.addAll(command.getAliases());
        }
        return list;
    }

    // TODO make it throws CommandException if command execution failed for other reasons
    public void executeCommandNow(@NonNull CommandSender commandSender, @NonNull String input, Message message) {
        executeCommand(commandSender, input, message)
                .whenComplete((commandResult, throwable) -> {
                    if (throwable instanceof CompletionException) {
                        throwable = throwable.getCause();
                    }
                    final Throwable finalThrowable = throwable;
                    if (throwable instanceof InvalidSyntaxException) {
                        handleException(commandSender,
                                InvalidSyntaxException.class,
                                (InvalidSyntaxException) throwable, (c, e) ->
                                        replay(message, "Invalid Command Syntax. "
                                                + "Correct command syntax is: /"
                                                + ((InvalidSyntaxException) finalThrowable)
                                                .getCorrectSyntax())
                        );
                    } else if (throwable instanceof InvalidCommandSenderException) {
                        handleException(commandSender,
                                InvalidCommandSenderException.class,
                                (InvalidCommandSenderException) throwable, (c, e) ->
                                        replay(message, finalThrowable.getMessage())
                        );
                    } else if (throwable instanceof NoPermissionException) {
                        handleException(commandSender,
                                NoPermissionException.class,
                                (NoPermissionException) throwable, (c, e) ->
                                        replay(message, "You do not have permission to execute this command")
                        );
                    } else if (throwable instanceof NoSuchCommandException) {
                        handleException(commandSender,
                                NoSuchCommandException.class,
                                (NoSuchCommandException) throwable, (c, e) -> {
                                    if (commandSender instanceof ConsoleCommandSender) {
                                        replay(message, "Unknown command. Type \"/help\" for help.");
                                    }
                                }
                        );
                    } else if (throwable instanceof ArgumentParseException) {
                        handleException(commandSender,
                                ArgumentParseException.class,
                                (ArgumentParseException) throwable, (c, e) ->
                                        replay(message, "Invalid Command Argument: "
                                                + finalThrowable.getCause().getMessage())
                        );
                    } else if (throwable instanceof CommandExecutionException) {
                        handleException(commandSender,
                                CommandExecutionException.class,
                                (CommandExecutionException) throwable, (c, e) -> {
                                    replay(message, "An internal error occurred while attempting to perform this command");
                                    plugin.getLogger().error(
                                            "Exception executing command handler", finalThrowable.getCause()
                                    );
                                }
                        );
                    } else if (throwable != null) {
                        replay(message, "An internal error occurred while attempting to perform this command");
                        plugin.getLogger().error("An unhandled exception was thrown during command execution",
                                throwable
                        );
                    }
                });
    }

    private void replay(Message message, String s) {
        if (message != null) {
            message.reply(s);
        } else {
            plugin.getLogger().info(s);
        }
    }

    @NonNull
    public CompletableFuture<CommandResult<CommandSender>> executeCommand(@NonNull CommandSender commandSender, @NonNull String input, Message message) {
        @NonNull String finalInput = input;
        Map.Entry<String, String> prefix = jKookCommandFullnameList.entrySet().stream().filter(e -> e.getKey().startsWith(finalInput)).findFirst().orElse(null);
        if (prefix != null) {
            input = input.substring(prefix.getValue().length());
        } else if (input.startsWith("/")) {
            input = input.substring(1);
        }
        final CommandContext<CommandSender> context = commandContextFactory.create(
                false,
                commandSender,
                this
        );
        context.set(KOOK_MESSAGE_KEY, message != null ? message : NullMessage.INSTANCE);
        final LinkedList<String> inputQueue = new CommandInputTokenizer(input).tokenize();
        /* Store a copy of the input queue in the context */
        context.store("__raw_input__", new LinkedList<>(inputQueue));
        try {
            if (this.preprocessContext(context, inputQueue) == State.ACCEPTED) {
                return commandExecutionCoordinator().coordinateExecution(context, inputQueue);
            }
        } catch (final Exception e) {
            final CompletableFuture<CommandResult<CommandSender>> future = new CompletableFuture<>();
            future.completeExceptionally(e);
            return future;
        }
        /* Wasn't allowed to execute the command */
        return CompletableFuture.completedFuture(null);
    }
}
