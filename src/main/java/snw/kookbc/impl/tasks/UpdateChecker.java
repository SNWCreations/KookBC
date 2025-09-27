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

package snw.kookbc.impl.tasks;

import static snw.kookbc.util.Util.getVersionDifference;

import java.util.Objects;

import com.fasterxml.jackson.databind.JsonNode;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import snw.kookbc.SharedConstants;
import snw.kookbc.impl.KBCClient;
import snw.kookbc.util.JacksonUtil;

public final class UpdateChecker extends Thread {
    private final KBCClient client;

    public UpdateChecker(KBCClient client) {
        super("Update Checker");
        this.client = client;
    }

    @Override
    public void run() {
        try {
            run0();
        } catch (Exception e) {
            client.getCore().getLogger().warn("Unable to check update from remote.", e);
        }
    }

    private void run0() throws Exception {
        client.getCore().getLogger().info("Checking updates...");
        if (!Objects.equals(SharedConstants.REPO_URL, "https://github.com/SNWCreations/KookBC")) {
            client.getCore().getLogger()
                    .warn("Not Official KookBC! We cannot check updates for you. Is this a fork version?");
            return;
        }
        final Request req = new Request.Builder()
                .get()
                .url("https://api.github.com/repos/SNWCreations/KookBC/releases/latest")
                .build();
        JsonNode resObj;
        try (Response response = new OkHttpClient().newCall(req).execute()) {
            final ResponseBody body = response.body();
            assert body != null;
            resObj = JacksonUtil.parse(body.string());
        }

        // 检查 GitHub API 错误响应
        JsonNode messageNode = resObj.get("message");
        if (messageNode != null && !messageNode.isNull()) {
            String errorMessage = messageNode.asText();
            if (errorMessage.contains("API rate limit exceeded")) {
                client.getCore().getLogger()
                        .warn("Cannot check update! GitHub API rate limit exceeded. Please try again later.");
            } else {
                client.getCore().getLogger()
                        .warn("Cannot check update! GitHub API returned error: {}", errorMessage);
            }
            return;
        }

        JsonNode tagNameNode = resObj.get("tag_name");
        if (tagNameNode == null || tagNameNode.isNull()) {
            client.getCore().getLogger()
                    .warn("Cannot check update! GitHub API response missing 'tag_name' field. API format may have changed.");
            return;
        }

        String receivedVersion = tagNameNode.asText();

        if (receivedVersion.startsWith("v")) { // normally I won't add "v" prefix.
            receivedVersion = receivedVersion.substring(1);
        }

        int versionDifference;
        try {
            versionDifference = getVersionDifference(client.getCore().getImplementationVersion(), receivedVersion);
        } catch (NumberFormatException e) {
            client.getCore().getLogger()
                    .warn("Cannot check update! We can't recognize version! Custom build or snapshot API?");
            return;
        }
        switch (versionDifference) {
            case -1: {
                client.getCore().getLogger().info("Update available! Information is following:");
                client.getCore().getLogger().info("New Version: {}, Currently on: {}", receivedVersion,
                        client.getCore().getImplementationVersion());

                JsonNode nameNode = resObj.get("name");
                String releaseName = nameNode != null && !nameNode.isNull() ? nameNode.asText() : "Unknown";
                client.getCore().getLogger().info("Release Title: {}", releaseName);

                JsonNode publishedAtNode = resObj.get("published_at");
                String publishedAt = publishedAtNode != null && !publishedAtNode.isNull() ? publishedAtNode.asText() : "Unknown";
                client.getCore().getLogger().info("Release Time: {}", publishedAt);

                client.getCore().getLogger().info("Release message is following:");
                JsonNode bodyNode = resObj.get("body");
                String releaseBody = bodyNode != null && !bodyNode.isNull() ? bodyNode.asText() : "No release notes available";
                for (String body : releaseBody.split("\r\n")) {
                    client.getCore().getLogger().info(body);
                }
                client.getCore().getLogger().info(
                        "You can get the new version of KookBC at: https://github.com/SNWCreations/KookBC/releases/{}",
                        receivedVersion);
                break;
            }
            case 0: {
                client.getCore().getLogger().info("You are using the latest version! :)");
                break;
            }
            case 1: {
                client.getCore().getLogger().info("Your KookBC is newer than the latest version from remote!");
                client.getCore().getLogger().info("Are you using development version?");
                break;
            }
            default: {
                client.getCore().getLogger().info("Unable to compare the version! Internal method returns {}",
                        versionDifference);
                break;
            }
        }

    }

}
