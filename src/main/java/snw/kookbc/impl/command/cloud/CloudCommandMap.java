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

import snw.jkook.command.JKookCommand;
import snw.jkook.plugin.Plugin;
import snw.kookbc.impl.command.SimpleCommandMap;

import java.util.List;

/**
 * @author huanmeng_qwq
 */
public class CloudCommandMap extends SimpleCommandMap {
    protected CloudCommandManagerImpl parent;

    public void init(CloudCommandManagerImpl parent) {
        if (this.parent != null) {
            throw new IllegalStateException("Already initialized");
        }
        this.parent = parent;
    }

    public void register(Plugin plugin, JKookCommand command) {
        super.register(plugin, command);
        this.parent.getCloudCommandManager().registerJKookCommand(plugin, command);
    }

    public void unregister(JKookCommand command) {
        super.unregister(command);
        this.parent.getCloudCommandManager().unregisterJKookCommand(command);
    }

    public void unregisterAll(Plugin plugin) {
        super.unregisterAll(plugin);
        this.parent.getCloudCommandManager().unregisterAll(plugin);
    }

    protected Plugin getOwnerOfCommand(JKookCommand command) {
        return getView(false).get(command.getRootName()).getPlugin();
    }

    public List<CloudCommandInfo> getCommandsInfo() {
        return parent.getCommandsInfo();
    }
}
