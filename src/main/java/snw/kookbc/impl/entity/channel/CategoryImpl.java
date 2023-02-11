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

import org.jetbrains.annotations.Nullable;
import snw.jkook.entity.Guild;
import snw.jkook.entity.Invitation;
import snw.jkook.entity.User;
import snw.jkook.entity.channel.Category;
import snw.jkook.entity.channel.Channel;
import snw.jkook.util.PageIterator;
import snw.kookbc.impl.KBCClient;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class CategoryImpl extends ChannelImpl implements Category {
    // Yes, this implementation provides the channel list by using self-hosted cache.
    // Current KOOK Channel view API does NOT provide the channels under the category.
    // So the getChannels method is UNSAFE. The result maybe not full.
    private final Collection<Channel> channels = new HashSet<>();

    public CategoryImpl(KBCClient client, String id, User master, Guild guild, boolean permSync, Collection<RolePermissionOverwrite> rpo, Collection<UserPermissionOverwrite> upo, int level, String name) {
        super(client, id, master, guild, permSync, null, name, rpo, upo, level);
    }

    @Override
    public Collection<Channel> getChannels() {
        return Collections.unmodifiableCollection(channels);
    }

    // to modify channels, use this instead of getChannels()
    public Collection<Channel> getChannels0() {
        return channels;
    }

    @Override
    public @Nullable Category getParent() {
        throw new UnsupportedOperationException("No parent will be provided for Category!");
    }

    @Override
    public void setParent(Category parent) {
        throw new UnsupportedOperationException("No parent will be provided for Category!");
    }

    @Override
    public PageIterator<Set<Invitation>> getInvitations() {
        throw new UnsupportedOperationException("No invitations available for Category!");
    }

}
