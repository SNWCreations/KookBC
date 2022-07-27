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

package snw.kookbc.impl.entity.channel;

import com.google.gson.JsonObject;
import snw.jkook.entity.Guild;
import snw.jkook.entity.User;
import snw.jkook.entity.channel.Category;
import snw.jkook.entity.channel.VoiceChannel;
import snw.kookbc.impl.KBCClient;
import snw.kookbc.impl.network.HttpAPIRoute;
import snw.kookbc.util.MapBuilder;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;

public class VoiceChannelImpl extends ChannelImpl implements VoiceChannel {
    private final Collection<User> users = new HashSet<>();
    private boolean passwordProtected;
    private int maxSize;

    public VoiceChannelImpl(String id, User master, Guild guild, boolean permSync, Category parent, String name, Collection<RolePermissionOverwrite> rpo, Collection<UserPermissionOverwrite> upo, boolean passwordProtected, int maxSize) {
        super(id, master, guild, permSync, parent, name, rpo, upo);
        this.passwordProtected = passwordProtected;
        this.maxSize = maxSize;
    }

    @Override
    public String createInvite(int validSeconds, int validTimes) {
        Map<String, Object> body = new MapBuilder()
                .put("channel_id", getId())
                .put("duration", validSeconds)
                .put("setting_times", validTimes)
                .build();
        JsonObject object = KBCClient.getInstance().getConnector().getClient().post(HttpAPIRoute.INVITE_CREATE.toFullURL(), body);
        return object.get("url").getAsString();
    }

    @Override
    public boolean hasPassword() {
        return passwordProtected;
    }

    @Override
    public int getMaxSize() {
        return maxSize;
    }

    public void setMaxSize(int maxSize) {
        this.maxSize = maxSize;
    }

    @Override
    public Collection<User> getUsers() {
        return Collections.unmodifiableCollection(users);
    }

    // if we need to modify the users, use this method instead of getUsers().
    public Collection<User> getUsers0() {
        return users;
    }

    @Override
    public void moveToHere(Collection<User> users) {
        Map<String, Object> body = new MapBuilder()
                .put("target", getId())
                .put("user_ids", users.stream().map(User::getId).toArray(String[]::new))
                .build();
        KBCClient.getInstance().getConnector().getClient().post(HttpAPIRoute.MOVE_USER.toFullURL(), body);
        getUsers0().addAll(users);
    }

    public void setPasswordProtected(boolean passwordProtected) {
        this.passwordProtected = passwordProtected;
    }
}
