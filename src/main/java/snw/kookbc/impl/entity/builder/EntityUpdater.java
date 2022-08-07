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

package snw.kookbc.impl.entity.builder;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import snw.jkook.entity.CustomEmoji;
import snw.jkook.entity.Guild;
import snw.jkook.entity.Role;
import snw.jkook.entity.User;
import snw.jkook.entity.channel.Category;
import snw.jkook.entity.channel.Channel;
import snw.kookbc.impl.KBCClient;
import snw.kookbc.impl.entity.CustomEmojiImpl;
import snw.kookbc.impl.entity.GuildImpl;
import snw.kookbc.impl.entity.RoleImpl;
import snw.kookbc.impl.entity.UserImpl;
import snw.kookbc.impl.entity.channel.CategoryImpl;
import snw.kookbc.impl.entity.channel.ChannelImpl;
import snw.kookbc.impl.entity.channel.TextChannelImpl;
import snw.kookbc.impl.entity.channel.VoiceChannelImpl;
import snw.jkook.util.Validate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;

// The class for building entities.
public class EntityUpdater {

    public void updateUser(JsonObject object, User user) {
        Validate.isTrue(Objects.equals(user.getId(), object.get("id").getAsString()), "You can't update user by using different data");
        String userName = object.get("username").getAsString();
        String avatar = object.get("avatar").getAsString();
        String vipAvatar = object.get("vip_avatar").getAsString();
        int identify = object.get("identify_num").getAsInt();
        boolean online = object.get("online").getAsBoolean();
        boolean ban = object.get("status").getAsInt() == 10;
        boolean vip = object.get("is_vip").getAsBoolean();
        JsonArray roleArray = object.get("roles").getAsJsonArray();
        Collection<Integer> roles = new HashSet<>();
        roleArray.forEach(IT -> roles.add(IT.getAsInt()));
        UserImpl usr = (UserImpl) user;
        usr.setName(userName);
        usr.setAvatarUrl(avatar);
        usr.setVipAvatarUrl(vipAvatar);
        usr.setIdentify(identify);
        usr.setOnline(online);
        usr.setBan(ban);
        usr.setVip(vip);
        usr.setRoles(roles);
    }

    public void updateGuild(JsonObject object, Guild guild) {
        Validate.isTrue(Objects.equals(guild.getId(), object.get("id").getAsString()), "You can't update guild by using different data");
        String name = object.get("name").getAsString();
        boolean isPublic = object.get("enable_open").getAsBoolean();
        String region = object.get("region").getAsString();
        Guild.NotifyType type = Guild.NotifyType.value(object.get("notify_type").getAsInt());
        Validate.notNull(type, "Internal Error: Unexpected NotifyType from remote");
        String avatar = object.get("icon").getAsString();

        GuildImpl guildimpl = (GuildImpl) guild;
        guildimpl.setName(name);
        guildimpl.setPublic(isPublic);
        guildimpl.setRegion(region);
        guildimpl.setAvatar(avatar);
    }

    public void updateChannel(JsonObject object, Channel channel) {
        // basic information
        Validate.isTrue(Objects.equals(channel.getId(), object.get("id").getAsString()), "You can't update channel by using different data");
        String name = object.get("name").getAsString();
        boolean isPermSync = object.get("permission_sync").getAsInt() != 0;
        // rpo parse
        Collection<Channel.RolePermissionOverwrite> rpo = new ArrayList<>();
        for (JsonElement element : object.get("permission_overwrites").getAsJsonArray()) {
            JsonObject orpo = element.getAsJsonObject();
            rpo.add(
                    new Channel.RolePermissionOverwrite(
                            orpo.get("role_id").getAsInt(),
                            orpo.get("allow").getAsInt(),
                            orpo.get("deny").getAsInt()
                    )
            );
        }

        // upo parse
        Collection<Channel.UserPermissionOverwrite> upo = new ArrayList<>();
        for (JsonElement element : object.get("permission_users").getAsJsonArray()) {
            JsonObject oupo = element.getAsJsonObject();
            JsonObject rawUser = oupo.getAsJsonObject("user");
            upo.add(
                    new Channel.UserPermissionOverwrite(
                            KBCClient.getInstance().getStorage().getUser(rawUser.get("id").getAsString(), rawUser),
                            oupo.get("allow").getAsInt(),
                            oupo.get("deny").getAsInt()
                    )
            );
        }

        ((ChannelImpl) channel).setOverwrittenRolePermissions(rpo);
        ((ChannelImpl) channel).setOverwrittenUserPermissions(upo);

        if (object.get("is_category").getAsBoolean()) {
            CategoryImpl category = (CategoryImpl) channel;
            category.setName(name);
            category.setPermissionSync(isPermSync);
        } else {
            String parentId = object.get("parent_id").getAsString();
            Category parent = (parentId.isEmpty()) ? null : (Category) KBCClient.getInstance().getStorage().getChannel(parentId);
            ((ChannelImpl) channel).setParent(parent);
            int type = object.get("type").getAsInt();
            if (type == 1) { // TextChannel
                int chatLimitTime = object.get("slow_mode").getAsInt();
                String topic = object.get("topic").getAsString();
                ((TextChannelImpl) channel).setChatLimitTime0(chatLimitTime);
                ((TextChannelImpl) channel).setTopic0(topic);
            } else if (type == 2) { // VoiceChannel
                boolean hasPassword = object.has("has_password") && object.get("has_password").getAsBoolean();
                int size = object.get("limit_amount").getAsInt();
                ((VoiceChannelImpl) channel).setPasswordProtected(hasPassword);
                ((VoiceChannelImpl) channel).setMaxSize(size);
            }
        }
    }

    public void updateRole(JsonObject object, Role role) {
        Validate.isTrue(role.getId() == object.get("role_id").getAsInt(), "You can't update the role by using different data");
        String name = object.get("name").getAsString();
        int color = object.get("color").getAsInt();
        int pos = object.get("position").getAsInt();
        boolean hoist = object.get("hoist").getAsInt() == 1;
        boolean mentionable = object.get("mentionable").getAsInt() == 1;
        int permissions = object.get("permissions").getAsInt();
        RoleImpl roleImpl = (RoleImpl) role;
        roleImpl.setName(name);
        roleImpl.setPermSum(permissions);
        roleImpl.setColor(color);
        roleImpl.setPosition(pos);
        roleImpl.setHoist0(hoist);
        roleImpl.setMentionable0(mentionable);
    }

    public void updateEmoji(JsonObject object, CustomEmoji emoji) {
        Validate.isTrue(Objects.equals(emoji.getId(), object.get("id").getAsString()), "You can't update the emoji by using different data");
        String name = object.get("name").getAsString();
        CustomEmojiImpl emojiImpl = (CustomEmojiImpl) emoji;
        emojiImpl.setName0(name);
    }
}
