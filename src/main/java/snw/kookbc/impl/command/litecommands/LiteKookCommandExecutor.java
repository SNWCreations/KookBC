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

import dev.rollczi.litecommands.argument.parser.input.ParseableInput;
import dev.rollczi.litecommands.argument.suggester.input.SuggestionInput;
import dev.rollczi.litecommands.command.CommandRoute;
import dev.rollczi.litecommands.invocation.Invocation;
import dev.rollczi.litecommands.invocation.InvocationContext;
import dev.rollczi.litecommands.platform.PlatformInvocationListener;
import dev.rollczi.litecommands.platform.PlatformSuggestionListener;
import org.jetbrains.annotations.Nullable;
import snw.jkook.Core;
import snw.jkook.command.CommandExecutor;
import snw.jkook.command.CommandSender;
import snw.jkook.message.Message;

import java.util.Arrays;

/**
 * 2024/2/12<br>
 * KookBC<br>
 *
 * @author huanmeng_qwq
 */
public class LiteKookCommandExecutor implements CommandExecutor {
    private final Core core;
    private final LiteKookSettings settings;
    private final CommandRoute<CommandSender> commandSection;
    private final String label;
    private final PlatformInvocationListener<CommandSender> executeListener;
    private final PlatformSuggestionListener<CommandSender> suggestionListener;

    public LiteKookCommandExecutor(Core core, LiteKookSettings settings, CommandRoute<CommandSender> commandSection, String label, PlatformInvocationListener<CommandSender> executeListener, PlatformSuggestionListener<CommandSender> suggestionListener) {
        this.core = core;
        this.settings = settings;
        this.commandSection = commandSection;
        this.label = label;
        this.executeListener = executeListener;
        this.suggestionListener = suggestionListener;
    }

    @Override
    public void onCommand(CommandSender commandSender, Object[] objects, @Nullable Message message) {
        ParseableInput<?> input = ParseableInput.raw(Arrays.stream(objects).map(Object::toString).toArray(String[]::new));
        KookSender platformSender = new KookSender(commandSender);
        InvocationContext invocationContext = createContext(message);
        Invocation<CommandSender> invocation = new Invocation<>(commandSender, platformSender, this.commandSection.getName(), this.label, input, invocationContext);

        this.executeListener.execute(invocation, input);
    }

    public Iterable<String> getSuggestions(CommandSender sender, String[] args) {
        SuggestionInput<?> input = SuggestionInput.raw(args);
        KookSender platformSender = new KookSender(sender);
        InvocationContext invocationContext = createContext(null);
        Invocation<CommandSender> invocation = new Invocation<>(sender, platformSender, this.commandSection.getName(), this.label, input, invocationContext);

        return this.suggestionListener.suggest(invocation, input)
                .asMultiLevelList();
    }

    private InvocationContext createContext(@Nullable Message message) {
        InvocationContext.Builder builder = InvocationContext.builder();
        builder.put(Core.class, core)
                .put(Message.class, message);
        return builder.build();
    }
}
