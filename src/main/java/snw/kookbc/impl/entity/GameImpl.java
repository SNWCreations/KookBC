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

import org.jetbrains.annotations.NotNull;
import snw.jkook.entity.Game;
import snw.kookbc.impl.KBCClient;
import snw.kookbc.impl.network.HttpAPIRoute;
import snw.kookbc.util.MapBuilder;

import java.util.Map;

public class GameImpl implements Game {
    private final KBCClient client;
    private final int id;
    private String name;
    private String icon;

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
        setName0(name);
    }

    public void setName0(String name) {
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
        setIcon0(iconUrl);
    }

    public void setIcon0(String iconUrl) {
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
        setName0(name);
        setIcon0(icon);
    }

}
