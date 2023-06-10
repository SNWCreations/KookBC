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

package snw.kookbc.impl.console;

import net.minecrell.terminalconsole.SimpleTerminalConsole;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import snw.kookbc.SharedConstants;
import snw.kookbc.impl.KBCClient;
import java.nio.file.Paths;

public class Console extends SimpleTerminalConsole {
    private final KBCClient client;

    public Console(KBCClient client) {
        this.client = client;
    }

    @Override
    protected boolean isRunning() {
        return client.isRunning();
    }

    @Override
    protected void runCommand(String s) {
        client.getCore().getLogger().info("Console issued command: {}", s);
        boolean foundCommand = true;
        try {
            foundCommand = client.getCore().getCommandManager().executeCommand(client.getCore().getConsoleCommandSender(), s);
        } catch (Exception e) {
            client.getCore().getLogger().error("Unexpected situation happened during the execution of the command.", e);
        }
        if (!foundCommand) {
            client.getCore().getLogger().info("Unknown command. Type \"/help\" for help.");
        }
    }

    @Override
    protected void shutdown() {
        client.getCore().getLogger().debug("Got shutdown request from console! Stopping!");
        client.shutdown();
    }

    @Override
    protected LineReader buildReader(LineReaderBuilder builder) {
        builder.appName(SharedConstants.IMPL_NAME);
        if (client.getConfig().getBoolean("save-console-history", true)) {
            builder.variable("history-file", Paths.get(".console_history"));
        }
        return super.buildReader(builder);
    }
}
