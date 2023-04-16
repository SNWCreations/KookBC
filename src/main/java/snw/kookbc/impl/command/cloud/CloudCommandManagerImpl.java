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

import cloud.commandframework.arguments.parser.ParserParameters;
import cloud.commandframework.meta.CommandMeta;
import cloud.commandframework.meta.SimpleCommandMeta;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.jetbrains.annotations.NotNull;
import snw.jkook.command.CommandException;
import snw.jkook.command.CommandSender;
import snw.jkook.message.Message;
import snw.jkook.plugin.Plugin;
import snw.kookbc.impl.KBCClient;
import snw.kookbc.impl.command.CommandManagerImpl;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public class CloudCommandManagerImpl extends CommandManagerImpl {
    public static final CommandMeta.Key<Plugin> PLUGIN_KEY = CommandMeta.Key.of(Plugin.class, "jkook_plugin");
    private final CloudBasedCommandManager manager;
    private final Map<Plugin, CloudBasedCommandManager> cloudCommandManagerMap = new ConcurrentHashMap<>();

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
//        String head = cmdLine.contains(" ") ? cmdLine.substring(0, cmdLine.indexOf(" ")) : cmdLine;
//        WrappedCommand wrapped = sender instanceof ConsoleCommandSender ? getCommand(head) : getCommandWithPrefix(head);
//        if (wrapped == null) {
//            // TODO if cloud? find the cloud command manager through the command line
//            return false;
//        }
        return getCloudCommandManager().executeCommandNow(sender, cmdLine, msg);
    }

    public CloudBasedCommandManager getCloudCommandManager() {
        return manager;
        // return cloudCommandManagerMap.computeIfAbsent(plugin, i -> new CloudBasedCommandManager(client, this, plugin));
    }

    public void registerCloudCommands(@NotNull CloudAnnotationParser annotationParser, @NotNull Plugin plugin, @NotNull Function<@NonNull ParserParameters, @NonNull CommandMeta> metaMapper) throws Exception {
        annotationParser.parse(plugin.getClass().getClassLoader());
    }

    public void registerCloudCommands(@NotNull CloudBasedCommandManager commandManager, @NotNull Plugin plugin, @NotNull Function<@NonNull ParserParameters, @NonNull CommandMeta> metaMapper) throws Exception {
        Function<ParserParameters, CommandMeta> wrapped = wrap(plugin, metaMapper);
        registerCloudCommands(CloudCommandBuilder.createParser(commandManager, wrapped), plugin, wrapped);
    }

    public void registerCloudCommands(@NotNull Plugin plugin, @NotNull Function<@NonNull ParserParameters, @NonNull CommandMeta> metaMapper) throws Exception {
        registerCloudCommands(getCloudCommandManager(), plugin, metaMapper);
    }

    public void registerCloudCommands(@NotNull Plugin plugin) throws Exception {
        registerCloudCommands(plugin, parserParameters -> SimpleCommandMeta.empty());
    }

    public void registerCloudCommand(@NotNull CloudBasedCommandManager commandManager, @NotNull Function<@NonNull ParserParameters, @NonNull CommandMeta> metaMapper, @NotNull Object instance) throws Exception {
        CloudCommandBuilder.createParser(commandManager, metaMapper).parse(instance);
    }

    public void registerCloudCommand(@NotNull CloudAnnotationParser annotationParser, @NotNull Object instance) throws Exception {
        annotationParser.parse(instance);
    }

    public void registerCloudCommand(@NotNull Plugin plugin, @NotNull Function<@NonNull ParserParameters, @NonNull CommandMeta> metaMapper, @NotNull Object instance) throws Exception {
        registerCloudCommand(getCloudCommandManager(), metaMapper, instance);
    }

    public void registerCloudCommand(@NotNull Plugin plugin, @NotNull Object instance) throws Exception {
        registerCloudCommand(plugin, parserParameters -> SimpleCommandMeta.empty(), instance);
    }

    public void registerCloudCommand(@NotNull CloudBasedCommandManager commandManager, @NotNull Object instance) throws Exception {
        registerCloudCommand(commandManager, parserParameters -> SimpleCommandMeta.empty(), instance);
    }

    @Override
    public CloudCommandMap getCommandMap() {
        return (CloudCommandMap) super.getCommandMap();
    }

    protected static Function<ParserParameters, CommandMeta> wrap(Plugin plugin, Function<ParserParameters, CommandMeta> origin) {
        return p -> SimpleCommandMeta.builder().with(PLUGIN_KEY, plugin).with(origin.apply(p)).build();
    }
}
