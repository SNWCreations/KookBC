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
import dev.rollczi.litecommands.meta.MetaKey;
import snw.jkook.command.CommandSender;
import snw.jkook.message.Message;
import snw.kookbc.impl.command.litecommands.ReplyResultHandler;

/**
 * 可扩展接口
 * 自定义结果处理方式
 * 默认只有resultHandler注册为 {@link ReplyResultHandler} 时触发
 * Example:
 * <blockquote><pre>
 * LiteKookFactory.builder(plugin)
 *     .result(String.class, new ReplyResultHandler&lt;&gt;())
 * </pre></blockquote>
 * <blockquote><pre>
 * {@code @Execute}
 * {@code @Result(ResultTypes.SEND_TEMP)}
 * public String doIt(){
 *     // ...
 * }
 * </pre></blockquote>
 * <p>
 */
public interface ResultType {
    MetaKey<ResultType> RESULT_TYPE_KEY = MetaKey.of("result-type", ResultType.class);

    void message(Invocation<CommandSender> invocation, Message message, Object result);
}
