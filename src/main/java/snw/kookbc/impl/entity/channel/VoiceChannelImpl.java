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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import snw.jkook.entity.Guild;
import snw.jkook.entity.User;
import snw.jkook.entity.channel.Category;
import snw.jkook.entity.channel.VoiceChannel;
import snw.kookbc.impl.KBCClient;
import snw.kookbc.impl.network.HttpAPIRoute;
import snw.kookbc.util.MapBuilder;

import java.util.*;

import static snw.kookbc.util.GsonUtil.get;

public class VoiceChannelImpl extends ChannelImpl implements VoiceChannel {
    private boolean passwordProtected;
    private int maxSize;

    public VoiceChannelImpl(KBCClient client, String id, User master, Guild guild, boolean permSync, Category parent, String name, Collection<RolePermissionOverwrite> rpo, Collection<UserPermissionOverwrite> upo, int level, boolean passwordProtected, int maxSize) {
        super(client, id, master, guild, permSync, parent, name, rpo, upo, level);
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
        JsonObject object = client.getNetworkClient().post(HttpAPIRoute.INVITE_CREATE.toFullURL(), body);
        return get(object, "url").getAsString();
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
        String rawContent = client.getNetworkClient().getRawContent(HttpAPIRoute.CHANNEL_USER_LIST.toFullURL() + "?channel_id=" + getId());
        JsonArray array = JsonParser.parseString(rawContent).getAsJsonObject().getAsJsonArray("data");
        Set<User> users = new HashSet<>();
        for (JsonElement element : array) {
            JsonObject obj = element.getAsJsonObject();
            users.add(client.getStorage().getUser(obj.get("id").getAsString(), obj));
        }
        return Collections.unmodifiableCollection(users);
    }

    @Override
    public void moveToHere(Collection<User> users) {
        Map<String, Object> body = new MapBuilder()
                .put("target", getId())
                .put("user_ids", users.stream().map(User::getId).toArray(String[]::new))
                .build();
        client.getNetworkClient().post(HttpAPIRoute.MOVE_USER.toFullURL(), body);
    }

    public void setPasswordProtected(boolean passwordProtected) {
        this.passwordProtected = passwordProtected;
    }
}
