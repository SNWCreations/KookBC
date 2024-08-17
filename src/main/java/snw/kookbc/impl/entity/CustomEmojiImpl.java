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

import static snw.jkook.util.Validate.isTrue;
import static snw.kookbc.util.GsonUtil.getAsString;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

import com.google.gson.JsonObject;

import snw.jkook.entity.CustomEmoji;
import snw.jkook.entity.Guild;
import snw.kookbc.impl.KBCClient;
import snw.kookbc.impl.network.HttpAPIRoute;
import snw.kookbc.interfaces.Updatable;
import snw.kookbc.util.MapBuilder;

public class CustomEmojiImpl implements CustomEmoji, Updatable {
    private final KBCClient client;
    private final String id;
    private final Guild guild;
    private String name;

    public CustomEmojiImpl(KBCClient client, String id, Guild guild, String name) {
        this.client = client;
        this.id = id;
        this.guild = guild; // optional attributes are following:
        this.name = name;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        Map<String, Object> body = new MapBuilder()
                .put("id", getId())
                .put("name", name)
                .build();
        client.getNetworkClient().post(HttpAPIRoute.GUILD_EMOJI_UPDATE.toFullURL(), body);
        this.name = name;
    }

    @Override
    public Guild getGuild() {
        return guild;
    }

    @Override
    public void delete() {
        client.getNetworkClient().post(HttpAPIRoute.GUILD_EMOJI_DELETE.toFullURL(),
                Collections.singletonMap("id", getId()));
    }

    public void setName0(String name) {
        this.name = name;
    }

    @Override
    public synchronized void update(JsonObject data) {
        isTrue(Objects.equals(getId(), getAsString(data, "id")), "You can't update the emoji by using different data");
        this.name = getAsString(data, "name");
    }
}
