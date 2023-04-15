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
import snw.jkook.command.ConsoleCommandSender;
import snw.jkook.command.JKookCommand;
import snw.jkook.message.Message;
import snw.jkook.plugin.Plugin;
import snw.kookbc.impl.KBCClient;
import snw.kookbc.impl.command.WrappedCommand;
import snw.kookbc.impl.message.NullMessage;

import java.util.Collections;
import java.util.LinkedList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author huanmeng_qwq
 */
public class CloudBasedCommandManager extends CommandManager<CommandSender> {
    public static final CloudKey<Message> KOOK_MESSAGE_KEY = SimpleCloudKey.of("kook_message", TypeToken.get(Message.class));
    protected final KBCClient client;
    private final CloudCommandManagerImpl parent;
    private final Plugin plugin;
    private final CommandContextFactory<CommandSender> commandContextFactory = new StandardCommandContextFactory<>();

    CloudBasedCommandManager(KBCClient client, @NonNull CloudCommandManagerImpl parent, @NonNull Plugin plugin) {
        super(CommandExecutionCoordinator.simpleCoordinator(), /*new CloudCommandRegistrationHandlerImpl(plugin)*/CommandRegistrationHandler.nullCommandRegistrationHandler());
        this.client = client;
        /*((CloudCommandRegistrationHandlerImpl) commandRegistrationHandler()).initialize(this);*/
        this.parent = parent;
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

    public void registerJKookCommand(JKookCommand jKookCommand) {
        StringArrayArgument<CommandSender> args = StringArrayArgument.optional("args", (commandSenderCommandContext, s) -> Collections.emptyList());
        command(
                commandBuilder(jKookCommand.getRootName(), jKookCommand.getAliases(), SimpleCommandMeta.builder().with(CommandMeta.DESCRIPTION, jKookCommand.getDescription()).build())
                        .handler(new CloudWrappedCommandExecutionHandler(parent, jKookCommand))
                        .argument(args)
                        .build()
        );
    }

    public void unregisterJKookCommand(JKookCommand jKookCommand) {
        deleteRootCommand(jKookCommand.getRootName());
    }

    public void executeCommandNow(@NonNull CommandSender commandSender, @NonNull String input, Message message) throws CommandException {
        AtomicReference<Throwable> unhandledException = new AtomicReference<>();
        try {
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
                                            message.sendToSource(finalThrowable.getMessage())
                            );
                        } else if (throwable instanceof InvalidCommandSenderException) {
                            handleException(commandSender,
                                    InvalidCommandSenderException.class,
                                    (InvalidCommandSenderException) throwable, (c, e) ->
                                            message.sendToSource(finalThrowable.getMessage())
                            );
                        } else if (throwable instanceof NoPermissionException) {
                            handleException(commandSender,
                                    NoPermissionException.class,
                                    (NoPermissionException) throwable, (c, e) ->
                                            message.sendToSource("You do not have permission to execute this command")
                            );
                        } else if (throwable instanceof NoSuchCommandException) {
                            handleException(commandSender,
                                    NoSuchCommandException.class,
                                    (NoSuchCommandException) throwable, (c, e) -> {
                                        if (commandSender instanceof ConsoleCommandSender) {
                                            client.getCore().getLogger().info("Unknown command. Type \"/help\" for help.");
                                        }
                                    }
                            );
                        } else if (throwable instanceof ArgumentParseException) {
                            handleException(commandSender,
                                    ArgumentParseException.class,
                                    (ArgumentParseException) throwable, (c, e) ->
                                            message.sendToSource("Invalid Command Argument: "
                                                    + finalThrowable.getCause().getMessage())
                            );
                        } else if (throwable != null) {
                            message.sendToSource("An internal error occurred while attempting to perform this command");
                            unhandledException.set(throwable); // provide the unhandled exception
                            plugin.getLogger().error("An unhandled exception was thrown during command execution",
                                    throwable
                            );
                        }
                    }).get();
        } catch (InterruptedException | ExecutionException ignored) { // impossible
        }
        if (unhandledException.get() != null) {
            throw new CommandException("Something unexpected happened.", unhandledException.get());
        }
    }

    @NonNull
    public CompletableFuture<CommandResult<CommandSender>> executeCommand(@NonNull CommandSender commandSender, @NonNull String input, Message message) {
        String head;
        if (input.contains(" ")) {
            head = input.substring(0, input.indexOf(" "));
        } else {
            head = input;
        }
        WrappedCommand wrappedCommand = parent.getCommandMap().getView(true).get(head);
        if (wrappedCommand == null) {
            return CompletableFuture.completedFuture(null);
        } else {
            JKookCommand origin = wrappedCommand.getCommand();
            int rIndex = head.indexOf(origin.getRootName());
            input = input.substring(rIndex);
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

    public void unregisterAll() {
        rootCommands().forEach(this::deleteRootCommand);
    }
}
