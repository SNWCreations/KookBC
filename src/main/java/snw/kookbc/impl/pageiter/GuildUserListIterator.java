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
import snw.jkook.entity.User;
import snw.kookbc.impl.KBCClient;
import snw.kookbc.impl.network.HttpAPIRoute;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class GuildUserListIterator extends PageIteratorImpl<Set<User>> {
    private final String guildId;
    private final String keyword;
    private final Integer roleId;
    private final Boolean requireMobileVerified;
    private final Boolean activeTimeFirst;
    private final Boolean joinedTimeFirst;

    public GuildUserListIterator(KBCClient client, String guildId) {
        this(client, guildId, null, null, null, null, null);
    }

    public GuildUserListIterator(KBCClient client, String guildId, String keyword, Integer roleId, Boolean requireMobileVerified, Boolean activeTimeFirst, Boolean joinedTimeFirst) {
        super(client);
        this.guildId = guildId;
        this.keyword = keyword;
        this.roleId = roleId;
        this.requireMobileVerified = requireMobileVerified;
        this.activeTimeFirst = activeTimeFirst;
        this.joinedTimeFirst = joinedTimeFirst;
    }

    @Override
    protected String getRequestURL() {
        String base = String.format("%s?guild_id=%s", HttpAPIRoute.GUILD_USERS.toFullURL(), guildId);
        if (keyword != null) {
            base = base + "&search=" + keyword;
        }
        if (roleId != null) {
            base = base + "&role_id=" + roleId;
        }
        if (requireMobileVerified != null) {
            base = base + "&mobile_verified=" + (requireMobileVerified ? 1 : 0);
        }
        if (activeTimeFirst != null) {
            base = base + "&active_time=" + (activeTimeFirst ? 1 : 0);
        }
        if (joinedTimeFirst != null) {
            base = base + "&joined_at=" + (joinedTimeFirst ? 1 : 0);
        }
        return base;
    }

    @Override
    protected void processElements(JsonNode node) {
        object = new HashSet<>(node.size());
        for (JsonNode element : node) {
            String userId = element.get("id").asText();
            // 使用完整的用户数据,避免额外的 HTTP 请求
            object.add(client.getStorage().getUser(userId, element));
        }
    }


    @Override
    public Set<User> next() {
        return Collections.unmodifiableSet(super.next());
    }
}
