//
// MIT License
//
// Copyright (c) 2022 Alexander Söderberg & Contributors
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files (the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included in all
// copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
// SOFTWARE.
//
package cloud.commandframework.arguments;

import cloud.commandframework.ArgumentDescription;
import cloud.commandframework.Command;
import cloud.commandframework.CommandManager;
import cloud.commandframework.arguments.parser.ArgumentParseResult;
import cloud.commandframework.arguments.parser.ArgumentParser;
import cloud.commandframework.arguments.parser.ParserParameters;
import cloud.commandframework.context.CommandContext;
import cloud.commandframework.keys.CloudKey;
import cloud.commandframework.keys.CloudKeyHolder;
import cloud.commandframework.keys.SimpleCloudKey;
import io.leangen.geantyref.TypeToken;

import java.util.*;
import java.util.function.BiFunction;
import java.util.regex.Pattern;

/**
 * AN argument that belongs to a command
 *
 * @param <C> Command sender type
 * @param <T> The type that the argument parses into
 */
@SuppressWarnings({"unused", "UnusedReturnValue"})
public class CommandArgument<C, T> implements Comparable<CommandArgument>, CloudKeyHolder<T> {

    /**
     * Pattern for command argument names
     */
    private static final Pattern NAME_PATTERN = Pattern.compile("\\S+");
//    private static final Pattern NAME_PATTERN = Pattern.compile("[A-Za-z0-9\\-_]+");

    /**
     * A typed key representing this argument
     */
    private final CloudKey<T> key;
    /**
     * Indicates whether the argument is required
     * or not. All arguments prior to any other required
     * argument must also be required, such that the predicate
     * (∀ c_i ∈ required)({c_0, ..., c_i-1} ⊂ required) holds true,
     * where {c_0, ..., c_n-1} is the set of command arguments.
     */
    private final boolean required;
    /**
     * The command argument name. This might be exposed
     * to command senders and so should be chosen carefully.
     */
    private final String name;
    /**
     * The parser that is used to parse the command input
     * into the corresponding command type
     */
    private final ArgumentParser<C, T> parser;
    /**
     * Default value, will be empty if none was supplied
     */
    private final String defaultValue;
    /**
     * The type that is produces by the argument's parser
     */
    private final TypeToken<T> valueType;
    /**
     * Suggestion provider
     */
    private final BiFunction<CommandContext<C>, String, List<String>> suggestionsProvider;
    /**
     * Argument preprocessors that allows for extensions to exist argument types
     * without having to update all parsers
     */
    private final Collection<BiFunction<CommandContext<C>,
            Queue<String>, ArgumentParseResult<Boolean>>> argumentPreprocessors;

    /**
     * A description that will be used when registering this argument if no override is provided.
     */
    private final ArgumentDescription defaultDescription;

    /**
     * Whether the argument has been used before
     */
    private boolean argumentRegistered = false;

    private Command<C> owningCommand;

    /**
     * Construct a new command argument
     *
     * @param required              Whether the argument is required
     * @param name                  The argument name
     * @param parser                The argument parser
     * @param defaultValue          Default value used when no value is provided by the command sender
     * @param valueType             Type produced by the parser
     * @param suggestionsProvider   Suggestions provider
     * @param defaultDescription    Default description to use when registering
     * @param argumentPreprocessors Argument preprocessors
     * @since 1.4.0
     */
    public CommandArgument(
            final boolean required,
            final String name,
            final ArgumentParser<C, T> parser,
            final String defaultValue,
            final TypeToken<T> valueType,
            final BiFunction<CommandContext<C>, String, List<String>> suggestionsProvider,
            final ArgumentDescription defaultDescription,
            final Collection<BiFunction<CommandContext<C>, Queue<String>,
                    ArgumentParseResult<Boolean>>> argumentPreprocessors
    ) {
        this.required = required;
        this.name = Objects.requireNonNull(name, "Name may not be null");
        if (!NAME_PATTERN.asPredicate().test(name)) {
            throw new IllegalArgumentException("Name must not include space character");
        }
        this.parser = Objects.requireNonNull(parser, "Parser may not be null");
        this.defaultValue = defaultValue;
        this.valueType = valueType;
        this.suggestionsProvider = suggestionsProvider == null
                ? buildDefaultSuggestionsProvider(this)
                : suggestionsProvider;
        this.defaultDescription = Objects.requireNonNull(defaultDescription, "Default description may not be null");
        this.argumentPreprocessors = new LinkedList<>(argumentPreprocessors);
        this.key = SimpleCloudKey.of(this.name, this.valueType);
    }

    /**
     * Construct a new command argument
     *
     * @param required              Whether the argument is required
     * @param name                  The argument name
     * @param parser                The argument parser
     * @param defaultValue          Default value used when no value is provided by the command sender
     * @param valueType             Type produced by the parser
     * @param suggestionsProvider   Suggestions provider
     * @param argumentPreprocessors Argument preprocessors
     */
    public CommandArgument(
            final boolean required,
            final String name,
            final ArgumentParser<C, T> parser,
            final String defaultValue,
            final TypeToken<T> valueType,
            final BiFunction<CommandContext<C>, String, List<String>> suggestionsProvider,
            final Collection<BiFunction<CommandContext<C>, Queue<String>,
                    ArgumentParseResult<Boolean>>> argumentPreprocessors
    ) {
        this(
                required,
                name,
                parser,
                defaultValue,
                valueType,
                suggestionsProvider,
                ArgumentDescription.empty(),
                argumentPreprocessors
        );
    }

    /**
     * Construct a new command argument
     *
     * @param required            Whether the argument is required
     * @param name                The argument name
     * @param parser              The argument parser
     * @param defaultValue        Default value used when no value is provided by the command sender
     * @param valueType           Type produced by the parser
     * @param suggestionsProvider Suggestions provider
     */
    public CommandArgument(
            final boolean required,
            final String name,
            final ArgumentParser<C, T> parser,
            final String defaultValue,
            final TypeToken<T> valueType,
            final BiFunction<CommandContext<C>, String, List<String>> suggestionsProvider
    ) {
        this(required, name, parser, defaultValue, valueType, suggestionsProvider, Collections.emptyList());
    }

    /**
     * Construct a new command argument
     *
     * @param required            Whether the argument is required
     * @param name                The argument name
     * @param parser              The argument parser
     * @param defaultValue        Default value used when no value is provided by the command sender
     * @param valueType           Type produced by the parser
     * @param suggestionsProvider Suggestions provider
     * @param defaultDescription  Default description to use when registering
     * @since 1.4.0
     */

    public CommandArgument(
            final boolean required,
            final String name,
            final ArgumentParser<C, T> parser,
            final String defaultValue,
            final TypeToken<T> valueType,
            final BiFunction<CommandContext<C>, String, List<String>> suggestionsProvider,
            final ArgumentDescription defaultDescription
    ) {
        this(required, name, parser, defaultValue, valueType, suggestionsProvider, defaultDescription, Collections.emptyList());
    }

    /**
     * Construct a new command argument
     *
     * @param required            Whether the argument is required
     * @param name                The argument name
     * @param parser              The argument parser
     * @param defaultValue        Default value used when no value is provided by the command sender
     * @param valueType           Type produced by the parser
     * @param suggestionsProvider Suggestions provider
     */
    public CommandArgument(
            final boolean required,
            final String name,
            final ArgumentParser<C, T> parser,
            final String defaultValue,
            final Class<T> valueType,
            final BiFunction<CommandContext<C>,
                    String, List<String>> suggestionsProvider
    ) {
        this(required, name, parser, defaultValue, TypeToken.get(valueType), suggestionsProvider);
    }

    /**
     * Construct a new command argument
     *
     * @param required            Whether the argument is required
     * @param name                The argument name
     * @param parser              The argument parser
     * @param defaultValue        Default value used when no value is provided by the command sender
     * @param valueType           Type produced by the parser
     * @param suggestionsProvider Suggestions provider
     * @param defaultDescription  Default description to use when registering
     * @since 1.4.0
     */

    public CommandArgument(
            final boolean required,
            final String name,
            final ArgumentParser<C, T> parser,
            final String defaultValue,
            final Class<T> valueType,
            final BiFunction<CommandContext<C>,
                    String, List<String>> suggestionsProvider,
            final ArgumentDescription defaultDescription
    ) {
        this(required, name, parser, defaultValue, TypeToken.get(valueType), suggestionsProvider, defaultDescription);
    }

    /**
     * Construct a new command argument
     *
     * @param required  Whether the argument is required
     * @param name      The argument name
     * @param parser    The argument parser
     * @param valueType Type produced by the parser
     */
    public CommandArgument(
            final boolean required,
            final String name,
            final ArgumentParser<C, T> parser,
            final Class<T> valueType
    ) {
        this(required, name, parser, "", valueType, null);
    }

    private static <C> BiFunction<CommandContext<C>, String,
            List<String>> buildDefaultSuggestionsProvider(final CommandArgument<C, ?> argument) {
        return new DelegatingSuggestionsProvider<>(argument.getName(), argument.getParser());
    }

    /**
     * Create a new command argument
     *
     * @param clazz Argument class
     * @param name  Argument name
     * @param <C>   Command sender type
     * @param <T>   Argument Type. Used to make the compiler happy.
     * @return Argument builder
     */
    public static <C, T> CommandArgument.Builder<C, T> ofType(
            final TypeToken<T> clazz,
            final String name
    ) {
        return new Builder<>(clazz, name);
    }

    /**
     * Create a new command argument
     *
     * @param clazz Argument class
     * @param name  Argument name
     * @param <C>   Command sender type
     * @param <T>   Argument Type. Used to make the compiler happy.
     * @return Argument builder
     */
    public static <C, T> CommandArgument.Builder<C, T> ofType(
            final Class<T> clazz,
            final String name
    ) {
        return new Builder<>(TypeToken.get(clazz), name);
    }

    @Override
    public final CloudKey<T> getKey() {
        return this.key;
    }

    /**
     * Check whether the command argument is required
     *
     * @return {@code true} if the argument is required, {@code false} if not
     */
    public boolean isRequired() {
        return this.required;
    }

    /**
     * Get the command argument name;
     *
     * @return Argument name
     */
    public String getName() {
        return this.name;
    }

    /**
     * Get the parser that is used to parse the command input
     * into the corresponding command type
     *
     * @return Command parser
     */
    public ArgumentParser<C, T> getParser() {
        return this.parser;
    }

    @Override
    public final String toString() {
        return String.format("%s{name=%s}", this.getClass().getSimpleName(), this.name);
    }

    /**
     * Register a new preprocessor. If all preprocessor has succeeding {@link ArgumentParseResult results}
     * that all return {@code true}, the argument will be passed onto the parser.
     * <p>
     * It is important that the preprocessor doesn't pop any input. Instead, it should only peek.
     *
     * @param preprocessor Preprocessor
     * @return {@code this}
     */
    public CommandArgument<C, T> addPreprocessor(
            final BiFunction<CommandContext<C>, Queue<String>,
                    ArgumentParseResult<Boolean>> preprocessor
    ) {
        this.argumentPreprocessors.add(preprocessor);
        return this;
    }

    /**
     * Preprocess command input. This will immediately forward any failed argument parse results.
     * If none fails, a {@code true} result will be returned
     *
     * @param context Command context
     * @param input   Remaining command input. None will be popped
     * @return Parsing error, or argument containing {@code true}
     */
    public ArgumentParseResult<Boolean> preprocess(
            final CommandContext<C> context,
            final Queue<String> input
    ) {
        for (final BiFunction<CommandContext<C>, Queue<String>,
                ArgumentParseResult<Boolean>> preprocessor : this.argumentPreprocessors) {
            final ArgumentParseResult<Boolean> result = preprocessor.apply(
                    context,
                    input
            );
            if (result.getFailure().isPresent()) {
                return result;
            }
        }
        return ArgumentParseResult.success(true);
    }

    /**
     * Get the owning command
     *
     * @return Owning command
     */
    public Command<C> getOwningCommand() {
        return this.owningCommand;
    }

    /**
     * Set the owning command
     *
     * @param owningCommand Owning command
     */
    public void setOwningCommand(final Command<C> owningCommand) {
        if (this.owningCommand != null) {
            throw new IllegalStateException("Cannot replace owning command");
        }
        this.owningCommand = owningCommand;
    }

    /**
     * Get the argument suggestions provider
     *
     * @return Suggestions provider
     */
    public final BiFunction<CommandContext<C>, String,
            List<String>> getSuggestionsProvider() {
        return this.suggestionsProvider;
    }

    /**
     * Get the default description to use when registering and no other is provided.
     *
     * @return the default description
     */
    public final ArgumentDescription getDefaultDescription() {
        return this.defaultDescription;
    }

    @Override
    public final boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final CommandArgument<?, ?> that = (CommandArgument<?, ?>) o;
        return this.isRequired() == that.isRequired() && Objects.equals(this.getName(), that.getName());
    }

    @Override
    public final int hashCode() {
        return Objects.hash(this.isRequired(), this.getName());
    }

    @Override
    public final int compareTo(final CommandArgument o) {
        if (this instanceof StaticArgument) {
            if (o instanceof StaticArgument) {
                return this.getName().compareTo(o.getName());
            } else {
                return -1;
            }
        } else {
            if (o instanceof StaticArgument) {
                return 1;
            } else {
                return 0;
            }
        }
    }

    /**
     * Get the default value
     *
     * @return Default value
     */
    public String getDefaultValue() {
        return this.defaultValue;
    }

    /**
     * Check if the argument has a default value
     *
     * @return {@code true} if the argument has a default value, {@code false} if not
     */
    public boolean hasDefaultValue() {
        return !this.isRequired()
                && !this.getDefaultValue().isEmpty();
    }

    /**
     * Get the type of this argument's value
     *
     * @return Value type
     */
    public TypeToken<T> getValueType() {
        return this.valueType;
    }

    /**
     * Create a copy of the command argument
     *
     * @return Copied argument
     */
    public CommandArgument<C, T> copy() {
        CommandArgument.Builder<C, T> builder = ofType(this.valueType, this.name);
        builder = builder.withSuggestionsProvider(this.suggestionsProvider);
        builder = builder.withParser(this.parser);
        if (this.isRequired()) {
            builder = builder.asRequired();
        } else if (this.defaultValue.isEmpty()) {
            builder = builder.asOptional();
        } else {
            builder = builder.asOptionalWithDefault(this.defaultValue);
        }
        builder = builder.withDefaultDescription(this.defaultDescription);

        return builder.build();
    }

    /**
     * Check whether the argument has been used in a command
     *
     * @return {@code true} if the argument has been used in a command, else {@code false}
     */
    public boolean isArgumentRegistered() {
        return this.argumentRegistered;
    }

    /**
     * Indicate that the argument has been associated with a command
     */
    public void setArgumentRegistered() {
        this.argumentRegistered = true;
    }


    /**
     * Mutable builder for {@link CommandArgument} instances
     *
     * @param <C> Command sender type
     * @param <T> Argument value type
     */
    public static class Builder<C, T> {

        private final TypeToken<T> valueType;
        private final String name;

        private CommandManager<C> manager;
        private boolean required = true;
        private ArgumentParser<C, T> parser;
        private String defaultValue = "";
        private BiFunction<CommandContext<C>, String, List<String>> suggestionsProvider;
        private ArgumentDescription defaultDescription = ArgumentDescription.empty();

        private final Collection<BiFunction<CommandContext<C>,
                String, ArgumentParseResult<Boolean>>> argumentPreprocessors = new LinkedList<>();

        protected Builder(
                final TypeToken<T> valueType,
                final String name
        ) {
            this.valueType = valueType;
            this.name = name;
        }

        protected Builder(
                final Class<T> valueType,
                final String name
        ) {
            this(TypeToken.get(valueType), name);
        }

        /**
         * Set the command manager. Will be used to create a default parser
         * if none was provided
         *
         * @param manager Command manager
         * @return Builder instance
         */
        public Builder<C, T> manager(final CommandManager<C> manager) {
            this.manager = manager;
            return this;
        }

        /**
         * Indicates that the argument is required.
         * All arguments prior to any other required
         * argument must also be required, such that the predicate
         * (∀ c_i ∈ required)({c_0, ..., c_i-1} ⊂ required) holds true,
         * where {c_0, ..., c_n-1} is the set of command arguments.
         *
         * @return Builder instance
         */
        public Builder<C, T> asRequired() {
            this.required = true;
            return this;
        }

        /**
         * Indicates that the argument is optional.
         * All arguments prior to any other required
         * argument must also be required, such that the predicate
         * (∀ c_i ∈ required)({c_0, ..., c_i-1} ⊂ required) holds true,
         * where {c_0, ..., c_n-1} is the set of command arguments.
         *
         * @return Builder instance
         */
        public Builder<C, T> asOptional() {
            this.required = false;
            return this;
        }

        /**
         * Indicates that the argument is optional.
         * All arguments prior to any other required
         * argument must also be required, such that the predicate
         * (∀ c_i ∈ required)({c_0, ..., c_i-1} ⊂ required) holds true,
         * where {c_0, ..., c_n-1} is the set of command arguments.
         *
         * @param defaultValue Default value that will be used if none was supplied
         * @return Builder instance
         */
        public Builder<C, T> asOptionalWithDefault(final String defaultValue) {
            this.defaultValue = defaultValue;
            this.required = false;
            return this;
        }

        /**
         * Set the argument parser
         *
         * @param parser Argument parser
         * @return Builder instance
         */
        public Builder<C, T> withParser(final ArgumentParser<C, T> parser) {
            this.parser = Objects.requireNonNull(parser, "Parser may not be null");
            return this;
        }

        /**
         * Set the suggestions provider
         *
         * @param suggestionsProvider Suggestions provider
         * @return Builder instance
         */
        public Builder<C, T> withSuggestionsProvider(
                final BiFunction<CommandContext<C>,
                        String, List<String>> suggestionsProvider
        ) {
            this.suggestionsProvider = suggestionsProvider;
            return this;
        }

        /**
         * Set the default description to be used for this argument.
         *
         * <p>The default description is used when no other description is provided for a certain argument.</p>
         *
         * @param defaultDescription The default description
         * @return Builder instance
         * @since 1.4.0
         */

        public Builder<C, T> withDefaultDescription(
                final ArgumentDescription defaultDescription
        ) {
            this.defaultDescription = Objects.requireNonNull(defaultDescription, "Default description may not be null");
            return this;
        }

        /**
         * Construct a command argument from the builder settings
         *
         * @return Constructed argument
         */
        public CommandArgument<C, T> build() {
            if (this.parser == null && this.manager != null) {
                this.parser = this.manager.parserRegistry().createParser(this.valueType, ParserParameters.empty())
                        .orElse(null);
            }
            if (this.parser == null) {
                this.parser = (c, i) -> ArgumentParseResult
                        .failure(new UnsupportedOperationException("No parser was specified"));
            }
            if (this.suggestionsProvider == null) {
                this.suggestionsProvider = new DelegatingSuggestionsProvider<>(this.name, this.parser);
            }
            return new CommandArgument<>(
                    this.required,
                    this.name,
                    this.parser,
                    this.defaultValue,
                    this.valueType,
                    this.suggestionsProvider,
                    this.defaultDescription
            );
        }

        protected final String getName() {
            return this.name;
        }

        protected final boolean isRequired() {
            return this.required;
        }

        protected final ArgumentParser<C, T> getParser() {
            return this.parser;
        }

        protected final String getDefaultValue() {
            return this.defaultValue;
        }

        protected final BiFunction<CommandContext<C>, String, List<String>>
        getSuggestionsProvider() {
            return this.suggestionsProvider;
        }

        protected final ArgumentDescription getDefaultDescription() {
            return this.defaultDescription;
        }

        protected final TypeToken<T> getValueType() {
            return this.valueType;
        }
    }

    /**
     * A variant of builders designed for subclassing, that returns a self type.
     *
     * @param <C> sender type
     * @param <T> argument value type
     * @param <B> the subclass type
     * @since 1.5.0
     */
    public abstract static class TypedBuilder<C, T, B extends Builder<C, T>> extends Builder<C, T> {

        protected TypedBuilder(
                final TypeToken<T> valueType,
                final String name
        ) {
            super(valueType, name);
        }

        protected TypedBuilder(
                final Class<T> valueType,
                final String name
        ) {
            super(valueType, name);
        }

        @SuppressWarnings("unchecked")
        protected final B self() {
            return (B) this;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public B manager(final CommandManager<C> manager) {
            super.manager(manager);
            return this.self();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public B asRequired() {
            super.asRequired();
            return this.self();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public B asOptional() {
            super.asOptional();
            return this.self();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public B asOptionalWithDefault(final String defaultValue) {
            super.asOptionalWithDefault(defaultValue);
            return this.self();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public B withParser(final ArgumentParser<C, T> parser) {
            super.withParser(parser);
            return this.self();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Builder<C, T> withSuggestionsProvider(
                final BiFunction<CommandContext<C>,
                        String, List<String>> suggestionsProvider
        ) {
            super.withSuggestionsProvider(suggestionsProvider);
            return this.self();
        }
    }
}
