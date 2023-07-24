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

import cloud.commandframework.annotations.AnnotationParser;
import cloud.commandframework.arguments.parser.ParserParameters;
import cloud.commandframework.arguments.parser.StandardParameters;
import cloud.commandframework.meta.CommandMeta;
import cloud.commandframework.meta.SimpleCommandMeta;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import snw.jkook.command.CommandException;
import snw.jkook.command.CommandSender;
import snw.jkook.message.Message;
import snw.jkook.plugin.Plugin;
import snw.kookbc.impl.KBCClient;
import snw.kookbc.impl.command.CommandManagerImpl;

import java.util.List;
import java.util.function.Function;

import static snw.kookbc.impl.command.cloud.CloudConstants.PLUGIN_KEY;

/**
 * @author huanmeng_qwq
 */
public class CloudCommandManagerImpl extends CommandManagerImpl {
    private final CloudBasedCommandManager manager;

    public CloudCommandManagerImpl(KBCClient client) {
        this(client, new CloudCommandMap());
        getCommandMap().init(this);
    }

    public CloudCommandManagerImpl(KBCClient client, CloudCommandMap commandMap) {
        super(client, commandMap);
        this.manager = new CloudBasedCommandManager(client, this);
    }

    @Override
    public boolean executeCommand(CommandSender sender, String cmdLine, Message msg) throws CommandException {
        if (cmdLine.isEmpty()) {
            client.getCore().getLogger().debug("Received empty command!");
            return false;
        }

        long startTimeStamp = System.currentTimeMillis(); // debug

        boolean result;
        try {
            result = getCloudCommandManager().executeCommandNow(sender, cmdLine, msg);
        } catch (Throwable e) {
            client.getCore().getLogger().debug("The execution of command line '{}' is FAILED, time elapsed: {}ms", cmdLine, System.currentTimeMillis() - startTimeStamp);
            // Why Throwable? We need to keep the client safe.
            // it is easy to understand. NoClassDefError? NoSuchMethodError?
            // It is OutOfMemoryError? nothing matters lol.
            throw new CommandException("Something unexpected happened.", e);
        }
        // Do not put this in the try statement because we don't know if the logging system will throw an exception.
        client.getCore().getLogger().debug("The execution of command line \"{}\" is done, time elapsed: {}ms", cmdLine, System.currentTimeMillis() - startTimeStamp);
        return result;
    }

    public CloudBasedCommandManager getCloudCommandManager() {
        return manager;
    }

    public void registerCloudCommands(@NotNull AnnotationParser<CommandSender> annotationParser, @NotNull Plugin plugin) {
        annotationParser.parse(plugin.getClass().getClassLoader());
    }

    public void registerCloudCommands(@NotNull CloudBasedCommandManager commandManager, @NotNull Plugin plugin, @Nullable Function<@NonNull ParserParameters, @NonNull CommandMeta> metaMapper) {
        Function<ParserParameters, CommandMeta> wrapped = wrap(plugin, metaMapper);
        registerCloudCommands(CloudCommandBuilder.createParser(commandManager, wrapped), plugin);
    }

    public void registerCloudCommands(@NotNull Plugin plugin, @Nullable Function<@NonNull ParserParameters, @NonNull CommandMeta> metaMapper) {
        registerCloudCommands(getCloudCommandManager(), plugin, metaMapper);
    }

    public void registerCloudCommands(@NotNull Plugin plugin) {
        registerCloudCommands(plugin, null);
    }

    public void registerCloudCommand(@NotNull Plugin plugin, @NotNull CloudBasedCommandManager commandManager, @Nullable Function<@NonNull ParserParameters, @NonNull CommandMeta> metaMapper, @NotNull Object instance) {
        CloudCommandBuilder.createParser(commandManager, wrap(plugin, metaMapper)).parse(instance);
    }

    public void registerCloudCommand(@NotNull AnnotationParser<CommandSender> annotationParser, @NotNull Object instance) {
        annotationParser.parse(instance);
    }

    public void registerCloudCommand(@NotNull Plugin plugin, @Nullable Function<@NonNull ParserParameters, @Nullable CommandMeta> metaMapper, @NotNull Object instance) {
        registerCloudCommand(plugin, getCloudCommandManager(), metaMapper, instance);
    }

    public void registerCloudCommand(@NotNull Plugin plugin, @NotNull Object instance) {
        registerCloudCommand(plugin, (Function<ParserParameters, CommandMeta>) null, instance);
    }

    public void registerCloudCommand(@NotNull Plugin plugin, @NotNull CloudBasedCommandManager commandManager, @NotNull Object instance) {
        registerCloudCommand(plugin, commandManager, null, instance);
    }

    @Override
    public CloudCommandMap getCommandMap() {
        return (CloudCommandMap) super.getCommandMap();
    }

    protected static Function<ParserParameters, CommandMeta> wrap(Plugin plugin, Function<ParserParameters, CommandMeta> origin) {
        return p -> {
            SimpleCommandMeta.Builder builder = SimpleCommandMeta.builder()
                    .with(PLUGIN_KEY, plugin)
                    .with(CommandMeta.DESCRIPTION, p.get(StandardParameters.DESCRIPTION, ""));
            if (origin != null) {
                builder.with(origin.apply(p));
            }
            return builder.build();
        };
    }

    public List<CloudCommandInfo> getCommandsInfo() {
        return getCloudCommandManager().getCommandsInfo();
    }
}
