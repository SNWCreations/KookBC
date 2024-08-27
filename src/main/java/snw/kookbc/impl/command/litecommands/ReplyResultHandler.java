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

package snw.kookbc.impl.command.litecommands;

import dev.rollczi.litecommands.handler.result.ResultHandler;
import dev.rollczi.litecommands.handler.result.ResultHandlerChain;
import dev.rollczi.litecommands.invocation.Invocation;
import snw.jkook.command.CommandSender;
import snw.jkook.message.Message;
import snw.kookbc.impl.command.litecommands.result.ResultType;
import snw.kookbc.impl.command.litecommands.result.ResultTypes;

public class ReplyResultHandler<T> implements ResultHandler<CommandSender, T> {
    @Override
    public void handle(Invocation<CommandSender> invocation, T result, ResultHandlerChain<CommandSender> chain) {
        Message message = invocation.context().get(Message.class).orElse(null);
        ResultType resultType = invocation.context().get(ResultType.class).orElse(ResultTypes.REPLY);// if set null?
        resultType.message(invocation, message, result);
    }

}