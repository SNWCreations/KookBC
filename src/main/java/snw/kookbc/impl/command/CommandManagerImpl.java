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
import java.util.concurrent.TimeUnit;

public class CommandManagerImpl implements CommandManager {
    private final KBCClient client;
    private final ArrayList<JKookCommand> commands = new ArrayList<>();

    public CommandManagerImpl(KBCClient client) {
        this.client = client;
    }

    @Override
    public void registerCommand(JKookCommand command) throws IllegalArgumentException {
        for (String p : command.getPrefixes()) {
            for (JKookCommand cmd : commands) {
                for (String p2 : cmd.getPrefixes()) {
                    if (Objects.equals(p + command.getRootName(), p2 + cmd.getRootName())) {
                        throw new IllegalArgumentException(String.format("Conflict rootname-with-prefix detected between registered command %s and incoming %s", cmd.getRootName(), command.getRootName()));
                    }
                }
            }
        }
        if (getCommand(command.getRootName()) != null
                ||
                commands.stream().anyMatch(
                        IT -> IT.getAliases().stream().anyMatch(C -> Objects.equals(C, IT.getRootName()))
                )
        ) {
            throw new IllegalArgumentException("The command with the same root name (or alias) has already registered.");
        }
        commands.add(command);
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
        client.getCore().getLogger().info(
                "{} issued command: {}",
                (sender instanceof User ? ((User) sender).getId() : (sender instanceof ConsoleCommandSender ? "Console" : "UNKNOWN")),
                cmdLine
        );

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

    public ArrayList<JKookCommand> getCommands() {
        return commands;
    }

    public JKookCommand getCommand(String rootName) {
        if (rootName.isEmpty()) return null; // do not execute invalid for loop!
        for (JKookCommand command : commands) {
            if (Objects.equals(command.getRootName(), rootName)) {
                return command;
            }
        }
        return null;
    }

    protected JKookCommand getCommandWithPrefix(String cmdHeader) {
        if (cmdHeader.isEmpty()) return null; // do not execute invalid for loop!
        for (JKookCommand command : commands) {
            if (command.getPrefixes().stream().anyMatch(IT -> Objects.equals(IT + command.getRootName(), cmdHeader))) {
                return command;
            }
        }
        return null;
    }
}
