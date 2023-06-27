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

package snw.kookbc.impl.serializer.event.guild;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import snw.jkook.entity.Guild;
import snw.jkook.entity.User;
import snw.jkook.event.guild.GuildUserNickNameUpdateEvent;
import snw.kookbc.impl.KBCClient;
import snw.kookbc.impl.entity.EntityStorage;
import snw.kookbc.impl.serializer.event.NormalEventDeserializer;

import java.lang.reflect.Type;

import static snw.kookbc.util.GsonUtil.get;
import static snw.kookbc.util.GsonUtil.has;

public class GuildUserNickNameUpdateEventDeserializer extends NormalEventDeserializer<GuildUserNickNameUpdateEvent> {

    public GuildUserNickNameUpdateEventDeserializer(KBCClient client) {
        super(client);
    }

    @Override
    protected GuildUserNickNameUpdateEvent deserialize(JsonObject object, Type type, JsonDeserializationContext ctx, long timeStamp, JsonObject body) throws JsonParseException {
        String guildId;
        User user;
        String nickname;
        EntityStorage entityStorage = client.getStorage();
        if (has(body, "my_nickname")) { // it is from GuildInfoUpdateEvent body...
            guildId = get(body, "id").getAsString();
            user = client.getCore().getUser();
            nickname = get(body, "my_nickname").getAsString();
        } else {
            guildId = get(object, "target_id").getAsString();
            user = entityStorage.getUser(get(body, "user_id").getAsString());
            nickname = get(body, "nickname").getAsString();
        }
        final Guild guild = entityStorage.getGuild(guildId);
        return new GuildUserNickNameUpdateEvent(
                timeStamp, guild, user, nickname
        );
    }

}
