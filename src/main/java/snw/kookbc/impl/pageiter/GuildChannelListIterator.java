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

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;

import snw.jkook.entity.channel.Channel;
import snw.kookbc.impl.KBCClient;
import snw.kookbc.impl.network.HttpAPIRoute;

public class GuildChannelListIterator extends PageIteratorImpl<Set<Channel>> {
    private final String guildId;

    public GuildChannelListIterator(KBCClient client, String guildId) {
        super(client);
        this.guildId = guildId;
    }

    @Override
    protected String getRequestURL() {
        return String.format("%s?guild_id=%s", HttpAPIRoute.CHANNEL_LIST.toFullURL(), guildId);
    }

    @Override
    protected void processElements(JsonNode node) {
        object = new HashSet<>(node.size());
        for (JsonNode element : node) {
            // 使用完整的频道数据,避免额外的 HTTP 请求
            object.add(client.getStorage().getChannel(element.get("id").asText(), element));
        }
    }

    @Override
    public Set<Channel> next() {
        return Collections.unmodifiableSet(super.next());
    }
}
