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

package snw.kookbc.impl.command.litecommands.tools;

import dev.rollczi.litecommands.context.ContextProvider;
import dev.rollczi.litecommands.context.ContextResult;
import dev.rollczi.litecommands.invocation.Invocation;
import snw.jkook.command.CommandSender;
import snw.jkook.command.ConsoleCommandSender;

import java.util.function.Supplier;

public class KookOnlyConsoleContextual<MESSAGE> implements ContextProvider<CommandSender, ConsoleCommandSender> {

    private final Supplier<MESSAGE> onlyConsoleMessage;

    public KookOnlyConsoleContextual(Supplier<MESSAGE> onlyConsoleMessage) {
        this.onlyConsoleMessage = onlyConsoleMessage;
    }

    public KookOnlyConsoleContextual(MESSAGE onlyConsoleMessage) {
        this(() -> onlyConsoleMessage);
    }

    @Override
    public ContextResult<ConsoleCommandSender> provide(Invocation<CommandSender> invocation) {
        if (invocation.sender() instanceof ConsoleCommandSender) {
            return ContextResult.ok(() -> (ConsoleCommandSender) invocation.sender());
        }

        return ContextResult.error(onlyConsoleMessage.get());
    }

}