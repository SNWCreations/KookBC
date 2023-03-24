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

import com.google.gson.JsonObject;

import java.util.Objects;

// represents a message using KOOK standard event format.
public class Frame {
    private final MessageType type;
    private final int sn;
    private final JsonObject d;

    public Frame(int s, int sn, JsonObject d) {
        this.type = Objects.requireNonNull(MessageType.valueOf(s));
        this.sn = sn;
        this.d = d;
    }

    public MessageType getType() {
        return type;
    }

    public int getSN() {
        return sn;
    }

    public JsonObject getData() {
        return d;
    }

    @Override
    public String toString() {
        return "Frame{" +
                "type=" + type +
                ", sn=" + sn +
                ", d=" + d +
                '}';
    }
}
