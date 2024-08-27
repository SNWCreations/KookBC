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

package snw.kookbc.impl.command.litecommands.result;

import dev.rollczi.litecommands.invocation.Invocation;
import snw.jkook.command.CommandSender;
import snw.jkook.message.Message;

public class ExecuteResultType implements ResultType {
    private ResultType resultType;

    public ExecuteResultType(ResultType resultType) {
        this.resultType = resultType;
    }

    public void setResultType(ResultType resultType) {
        this.resultType = resultType;
    }

    public ResultType getResultType() {
        return resultType;
    }

    @Override
    public void message(Invocation<CommandSender> invocation, Message message, Object result) {
        if (resultType == null) {
            ResultTypes.REPLY.message(invocation, message, result);
        } else {
            resultType.message(invocation, message, result);
        }
    }

}
