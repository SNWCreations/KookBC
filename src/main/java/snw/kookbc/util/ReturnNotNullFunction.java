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

package snw.kookbc.util;

import java.util.Objects;

@FunctionalInterface
public interface ReturnNotNullFunction<T, R> {

    R doApply(T input);

    // DO NOT OVERRIDE THIS METHOD, SHOULD BE CONSIDERED AS FINAL
    default R apply(T input) {
        return Objects.requireNonNull(doApply(input),
                "Function tried to return null from a function which is required to return non-null value");
    }
}
