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

package snw.kookbc.impl.network.exceptions;

// Thrown if the response is not 200 OK.
public class BadResponseException extends RuntimeException {
    private final int code;
    private final String rawMessage;

    public BadResponseException(int code, String message) {
        super("Response code: " + code + ", message: " + message);
        this.code = code;
        this.rawMessage = message;
    }

    public int getCode() {
        return code;
    }

    public String getRawMessage() {
        return rawMessage;
    }
}
