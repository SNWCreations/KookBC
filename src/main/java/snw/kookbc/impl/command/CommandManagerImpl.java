/*
 *     KookBC -- The Kook Bot Client & JKook API standard implementation for Java.
 *     Copyright (C) 2022 KookBC contributors
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

import snw.jkook.command.*;
import snw.jkook.entity.User;
import snw.jkook.entity.channel.TextChannel;
import snw.jkook.message.Message;
import snw.jkook.message.component.MarkdownComponent;
import snw.jkook.plugin.Plugin;
import snw.jkook.util.Validate;
import snw.kookbc.impl.KBCClient;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;

import static snw.kookbc.util.Util.toEnglishNumOrder;
import static snw.kookbc.util.Util.ensurePluginEnabled;

public class CommandManagerImpl implements CommandManager {
    private final KBCClient client;
    private final Map<JKookCommand, Plugin> commands = new HashMap<>();
    private final Map<Class<?>, Function<String, ?>> parsers = new HashMap<>();

    public CommandManagerImpl(KBCClient client) {
        this.client = client;
        registerInternalParsers();
    }

    @Override
    public void registerCommand(Plugin plugin, JKookCommand command) throws IllegalArgumentException {
        ensurePluginEnabled(plugin); // null plugin is unsupported, but internal commands are allowed.
        Validate.isFalse(command.getOptionalArguments().containsKey(Object.class), "Optional arguments cannot contain java.lang.Object class (excluding its subclasses)");
        if (getCommand(command.getRootName()) != null
                ||
                commands.keySet().stream().anyMatch(
                        IT -> IT.getAliases().stream().anyMatch(C -> Objects.equals(C, IT.getRootName()))
                )
        ) {
            throw new IllegalArgumentException("The command with the same root name (or alias) has already registered.");
        }
        for (Class<?> clazz : command.getArguments()) {
            if (parsers.get(clazz) == null) {
                throw new IllegalArgumentException("Unsupported argument type: " + clazz);
            }
        }
        commands.put(command, plugin);
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

    // this method should only be used for executing Bot commands. And the executor is Console.
    // user commands should be handled by using executeCommand0, NOT THIS
    // we hope the Bot can only execute the commands they registered. Not Internal command. It is not safe.
    // But we won't refuse the Bot to execute internal command (e.g. /stop).
    // We wish the Bot know what are they doing!
    @Override
    public boolean executeCommand(CommandSender sender, String cmdLine) throws CommandException {
        return executeCommand0(sender, cmdLine, null);
    }

    public boolean executeCommand0(CommandSender sender, String cmdLine, Message msg) throws CommandException {
        client.getCore().getLogger().debug("A command execution request has received. Command line: {}", cmdLine);
        if (cmdLine.isEmpty()) {
            client.getCore().getLogger().debug("Received empty command!");
            return false;
        }
        long startTimeStamp = System.currentTimeMillis(); // debug

        List<String> args = new ArrayList<>(Arrays.asList(cmdLine.split(" "))); // arguments, token " ? it's developer's work, lol
        String root = args.remove(0);
        JKookCommand commandObject = (sender instanceof User) ? getCommandWithPrefix(root) : getCommand(root); // the root command
        if (commandObject == null) {
            if (sender instanceof ConsoleCommandSender) {
                client.getCore().getLogger().info("Unknown command. Type \"help\" for help.");
            }
            return false;
        }

        // first get commands
        List<JKookCommand> sub = (List<JKookCommand>) commandObject.getSubcommands();
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

        // maybe some commands don't have subcommand?
        JKookCommand finalCommand = (actualCommand == null) ? commandObject : actualCommand;

        Object[] arguments;
        try {
            arguments = processArguments(commandObject, args);
        } catch (NoSuchElementException e) {
            if (sender instanceof ConsoleCommandSender) {
                client.getCore().getLogger().info("Unable to execute command: No enough arguments.");
            } else {
                if (msg != null) {
                    msg.reply(new MarkdownComponent("执行命令失败：参数不足。"));
                } else {
                    if (sender instanceof User) {
                        ((User) sender).sendPrivateMessage(new MarkdownComponent("执行命令失败：参数不足。"));
                    }
                }
            }
            return false;
        } catch (UnknownArgumentException e) {
            if (sender instanceof ConsoleCommandSender) {
                client.getCore().getLogger().info("Unable to execute command: unable to parse the " + toEnglishNumOrder(e.argIndex) + " argument.");
            } else {
                if (msg != null) {
                    msg.reply(new MarkdownComponent("执行命令时失败：无法解析第 " + e.argIndex + " 个参数。"));
                } else {
                    if (sender instanceof User) {
                        ((User) sender).sendPrivateMessage(new MarkdownComponent("执行命令时失败：无法解析第 " + e.argIndex + " 个参数。"));
                    }
                }
            }
            return false;
        }

        // region support for the syntax sugar that added in JKook 0.24.0
        if (sender instanceof ConsoleCommandSender) {
            ConsoleCommandExecutor consoleCommandExecutor = finalCommand.getConsoleCommandExecutor();
            if (consoleCommandExecutor != null) {
                try {
                    consoleCommandExecutor.onCommand((ConsoleCommandSender) sender, arguments);
                    return true; // prevent CommandExecutor execution.
                } catch (Throwable e) {
                    client.getCore().getLogger().debug("The execution of command line {} is FAILED, time elapsed: {}", cmdLine, TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - startTimeStamp)); // debug, so ignore it
                    throw new CommandException("Something unexpected happened.", e);
                }
            }
        }
        if (sender instanceof User) {
            UserCommandExecutor userCommandExecutor = finalCommand.getUserCommandExecutor();
            if (userCommandExecutor != null) {
                try {
                    userCommandExecutor.onCommand((User) sender, arguments, msg);
                    return true; // prevent CommandExecutor execution.
                } catch (Throwable e) {
                    client.getCore().getLogger().debug("The execution of command line {} is FAILED, time elapsed: {}", cmdLine, TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - startTimeStamp)); // debug, so ignore it
                    throw new CommandException("Something unexpected happened.", e);
                }
            }
        }
        // endregion

        CommandExecutor executor = finalCommand.getExecutor();
        if (executor == null) { // no executor?
            if (sender instanceof ConsoleCommandSender) {
                client.getCore().getLogger().info("No executor was registered for provided command line.");
            } // do nothing! lol
            else {
                if (sender instanceof User) {
                    if (msg != null) {
                        msg.reply(new MarkdownComponent("此命令没有对应的执行器。"));
                    } else {
                        ((User) sender).sendPrivateMessage(new MarkdownComponent("此命令没有对应的执行器。"));
                    }
                }
            }
            return false;
        }

        // alright, it is time to execute it!
        try {
            executor.onCommand(sender, arguments, msg);
        } catch (Throwable e) {
            client.getCore().getLogger().debug("The execution of command line {} is FAILED, time elapsed: {}", cmdLine, TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - startTimeStamp)); // debug, so ignore it
            // Why Throwable? We need to keep the client safe.
            // it is easy to understand. NoClassDefError? NoSuchMethodError?
            // It is OutOfMemoryError? nothing matters lol.
            throw new CommandException("Something unexpected happened.", e);
        }

        client.getCore().getLogger().debug("The execution of command line \"{}\" is done, time elapsed: {}", cmdLine, TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - startTimeStamp)); // debug, so ignore it

        return true; // ok, the command is ok, so we can return true.
    }

    public Set<JKookCommand> getCommandSet() {
        return commands.keySet();
    }

    public Map<JKookCommand, Plugin> getCommands() {
        return commands;
    }

    public JKookCommand getCommand(String rootName) {
        if (rootName.isEmpty()) return null; // do not execute invalid for loop!
        for (JKookCommand command : commands.keySet()) {
            if (Objects.equals(command.getRootName(), rootName)) {
                return command;
            }
        }
        return null;
    }

    protected JKookCommand getCommandWithPrefix(String cmdHeader) {
        if (cmdHeader.isEmpty()) return null; // do not execute invalid for loop!
        for (JKookCommand command : commands.keySet()) {
            if (command.getPrefixes().stream().anyMatch(IT -> Objects.equals(IT + command.getRootName(), cmdHeader))) {
                return command;
            }
        }
        return null;
    }
    
    protected Object[] processArguments(JKookCommand command, List<String> rawArgs) {
        if (command.getArguments().isEmpty()) { // If this command don't want to use this feature?
            // Do nothing, but the command executor should turn the Object array into String array manually.
            return rawArgs.toArray();
        }
        if (rawArgs.size() < command.getArguments().size()) {
            throw new NoSuchElementException(); // no enough arguments
        }
        List<Object> args = new ArrayList<>(rawArgs.size());
        Iterator<String> iterator = rawArgs.iterator();
        int index = 0;
        while (iterator.hasNext()) {
            index++;
            String rawArg = iterator.next();
            for (Class<?> clazz : command.getArguments()) {
                Function<String, ?> parser = parsers.get(clazz);
                Object result = parser.apply(rawArg);
                if (result == null) {
                    throw new UnknownArgumentException(index); // We can't understand this argument!
                }
                args.add(result);
                iterator.remove();
            }
        }
//        if (!rawArgs.isEmpty()) {
//            throw new IllegalStateException("Too many arguments.");
//        }
        args.addAll(rawArgs); // Add all not parsed strings to the list.
        return args.toArray();
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
}

final class UnknownArgumentException extends RuntimeException {
    final int argIndex;

    UnknownArgumentException(int argIndex) {
        this.argIndex = argIndex;
    }
}