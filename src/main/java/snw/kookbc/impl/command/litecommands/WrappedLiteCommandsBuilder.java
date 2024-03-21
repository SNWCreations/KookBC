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

import dev.rollczi.litecommands.LiteCommands;
import dev.rollczi.litecommands.LiteCommandsBuilder;
import dev.rollczi.litecommands.LiteCommandsProvider;
import dev.rollczi.litecommands.annotations.LiteCommandsAnnotations;
import dev.rollczi.litecommands.argument.ArgumentKey;
import dev.rollczi.litecommands.argument.parser.Parser;
import dev.rollczi.litecommands.argument.resolver.ArgumentResolverBase;
import dev.rollczi.litecommands.argument.suggester.Suggester;
import dev.rollczi.litecommands.bind.BindProvider;
import dev.rollczi.litecommands.configurator.LiteConfigurator;
import dev.rollczi.litecommands.context.ContextProvider;
import dev.rollczi.litecommands.editor.Editor;
import dev.rollczi.litecommands.extension.LiteExtension;
import dev.rollczi.litecommands.extension.annotations.AnnotationsExtension;
import dev.rollczi.litecommands.handler.exception.ExceptionHandler;
import dev.rollczi.litecommands.handler.result.ResultHandler;
import dev.rollczi.litecommands.invalidusage.InvalidUsageHandler;
import dev.rollczi.litecommands.message.InvokedMessage;
import dev.rollczi.litecommands.message.Message;
import dev.rollczi.litecommands.message.MessageKey;
import dev.rollczi.litecommands.permission.MissingPermissionsHandler;
import dev.rollczi.litecommands.platform.PlatformSettings;
import dev.rollczi.litecommands.platform.PlatformSettingsConfigurator;
import dev.rollczi.litecommands.processor.LiteBuilderProcessor;
import dev.rollczi.litecommands.programmatic.LiteCommand;
import dev.rollczi.litecommands.programmatic.LiteCommandsProgrammatic;
import dev.rollczi.litecommands.reflect.type.TypeRange;
import dev.rollczi.litecommands.scheduler.Scheduler;
import dev.rollczi.litecommands.schematic.SchematicFormat;
import dev.rollczi.litecommands.schematic.SchematicGenerator;
import dev.rollczi.litecommands.scope.Scope;
import dev.rollczi.litecommands.suggestion.SuggestionResult;
import dev.rollczi.litecommands.validator.Validator;
import dev.rollczi.litecommands.wrapper.Wrapper;
import snw.kookbc.impl.command.litecommands.annotations.prefix.PrefixAnnotationResolver;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

public class WrappedLiteCommandsBuilder<SENDER, SETTINGS extends PlatformSettings, B extends LiteCommandsBuilder<SENDER, SETTINGS, B>> implements LiteCommandsBuilder<SENDER, SETTINGS, B> {
    private LiteCommandsBuilder<SENDER, SETTINGS, B> delegate;

    public WrappedLiteCommandsBuilder(LiteCommandsBuilder<SENDER, SETTINGS, B> delegate) {
        this.delegate = delegate;
    }

    @Override
    public B settings(PlatformSettingsConfigurator<SETTINGS> configurator) {
        return delegate.settings(configurator);
    }

    @Override
    public B commands(LiteCommandsProvider<SENDER> commandsProvider) {
        return delegate.commands(commandsProvider);
    }

    @Override
    public B commands(Object... commands) {
        List<LiteCommandsProvider<SENDER>> providers = new ArrayList<>();
        Collection<LiteCommand<SENDER>> programmatic = new ArrayList<>();
        List<Class<?>> classes = new ArrayList<>();
        List<Object> instances = new ArrayList<>();

        for (Object command : commands) {
            if (command instanceof LiteCommandsProvider) {
                providers.add((LiteCommandsProvider<SENDER>) command);
                continue;
            }

            if (command instanceof LiteCommand) {
                programmatic.add((LiteCommand<SENDER>) command);
                continue;
            }

            if (command instanceof Class) {
                classes.add((Class<?>) command);
                continue;
            }

            instances.add(command);
        }

        for (LiteCommandsProvider<SENDER> provider : providers) {
            this.commands(provider);
        }

        if (!programmatic.isEmpty()) {
            this.commands(LiteCommandsProgrammatic.of(programmatic));
        }

        if (!classes.isEmpty() || !instances.isEmpty()) {
            LiteCommandsAnnotations<SENDER> commandsAnnotations = LiteCommandsAnnotations.create();
            commandsAnnotations.getAnnotationProcessorService().register(new PrefixAnnotationResolver<>());
            this.commands(commandsAnnotations
                    .load(instances.toArray(new Object[0]))
                    .loadClasses(classes.toArray(new Class<?>[0]))
            );
        }

        return self();
    }

    @Override
    public <T> B argumentParser(Class<T> type, Parser<SENDER, T> parser) {
        return delegate.argumentParser(type, parser);
    }

    @Override
    public <T> B argumentParser(Class<T> type, ArgumentKey key, Parser<SENDER, T> parser) {
        return delegate.argumentParser(type, key, parser);
    }

    @Override
    public <T> B argumentParser(TypeRange<T> type, ArgumentKey key, Parser<SENDER, T> parser) {
        return delegate.argumentParser(type, key, parser);
    }

    @SuppressWarnings("unchecked")
    protected B self() {
        return ((B) delegate);
    }

    @Override
    public <T> B argumentSuggestion(Class<T> type, SuggestionResult suggestion) {
        return delegate.argumentSuggestion(type, suggestion);
    }

    @Override
    public <T> B argumentSuggestion(Class<T> type, ArgumentKey key, SuggestionResult suggestion) {
        return delegate.argumentSuggestion(type, key, suggestion);
    }

    @Override
    public <T> B argumentSuggestion(TypeRange<T> type, ArgumentKey key, SuggestionResult suggestion) {
        return delegate.argumentSuggestion(type, key, suggestion);
    }

    @Override
    public <T> B argumentSuggester(Class<T> type, Suggester<SENDER, T> suggester) {
        return delegate.argumentSuggester(type, suggester);
    }

    @Override
    public <T> B argumentSuggester(Class<T> type, ArgumentKey key, Suggester<SENDER, T> suggester) {
        return delegate.argumentSuggester(type, key, suggester);
    }

    @Override
    public <T> B argumentSuggester(TypeRange<T> type, ArgumentKey key, Suggester<SENDER, T> suggester) {
        return delegate.argumentSuggester(type, key, suggester);
    }

    @Override
    public <T> B argument(Class<T> type, ArgumentResolverBase<SENDER, T> resolver) {
        return delegate.argument(type, resolver);
    }

    @Override
    public <T> B argument(Class<T> type, ArgumentKey key, ArgumentResolverBase<SENDER, T> resolver) {
        return delegate.argument(type, key, resolver);
    }

    @Override
    public <T> B argument(TypeRange<T> type, ArgumentResolverBase<SENDER, T> resolver) {
        return delegate.argument(type, resolver);
    }

    @Override
    public <T> B argument(TypeRange<T> type, ArgumentKey key, ArgumentResolverBase<SENDER, T> resolver) {
        return delegate.argument(type, key, resolver);
    }

    @Override
    public <T> B context(Class<T> on, ContextProvider<SENDER, T> bind) {
        return delegate.context(on, bind);
    }

    @Override
    public <T> B bind(Class<T> on, BindProvider<T> bindProvider) {
        return delegate.bind(on, bindProvider);
    }

    @Override
    public <T> B bind(Class<T> on, Supplier<T> bind) {
        return delegate.bind(on, bind);
    }

    @Override
    public B bindUnsafe(Class<?> on, Supplier<?> bind) {
        return delegate.bindUnsafe(on, bind);
    }

    @Override
    public B scheduler(Scheduler scheduler) {
        return delegate.scheduler(scheduler);
    }

    @Override
    public <T, CONTEXT> B message(MessageKey<CONTEXT> key, Message<T, CONTEXT> message) {
        return delegate.message(key, message);
    }

    @Override
    public <T, CONTEXT> B message(MessageKey<CONTEXT> key, InvokedMessage<SENDER, T, CONTEXT> message) {
        return delegate.message(key, message);
    }

    @Override
    public <T, CONTEXT> B message(MessageKey<CONTEXT> key, T message) {
        return delegate.message(key, message);
    }

    @Override
    public B editorGlobal(Editor<SENDER> editor) {
        return delegate.editorGlobal(editor);
    }

    @Override
    public B editor(Scope scope, Editor<SENDER> editor) {
        return delegate.editor(scope, editor);
    }

    @Override
    public B validatorGlobal(Validator<SENDER> validator) {
        return delegate.validatorGlobal(validator);
    }

    @Override
    public B validator(Scope scope, Validator<SENDER> validator) {
        return delegate.validator(scope, validator);
    }

    @Override
    public <T> B result(Class<T> resultType, ResultHandler<SENDER, ? extends T> handler) {
        return delegate.result(resultType, handler);
    }

    @Override
    public B resultUnexpected(ResultHandler<SENDER, Object> handler) {
        return delegate.resultUnexpected(handler);
    }

    @Override
    public <E extends Throwable> B exception(Class<E> exceptionType, ExceptionHandler<SENDER, ? extends E> handler) {
        return delegate.exception(exceptionType, handler);
    }

    @Override
    public B exceptionUnexpected(ExceptionHandler<SENDER, Throwable> handler) {
        return delegate.exceptionUnexpected(handler);
    }

    @Override
    public B missingPermission(MissingPermissionsHandler<SENDER> handler) {
        return delegate.missingPermission(handler);
    }

    @Override
    public B invalidUsage(InvalidUsageHandler<SENDER> handler) {
        return delegate.invalidUsage(handler);
    }

    @Override
    public B wrapper(Wrapper wrapper) {
        return delegate.wrapper(wrapper);
    }

    @Override
    public B schematicGenerator(SchematicGenerator<SENDER> schematicGenerator) {
        return delegate.schematicGenerator(schematicGenerator);
    }

    @Override
    public B schematicGenerator(SchematicFormat format) {
        return delegate.schematicGenerator(format);
    }

    @Override
    public B selfProcessor(LiteBuilderProcessor<SENDER, SETTINGS> processor) {
        return delegate.selfProcessor(processor);
    }

    @Override
    public B preProcessor(LiteBuilderProcessor<SENDER, SETTINGS> preProcessor) {
        return delegate.preProcessor(preProcessor);
    }

    @Override
    public B postProcessor(LiteBuilderProcessor<SENDER, SETTINGS> postProcessor) {
        return delegate.postProcessor(postProcessor);
    }

    @Override
    public <CONFIGURATION> B extension(LiteExtension<SENDER, CONFIGURATION> extension) {
        return delegate.extension(extension);
    }

    @Override
    public <CONFIGURATION, E extends LiteExtension<SENDER, CONFIGURATION>> B extension(E extension, LiteConfigurator<CONFIGURATION> configurator) {
        return delegate.extension(extension, configurator);
    }

    @Override
    public B annotations(LiteConfigurator<AnnotationsExtension<SENDER>> configurator) {
        return delegate.annotations(configurator);
    }

    @Override
    public LiteCommands<SENDER> build() {
        return delegate.build();
    }

    @Override
    public LiteCommands<SENDER> build(boolean register) {
        return delegate.build(register);
    }
}
