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

package snw.kookbc.impl.network;

import java.util.HashMap;
import java.util.Map;

public enum MessageType {
    EVENT(0),
    HELLO(1),
    PING(2),
    PONG(3),
    RESUME(4),
    RECONNECT(5),
    RESUME_ACK(6);

    private static final Map<Integer, MessageType> values = new HashMap<>();

    static {
        for (MessageType value : values()) {
            values.put(value.getType(), value);
        }
    }

    private final int type;

    MessageType(int type) {
        this.type = type;
    }

    public static MessageType valueOf(int type) {
        return values.get(type);
    }

    public int getType() {
        return type;
    }
}
