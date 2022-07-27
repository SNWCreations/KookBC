/*
 *     KookBC -- The Kook Bot Client & JKook API standard implementation for Java.
 *     Copyright (C) 2022 KookBC contributors
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

package snw.kookbc.impl.tasks;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import snw.jkook.JKook;

import static snw.kookbc.util.Util.getVersionDifference;

public final class UpdateChecker implements Runnable {

    @Override
    public void run() {
        try {
            run0();
        } catch (Exception e) {
            JKook.getLogger().warn("Unable to check update from remote.", e);
        }
    }

    private void run0() throws Exception {
        JKook.getLogger().info("Checking updates...");

        JsonObject resObj;
        try (Response response = new OkHttpClient().newCall(
                new Request.Builder()
                        .get()
                        .url("https://api.github.com/repos/SNWCreations/KookBC/releases/latest")
                        .build()
        ).execute()) {
            assert response.body() != null;
            String rawResponse = response.body().string();
            resObj = JsonParser.parseString(rawResponse).getAsJsonObject();
        }

        String recievedVersion = resObj.get("tag_name").getAsString();

        if (recievedVersion.startsWith("v")) { // normally I won't add "v" prefix.
            recievedVersion = recievedVersion.substring(1);
        }

        int versionDifference = getVersionDifference(JKook.getImplementationVersion(), recievedVersion);
        if (versionDifference == -1) {
            JKook.getLogger().info("Update available! Information is following:");
            JKook.getLogger().info("New Version: {}, Currently on: {}", recievedVersion, JKook.getImplementationVersion());
            JKook.getLogger().info("Release Title: {}", resObj.get("name").getAsString());
            JKook.getLogger().info("Release Time: {}", resObj.get("published_at").getAsString());
            JKook.getLogger().info("Release message is following:");
            for (String body : resObj.get("body").getAsString().split("\r\n")) {
                JKook.getLogger().info(body);
            }
            JKook.getLogger().info("You can get the new version of KookBC at: https://github.com/SNWCreations/KookBC/releases/{}", recievedVersion);
        } else if (versionDifference == 0) {
            JKook.getLogger().info("You are using the latest version! :)");
        } else if (versionDifference == 1) {
            JKook.getLogger().info("The latest version from remote is not released on Github yet.");
            JKook.getLogger().info("Are you using development version?");
        } else {
            JKook.getLogger().info("Unable to compare the version! Internal method returns {}", versionDifference);
        }

    }

}
