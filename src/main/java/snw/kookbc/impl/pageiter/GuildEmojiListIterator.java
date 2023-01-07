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
import snw.jkook.entity.CustomEmoji;
import snw.jkook.entity.Guild;
import snw.kookbc.impl.KBCClient;
import snw.kookbc.impl.network.HttpAPIRoute;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class GuildEmojiListIterator extends PageIteratorImpl<Set<CustomEmoji>> {
    private final Guild guild;

    public GuildEmojiListIterator(KBCClient client, Guild guild) {
        super(client);
        this.guild = guild;
    }

    @Override
    protected String getRequestURL() {
        return String.format("%s?guild_id=%s", HttpAPIRoute.GUILD_EMOJI_LIST.toFullURL(), guild.getId());
    }

    @Override
    protected void processElements(JsonArray array) {
        object = new HashSet<>(array.size());
        for (JsonElement element : array) {
            JsonObject rawObj = element.getAsJsonObject();
            object.add(client.getStorage().getEmoji(rawObj.get("id").getAsString(), rawObj));
        }
    }

    @Override
    public Set<CustomEmoji> next() {
        return Collections.unmodifiableSet(super.next());
    }
}
