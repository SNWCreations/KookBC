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

package snw.kookbc.impl.entity.mute;

import snw.jkook.entity.User;
import snw.jkook.entity.mute.MuteData;

public class MuteDataImpl implements MuteData {
    private final User user;
    private boolean input = false;
    private boolean output = false;

    public MuteDataImpl(User user) {
        this.user = user;
    }

    @Override
    public User getUser() {
        return user;
    }

    @Override
    public boolean isInputDisabled() {
        return input;
    }

    public void setInputDisabled(boolean input) {
        this.input = input;
    }

    @Override
    public boolean isOutputDisabled() {
        return output;
    }

    public void setOutputDisabled(boolean output) {
        this.output = output;
    }
}
