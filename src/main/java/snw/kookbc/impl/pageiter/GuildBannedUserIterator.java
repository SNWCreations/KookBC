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

import com.fasterxml.jackson.databind.JsonNode;
import snw.jkook.entity.Guild;
import snw.jkook.entity.User;
import snw.kookbc.impl.KBCClient;
import snw.kookbc.impl.network.HttpAPIRoute;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class GuildBannedUserIterator extends PageIteratorImpl<Set<User>> {
    private final String guildId;

    public GuildBannedUserIterator(KBCClient client, Guild guild) {
        super(client);
        guildId = guild.getId();
    }

    @Override
    protected String getRequestURL() {
        return String.format("%s?guild_id=%s", HttpAPIRoute.BLACKLIST_LIST.toFullURL(), guildId);
    }

    @Override
    protected void processElements(JsonNode node) {
        object = new HashSet<>(node.size());
        for (JsonNode element : node) {
            JsonNode userNode = element.get("user");
            String userId = userNode.get("id").asText();
            // 使用完整的用户数据,避免额外的 HTTP 请求
            object.add(client.getStorage().getUser(userId, userNode));
        }
    }

    @Override
    public Set<User> next() {
        return Collections.unmodifiableSet(super.next());
    }
}
