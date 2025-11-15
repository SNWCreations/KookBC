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

package snw.kookbc.impl.command.litecommands.annotations.result;

import dev.rollczi.litecommands.annotations.AnnotationInvoker;
import dev.rollczi.litecommands.annotations.AnnotationProcessor;
import org.slf4j.Logger;
import snw.kookbc.impl.command.litecommands.result.ResultType;
import snw.kookbc.impl.command.litecommands.result.ResultTypes;

import java.lang.reflect.InvocationTargetException;
import java.util.Objects;

public class ResultAnnotationResolver<SENDER> implements AnnotationProcessor<SENDER> {
    private final Logger logger;

    public ResultAnnotationResolver(Logger logger) {
        this.logger = logger;
    }

    @Override
    public AnnotationInvoker<SENDER> process(AnnotationInvoker<SENDER> invoker) {
        return invoker.onMethod(Result.class, (method, annotation, builder, executorProvider) -> {
            ResultType resultTypes = annotation.value();
            if (!Objects.equals(ResultType.class, annotation.custom())) {
                try {
                    resultTypes = annotation.custom().getConstructor().newInstance();
                } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                         NoSuchMethodException e) {
                    logger.error("@Result(custom) 创建失败，使用 value() 方法", e);
                }
            }
            if (resultTypes == ResultTypes.DEFAULT) return;
            builder.meta().put(ResultType.RESULT_TYPE_KEY, resultTypes);
        });
    }

}
