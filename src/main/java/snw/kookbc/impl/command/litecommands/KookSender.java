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

import dev.rollczi.litecommands.identifier.Identifier;
import dev.rollczi.litecommands.platform.AbstractPlatformSender;
import snw.jkook.command.CommandSender;
import snw.jkook.entity.User;

class KookSender extends AbstractPlatformSender {

    private final CommandSender handle;

    public KookSender(CommandSender handle) {
        this.handle = handle;
    }

    @Override
    public String getName() {
        if (this.handle instanceof User) {
            return ((User) this.handle).getName();
        }

        return handle.getClass().getSimpleName();
    }

    @Override
    public Identifier getIdentifier() {
        if (this.handle instanceof User) {
            return Identifier.of(((User) this.handle).getId());
        }

        return Identifier.CONSOLE;
    }

    @Override
    public boolean hasPermission(String permission) {
        return false;
    }

}