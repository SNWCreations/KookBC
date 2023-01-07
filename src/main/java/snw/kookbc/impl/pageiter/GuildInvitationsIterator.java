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

package snw.kookbc.impl.pageiter;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import snw.jkook.entity.Guild;
import snw.jkook.entity.Invitation;
import snw.jkook.entity.User;
import snw.kookbc.impl.KBCClient;
import snw.kookbc.impl.entity.InvitationImpl;
import snw.kookbc.impl.network.HttpAPIRoute;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class GuildInvitationsIterator extends PageIteratorImpl<Set<Invitation>> {
    private final KBCClient client;
    private final String guildId;

    public GuildInvitationsIterator(KBCClient client, Guild guild) {
        super(client);
        this.client = client;
        this.guildId = guild.getId();
    }

    @Override
    protected String getRequestURL() {
        return String.format("%s?guild_id=%s", HttpAPIRoute.INVITE_LIST.toFullURL(), guildId);
    }

    @Override
    protected void processElements(JsonArray array) {
        object = new HashSet<>(array.size());
        for (JsonElement element : array) {
            JsonObject rawObj = element.getAsJsonObject();
            Guild guild = client.getStorage().getGuild(rawObj.get("guild_id").getAsString());
            String urlCode = rawObj.get("url_code").getAsString();
            String url = rawObj.get("url").getAsString();
            User master = client.getStorage().getUser(rawObj.getAsJsonObject("user").get("id").getAsString());
            object.add(new InvitationImpl(
                    client, guild, null, urlCode, url, master
            ));
        }
    }

    @Override
    public Set<Invitation> next() {
        return Collections.unmodifiableSet(super.next());
    }
}
