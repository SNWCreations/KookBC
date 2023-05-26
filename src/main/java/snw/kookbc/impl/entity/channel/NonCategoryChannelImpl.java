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

import com.google.gson.JsonObject;
import org.jetbrains.annotations.Nullable;
import snw.jkook.entity.Guild;
import snw.jkook.entity.Invitation;
import snw.jkook.entity.User;
import snw.jkook.entity.channel.Category;
import snw.jkook.entity.channel.NonCategoryChannel;
import snw.jkook.util.PageIterator;
import snw.kookbc.impl.KBCClient;
import snw.kookbc.impl.network.HttpAPIRoute;
import snw.kookbc.impl.pageiter.ChannelInvitationIterator;
import snw.kookbc.util.MapBuilder;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import static snw.kookbc.util.GsonUtil.get;

public abstract class NonCategoryChannelImpl extends ChannelImpl implements NonCategoryChannel {
    private Category parent;

    protected NonCategoryChannelImpl(KBCClient client, String id, User master, Guild guild, boolean permSync, Category parent, String name, Collection<RolePermissionOverwrite> rpo, Collection<UserPermissionOverwrite> upo, int level) {
        super(client, id, master, guild, permSync, name, rpo, upo, level);
        this.parent = parent;
    }

    @Override
    public String createInvite(int validSeconds, int validTimes) {
        Map<String, Object> body = new MapBuilder()
                .put("channel_id", getId())
                .put("duration", validSeconds)
                .put("setting_times", validTimes)
                .build();
        JsonObject object = client.getNetworkClient().post(HttpAPIRoute.INVITE_CREATE.toFullURL(), body);
        return get(object, "url").getAsString();
    }


    @Override
    public @Nullable Category getParent() {
        return parent;
    }

    @Override
    public void setParent(Category parent) {
        Map<String, Object> body = new MapBuilder()
                .put("channel_id", getId())
                .put("parent_id", (parent == null) ? 0 : parent.getId())
                .build();
        client.getNetworkClient().post(HttpAPIRoute.CHANNEL_UPDATE.toFullURL(), body);
        setParent0(parent);
    }

    public void setParent0(Category parent) {
        this.parent = parent;
    }

    @Override
    public PageIterator<Set<Invitation>> getInvitations() {
        return new ChannelInvitationIterator(client, this);
    }

}
