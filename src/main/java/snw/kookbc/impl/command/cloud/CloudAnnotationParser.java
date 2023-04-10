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
package snw.kookbc.impl.command.cloud;

import cloud.commandframework.Command;
import cloud.commandframework.annotations.AnnotationParser;
import cloud.commandframework.annotations.processing.CommandContainerProcessor;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.NonNull;
import snw.jkook.command.CommandSender;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author huanmeng_qwq
 */
public class CloudAnnotationParser {
    private final AnnotationParser<CommandSender> parser;

    public CloudAnnotationParser(AnnotationParser<CommandSender> parser) {
        this.parser = parser;
    }

    public <T> @NonNull Collection<@NonNull Command<CommandSender>> parse(Object instance) {
        return parser.parse(instance);
    }

    public @NonNull Collection<@NonNull Command<CommandSender>> parse(ClassLoader classLoader) throws Exception {
        final List<Command<CommandSender>> commands = new LinkedList<>();

        final List<String> classes;
        try (InputStream stream = classLoader.getResourceAsStream(CommandContainerProcessor.PATH)) {
            if (stream == null) {
                return Collections.emptyList();
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                classes = reader.lines().distinct().collect(Collectors.toList());
            }
        }

        for (final String className : classes) {
            final Class<?> commandContainer = Class.forName(className);

            // We now have the class, and we now just need to decide what constructor to invoke.
            // We first try to find a constructor which takes in the parser.
            @MonotonicNonNull Object instance;
            try {
                instance = commandContainer.getConstructor(AnnotationParser.class).newInstance(parser);
            } catch (final NoSuchMethodException ignored) {
                try {
                    // Then we try to find a no-arg constructor.
                    instance = commandContainer.getConstructor().newInstance();
                } catch (final NoSuchMethodException e) {
                    // If neither are found, we panic!
                    throw new IllegalStateException(
                            String.format(
                                    "Command container %s has no valid constructors",
                                    commandContainer
                            ),
                            e
                    );
                }
            }
            commands.addAll(parser.parse(instance));
        }

        return Collections.unmodifiableList(commands);
    }
}
