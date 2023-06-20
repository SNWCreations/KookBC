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
import cloud.commandframework.keys.CloudKey;
import cloud.commandframework.keys.SimpleCloudKey;
import cloud.commandframework.meta.CommandMeta;
import cloud.commandframework.meta.SimpleCommandMeta;
import cloud.commandframework.services.State;
import io.leangen.geantyref.TypeToken;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.jetbrains.annotations.NotNull;
import snw.jkook.command.CommandException;
import snw.jkook.command.CommandSender;
import snw.jkook.command.JKookCommand;
import snw.jkook.message.Message;
import snw.jkook.plugin.Plugin;
import snw.kookbc.impl.KBCClient;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static snw.kookbc.impl.command.cloud.CloudCommandManagerImpl.*;

/**
 * @author huanmeng_qwq
 */
@SuppressWarnings("unused")
public class CloudBasedCommandManager extends CommandManager<CommandSender> {
    public static final CloudKey<Message> KOOK_MESSAGE_KEY = SimpleCloudKey.of("kook_message", TypeToken.get(Message.class));
    protected final KBCClient client;
    private final CloudCommandManagerImpl parent;
    private final CommandContextFactory<CommandSender> commandContextFactory = new StandardCommandContextFactory<>();

    CloudBasedCommandManager(KBCClient client, @NonNull CloudCommandManagerImpl parent) {
        super(CommandExecutionCoordinator.simpleCoordinator(), /*new CloudCommandRegistrationHandlerImpl(plugin)*/CommandRegistrationHandler.nullCommandRegistrationHandler());
        this.client = client;
        /*((CloudCommandRegistrationHandlerImpl) commandRegistrationHandler()).initialize(this);*/
        this.parent = parent;
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
                                .build()
                )
                        .handler(new CloudWrappedCommandExecutionHandler(parent, jKookCommand))
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
                    .whenComplete((commandResult, throwable) -> {
                        handleThrowable(commandSender, message, unhandledException, foundCommand, throwable);
                    }).get();
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
        final Throwable finalThrowable = throwable;
        if (throwable instanceof InvalidSyntaxException) {
            handleException(commandSender,
                    InvalidSyntaxException.class,
                    (InvalidSyntaxException) throwable, (c, e) -> {
                        if (message != null) {
                            message.sendToSource(finalThrowable.getMessage());
                        }
                    }
            );
        } else if (throwable instanceof InvalidCommandSenderException) {
            handleException(commandSender,
                    InvalidCommandSenderException.class,
                    (InvalidCommandSenderException) throwable, (c, e) -> {
                        if (message != null) {
                            message.sendToSource(finalThrowable.getMessage());
                        }
                    }
            );
        } else if (throwable instanceof NoPermissionException) {
            handleException(commandSender,
                    NoPermissionException.class,
                    (NoPermissionException) throwable, (c, e) -> {
                        if (message != null) {
                            message.sendToSource("您没有执行此命令的权限");
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
                        }
                    }
            );
        } else if (throwable != null) {
            if (message != null) {
                message.sendToSource("尝试执行此命令时发生内部错误");
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
        final CommandContext<CommandSender> context = commandContextFactory.create(
                false,
                commandSender,
                this
        );
        if (message != null) {
            context.set(KOOK_MESSAGE_KEY, message);
        }
        final LinkedList<String> inputQueue = new CommandInputTokenizer(input).tokenize();
        /* Store a copy of the input queue in the context */
        context.store("__raw_input__", new LinkedList<>(inputQueue));
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

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    public List<CloudCommandInfo> getCommandsInfo() {
        List<CloudCommandInfo> result = new ArrayList<>();
        for (Command<CommandSender> command : commands().stream().filter(i -> i.getCommandMeta().get(PLUGIN_KEY).isPresent()).collect(Collectors.toList())) {
            String[] prefixes = command.getCommandMeta().get(PREFIX_KEY).map(e -> e.toArray(new String[0])).orElse(new String[0]);
            String[] aliases = command.getCommandMeta().get(ALIAS_KEY).map(e -> e.toArray(new String[0])).orElse(new String[0]);
            String rootName = removePrefix(prefixes, command.getArguments().get(0).getName()).get(0);
            String[] finalAliases = removePrefix(prefixes, aliases).stream().distinct().filter(e -> !e.equals(rootName)).toArray(String[]::new);
            result.add(new CloudCommandInfo(
                    command.getCommandMeta().get(PLUGIN_KEY).get(),
                    rootName,
                    finalAliases,
                    prefixes,
                    command.getCommandMeta().get(CommandMeta.DESCRIPTION).orElse(""),
                    command.getCommandMeta().getOrDefault(JKOOK_COMMAND_KEY, false)));
        }
        return result;
    }
}
