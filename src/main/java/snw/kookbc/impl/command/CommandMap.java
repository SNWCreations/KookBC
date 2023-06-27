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

import java.util.Map;

// A command map representation.
public interface CommandMap {

    void register(Plugin plugin, JKookCommand command) throws IllegalArgumentException;

    void unregister(JKookCommand command);

    void unregisterAll(Plugin plugin);

    void clear(); // use it to help GC

    // The result should NOT be modifiable.
    Map<String, WrappedCommand> getView(boolean withPrefix);

    @Nullable // return null if nothing matches the head.
        // If withPrefix is true, regard the head as the result of prefix + root name
        // Otherwise, just regard the head as the root name
    JKookCommand getByRootName(String head, boolean withPrefix);

}
