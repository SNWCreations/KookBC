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

import com.google.gson.JsonElement;
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

import java.util.ArrayList;
import java.util.Collection;

// The class for building entities.
public class EntityBuilder {
    private final KBCClient client;

    public EntityBuilder(KBCClient client) {
        this.client = client;
    }

    public User buildUser(JsonObject object) {
        String id = object.get("id").getAsString();
        boolean bot = object.get("bot").getAsBoolean();
        String userName = object.get("username").getAsString();
        String avatar = object.get("avatar").getAsString();
        String vipAvatar = object.get("vip_avatar").getAsString();
        int identify = object.get("identify_num").getAsInt();
        boolean ban = object.get("status").getAsInt() == 10;
        boolean vip = object.get("is_vip").getAsBoolean();
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

    public Guild buildGuild(JsonObject object) {
        String id = object.get("id").getAsString();
        String name = object.get("name").getAsString();
        boolean isPublic = object.get("enable_open").getAsBoolean();
        String region = object.get("region").getAsString();
        User master = client.getStorage().getUser(object.get("master_id").getAsString());
        int rawNotifyType = object.get("notify_type").getAsInt();

        Guild.NotifyType type = null;
        for (Guild.NotifyType value : Guild.NotifyType.values()) {
            if (value.getValue() == rawNotifyType) {
                type = value;
                break;
            }
        }
        Validate.notNull(type, String.format("Internal Error: Unexpected NotifyType from remote: %s", rawNotifyType));

        String avatar = object.get("icon").getAsString();
        return new GuildImpl(
                client,
                id,
                name,
                isPublic,
                region,
                master,
                type,
                avatar
        );
    }

    public Channel buildChannel(JsonObject object) {
        // basic information
        String id = object.get("id").getAsString();
        String name = object.get("name").getAsString();
        Guild guild = client.getStorage().getGuild(object.get("guild_id").getAsString());
        User master = client.getStorage().getUser(object.get("user_id").getAsString());
        String parentId = object.get("parent_id").getAsString();
        Category parent = ("".equals(parentId) || "0".equals(parentId)) ? null : (Category) client.getStorage().getChannel(parentId);

        boolean isPermSync = object.get("permission_sync").getAsInt() != 0;
        int level = object.get("level").getAsInt();

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
                            client.getStorage().getUser(rawUser.get("id").getAsString(), rawUser),
                            oupo.get("allow").getAsInt(),
                            oupo.get("deny").getAsInt()
                    )
            );
        }

        if (object.get("is_category").getAsBoolean()) {
            return new CategoryImpl(
                    client, id,
                    master,
                    guild,
                    parent, isPermSync,
                    rpo, upo, level, name
            );
        } else {
            int type = object.get("type").getAsInt();
            if (type == 1) { // TextChannel
                int chatLimitTime = object.get("slow_mode").getAsInt();
                String topic = object.get("topic").getAsString();
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
                boolean hasPassword = object.has("has_password") && object.get("has_password").getAsBoolean();
                int size = object.get("limit_amount").getAsInt();
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
                        size
                );
            }
        }
        throw new IllegalArgumentException("We can't construct the Channel using given information. Is your information correct?");
    }

    public Role buildRole(Guild guild, JsonObject object) {
        int id = object.get("role_id").getAsInt();
        String name = object.get("name").getAsString();
        int color = object.get("color").getAsInt();
        int pos = object.get("position").getAsInt();
        boolean hoist = object.get("hoist").getAsInt() == 1;
        boolean mentionable = object.get("mentionable").getAsInt() == 1;
        int permissions = object.get("permissions").getAsInt();
        return new RoleImpl(client, guild, id, color, pos, permissions, mentionable, hoist, name);
    }

    public CustomEmoji buildEmoji(JsonObject object) {
        String id = object.get("id").getAsString();
        Guild guild = null;
        if (id.contains("/")) {
            try {
                guild = client.getStorage().getGuild(id.substring(0, id.indexOf("/")));
            } catch (RuntimeException ignored) {
            } // you don't have permission to access it!
        }
        String name = object.get("name").getAsString();
        return new CustomEmojiImpl(client, id, name, guild);
    }

    public Game buildGame(JsonObject object) {
        int id = object.get("id").getAsInt();
        String name = object.get("name").getAsString();
        String icon = object.get("icon").getAsString();
        return new GameImpl(client, id, name, icon);
    }
}
