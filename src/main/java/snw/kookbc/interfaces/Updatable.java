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

package snw.kookbc.interfaces;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;

// Represents an object which can be updated.
public interface Updatable {

    // Use the provided object to update the data inside this instance.
    // MUST lock the object itself to ensure the read operations during the update progress
    // will be blocked until the update is done.
    void update(JsonObject data);

    // ===== Jackson API - 高性能版本 =====

    // Jackson版本的update方法 - 高性能JSON处理
    default void update(JsonNode data) {
        // 使用Gson JsonParser转换（避免依赖JacksonUtil.NORMAL_GSON）
        update(JsonParser.parseString(data.toString()).getAsJsonObject());
    }

}
