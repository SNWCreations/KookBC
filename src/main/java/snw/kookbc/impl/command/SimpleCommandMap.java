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

import java.util.Map;

import org.jetbrains.annotations.Nullable;

import snw.jkook.command.JKookCommand;
import snw.jkook.plugin.Plugin;

// A simple command map as the storage of the command objects. // TODO implement methods
public class SimpleCommandMap {
    
    public void register(Plugin plugin, JKookCommand command) throws IllegalArgumentException {

    }

    public void unregister(JKookCommand command) {

    }

    public void unregisterAll(Plugin plugin) {

    }

    public Map<String, WrappedCommand> getView(boolean withPrefix) {
        return null;
    }

    public @Nullable JKookCommand getByRootName(String head, boolean withPrefix) {
        return null;
    }

}
