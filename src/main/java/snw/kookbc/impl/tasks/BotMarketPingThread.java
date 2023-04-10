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

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import snw.kookbc.impl.KBCClient;
import snw.kookbc.impl.network.Connector;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static snw.kookbc.util.GsonUtil.get;

public class BotMarketPingThread extends Thread {
    private final KBCClient client;
    private final Request request;
    private static final OkHttpClient networkClient = new OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .callTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build();

    public BotMarketPingThread(KBCClient client, String rawBotMarketUUID) {
        this.client = client;
        this.request = new Request.Builder()
                .get()
                .url("https://bot.gekj.net/api/v1/online.bot")
                .header("uuid", rawBotMarketUUID)
                .build();
        this.setDaemon(true);
    }

    @Override
    public void run() {
        try {
            run0();
        } catch (InterruptedException ignored) {
        } catch (Exception e) {
            client.getCore().getLogger().error("Thread terminated because an exception occurred.", e);
        }
    }

    public void run0() throws InterruptedException {
        while (client.isRunning()) {
            //noinspection BusyWait
            Thread.sleep(1000L * 60 * 5);
            if (!client.isRunning()) return;
            if (
                    !Optional.ofNullable(client.getConnector())
                            .map(Connector::isConnected)
                            .orElse(true) // in Webhook Mode?
            ) {
                continue;
            }
            client.getCore().getLogger().debug("PING BotMarket...");
            try (Response response = networkClient.newCall(request).execute()) {
                if (response.body() != null) {
                    String resStr = response.body().string();
                    JsonObject object = JsonParser.parseString(resStr).getAsJsonObject();
                    int status = get(object, "code").getAsInt();
                    if (status != 0) {
                        throw new RuntimeException(String.format("Unexpected Response Code: %s, message: %s", status,
                                get(object, "message").getAsString()));
                    }
                } else {
                    throw new RuntimeException("No response body when we attempting to PING BotMarket.");
                }
            } catch (Exception e) {
                client.getCore().getLogger().error("Unable to PING BotMarket.", e);
                continue;
            }
            client.getCore().getLogger().debug("PING BotMarket success");
        }
    }
}
