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

package snw.kookbc.impl.command;

import org.jetbrains.annotations.Nullable;
import snw.jkook.command.JKookCommand;
import snw.jkook.plugin.Plugin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// A simple command map as the storage of the command objects.
public class SimpleCommandMap {
    protected final Map<String, WrappedCommand> commandsWithoutPrefix = new ConcurrentHashMap<>();
    protected final Map<String, WrappedCommand> commandsWithPrefix = new ConcurrentHashMap<>();
    protected final Map<String, WrappedCommand> commandsWithoutPrefixView = Collections.unmodifiableMap(commandsWithoutPrefix);
    protected final Map<String, WrappedCommand> commandsWithPrefixView = Collections.unmodifiableMap(commandsWithPrefix);

    protected SimpleCommandMap() {
    }

    public void register(Plugin plugin, JKookCommand command) {
        WrappedCommand wrapped = new WrappedCommand(command, plugin);

        commandsWithoutPrefix.put(command.getRootName(), wrapped);
        for (String head : createHeaders(command)) {
            commandsWithPrefix.put(head, wrapped);
        }
    }

    public void unregister(JKookCommand command) {
        commandsWithPrefix.entrySet().removeIf(i -> i.getValue().getCommand() == command);
        commandsWithoutPrefix.entrySet().removeIf(i -> i.getValue().getCommand() == command);
    }

    public void unregisterAll(Plugin plugin) {
        commandsWithPrefix.entrySet().removeIf(i -> i.getValue().getPlugin() == plugin);
        commandsWithoutPrefix.entrySet().removeIf(i -> i.getValue().getPlugin() == plugin);
    }

    public Map<String, WrappedCommand> getView(boolean withPrefix) {
        return withPrefix ? commandsWithPrefixView : commandsWithoutPrefixView;
    }

    public @Nullable JKookCommand getByRootName(String head, boolean withPrefix) {
        Map<String, WrappedCommand> view = getView(withPrefix);
        if (view.containsKey(head)) {
            return view.get(head).getCommand();
        }
        return null;
    }


    protected static Collection<String> createHeaders(JKookCommand command) {
        Collection<String> result = new ArrayList<>(command.getPrefixes().size() * (command.getAliases().size() + 1));
        for (String prefix : command.getPrefixes()) {
            result.add(prefix + command.getRootName());
            for (String alias : command.getAliases()) {
                result.add(prefix + alias);
            }
        }
        return result;
    }

}
