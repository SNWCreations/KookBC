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

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntUnaryOperator;

public class Session {
    public static final IntUnaryOperator UPDATE_FUNC = i -> i + 1;
    private final AtomicInteger sn;
    private final Set<Frame> buffer = new HashSet<>();
    private String id;

    public Session(String id) {
        this(id, new AtomicInteger());
    }

    public Session(String id, AtomicInteger sn) {
        this.id = id;
        this.sn = sn;
    }

    public AtomicInteger getSN() {
        return sn;
    }

    public void increaseSN() {
        sn.updateAndGet(UPDATE_FUNC);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Set<Frame> getBuffer() {
        return buffer;
    }
}
