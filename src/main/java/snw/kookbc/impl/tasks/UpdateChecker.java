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
            client.getCore().getLogger().warn("无法从远程检查更新", e);
        }
    }

    private void run0() throws Exception {
        client.getCore().getLogger().info("正在检查更新...");
        if (!Objects.equals(SharedConstants.REPO_URL, "https://github.com/SNWCreations/KookBC")) {
            client.getCore().getLogger()
                    .warn("非官方 KookBC！我们无法为您检查更新。这是一个分支版本吗？");
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
                        .warn("无法检查更新！GitHub API 请求频率限制已超出，请稍后重试");
            } else {
                client.getCore().getLogger()
                        .warn("无法检查更新！GitHub API 返回错误: {}", errorMessage);
            }
            return;
        }

        JsonNode tagNameNode = resObj.get("tag_name");
        if (tagNameNode == null || tagNameNode.isNull()) {
            client.getCore().getLogger()
                    .warn("无法检查更新！GitHub API 响应缺少 'tag_name' 字段，API 格式可能已更改");
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
                    .warn("无法检查更新！版本号无法识别！自定义构建版本或快照 API？");
            return;
        }
        switch (versionDifference) {
            case -1: {
                client.getCore().getLogger().info("发现可用更新！相关信息如下：");
                client.getCore().getLogger().info("最新版本: {}，当前版本: {}", receivedVersion,
                        client.getCore().getImplementationVersion());

                JsonNode nameNode = resObj.get("name");
                String releaseName = nameNode != null && !nameNode.isNull() ? nameNode.asText() : "未知";
                client.getCore().getLogger().info("发布标题: {}", releaseName);

                JsonNode publishedAtNode = resObj.get("published_at");
                String publishedAt = publishedAtNode != null && !publishedAtNode.isNull() ? publishedAtNode.asText() : "未知";
                client.getCore().getLogger().info("发布时间: {}", publishedAt);

                client.getCore().getLogger().info("发布说明如下：");
                JsonNode bodyNode = resObj.get("body");
                String releaseBody = bodyNode != null && !bodyNode.isNull() ? bodyNode.asText() : "无发布说明";
                for (String body : releaseBody.split("\r\n")) {
                    client.getCore().getLogger().info(body);
                }
                client.getCore().getLogger().info(
                        "您可以在以下地址获取新版本的 KookBC: https://github.com/SNWCreations/KookBC/releases/{}",
                        receivedVersion);
                break;
            }
            case 0: {
                client.getCore().getLogger().info("您正在使用最新版本！:)");
                break;
            }
            case 1: {
                client.getCore().getLogger().info("您的 KookBC 版本比远程最新版本还要新！");
                client.getCore().getLogger().info("您是否正在使用开发版本？");
                break;
            }
            default: {
                client.getCore().getLogger().info("无法比较版本！内部方法返回值: {}",
                        versionDifference);
                break;
            }
        }

    }

}
