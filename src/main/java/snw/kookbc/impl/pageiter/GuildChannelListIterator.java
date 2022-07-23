/*
 *     KookBC -- The Kook Bot Client & JKook API standard implementation for Java.
 *     Copyright (C) 2022 KookBC contributors
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
import snw.jkook.entity.channel.Channel;
import snw.kookbc.impl.KBCClient;
import snw.kookbc.impl.network.HttpAPIRoute;

import java.util.Collections;
import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.Set;

public class GuildChannelListIterator extends PageIteratorImpl<Set<Channel>> {
    private final String guildId;
    private Set<Channel> result;

    public GuildChannelListIterator(String guildId) {
        this.guildId = guildId;
    }

    @Override
    protected String getRequestURL() {
        return String.format("%s?guild_id=%s", HttpAPIRoute.CHANNEL_LIST.toFullURL(), guildId);
    }

    @Override
    protected void processElements(JsonArray array) {
        result = new HashSet<>();
        for (JsonElement element : array) {
            result.add(KBCClient.getInstance().getStorage().getChannel(element.getAsJsonObject().get("id").getAsString()));
        }
    }

    @Override
    protected void onHasNextButNoMoreElement() {
        result = null;
    }

    @Override
    public Set<Channel> next() {
        if (result == null) {
            throw new NoSuchElementException();
        }
        return Collections.unmodifiableSet(result);
    }
}
