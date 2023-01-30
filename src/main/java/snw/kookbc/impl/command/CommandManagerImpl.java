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

import snw.jkook.command.*;
import snw.jkook.entity.User;
import snw.jkook.message.Message;
import snw.jkook.message.component.MarkdownComponent;
import snw.kookbc.impl.KBCClient;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class CommandManagerImpl implements CommandManager {
    private final KBCClient client;
    // The following comments will tell you the difference between the following two member variables.
    // For example, there is a command called "hello"
    // Its prefixes are "/", "."
    // So the commands Map will contain a key called "hello"
    // But the commandWithPrefix Map will contain two keys, "/hello", ".hello"
    private final Map<String, JKookCommand> commands = new ConcurrentHashMap<>();
    private final Map<String, JKookCommand> commandWithPrefix = new ConcurrentHashMap<>();

    public CommandManagerImpl(KBCClient client) {
        this.client = client;
    }

    @Override
    public void registerCommand(JKookCommand command) throws IllegalArgumentException {
        checkCommand(command);
        Collection<String> allCommandHeader = getAllCommandHeader(command);
        commands.put(command.getRootName(), command);
        allCommandHeader.forEach(i -> commandWithPrefix.put(i, command));
    }

    // Return true if this command can be registered
    private void checkCommand(JKookCommand command) throws IllegalArgumentException {
        if (getCommand(command.getRootName()) != null) {
            throw new IllegalArgumentException("The command with the same root name has already registered.");
        }
        boolean duplicateNameWithPrefix = command.getPrefixes()
                .stream()
                .map(i -> i + command.getRootName())
                .anyMatch(commandWithPrefix::containsKey);
        if (duplicateNameWithPrefix) {
            throw new IllegalArgumentException("The command with the same (prefix + root name) result has already registered.");
        }
        boolean duplicateAliasWithPrefix = command.getPrefixes()
                .stream()
                .anyMatch(i -> !checkAliasWithPrefix(i, command.getAliases()));
        if (duplicateAliasWithPrefix) {
            throw new IllegalArgumentException("The command with the same (prefix + alias) result has already registered.");
        }
    }

    // Return true if there is no conflict with the prefix and the aliases in the provided command
    private boolean checkAliasWithPrefix(String prefix, Collection<String> aliases) {
        for (String alias : aliases) {
            if (commandWithPrefix.containsKey(prefix + alias)) {
                return false;
            }
        }
        return true;
    }

    private Collection<String> getAllCommandHeader(JKookCommand command) {
        Collection<String> result = new ArrayList<>(command.getPrefixes().size() * (command.getAliases().size() + 1));
        for (String prefix : command.getPrefixes()) {
            result.add(prefix + command.getRootName());
            for (String alias : command.getAliases()) {
                result.add(prefix + alias);
            }
        }
        return result;
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
        Collection<JKookCommand> sub = commandObject.getSubcommands();
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
        JKookCommand finalCommand = (actualCommand == null) ? commandObject : actualCommand;

        // region support for the syntax sugar that added in JKook 0.24.0
        if (sender instanceof ConsoleCommandSender) {
            ConsoleCommandExecutor consoleCommandExecutor = finalCommand.getConsoleCommandExecutor();
            if (consoleCommandExecutor != null) {
                try {
                    consoleCommandExecutor.onCommand((ConsoleCommandSender) sender, args.toArray(new String[0]));
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
                    userCommandExecutor.onCommand((User) sender, args.toArray(new String[0]), msg);
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
            executor.onCommand(sender, args.toArray(new String[0]), msg);
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

    public Collection<JKookCommand> getCommands() {
        return commands.values();
    }

    public JKookCommand getCommand(String rootName) {
        if (rootName.isEmpty()) return null;
        return commands.get(rootName);
    }

    protected JKookCommand getCommandWithPrefix(String cmdHeader) {
        if (cmdHeader.isEmpty()) return null; // do not execute invalid for loop!
        return commandWithPrefix.get(cmdHeader);
    }
}
