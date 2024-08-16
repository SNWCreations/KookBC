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

import com.google.gson.JsonObject;
import snw.jkook.entity.*;
import snw.jkook.entity.channel.Category;
import snw.jkook.entity.channel.Channel;
import snw.jkook.util.Validate;
import snw.kookbc.impl.KBCClient;
import snw.kookbc.impl.entity.*;
import snw.kookbc.impl.entity.channel.CategoryImpl;
import snw.kookbc.impl.entity.channel.TextChannelImpl;
import snw.kookbc.impl.entity.channel.VoiceChannelImpl;
import snw.kookbc.impl.network.exceptions.BadResponseException;

import java.util.Collection;

import static snw.kookbc.impl.entity.builder.EntityBuildUtil.parseRPO;
import static snw.kookbc.impl.entity.builder.EntityBuildUtil.parseUPO;
import static snw.kookbc.util.GsonUtil.get;

// The class for building entities.
public class EntityBuilder {
    private final KBCClient client;

    public EntityBuilder(KBCClient client) {
        this.client = client;
    }

    public User buildUser(JsonObject object) {
        String id = get(object, "id").getAsString();
        boolean bot = get(object, "bot").getAsBoolean();
        String userName = get(object, "username").getAsString();
        String avatar = get(object, "avatar").getAsString();
        String vipAvatar = get(object, "vip_avatar").getAsString();
        int identify = get(object, "identify_num").getAsInt();
        boolean ban = get(object, "status").getAsInt() == 10;
        boolean vip = get(object, "is_vip").getAsBoolean();
        return new UserImpl(
                client,
                id,
                bot,
                userName,
                avatar,
                vipAvatar,
                identify,
                ban,
                vip
        );
    }

    // only use on /api/v3/user/me
    public User buildBot(JsonObject object) {
        String id = get(object, "id").getAsString();
        boolean bot = get(object, "bot").getAsBoolean();
        String userName = get(object, "username").getAsString();
        String avatar = get(object, "avatar").getAsString();
        int identify = get(object, "identify_num").getAsInt();
        boolean ban = get(object, "status").getAsInt() == 10;
        return new UserImpl(
                client,
                id,
                bot,
                userName,
                avatar,
                avatar,
                identify,
                ban,
                false
        );
    }

    public Guild buildGuild(JsonObject object) {
        String id = get(object, "id").getAsString();
        String name = get(object, "name").getAsString();
        boolean isPublic = get(object, "enable_open").getAsBoolean();
        String region = get(object, "region").getAsString();
        String ownerId = get(object, "master_id").getAsString();
        int rawNotifyType = get(object, "notify_type").getAsInt();

        Guild.NotifyType type = null;
        for (Guild.NotifyType value : Guild.NotifyType.values()) {
            if (value.getValue() == rawNotifyType) {
                type = value;
                break;
            }
        }
        Validate.notNull(type, String.format("Internal Error: Unexpected NotifyType from remote: %s", rawNotifyType));

        String avatar = get(object, "icon").getAsString();
        return new GuildImpl(
                client,
                id,
                name,
                isPublic,
                region,
                ownerId,
                type,
                avatar
        );
    }

    public Channel buildChannel(JsonObject object) {
        // basic information
        String id = get(object, "id").getAsString();
        String name = get(object, "name").getAsString();
        Guild guild = client.getStorage().getGuild(get(object, "guild_id").getAsString());
        User master = client.getStorage().getUser(get(object, "user_id").getAsString());

        boolean isPermSync = get(object, "permission_sync").getAsInt() != 0;
        int level = get(object, "level").getAsInt();

        // rpo parse
        Collection<Channel.RolePermissionOverwrite> rpo = parseRPO(object);

        // upo parse
        Collection<Channel.UserPermissionOverwrite> upo = parseUPO(client, object);

        if (get(object, "is_category").getAsBoolean()) {
            return new CategoryImpl(
                    client, id,
                    master,
                    guild,
                    isPermSync,
                    rpo, upo, level, name
            );
        } else {
            String parentId = get(object, "parent_id").getAsString();
            Category parent = ("".equals(parentId) || "0".equals(parentId)) ? null : (Category) client.getStorage().getChannel(parentId);

            int type = get(object, "type").getAsInt();
            if (type == 1) { // TextChannel
                int chatLimitTime = get(object, "slow_mode").getAsInt();
                String topic = get(object, "topic").getAsString();
                return new TextChannelImpl(
                        client,
                        id,
                        master,
                        guild,
                        isPermSync,
                        parent,
                        name,
                        rpo,
                        upo,
                        level,
                        chatLimitTime,
                        topic
                );
            } else if (type == 2) { // VoiceChannel
                int chatLimitTime = get(object, "slow_mode").getAsInt();
                boolean hasPassword = object.has("has_password") && get(object, "has_password").getAsBoolean();
                int size = get(object, "limit_amount").getAsInt();
                int quality = get(object, "voice_quality").getAsInt();
                return new VoiceChannelImpl(
                        client,
                        id,
                        master,
                        guild,
                        isPermSync,
                        parent,
                        name,
                        rpo,
                        upo,
                        level,
                        hasPassword,
                        size,
                        quality,
                        chatLimitTime
                );
            }
        }
        throw new IllegalArgumentException("We can't construct the Channel using given information. Is your information correct?");
    }

    public Role buildRole(Guild guild, JsonObject object) {
        int id = get(object, "role_id").getAsInt();
        String name = get(object, "name").getAsString();
        int color = get(object, "color").getAsInt();
        int pos = get(object, "position").getAsInt();
        boolean hoist = get(object, "hoist").getAsInt() == 1;
        boolean mentionable = get(object, "mentionable").getAsInt() == 1;
        int permissions = get(object, "permissions").getAsInt();
        return new RoleImpl(client, guild, id, color, pos, permissions, mentionable, hoist, name);
    }

    public CustomEmoji buildEmoji(JsonObject object) {
        String id = get(object, "id").getAsString();
        Guild guild = null;
        if (id.contains("/")) {
            try {
                guild = client.getStorage().getGuild(id.substring(0, id.indexOf("/")));
            } catch (BadResponseException e) {
                if (!(e.getCode() == 403)) {
                    throw e;
                }
                // or you don't have permission to access it!
            }
        }
        String name = get(object, "name").getAsString();
        return new CustomEmojiImpl(client, id, name, guild);
    }

    public Game buildGame(JsonObject object) {
        int id = get(object, "id").getAsInt();
        String name = get(object, "name").getAsString();
        String icon = get(object, "icon").getAsString();
        return new GameImpl(client, id, name, icon);
    }
}
