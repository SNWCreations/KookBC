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

import dev.rollczi.litecommands.flow.Flow;
import dev.rollczi.litecommands.invocation.Invocation;
import dev.rollczi.litecommands.meta.MetaHolder;
import dev.rollczi.litecommands.validator.Validator;
import snw.jkook.command.CommandSender;

import java.util.List;

public class ResultTypeValidator implements Validator<CommandSender> {
    @Override
    public Flow validate(Invocation<CommandSender> invocation, MetaHolder metaHolder) {
        ExecuteResultType executeResultType = (ExecuteResultType) invocation.context().get(ResultType.class).orElse(null);
        if (executeResultType != null) {
            List<ResultType> list = metaHolder.metaCollector().collect(ResultType.RESULT_TYPE_KEY);
            list.stream().filter(it -> it != ResultTypes.DEFAULT).forEach(executeResultType::setResultType);
        }
        return Flow.continueFlow();
    }
}
