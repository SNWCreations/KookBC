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
import cloud.commandframework.Command;
import cloud.commandframework.CommandManager;
import cloud.commandframework.arguments.CommandArgument;
import cloud.commandframework.arguments.standard.StringArrayArgument;
import cloud.commandframework.context.CommandContext;
import cloud.commandframework.context.CommandContextFactory;
import cloud.commandframework.context.StandardCommandContextFactory;
import cloud.commandframework.exceptions.*;
import cloud.commandframework.execution.CommandExecutionCoordinator;
import cloud.commandframework.execution.CommandResult;
import cloud.commandframework.internal.CommandInputTokenizer;
import cloud.commandframework.internal.CommandRegistrationHandler;
import cloud.commandframework.meta.CommandMeta;
import cloud.commandframework.meta.SimpleCommandMeta;
import cloud.commandframework.services.State;
import io.leangen.geantyref.TypeToken;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.jetbrains.annotations.NotNull;
import snw.jkook.command.CommandException;
import snw.jkook.command.CommandSender;
import snw.jkook.command.ConsoleCommandSender;
import snw.jkook.command.JKookCommand;
import snw.jkook.entity.Guild;
import snw.jkook.entity.User;
import snw.jkook.message.Message;
import snw.jkook.plugin.Plugin;
import snw.kookbc.impl.KBCClient;
import snw.kookbc.impl.command.cloud.exception.CommandPluginDisabledException;
import snw.kookbc.impl.command.cloud.parser.GuildArgumentParser;
import snw.kookbc.impl.command.cloud.parser.PluginArgumentParser;
import snw.kookbc.impl.command.cloud.parser.UserArgumentParser;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static snw.kookbc.impl.command.cloud.CloudConstants.*;

/**
 * @author huanmeng_qwq
 */
@SuppressWarnings("unused")
public class CloudBasedCommandManager extends CommandManager<CommandSender> {
    protected final KBCClient client;
    private final CloudCommandManagerImpl parent;
    private final CommandContextFactory<CommandSender> commandContextFactory = new StandardCommandContextFactory<>();

    CloudBasedCommandManager(KBCClient client, @NonNull CloudCommandManagerImpl parent) {
        super(CommandExecutionCoordinator.simpleCoordinator(), CommandRegistrationHandler.nullCommandRegistrationHandler());
        this.client = client;
        this.parent = parent;
        registerCapability(CloudCapability.StandardCapabilities.ROOT_COMMAND_DELETION);
        parserRegistry().registerParserSupplier(TypeToken.get(Plugin.class), p -> new PluginArgumentParser(client));
        parserRegistry().registerParserSupplier(TypeToken.get(User.class), p -> new UserArgumentParser(client));
        parserRegistry().registerParserSupplier(TypeToken.get(Guild.class), p -> new GuildArgumentParser(client));
        registerCommandPostProcessor(context -> {
            Command<@NonNull CommandSender> command = context.getCommand();
            CommandContext<@NonNull CommandSender> commandContext = context.getCommandContext();
            commandContext.set(PLUGIN_KEY, command.getCommandMeta().get(PLUGIN_KEY).orElse(null));
            if (!(commandContext.getSender() instanceof ConsoleCommandSender)) {
                logCommand(commandContext.getSender(), commandContext.getRawInputJoined());
            }
        });
    }

    @Override
    public boolean hasPermission(@NonNull CommandSender sender, @NonNull String permission) {
        return true;
    }

    @Override
    public @NonNull CommandMeta createDefaultCommandMeta() {
        return SimpleCommandMeta.empty();
    }

    public void registerJKookCommand(Plugin plugin, JKookCommand jKookCommand) {
        StringArrayArgument<CommandSender> args = StringArrayArgument.optional("args", (commandSenderCommandContext, s) -> Collections.emptyList());
        List<String> commands = withPrefix(jKookCommand, jKookCommand.getRootName());
        commands.addAll(withPrefixes(jKookCommand, jKookCommand.getAliases()));
        Iterator<String> iterator = commands.iterator();
        if (!iterator.hasNext()) {
            return;
        }
        List<String> alias = new ArrayList<>();
        String mainCommand = iterator.next();
        while (iterator.hasNext()) {
            alias.add(iterator.next());
        }
        Collection<String> prefixes = jKookCommand.getPrefixes();
        command(
                commandBuilder(
                        mainCommand,
                        alias,
                        SimpleCommandMeta.builder()
                                .with(CommandMeta.DESCRIPTION, jKookCommand.getDescription())
                                .with(PLUGIN_KEY, plugin)
                                .with(PREFIX_KEY, prefixes)
                                .with(ALIAS_KEY, alias)
                                .with(JKOOK_COMMAND_KEY, true)
                                .with(HELP_CONTENT_KEY, jKookCommand.getHelpContent())
                                .build()
                )
                        .handler(new CloudWrappedCommandExecutionHandler(client, parent, jKookCommand))
                        .argument(args)
                        .build()
        );
    }

    public void unregisterJKookCommand(JKookCommand jKookCommand) {
        for (String commandName : withPrefix(jKookCommand, jKookCommand.getRootName())) {
            deleteRootCommand(commandName);
        }
    }

    protected List<String> withPrefixes(JKookCommand jKookCommand, Collection<String> commands) {
        return commands.stream().flatMap(command -> withPrefix(jKookCommand, command).stream()).collect(Collectors.toList());
    }

    protected List<String> withPrefix(JKookCommand jKookCommand, String cmd) {
        Collection<String> prefixes = jKookCommand.getPrefixes();
        ArrayList<String> list = new ArrayList<>(prefixes.size());
        for (String prefix : prefixes) {
            list.add(prefix + cmd);
        }
        return list;
    }

    protected List<String> removePrefix(String[] prefixes, String... commands) {
        if (prefixes.length == 0) {
            return Arrays.asList(commands);
        }
        List<String> list = new ArrayList<>(commands.length);
        for (String command : commands) {
            boolean match = false;
            for (String prefix : prefixes) {
                if (command.startsWith(prefix)) {
                    match = true;
                    list.add(command.substring(prefix.length()));
                }
            }
            if (!match) {
                list.add(command);
            }
        }
        return list;
    }

    public boolean executeCommandNow(@NonNull CommandSender commandSender, @NonNull String input, Message message) throws CommandException {
        AtomicReference<Throwable> unhandledException = new AtomicReference<>();
        AtomicBoolean foundCommand = new AtomicBoolean(true);
        try {
            executeCommand(commandSender, input, message)
                    .whenComplete((commandResult, throwable) -> handleThrowable(commandSender, message, unhandledException, foundCommand, throwable)).get();
        } catch (InterruptedException | ExecutionException ignored) { // impossible
        }
        if (unhandledException.get() != null) {
            throw new CommandException("Something unexpected happened.", unhandledException.get());
        }
        return foundCommand.get();
    }

    protected void handleThrowable(@NotNull CommandSender commandSender, Message message, AtomicReference<Throwable> unhandledException, AtomicBoolean foundCommand, Throwable throwable) {
        if (throwable instanceof CompletionException) {
            throwable = throwable.getCause();
        }
        if (throwable instanceof CommandExecutionException) {
            throwable = throwable.getCause();
        }
        final Throwable finalThrowable = throwable;
        if (throwable instanceof InvalidSyntaxException) {
            handleException(commandSender,
                    InvalidSyntaxException.class,
                    (InvalidSyntaxException) throwable, (c, e) -> {
                        if (message != null) {
                            message.sendToSource("指令语法错误。正确的用法: "
                                    + ((InvalidSyntaxException) finalThrowable)
                                    .getCorrectSyntax());
                        } else {
                            client.getCore().getLogger().info("指令语法错误。正确的用法: "
                                    + ((InvalidSyntaxException) finalThrowable)
                                    .getCorrectSyntax());
                        }
                    }
            );
        } else if (throwable instanceof InvalidCommandSenderException) {
            handleException(commandSender,
                    InvalidCommandSenderException.class,
                    (InvalidCommandSenderException) throwable, (c, e) -> {
                        if (message != null) {
                            message.sendToSource(finalThrowable.getMessage());
                        } else {
                            client.getCore().getLogger().info(finalThrowable.getMessage());
                        }
                    }
            );
        } else if (throwable instanceof NoPermissionException) {
            handleException(commandSender,
                    NoPermissionException.class,
                    (NoPermissionException) throwable, (c, e) -> {
                        if (message != null) {
                            message.sendToSource("您没有执行此命令的权限");
                        } else {
                            client.getCore().getLogger().info("您没有执行此命令的权限");
                        }
                    }
            );
        } else if (throwable instanceof NoSuchCommandException) {
            handleException(commandSender,
                    NoSuchCommandException.class,
                    (NoSuchCommandException) throwable, (c, e) -> foundCommand.set(false)
            );
        } else if (throwable instanceof ArgumentParseException) {
            handleException(commandSender,
                    ArgumentParseException.class,
                    (ArgumentParseException) throwable, (c, e) -> {
                        if (message != null) {
                            message.sendToSource("无效的命令参数: "
                                    + finalThrowable.getCause().getMessage());
                        } else {
                            client.getCore().getLogger().info("无效的命令参数: "
                                    + finalThrowable.getCause().getMessage());
                        }
                    }
            );
        } else if (throwable instanceof CommandPluginDisabledException) {
            handleException(commandSender,
                    CommandPluginDisabledException.class,
                    (CommandPluginDisabledException) throwable, (c, e) -> {
                        if (message != null) {
                            message.sendToSource("无法执行命令: 注册此命令的插件现已被禁用。");
                        } else {
                            client.getCore().getLogger().info("Unable to execute command: The owner plugin: ({}) of this command was disabled.", e.getPlugin() == null ? "null" : e.getPlugin().getDescription().getName());
                        }
                    }
            );
        } else if (throwable != null) {
            if (message != null) {
                message.sendToSource("尝试执行此命令时发生内部错误");
            } else {
                client.getCore().getLogger().info("尝试执行此命令时发生内部错误");
            }
            unhandledException.set(throwable); // provide the unhandled exception
        }
    }

    @Override
    public final @NonNull CompletableFuture<CommandResult<CommandSender>> executeCommand(@NonNull CommandSender commandSender, @NonNull String input) {
        throw new UnsupportedOperationException();
    }

    @NonNull
    public CompletableFuture<CommandResult<CommandSender>> executeCommand(@NonNull CommandSender commandSender, @NonNull String input, Message message) {
        return executeCommand(commandSender, input, message, commandSender instanceof ConsoleCommandSender);
    }

    // prefix:full_command_name
    public Map<String, LinkedHashSet<String>> collectCommands() {
        LinkedHashMap<String, LinkedHashSet<String>> map = new LinkedHashMap<>();
        map.put("", new LinkedHashSet<>());
        for (Command<CommandSender> command : commands()) {
            map.get("").add(command.getArguments().get(0).getName());
            Collection<String> prefixes = command.getCommandMeta().get(PREFIX_KEY).orElse(null);
            if (prefixes == null) {
                continue;
            }
            Collection<String> aliases = command.getCommandMeta().get(ALIAS_KEY).orElse(Collections.emptyList());
            checkPrefix(map, prefixes.toArray(new String[0]));
            for (String prefix : prefixes) {
                map.get(prefix).addAll(aliases);
                map.get(prefix).add(command.getArguments().get(0).getName());
            }
        }
        return map;
    }

    private void checkPrefix(Map<String, LinkedHashSet<String>> map, String... prefixes) {
        for (String prefix : prefixes) {
            if (!map.containsKey(prefix)) {
                map.put(prefix, new LinkedHashSet<>());
            }
        }
    }

    @NonNull
    public CompletableFuture<CommandResult<CommandSender>> executeCommand(@NonNull CommandSender commandSender, @NonNull String input, Message message, boolean ignorePrefix) {
        final CommandContext<CommandSender> context = commandContextFactory.create(
                false,
                commandSender,
                this
        );
        if (message != null) {
            context.set(KOOK_MESSAGE_KEY, message);
        }
        if (commandSender instanceof ConsoleCommandSender) {
            logCommand(commandSender, input);
        }
        final LinkedList<String> inputQueue = new CommandInputTokenizer(input).tokenize();
        /* Store a copy of the input queue in the context */
        context.store("__raw_input__", new LinkedList<>(inputQueue));
        if (ignorePrefix && !inputQueue.isEmpty()) {
            String command = inputQueue.getFirst();
            EACH:
            for (Map.Entry<String, LinkedHashSet<String>> entry : collectCommands().entrySet()) {
                String prefix = entry.getKey();
                LinkedHashSet<String> commands = entry.getValue();
                for (String cmd : commands) {
                    if (cmd.substring(prefix.length()).equalsIgnoreCase(command)) {
                        command = cmd;
                        break EACH;
                    }
                }
            }
            inputQueue.set(0, command);
        }
        try {
            CompletableFuture<CommandResult<CommandSender>> future = new CompletableFuture<>();
            if (this.preprocessContext(context, inputQueue) == State.ACCEPTED) {
                commandExecutionCoordinator().coordinateExecution(context, inputQueue).whenComplete(((commandSenderCommandResult, throwable) -> {
                    if (commandSenderCommandResult != null) {
                        future.complete(commandSenderCommandResult);
                    } else if (throwable != null) {
                        future.completeExceptionally(throwable);
                    }
                }));
            }
            return future;
        } catch (final Exception e) {
            final CompletableFuture<CommandResult<CommandSender>> future = new CompletableFuture<>();
            if (!future.completeExceptionally(e)) {
                throw e;
            }
            return future;
        }
    }

    private void logCommand(@NotNull CommandSender commandSender, @NotNull String input) {
        if (commandSender instanceof ConsoleCommandSender) {
            client.getCore().getLogger().info(
                    "Console issued command: {}",
                    input
            );
        } else if (commandSender instanceof User) {
            client.getCore().getLogger().info(
                    "{}(User ID: {}) issued command: {}",
                    ((User) commandSender).getName(),
                    ((User) commandSender).getId(),
                    input
            );
        }
    }

    public void unregisterAll(Plugin plugin) {
        List<? extends CommandArgument<@NonNull CommandSender, ?>> list = commands().stream()
                .filter(i ->
                        i.getCommandMeta().get(PLUGIN_KEY)
                                .orElseThrow(
                                        () -> new IllegalStateException(
                                                "Internal error: commands does not have plugin meta!"
                                        )
                                )
                                == plugin
                )
                .map(i -> i.getArguments().get(0)).collect(Collectors.toList());
        for (CommandArgument<CommandSender, ?> i : list) {
            deleteRootCommand(i.getName());
        }
    }

    public List<CloudCommandInfo> getCommandsInfo() {
        List<CloudCommandInfo> result = new ArrayList<>();
        for (Command<CommandSender> command : commands()/*.stream().filter(i -> i.getCommandMeta().get(PLUGIN_KEY).isPresent()).collect(Collectors.toList())*/) {
            String[] prefixes = command.getCommandMeta().get(PREFIX_KEY).map(e -> e.toArray(new String[0])).orElse(new String[0]);
            String[] aliases = command.getCommandMeta().get(ALIAS_KEY).map(e -> e.toArray(new String[0])).orElse(new String[0]);
            String rootName = removePrefix(prefixes, command.getArguments().get(0).getName()).get(0);
            String syntax = rootName;
            String[] finalAliases = removePrefix(prefixes, aliases).stream().distinct().filter(e -> !e.equals(rootName)).toArray(String[]::new);
            Boolean jKookCommand = command.getCommandMeta().getOrDefault(JKOOK_COMMAND_KEY, false);
            if (!jKookCommand) {
                syntax = commandSyntaxFormatter().apply(command.getArguments(), null);
            }
            result.add(new CloudCommandInfo(
                            command.getCommandMeta().get(PLUGIN_KEY).orElse(null),
                            rootName,
                            syntax,
                            finalAliases,
                            prefixes,
                            command.getCommandMeta().get(CommandMeta.DESCRIPTION).orElse(""),
                            command.getCommandMeta().get(HELP_CONTENT_KEY).orElse(""),
                            jKookCommand,
                            command.isHidden()
                    )
            );
        }
        return result;
    }
}
