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

package snw.kookbc.impl.entity.builder;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import snw.jkook.entity.*;
import snw.jkook.entity.channel.Category;
import snw.jkook.entity.channel.Channel;
import snw.jkook.util.Validate;
import snw.kookbc.impl.KBCClient;
import snw.kookbc.impl.entity.*;
import snw.kookbc.impl.entity.channel.CategoryImpl;
import snw.kookbc.impl.entity.channel.ChannelImpl;
import snw.kookbc.impl.entity.channel.TextChannelImpl;
import snw.kookbc.impl.entity.channel.VoiceChannelImpl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;

import static snw.kookbc.util.GsonUtil.get;

// The class for building entities.
public class EntityUpdater {
    private final KBCClient client;

    public EntityUpdater(KBCClient client) {
        this.client = client;
    }

    public void updateUser(JsonObject object, User user) {
        Validate.isTrue(Objects.equals(user.getId(), get(object, "id").getAsString()), "You can't update user by using different data");
        String userName = get(object, "username").getAsString();
        String avatar = get(object, "avatar").getAsString();
        String vipAvatar = get(object, "vip_avatar").getAsString();
        int identify = get(object, "identify_num").getAsInt();
        boolean ban = get(object, "status").getAsInt() == 10;
        boolean vip = get(object, "is_vip").getAsBoolean();
        UserImpl usr = (UserImpl) user;
        usr.setName(userName);
        usr.setAvatarUrl(avatar);
        usr.setVipAvatarUrl(vipAvatar);
        usr.setIdentify(identify);
        usr.setBan(ban);
        usr.setVip(vip);
    }

    public void updateGuild(JsonObject object, Guild guild) {
        Validate.isTrue(Objects.equals(guild.getId(), get(object, "id").getAsString()), "You can't update guild by using different data");
        String name = get(object, "name").getAsString();
        boolean isPublic = get(object, "enable_open").getAsBoolean();
        String region = get(object, "region").getAsString();
        Guild.NotifyType type = Guild.NotifyType.value(get(object, "notify_type").getAsInt());
        Validate.notNull(type, "Internal Error: Unexpected NotifyType from remote");
        String avatar = get(object, "icon").getAsString();

        GuildImpl guildimpl = (GuildImpl) guild;
        guildimpl.setName(name);
        guildimpl.setPublic(isPublic);
        guildimpl.setRegion(region);
        guildimpl.setAvatar(avatar);
    }

    public void updateChannel(JsonObject object, Channel channel) {
        // basic information
        Validate.isTrue(Objects.equals(channel.getId(), get(object, "id").getAsString()), "You can't update channel by using different data");
        String name = get(object, "name").getAsString();
        boolean isPermSync = get(object, "permission_sync").getAsInt() != 0;
        // rpo parse
        Collection<Channel.RolePermissionOverwrite> rpo = new ArrayList<>();
        for (JsonElement element : get(object, "permission_overwrites").getAsJsonArray()) {
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
        for (JsonElement element : get(object, "permission_users").getAsJsonArray()) {
            JsonObject oupo = element.getAsJsonObject();
            JsonObject rawUser = oupo.getAsJsonObject("user");
            upo.add(
                    new Channel.UserPermissionOverwrite(
                            client.getStorage().getUser(rawUser.get("id").getAsString(), rawUser),
                            oupo.get("allow").getAsInt(),
                            oupo.get("deny").getAsInt()
                    )
            );
        }

        ((ChannelImpl) channel).setName0(name);
        ((ChannelImpl) channel).setOverwrittenRolePermissions(rpo);
        ((ChannelImpl) channel).setOverwrittenUserPermissions(upo);

        if (get(object, "is_category").getAsInt() == 1) {
            CategoryImpl category = (CategoryImpl) channel;
            category.setPermissionSync(isPermSync);
        } else {
            String parentId = get(object, "parent_id").getAsString();
            Category parent = ("".equals(parentId) || "0".equals(parentId)) ? null : (Category) client.getStorage().getChannel(parentId);
            ((ChannelImpl) channel).setParent0(parent);
            int type = get(object, "type").getAsInt();
            if (type == 1) { // TextChannel
                int chatLimitTime = get(object, "slow_mode").getAsInt();
                String topic = get(object, "topic").getAsString();
                ((TextChannelImpl) channel).setChatLimitTime0(chatLimitTime);
                ((TextChannelImpl) channel).setTopic0(topic);
            } else if (type == 2) { // VoiceChannel
                boolean hasPassword = object.has("has_password") && get(object, "has_password").getAsBoolean();
                int size = object.has("limit_amount") ? get(object, "limit_amount").getAsInt() : 0;
                ((VoiceChannelImpl) channel).setPasswordProtected(hasPassword);
                ((VoiceChannelImpl) channel).setMaxSize(size);
            }
        }
    }

    public void updateRole(JsonObject object, Role role) {
        Validate.isTrue(role.getId() == get(object, "role_id").getAsInt(), "You can't update the role by using different data");
        String name = get(object, "name").getAsString();
        int color = get(object, "color").getAsInt();
        int pos = get(object, "position").getAsInt();
        boolean hoist = get(object, "hoist").getAsInt() == 1;
        boolean mentionable = get(object, "mentionable").getAsInt() == 1;
        int permissions = get(object, "permissions").getAsInt();
        RoleImpl roleImpl = (RoleImpl) role;
        roleImpl.setName(name);
        roleImpl.setPermSum(permissions);
        roleImpl.setColor(color);
        roleImpl.setPosition(pos);
        roleImpl.setHoist0(hoist);
        roleImpl.setMentionable0(mentionable);
    }

    public void updateEmoji(JsonObject object, CustomEmoji emoji) {
        Validate.isTrue(Objects.equals(emoji.getId(), get(object, "id").getAsString()), "You can't update the emoji by using different data");
        String name = get(object, "name").getAsString();
        CustomEmojiImpl emojiImpl = (CustomEmojiImpl) emoji;
        emojiImpl.setName0(name);
    }

    public void updateGame(JsonObject object, Game game) {
        GameImpl impl = (GameImpl) game;
        String name = get(object, "name").getAsString();
        String icon = get(object, "icon").getAsString();
        impl.setName0(name);
        impl.setIcon0(icon);
    }
}
