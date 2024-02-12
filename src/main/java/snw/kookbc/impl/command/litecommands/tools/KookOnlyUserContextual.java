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
import snw.jkook.entity.User;

import java.util.function.Supplier;

public class KookOnlyUserContextual<MESSAGE> implements ContextProvider<CommandSender, User> {

    private final Supplier<MESSAGE> onlyUserMessage;

    public KookOnlyUserContextual(Supplier<MESSAGE> onlyUserMessage) {
        this.onlyUserMessage = onlyUserMessage;
    }

    public KookOnlyUserContextual(MESSAGE onlyUserMessage) {
        this(() -> onlyUserMessage);
    }

    @Override
    public ContextResult<User> provide(Invocation<CommandSender> invocation) {
        if (invocation.sender() instanceof User) {
            return ContextResult.ok(() -> (User) invocation.sender());
        }

        return ContextResult.error(onlyUserMessage.get());
    }

}