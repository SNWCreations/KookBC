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
import cloud.commandframework.meta.CommandMeta;
import cloud.commandframework.meta.SimpleCommandMeta;
import io.leangen.geantyref.TypeToken;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.jetbrains.annotations.NotNull;
import snw.jkook.command.CommandException;
import snw.jkook.command.CommandSender;
import snw.jkook.message.Message;
import snw.jkook.plugin.Plugin;
import snw.kookbc.impl.KBCClient;
import snw.kookbc.impl.command.CommandManagerImpl;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;

/**
 * @author huanmeng_qwq
 */
public class CloudCommandManagerImpl extends CommandManagerImpl {
    public static final CommandMeta.Key<Plugin> PLUGIN_KEY = CommandMeta.Key.of(Plugin.class, "kookbc_plugin");
    public static final CommandMeta.Key<Collection<String>> PREFIX_KEY = CommandMeta.Key.of(new TypeToken<Collection<String>>() {
    }, "kookbc_prefixes");
    public static final CommandMeta.Key<Collection<String>> ALIAS_KEY = CommandMeta.Key.of(new TypeToken<Collection<String>>() {
    }, "kookbc_aliases");
    public static final CommandMeta.Key<Boolean> JKOOK_COMMAND_KEY = CommandMeta.Key.of(Boolean.class, "is_jkook_command");
    public static final CommandMeta.Key<String> HELP_CONTENT_KEY = CommandMeta.Key.of(String.class, "jkook_help_content");
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

    public void registerCloudCommands(@NotNull AnnotationParser<CommandSender> annotationParser, @NotNull Plugin plugin, @NotNull Function<@NonNull ParserParameters, @NonNull CommandMeta> metaMapper) {
        annotationParser.parse(plugin.getClass().getClassLoader(), plugin);
    }

    public void registerCloudCommands(@NotNull CloudBasedCommandManager commandManager, @NotNull Plugin plugin, @NotNull Function<@NonNull ParserParameters, @NonNull CommandMeta> metaMapper) {
        Function<ParserParameters, CommandMeta> wrapped = wrap(plugin, metaMapper);
        registerCloudCommands(CloudCommandBuilder.createParser(commandManager, wrapped), plugin, wrapped);
    }

    public void registerCloudCommands(@NotNull Plugin plugin, @NotNull Function<@NonNull ParserParameters, @NonNull CommandMeta> metaMapper) {
        registerCloudCommands(getCloudCommandManager(), plugin, metaMapper);
    }

    public void registerCloudCommands(@NotNull Plugin plugin) {
        registerCloudCommands(plugin, parserParameters -> SimpleCommandMeta.empty());
    }

    public void registerCloudCommand(@NotNull Plugin plugin, @NotNull CloudBasedCommandManager commandManager, @NotNull Function<@NonNull ParserParameters, @NonNull CommandMeta> metaMapper, @NotNull Object instance) {
        CloudCommandBuilder.createParser(commandManager, metaMapper).parse(instance, plugin);
    }

    public void registerCloudCommand(@NotNull Plugin plugin, @NotNull AnnotationParser<CommandSender> annotationParser, @NotNull Object instance) {
        annotationParser.parse(instance, plugin);
    }

    public void registerCloudCommand(@NotNull Plugin plugin, @NotNull Function<@NonNull ParserParameters, @NonNull CommandMeta> metaMapper, @NotNull Object instance) {
        registerCloudCommand(plugin, getCloudCommandManager(), metaMapper, instance);
    }

    public void registerCloudCommand(@NotNull Plugin plugin, @NotNull Object instance) {
        registerCloudCommand(plugin, parserParameters -> SimpleCommandMeta.empty(), instance);
    }

    public void registerCloudCommand(@NotNull Plugin plugin, @NotNull CloudBasedCommandManager commandManager, @NotNull Object instance) {
        registerCloudCommand(plugin, commandManager, parserParameters -> SimpleCommandMeta.empty(), instance);
    }

    @Override
    public CloudCommandMap getCommandMap() {
        return (CloudCommandMap) super.getCommandMap();
    }

    protected static Function<ParserParameters, CommandMeta> wrap(Plugin plugin, Function<ParserParameters, CommandMeta> origin) {
        return p -> SimpleCommandMeta.builder().with(PLUGIN_KEY, plugin).with(origin.apply(p)).build();
    }

    public List<CloudCommandInfo> getCommandsInfo() {
        return getCloudCommandManager().getCommandsInfo();
    }
}
