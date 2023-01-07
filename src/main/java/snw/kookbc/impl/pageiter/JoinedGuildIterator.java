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
import snw.kookbc.impl.KBCClient;
import snw.kookbc.impl.network.HttpAPIRoute;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

public class JoinedGuildIterator extends PageIteratorImpl<Collection<Guild>> {

    public JoinedGuildIterator(KBCClient client) {
        super(client);
    }

    @Override
    protected String getRequestURL() {
        return HttpAPIRoute.GUILD_JOINED_LIST.toFullURL();
    }

    @Override
    protected void processElements(JsonArray array) {
        object = new HashSet<>(array.size());
        for (JsonElement element : array) {
            JsonObject rawObj = element.getAsJsonObject();
            object.add(client.getStorage().getGuild(rawObj.get("id").getAsString(), rawObj));
        }
    }

    @Override
    public Collection<Guild> next() {
        return Collections.unmodifiableCollection(super.next());
    }
}
