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
import snw.jkook.entity.Invitation;
import snw.jkook.entity.User;
import snw.jkook.entity.channel.NonCategoryChannel;
import snw.kookbc.impl.KBCClient;
import snw.kookbc.impl.entity.InvitationImpl;
import snw.kookbc.impl.network.HttpAPIRoute;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class ChannelInvitationIterator extends PageIteratorImpl<Set<Invitation>> {
    private final NonCategoryChannel channel;

    public ChannelInvitationIterator(KBCClient client, NonCategoryChannel channel) {
        super(client);
        this.channel = channel;
    }

    @Override
    protected String getRequestURL() {
        return String.format("%s?channel_id=%s", HttpAPIRoute.INVITE_LIST.toFullURL(), channel.getId());
    }

    @Override
    protected void processElements(JsonNode node) {
        object = new HashSet<>(node.size());
        for (JsonNode element : node) {
            String guildId = element.get("guild_id").asText();
            Guild guild = client.getStorage().getGuild(guildId);
            String urlCode = element.get("url_code").asText();
            String url = element.get("url").asText();
            JsonNode userNode = element.get("user");
            String userId = userNode.get("id").asText();
            // 使用完整的用户数据,避免额外的 HTTP 请求
            User master = client.getStorage().getUser(userId, userNode);
            object.add(new InvitationImpl(
                    client, guild, channel, urlCode, url, master
            ));
        }
    }

    @Override
    public Set<Invitation> next() {
        return Collections.unmodifiableSet(super.next());
    }
}
