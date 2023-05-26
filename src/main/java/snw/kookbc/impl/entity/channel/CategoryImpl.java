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

package snw.kookbc.impl.entity.channel;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import snw.jkook.entity.Guild;
import snw.jkook.entity.User;
import snw.jkook.entity.channel.Category;
import snw.jkook.entity.channel.Channel;
import snw.kookbc.impl.KBCClient;
import snw.kookbc.impl.network.HttpAPIRoute;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;

import static snw.kookbc.util.GsonUtil.get;

public class CategoryImpl extends ChannelImpl implements Category {

    public CategoryImpl(KBCClient client, String id, User master, Guild guild, boolean permSync, Collection<RolePermissionOverwrite> rpo, Collection<UserPermissionOverwrite> upo, int level, String name) {
        super(client, id, master, guild, permSync, name, rpo, upo, level);
    }

    @Override
    public Collection<Channel> getChannels() {
        final JsonObject object = client.getNetworkClient()
                .get(HttpAPIRoute.CHANNEL_INFO.toFullURL() + "?target_id=" + getId() + "&need_children=true");
        client.getEntityUpdater().updateChannel(object, this);
        Collection<Channel> channels = new LinkedList<>();
        get(object, "children").getAsJsonArray()
                .asList()
                .stream()
                .map(JsonElement::getAsString)
                .forEach(id -> channels.add(client.getStorage().getChannel(id)));
        return Collections.unmodifiableCollection(channels);
    }

}
