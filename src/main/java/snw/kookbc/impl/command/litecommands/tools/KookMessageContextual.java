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
import snw.jkook.message.Message;

import java.util.Optional;

/**
 * 2024/2/12<br>
 * KookBC<br>
 *
 * @author huanmeng_qwq
 */
public class KookMessageContextual implements ContextProvider<CommandSender, Message> {
    @Override
    public ContextResult<Message> provide(Invocation<CommandSender> invocation) {
        Optional<Message> message = Optional.empty();
        try {
            message = invocation.context().get(Message.class);
        } catch (NullPointerException ignored) {
        }
        if (message.isPresent()) {
            return ContextResult.ok(message::get);
        }

        return ContextResult.ok(() -> null);
    }
}
