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

package snw.kookbc.interfaces.network.webhook;

// A interface which is designed for KOOK Webhook Event Requests.
// Represents a HTTP POST request, and there is some KOOK-specific methods.
// The T type means the JSON in Java object.
public interface Request<T> {

    // Get the decompressed (and decrypted if encrypted) data as the result.
    String getRawBody();

    // To parsed Java JSON Object.
    // No actual type for T, because it is depend on the dependency of implementation.
    T toJson();

    // Only for implementation use.
    boolean isCompressed();

    void reply(int status, String content);
    
    // Return true if reply method is never called during its lifecycle.
    // Only for implementation use.
    boolean isReplyPresent();
}
