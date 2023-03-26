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

package snw.kookbc.impl.event;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import snw.jkook.event.Event;

public final class EventTypeMap {
    public static final Map<String, Class<? extends Event>> MAP;

    static {
        final Map<String, Class<? extends Event>> mutableMap = new HashMap<>();
        // TODO put mapping here.
        MAP = Collections.unmodifiableMap(mutableMap);
    }

    private EventTypeMap() {} // can't call constructor
}
