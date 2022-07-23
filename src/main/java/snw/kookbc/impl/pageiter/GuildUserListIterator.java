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
import com.google.gson.JsonObject;
import snw.jkook.entity.User;
import snw.kookbc.impl.KBCClient;
import snw.kookbc.impl.network.HttpAPIRoute;

import java.util.Collections;
import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.Set;

public class GuildUserListIterator extends PageIteratorImpl<Set<User>> {
    private final String guildId;
    private final String keyword;
    private final Integer roleId;
    private final Boolean requireMobileVerified;
    private final Boolean activeTimeFirst;
    private final Boolean joinedTimeFirst;
    private Set<User> result;

    public GuildUserListIterator(String guildId) {
        this(guildId, null, null, null, null, null);
    }

    public GuildUserListIterator(String guildId, String keyword, Integer roleId, Boolean requireMobileVerified, Boolean activeTimeFirst, Boolean joinedTimeFirst) {
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
    protected void processElements(JsonArray array) {
        result = new HashSet<>();
        for (JsonElement element : array) {
            JsonObject object = element.getAsJsonObject();
            result.add(KBCClient.getInstance().getStorage().getUser(object.get("id").getAsString()));
        }
    }

    @Override
    protected void onHasNextButNoMoreElement() {
        result = null;
    }

    @Override
    public Set<User> next() {
        if (result == null) {
            throw new NoSuchElementException();
        }
        return Collections.unmodifiableSet(result);
    }
}
