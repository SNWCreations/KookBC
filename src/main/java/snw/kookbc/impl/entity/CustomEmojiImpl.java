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

import com.google.gson.JsonObject;
import snw.jkook.entity.CustomEmoji;
import snw.jkook.entity.Guild;
import snw.jkook.util.Validate;
import snw.kookbc.impl.KBCClient;
import snw.kookbc.impl.network.HttpAPIRoute;
import snw.kookbc.interfaces.Updatable;
import snw.kookbc.util.MapBuilder;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

import static snw.kookbc.util.GsonUtil.get;

public class CustomEmojiImpl implements CustomEmoji, Updatable {
    private final KBCClient client;
    private final String id;
    private final Guild guild;
    private String name;

    public CustomEmojiImpl(KBCClient client, String id, String name, Guild guild) {
        this.client = client;
        this.name = name;
        this.id = id;
        // optional attributes are following:
        this.guild = guild;
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
        client.getNetworkClient().post(HttpAPIRoute.GUILD_EMOJI_DELETE.toFullURL(), Collections.singletonMap("id", getId()));
    }

    public void setName0(String name) {
        this.name = name;
    }

    @Override
    public void update(JsonObject data) {
        Validate.isTrue(Objects.equals(getId(), get(data, "id").getAsString()), "You can't update the emoji by using different data");
        synchronized (this) {
            this.name = get(data, "name").getAsString();
        }
    }
}
