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

package snw.kookbc.interfaces.network;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.jetbrains.annotations.Nullable;
import snw.kookbc.impl.network.exceptions.BadResponseException;

import java.io.File;
import java.util.Map;

public interface NetworkClient {

    JsonElement get(String url) throws BadResponseException;

    JsonElement post(String url, @Nullable Map<?, ?> body) throws BadResponseException;

    // xxxToObject methods are the wrappers that will convert the returned json element into json objects.

    JsonObject getToObject(String url) throws BadResponseException;

    JsonObject postToObject(String url, @Nullable Map<?, ?> body) throws BadResponseException;

    JsonElement postFileInMultipartBody(String url, File file) throws BadResponseException;

    JsonElement postFileInMultipartBody(String url, String fileName, byte[] data) throws BadResponseException;

}
