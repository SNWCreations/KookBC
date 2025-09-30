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

package snw.kookbc.impl.entity;

import static snw.kookbc.util.JacksonUtil.getAsString;
import static snw.kookbc.util.JacksonUtil.getStringOrDefault;

import java.util.Map;

import org.jetbrains.annotations.NotNull;

import com.fasterxml.jackson.databind.JsonNode;

import snw.jkook.entity.Game;
import snw.kookbc.impl.KBCClient;
import snw.kookbc.impl.network.HttpAPIRoute;
import snw.kookbc.interfaces.Updatable;
import snw.kookbc.util.MapBuilder;

public class GameImpl implements Game, Updatable {
    private final KBCClient client;
    private final int id;
    private String name;
    private String icon;

    public GameImpl(KBCClient client, int id) {
        this.client = client;
        this.id = id;
    }

    public GameImpl(KBCClient client, int id, String name, String icon) {
        this.client = client;
        this.id = id;
        this.name = name;
        this.icon = icon;
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        Map<String, Object> body = new MapBuilder()
                .put("id", id)
                .put("name", name)
                .build();
        client.getNetworkClient().post(HttpAPIRoute.GAME_UPDATE.toFullURL(), body);
        this.name = name;
    }

    @Override
    public String getIcon() {
        return icon;
    }

    @Override
    public void setIcon(String iconUrl) {
        Map<String, Object> body = new MapBuilder()
                .put("id", id)
                .put("icon", iconUrl)
                .build();
        client.getNetworkClient().post(HttpAPIRoute.GAME_UPDATE.toFullURL(), body);
        this.icon = iconUrl;
    }

    @Override
    public void setNameAndIcon(@NotNull String name, @NotNull String icon) {
        Map<String, Object> body = new MapBuilder()
                .put("id", id)
                .put("name", name)
                .put("icon", icon)
                .build();
        client.getNetworkClient().post(HttpAPIRoute.GAME_UPDATE.toFullURL(), body);
        this.name = name;
        this.icon = icon;
    }

    // GSON compatibility method
    public synchronized void update(com.google.gson.JsonObject data) {
        update(snw.kookbc.util.JacksonUtil.parse(data.toString()));
    }

    // ===== Jackson API - 高性能版本 =====

    @Override
    public synchronized void update(JsonNode data) {
        this.name = getStringOrDefault(data, "name", "Unknown Game");
        this.icon = getStringOrDefault(data, "icon", "");
    }
}
