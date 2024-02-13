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
import snw.jkook.command.ConsoleCommandSender;
import snw.jkook.entity.User;
import snw.jkook.message.Message;

class StringHandler implements ResultHandler<CommandSender, String> {

    @Override
    public void handle(Invocation<CommandSender> invocation, String result, ResultHandlerChain<CommandSender> chain) {
        CommandSender sender = invocation.sender();
        Message message = invocation.context().get(Message.class).orElse(null);
        if (sender instanceof User) {
            if (message != null) {
                message.reply(result);
            }
        } else if (sender instanceof ConsoleCommandSender) {
            ((ConsoleCommandSender) sender).getLogger().info("The execution result of command {}: {}", invocation.name(), result);
        } else {
            throw new IllegalStateException("Unknown sender type: " + sender.getClass().getName());
        }
    }

}