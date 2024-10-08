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

import dev.rollczi.litecommands.platform.PlatformSettings;
import snw.kookbc.impl.command.litecommands.result.ResultType;
import snw.kookbc.impl.command.litecommands.result.ResultTypes;

public class LiteKookSettings implements PlatformSettings {
    private boolean nativePermissions = false;
    private ResultType defaultResultType = ResultTypes.REPLY;

    public LiteKookSettings defaultResultType(ResultType defaultResultType) {
        this.defaultResultType = defaultResultType;
        return this;
    }

    public LiteKookSettings nativePermissions(boolean nativePermissions) {
        this.nativePermissions = nativePermissions;
        return this;
    }

    boolean isNativePermissions() {
        return this.nativePermissions;
    }

    public ResultType defaultResultType() {
        return defaultResultType;
    }
}
