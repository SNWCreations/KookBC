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

package snw.kookbc.impl.command;

import cloud.commandframework.arguments.parser.ParserParameters;
import cloud.commandframework.meta.CommandMeta;
import cloud.commandframework.meta.SimpleCommandMeta;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import snw.jkook.command.*;
import snw.jkook.entity.User;
import snw.jkook.entity.channel.TextChannel;
import snw.jkook.message.Message;
import snw.jkook.plugin.Plugin;
import snw.kookbc.impl.KBCClient;
import snw.kookbc.impl.command.cloud.CloudAnnotationParser;
import snw.kookbc.impl.command.cloud.CloudCommandBuilder;
import snw.kookbc.impl.command.cloud.CloudBasedCommandManager;
import snw.kookbc.impl.command.cloud.CloudCommandMap;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static snw.kookbc.util.Util.ensurePluginEnabled;
import static snw.kookbc.util.Util.toEnglishNumOrder;

public class CommandManagerImpl implements CommandManager {
    private final KBCClient client;
    protected final CommandMap commandMap;
    private final Map<Class<?>, Function<String, ?>> parsers = new ConcurrentHashMap<>();
    private final Map<Plugin, CloudBasedCommandManager> cloudCommandManagerMap = new ConcurrentHashMap<>();

    public CommandManagerImpl(KBCClient client) {
        this(client, new SimpleCommandMap());
    }

    public CommandManagerImpl(KBCClient client, CommandMap commandMap) {
        this.client = client;
        this.commandMap = commandMap;
        if (this.commandMap instanceof CloudCommandMap) {
            ((CloudCommandMap) this.commandMap).initialize(this);
        }
        registerInternalParsers();
    }

    @Override
    public void registerCommand(Plugin plugin, JKookCommand command) throws IllegalArgumentException {
        ensurePluginEnabled(plugin);
        try {
            checkCommand(command);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("The command from '" + plugin.getDescription().getName() + "' plugin does not meet our standards.", e);
        }
        commandMap.register(plugin, command);
    }

    // Throw exception if the provided command can NOT be registered
    private void checkCommand(JKookCommand command) throws IllegalArgumentException {
        if (getCommand(command.getRootName()) != null) {
            throw new IllegalArgumentException("The command with the same root name has already registered.");
        }
        List<String> duplicateNameWithPrefix = command.getPrefixes()
                .stream()
                .map(i -> i + command.getRootName())
                .collect(Collectors.toList());
        for (String head : duplicateNameWithPrefix) {
            WrappedCommand cmd = getCommandWithPrefix(head);
            if (cmd != null) {
                throw new IllegalArgumentException("The command with the same (prefix + root name) result has been found in a command from '" + cmd.getPlugin().getDescription().getName() + "' plugin.");
            }
        }

        for (String prefix : command.getPrefixes()) {
            WrappedCommand cmd = checkAliasWithPrefix(prefix, command.getAliases());
            if (cmd != null) {
                throw new IllegalArgumentException("The command with the same (prefix + alias) result has been found in a command from '" + cmd.getPlugin().getDescription().getName() + "' plugin.");
            }
        }
        for (Class<?> clazz : command.getArguments()) {
            if (parsers.get(clazz) == null) {
                throw new IllegalArgumentException("Unsupported argument type: " + clazz);
            }
        }
        for (Class<?> clazz : command.getOptionalArguments().getKeys()) {
            if (parsers.get(clazz) == null) {
                throw new IllegalArgumentException("Unsupported argument type: " + clazz);
            }
        }
    }

    // Return the conflict command object if detected confliction, otherwise null.
    private WrappedCommand checkAliasWithPrefix(String prefix, Collection<String> aliases) {
        final Map<String, WrappedCommand> view = commandMap.getView(true);
        for (String alias : aliases) {
            String head = prefix + alias;
            if (view.containsKey(head)) {
                return view.get(head);
            }
        }
        return null;
    }

    @Override
    public void registerCommand(Plugin plugin, Supplier<JKookCommand> command) throws NullPointerException, IllegalArgumentException {
        JKookCommand result = command.get();
        if (result == null) {
            throw new NullPointerException();
        }
        registerCommand(plugin, result);
    }

    @Override
    public <T> void registerArgumentParser(Class<T> clazz, Function<String, T> parser) throws IllegalStateException {
        if (parsers.get(clazz) != null) {
            throw new IllegalStateException();
        }
        parsers.put(clazz, parser);
    }

    // Cloud - Start
    public CloudBasedCommandManager getCloudCommandManager(Plugin plugin) {
        if (commandMap instanceof CloudCommandMap) {
            // 统一CommandManager
            return ((CloudCommandMap) commandMap).cloudCommandManager();
        }
        if (!cloudCommandManagerMap.containsKey(plugin)) {
            CloudBasedCommandManager commandManager = CloudCommandBuilder.createManager(plugin);
            cloudCommandManagerMap.put(plugin, commandManager);
            return commandManager;
        }
        return cloudCommandManagerMap.get(plugin);
    }

    public void registerCloudCommands(@NotNull CloudAnnotationParser annotationParser, @NotNull Plugin plugin, @NotNull Function<@NonNull ParserParameters, @NonNull CommandMeta> metaMapper) throws Exception {
        annotationParser.parse(plugin.getClass().getClassLoader());
    }

    public void registerCloudCommands(@NotNull CloudBasedCommandManager commandManager, @NotNull Plugin plugin, @NotNull Function<@NonNull ParserParameters, @NonNull CommandMeta> metaMapper) throws Exception {
        registerCloudCommands(CloudCommandBuilder.createParser(commandManager, metaMapper), plugin, metaMapper);
    }

    public void registerCloudCommands(@NotNull Plugin plugin, @NotNull Function<@NonNull ParserParameters, @NonNull CommandMeta> metaMapper) throws Exception {
        registerCloudCommands(getCloudCommandManager(plugin), plugin, metaMapper);
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
        registerCloudCommand(getCloudCommandManager(plugin), metaMapper, instance);
    }

    public void registerCloudCommand(@NotNull Plugin plugin, @NotNull Object instance) throws Exception {
        registerCloudCommand(plugin, parserParameters -> SimpleCommandMeta.empty(), instance);
    }

    public void registerCloudCommand(@NotNull CloudBasedCommandManager commandManager, @NotNull Object instance) throws Exception {
        registerCloudCommand(commandManager, parserParameters -> SimpleCommandMeta.empty(), instance);
    }

    // Cloud - End

    // this method should only be used for executing Bot commands. And the executor is Console.
    // user commands should be handled by using executeCommand0, NOT THIS
    // we hope the Bot can only execute the commands they registered. Not Internal command. It is not safe.
    // But we won't refuse the Bot to execute internal command (e.g. /stop).
    // We wish the Bot know what are they doing!
    @Override
    public boolean executeCommand(CommandSender sender, String cmdLine) throws CommandException {
        return executeCommand(sender, cmdLine, null);
    }

    // Deprecated because this method has been written into the API.
    @Deprecated
    public boolean executeCommand0(CommandSender sender, String cmdLine, Message msg) throws CommandException {
        return executeCommand(sender, cmdLine, msg);
    }

    @Override
    public boolean executeCommand(CommandSender sender, String cmdLine, Message msg) throws CommandException {
        if (cmdLine.isEmpty()) {
            client.getCore().getLogger().debug("Received empty command!");
            return false;
        }

        if (commandMap instanceof CloudCommandMap) {
            getCloudCommandManager(client.getInternalPlugin()).executeCommandNow(sender, cmdLine, msg);
            return true;
        }

        long startTimeStamp = System.currentTimeMillis(); // debug

        List<String> args = new ArrayList<>(Arrays.asList(cmdLine.split(" "))); // arguments, token " ? it's developer's work, lol
        String root = args.remove(0);
        WrappedCommand commandObject = (sender instanceof User) ? getCommandWithPrefix(root) : getCommand(root); // the root command
        if (commandObject == null) {
            if (sender instanceof ConsoleCommandSender) {
                client.getCore().getLogger().info("Unknown command. Type \"help\" for help.");
            }
            return false;
        }

        // region Plugin.isEnabled check
        Plugin owner = commandObject.getPlugin();
        if (!owner.isEnabled()) {
            reply(
                    "无法执行命令: 注册此命令的插件现已被禁用。",
                    "Unable to execute command: The owner plugin of this command was disabled.",
                    sender, msg
            );
            return false;
        }
        // endregion

        // first get commands
        Collection<JKookCommand> sub = commandObject.getCommand().getSubcommands();
        // then we should know the latest command to be executed
        // we will use the "/hello a b" as the example
        JKookCommand actualCommand = null; // "a" is an actual subcommand, so we expect it is not null
        if (!sub.isEmpty()) { // if the command have subcommand, expect true
            client.getCore().getLogger().debug("The subcommand does exists. Attempting to search the final command.");
            while (!args.isEmpty()) {
                // get the first argument, so we got "a"
                String subName = args.get(0);
                client.getCore().getLogger().debug("Got temp subcommand root name: {}", subName);

                boolean found = false; // expect true
                // then get the command
                for (JKookCommand s : sub) {
                    // if the root name equals to the sub name
                    if (Objects.equals(s.getRootName(), subName)) { // expect true
                        client.getCore().getLogger().debug("Got valid subcommand: {}", subName); // debug
                        // then remove the first argument
                        args.remove(0); // "a" was removed, so we have "b" in next round
                        actualCommand = s; // got "a" subcommand
                        found = true; // found
                        sub = actualCommand.getSubcommands(); // update the search target, so we can search deeply
                        break; // it's not necessary to continue
                    }
                }
                if (!found) { // if the subcommand is not found
                    client.getCore().getLogger().debug("No subcommand matching current command root name. We will attempt to execute the command currently found."); // debug
                    // then we can regard the actualCommand as the final result to be executed
                    break; // exit the while loop
                }
            }
        }

        client.getCore().getLogger().debug("The final command has been found. Time elasped: {}ms", System.currentTimeMillis() - startTimeStamp);

        if (sender instanceof User) {
            if (msg == null) {
                client.getCore().getLogger().warn("A user issued command but the message object is null. Is the plugin calling a command as the user?");
            }
            client.getCore().getLogger().info(
                    "{}(User ID: {}) issued command: {}",
                    ((User) sender).getName(),
                    ((User) sender).getId(),
                    cmdLine
            );
            if (sender == client.getCore().getUser()) {
                client.getCore().getLogger().warn("Running a command as the bot in this client instance. It is impossible.");
            }
        }

        // maybe some commands don't have subcommand?
        JKookCommand finalCommand = (actualCommand == null) ? commandObject.getCommand() : actualCommand;

        Object[] arguments;
        try {
            arguments = processArguments(finalCommand, args);
        } catch (NoSuchElementException e) {
            reply("执行命令失败: 参数不足。", "Unable to execute command: No enough arguments.", sender, msg);
            return false;
        } catch (UnknownArgumentException e) {
            reply(
                    "执行命令失败: 无法解析第 " + e.argIndex + " 个参数。",
                    "Unable to execute command: unable to parse the " + toEnglishNumOrder(e.argIndex) + " argument.",
                    sender, msg
            );
            return false;
        }

        // region support for the syntax sugar that added in JKook 0.24.0
        if (sender instanceof ConsoleCommandSender) {
            ConsoleCommandExecutor consoleCommandExecutor = finalCommand.getConsoleCommandExecutor();
            if (consoleCommandExecutor != null) {
                exec(
                        () -> consoleCommandExecutor.onCommand((ConsoleCommandSender) sender, arguments),
                        startTimeStamp, cmdLine
                );
                return true;
            }
        }
        if (sender instanceof User) {
            UserCommandExecutor userCommandExecutor = finalCommand.getUserCommandExecutor();
            if (userCommandExecutor != null) {
                exec(
                        () -> userCommandExecutor.onCommand((User) sender, arguments, msg),
                        startTimeStamp, cmdLine
                );
                return true;
            }
        }
        // endregion

        CommandExecutor executor = finalCommand.getExecutor();
        if (executor == null) { // no executor?
            reply(
                    "执行命令失败: 此命令已注册，但它是一个空壳，没有可用的命令逻辑。",
                    "No executor was registered for provided command line.",
                    sender, msg
            );
            return false;
        }

        // alright, it is time to execute it!
        exec(
                () -> executor.onCommand(sender, arguments, msg),
                startTimeStamp, cmdLine
        );
        return true; // ok, the command is ok, so we can return true.
    }

    public CommandMap getCommandMap() {
        return commandMap;
    }

    public Set<JKookCommand> getCommandSet() {
        return commandMap.getView(false).values().stream().map(WrappedCommand::getCommand).collect(Collectors.toSet());
    }

    public Map<String, WrappedCommand> getCommands() {
        return commandMap.getView(false);
    }

    public Map<String, WrappedCommand> getCommandWithPrefix() {
        return commandMap.getView(true);
    }

    public WrappedCommand getCommand(String rootName) {
        if (rootName.isEmpty()) return null; // do not execute invalid for loop!
        return commandMap.getView(false).get(rootName);
    }

    protected WrappedCommand getCommandWithPrefix(String cmdHeader) {
        if (cmdHeader.isEmpty()) return null; // do not execute invalid for loop!
        return commandMap.getView(true).get(cmdHeader);
    }

    public Object[] processArguments(JKookCommand command, List<String> rawArgs) {
        if (command.getArguments().isEmpty() && command.getOptionalArguments().isEmpty()) { // If this command don't want to use this feature?
            // Do nothing, but the command executor should turn the Object array into String array manually.
            return rawArgs.toArray(new String[0]);
        }
        if (rawArgs.size() < command.getArguments().size()) {
            throw new NoSuchElementException(); // no enough arguments
        }
        AtomicInteger index = new AtomicInteger();
        List<Object> args = new ArrayList<>(rawArgs.size());
        // first stage - non-optional arguments
        command.getArguments().forEach(i -> processSingleArgument(i, rawArgs.remove(0), args, index.addAndGet(1)));

        // second stage - optional arguments
        if (!command.getOptionalArguments().isEmpty()) { // still not empty? yes! optional arguments!
            int optIndex = 0;
            if (!rawArgs.isEmpty()) {
                for (Class<?> clazz : command.getOptionalArguments().getKeys()) {
                    if (rawArgs.isEmpty()) break; // no more arguments to parse
                    processSingleArgument(clazz, rawArgs.remove(0), args, index.addAndGet(1));
                    optIndex++;
                }
            }
            if (command.getOptionalArguments().size() - optIndex > 0) {
                // still some default values available?
                List<Object> defValuesCopy = new ArrayList<>(command.getOptionalArguments().getValues());
                for (int a = 1; a <= optIndex; a++) {
                    defValuesCopy.remove(0); // remove already processed items
                }
                args.addAll(defValuesCopy);
            }
        }
//        if (!rawArgs.isEmpty()) {
//            throw new IllegalStateException("Too many arguments.");
//        }
        args.addAll(rawArgs); // Add all not parsed strings to the list.
        return args.toArray();
    }

    protected void processSingleArgument(Class<?> clazz, String rawArg, List<Object> saveTo, int index) {
        Object result = parsers.get(clazz).apply(rawArg);
        if (result == null) {
            throw new UnknownArgumentException(index);
        }
        saveTo.add(result);
    }

    private void registerInternalParsers() {

        // Standard data types
        registerArgumentParser(int.class, s -> {
            try {
                return Integer.parseInt(s);
            } catch (NumberFormatException e) {
                return null;
            }
        });
        registerArgumentParser(double.class, s -> {
            try {
                return Double.parseDouble(s);
            } catch (NumberFormatException e) {
                return null;
            }
        });
        registerArgumentParser(boolean.class, s -> {
            if (s.equals("true")) {
                return true;
            } else if (s.equals("false")) {
                return false;
            } else {
                return null;
            }
        });
        registerArgumentParser(String.class, s -> s);

        // Wrapper types (Maybe someone will use these wrapper types?)
        registerArgumentParser(Integer.class, s -> {
            try {
                return Integer.parseInt(s);
            } catch (NumberFormatException e) {
                return null;
            }
        });
        registerArgumentParser(Double.class, s -> {
            try {
                return Double.parseDouble(s);
            } catch (NumberFormatException e) {
                return null;
            }
        });
        registerArgumentParser(Boolean.class, s -> {
            if (s.equals("true")) {
                return true;
            } else if (s.equals("false")) {
                return false;
            } else {
                return null;
            }
        });

        // JKook entities
        registerArgumentParser(User.class, s -> {
            if (s.startsWith("(met)") && s.endsWith("(met)")) {
                return client.getStorage().getUser(s.substring(5, s.length() - 5));
            } else {
                return null;
            }
        });
        registerArgumentParser(TextChannel.class, s -> {
            if (s.startsWith("(chn)") && s.endsWith("(chn)")) {
                return (TextChannel) client.getStorage().getChannel(s.substring(5, s.length() - 5));
            } else {
                return null;
            }
        });
    }

    // execute the runnable, if it fails, a CommandException will be thrown
    private void exec(Runnable runnable, long startTimeStamp, String cmdLine) throws CommandException {
        try {
            runnable.run();
        } catch (Throwable e) {
            client.getCore().getLogger().debug("The execution of command line '{}' is FAILED, time elapsed: {}ms", cmdLine, System.currentTimeMillis() - startTimeStamp);
            // Why Throwable? We need to keep the client safe.
            // it is easy to understand. NoClassDefError? NoSuchMethodError?
            // It is OutOfMemoryError? nothing matters lol.
            throw new CommandException("Something unexpected happened.", e);
        }
        // Do not put this in the try statement because we don't know if the logging system will throw an exception.
        client.getCore().getLogger().debug("The execution of command line \"{}\" is done, time elapsed: {}ms", cmdLine, System.currentTimeMillis() - startTimeStamp);
    }

    private void reply(String content, String contentForConsole, CommandSender sender, @Nullable Message message) {
        if (sender instanceof ConsoleCommandSender) {
            client.getCore().getLogger().info(contentForConsole);
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
