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

import cloud.commandframework.CommandManager;
import cloud.commandframework.context.CommandContext;
import cloud.commandframework.context.CommandContextFactory;
import cloud.commandframework.context.StandardCommandContextFactory;
import cloud.commandframework.execution.CommandExecutionCoordinator;
import cloud.commandframework.execution.CommandResult;
import cloud.commandframework.internal.CommandInputTokenizer;
import cloud.commandframework.keys.CloudKey;
import cloud.commandframework.keys.SimpleCloudKey;
import cloud.commandframework.meta.CommandMeta;
import cloud.commandframework.meta.SimpleCommandMeta;
import cloud.commandframework.services.State;
import io.leangen.geantyref.TypeToken;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.jetbrains.annotations.NotNull;
import snw.jkook.command.CommandSender;
import snw.jkook.message.Message;
import snw.jkook.plugin.Plugin;
import snw.kookbc.impl.message.NullMessage;

import java.util.LinkedList;
import java.util.concurrent.CompletableFuture;

/**
 * @author huanmeng_qwq
 */
public class CloudCommandManagerImpl extends CommandManager<CommandSender> {
    public static final CloudKey<Message> KOOK_MESSAGE_KEY = SimpleCloudKey.of("kook_message", TypeToken.get(Message.class));

    private final Plugin plugin;

    private final CommandContextFactory<CommandSender> commandContextFactory = new StandardCommandContextFactory<>();

    CloudCommandManagerImpl(@NonNull Plugin plugin) {
        super(CommandExecutionCoordinator.simpleCoordinator(), new CloudCommandRegistrationHandlerImpl(plugin));
        ((CloudCommandRegistrationHandlerImpl) commandRegistrationHandler()).initialize(this);
        this.plugin = plugin;
        parameterInjectorRegistry().registerInjector(Message.class, (context, annotationAccessor) -> {
            return context.getOrDefault(KOOK_MESSAGE_KEY, NullMessage.INSTANCE);
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

    public @NotNull Plugin plugin() {
        return plugin;
    }

    @NonNull
    public CompletableFuture<CommandResult<CommandSender>> executeCommand(@NonNull CommandSender commandSender, @NonNull String input, Message message) {
        final CommandContext<CommandSender> context = commandContextFactory.create(
                false,
                commandSender,
                this
        );
        context.set(KOOK_MESSAGE_KEY, message);
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
