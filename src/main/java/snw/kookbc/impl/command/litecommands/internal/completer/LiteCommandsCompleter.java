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

package snw.kookbc.impl.command.litecommands.internal.completer;

import dev.rollczi.litecommands.util.StringUtil;
import org.jline.reader.Candidate;
import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;
import snw.jkook.command.JKookCommand;
import snw.kookbc.impl.KBCClient;
import snw.kookbc.impl.command.CommandManagerImpl;
import snw.kookbc.impl.command.WrappedCommand;
import snw.kookbc.impl.command.litecommands.LiteKookCommandExecutor;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 2024/2/13<br>
 * KookBC<br>
 *
 * @author huanmeng_qwq
 */
public class LiteCommandsCompleter implements Completer {
    private static final Pattern PATTERN_ON_SPACE = Pattern.compile(" ", Pattern.LITERAL);
    private final KBCClient client;

    public LiteCommandsCompleter(KBCClient client) {
        this.client = client;
    }

    @Override
    public void complete(LineReader reader, ParsedLine line, List<Candidate> candidates) {
        String input = line.line();
        String rootName = input;
        // end of char is blank
        String[] args = {""};
        int index = input.indexOf(" ");
        if (index != -1) {
            rootName = input.substring(0, index);
            String argLine = input.substring(index + 1);
            args = PATTERN_ON_SPACE.split(argLine, -1);
        }
        List<String> suggestions = suggestions(rootName, args);
        for (String suggestion : suggestions) {
            candidates.add(new Candidate(suggestion));
        }
    }

    public List<String> suggestions(String rootName, String[] args) {
        List<String> result = new ArrayList<>();
        for (WrappedCommand wrappedCommand : ((CommandManagerImpl) client.getCommandManager()).getCommandMap().getView(false).values()) {
            JKookCommand command = wrappedCommand.getCommand();
            if (rootName.isEmpty()) {
                result.add(command.getRootName());
                result.addAll(command.getAliases());
                continue;
            }
            if (command.getRootName().equalsIgnoreCase(rootName) || command.getAliases().stream().anyMatch(alias -> alias.equalsIgnoreCase(rootName))) {
                if (command.getExecutor() instanceof LiteKookCommandExecutor) {
                    Iterable<String> suggestions = ((LiteKookCommandExecutor) command.getExecutor()).getSuggestions(client.getCore().getConsoleCommandSender(), args);
                    for (String suggestion : suggestions) {
                        result.add(suggestion);
                    }
                }
            } else if (StringUtil.startsWithIgnoreCase(command.getRootName(), rootName)) {
                result.add(command.getRootName());
            } else if (command.getAliases().stream().anyMatch(alias -> StringUtil.startsWithIgnoreCase(alias, rootName))) {
                result.add(command.getRootName());
            }
        }
        return result;
    }
}
