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

package snw.kookbc;

import com.fasterxml.jackson.databind.JsonNode;
import snw.kookbc.util.JacksonUtil;

import java.io.IOException;
import java.io.InputStream;

// The shared constants as the symbol for KookBC.
// Want to modify them? see kookbc_version_data.json in src/main/resources folder.
// Why I don't write them in this class?
// The version string cannot be written as final variable, it should be loaded dynamically.
public final class SharedConstants {
    // The API name
    // Modify it if you forked JKook API
    public static final String SPEC_NAME;
    // The API version
    public static final String SPEC_VERSION;
    // The KookBC distribution name
    // If you want to edit this in your fork, see the following static statement.
    public static final String IMPL_NAME;
    // The KookBC version. Initialized in static statement.
    public static final String IMPL_VERSION;
    // The KookBC repository url, modify it to your fork repository if you want
    public static final String REPO_URL;
    public static final boolean IS_SNAPSHOT;

    static {
        // region Initialize data object
        JsonNode dataObject;
        try (InputStream inputStream = SharedConstants.class.getClassLoader().getResourceAsStream("kookbc_version_data.json")) {
            if (inputStream == null) {
                throw new Error("Cannot find kookbc_version_data.json");
            }
            dataObject = JacksonUtil.getMapper().readTree(inputStream);
        } catch (IOException e) {
            throw new Error("Cannot initialize KookBC data", e); // should never happen
        }
        // endregion

        try {
            SPEC_NAME = dataObject.get("spec_name").asText();
            SPEC_VERSION = dataObject.get("spec_version").asText();
            IMPL_NAME = dataObject.get("name").asText();
            IMPL_VERSION = dataObject.get("version").asText();
            REPO_URL = dataObject.get("repo_url").asText();
            IS_SNAPSHOT = Boolean.parseBoolean(dataObject.get("is_snapshot").asText());
        } catch (Exception e) {
            throw new Error("Cannot define KookBC data", e);
        }
    }

    // Constructor not available
    private SharedConstants() {
    }
}
