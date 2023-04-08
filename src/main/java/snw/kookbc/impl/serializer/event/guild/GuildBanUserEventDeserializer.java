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
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import snw.jkook.entity.User;
import snw.jkook.event.guild.GuildBanUserEvent;
import snw.kookbc.impl.KBCClient;
import snw.kookbc.impl.serializer.event.NormalEventDeserializer;
import snw.kookbc.impl.storage.EntityStorage;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static snw.kookbc.util.GsonUtil.get;

public class GuildBanUserEventDeserializer extends NormalEventDeserializer<GuildBanUserEvent> {

    public GuildBanUserEventDeserializer(KBCClient client) {
        super(client);
    }

    @Override
    protected GuildBanUserEvent deserialize(JsonObject object, Type type, JsonDeserializationContext ctx, long timeStamp, JsonObject body) throws JsonParseException {
        List<User> banned = Collections.unmodifiableList(
                body.getAsJsonArray("user_id")
                        .asList()
                        .stream()
                        .map(JsonElement::getAsString)
                        .map(i -> client.getStorage().getUser(i))
                        .collect(Collectors.toList())
        );
        EntityStorage entityStorage = client.getStorage();
        return new GuildBanUserEvent(
            timeStamp,
            entityStorage.getGuild(get(object, "target_id").getAsString()),
            banned,
            entityStorage.getUser(get(object, "operator_id").getAsString()),
            get(object, "remark").getAsString()
        );
    }

}
