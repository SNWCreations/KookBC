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

import snw.jkook.entity.mute.MuteData;
import snw.jkook.entity.mute.MuteResult;

import java.util.ArrayList;
import java.util.Objects;

// Use this as the result of Guild#getMuteStatus() instead of Collection<MuteData>.
public class MuteResultImpl extends ArrayList<MuteData> implements MuteResult {
    public MuteData getByUser(String id) {
        for (MuteData data : this) {
            if (Objects.equals(data.getUser().getId(), id)) {
                return data;
            }
        }
        return null;
    }
}
