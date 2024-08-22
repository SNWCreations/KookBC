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

import static snw.kookbc.impl.entity.builder.EntityBuildUtil.parseEmojiGuild;
import static snw.kookbc.impl.entity.builder.EntityBuildUtil.parseNotifyType;
import static snw.kookbc.impl.entity.builder.EntityBuildUtil.parseRPO;
import static snw.kookbc.impl.entity.builder.EntityBuildUtil.parseUPO;
import static snw.kookbc.util.GsonUtil.getAsBoolean;
import static snw.kookbc.util.GsonUtil.getAsInt;
import static snw.kookbc.util.GsonUtil.getAsString;

import java.util.Collection;

import com.google.gson.JsonObject;

import snw.jkook.entity.CustomEmoji;
import snw.jkook.entity.Game;
import snw.jkook.entity.Guild;
import snw.jkook.entity.Guild.NotifyType;
import snw.jkook.entity.Role;
import snw.jkook.entity.User;
import snw.jkook.entity.channel.Category;
import snw.jkook.entity.channel.Channel;
import snw.kookbc.impl.KBCClient;
import snw.kookbc.impl.entity.CustomEmojiImpl;
import snw.kookbc.impl.entity.GameImpl;
import snw.kookbc.impl.entity.GuildImpl;
import snw.kookbc.impl.entity.RoleImpl;
import snw.kookbc.impl.entity.UserImpl;
import snw.kookbc.impl.entity.channel.CategoryImpl;
import snw.kookbc.impl.entity.channel.TextChannelImpl;
import snw.kookbc.impl.entity.channel.VoiceChannelImpl;

// The class for building entities.
public class EntityBuilder {
    private final KBCClient client;

    public EntityBuilder(KBCClient client) {
        this.client = client;
    }

    public User buildUser(JsonObject s) {
        final String id = getAsString(s, "id");
        final boolean bot = getAsBoolean(s, "bot");
        final String name = getAsString(s, "username");
        final int identify = getAsInt(s, "identify_num");
        final boolean ban = getAsInt(s, "status") == 10;
        final boolean vip = getAsBoolean(s, "is_vip");
        final String avatarUrl = getAsString(s, "avatar");
        final String vipAvatarUrl = getAsString(s, "vip_avatar");
        return new UserImpl(client, id, bot, name, identify, ban, vip, avatarUrl, vipAvatarUrl);
    }

    public Guild buildGuild(JsonObject object) {
        final String id = getAsString(object, "id");
        final NotifyType notifyType = parseNotifyType(object);
        final User master = client.getStorage().getUser(getAsString(object, "master_id"));
        final String name = getAsString(object, "name");
        final boolean public_ = getAsBoolean(object, "enable_open");
        final String region = getAsString(object, "region");
        final String avatarUrl = getAsString(object, "icon");
        return new GuildImpl(client, id, notifyType, master, name, public_, region, avatarUrl);
    }

    public Channel buildChannel(JsonObject object) {
        final String id = getAsString(object, "id");
        final String name = getAsString(object, "name");
        final Guild guild = client.getStorage().getGuild(getAsString(object, "guild_id"));
        final User master = client.getStorage().getUser(getAsString(object, "user_id"));
        final boolean isPermSync = getAsInt(object, "permission_sync") != 0;
        final int level = getAsInt(object, "level");
        final Collection<Channel.RolePermissionOverwrite> rpo = parseRPO(object);
        final Collection<Channel.UserPermissionOverwrite> upo = parseUPO(client, object);
        if (getAsBoolean(object, "is_category")) {
            return new CategoryImpl(client, id, master, guild, isPermSync, rpo, upo, level, name);
        }
        final String parentId = getAsString(object, "parent_id");
        final Boolean needCategory = "".equals(parentId) || "0".equals(parentId);
        final Category parent = needCategory ? null : new CategoryImpl(client, parentId);
        switch (getAsInt(object, "type")) {
            case 1: {
                final int chatLimitTime = getAsInt(object, "slow_mode");
                final String topic = getAsString(object, "topic");
                return new TextChannelImpl(client, id, master, guild, isPermSync, parent, name, rpo, upo, level,
                        chatLimitTime, topic);
            }
            case 2: {
                final int chatLimitTime = getAsInt(object, "slow_mode");
                final boolean hasPassword = object.has("has_password") && getAsBoolean(object, "has_password");
                final int size = getAsInt(object, "limit_amount");
                final int quality = getAsInt(object, "voice_quality");
                return new VoiceChannelImpl(client, id, master, guild, isPermSync, parent, name, rpo, upo, level,
                        hasPassword, size, quality, chatLimitTime);
            }
            default: {
                final String msg = "We can't construct the Channel using given information. Is your information correct?";
                throw new IllegalArgumentException(msg);
            }
        }
    }

    public Role buildRole(Guild guild, JsonObject object) {
        final int id = getAsInt(object, "role_id");
        final String name = getAsString(object, "name");
        final int color = getAsInt(object, "color");
        final int pos = getAsInt(object, "position");
        final boolean hoist = getAsInt(object, "hoist") == 1;
        final boolean mentionable = getAsInt(object, "mentionable") == 1;
        final int permissions = getAsInt(object, "permissions");
        return new RoleImpl(client, guild, id, color, pos, permissions, mentionable, hoist, name);
    }

    public CustomEmoji buildEmoji(JsonObject object) {
        final String id = getAsString(object, "id");
        final Guild guild = parseEmojiGuild(id, client, object);
        final String name = getAsString(object, "name");
        return new CustomEmojiImpl(client, id, guild, name);
    }

    public Game buildGame(JsonObject object) {
        final int id = getAsInt(object, "id");
        final String name = getAsString(object, "name");
        final String icon = getAsString(object, "icon");
        return new GameImpl(client, id, name, icon);
    }
}
