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

import static snw.jkook.util.Validate.notNull;
import static snw.kookbc.util.GsonUtil.get;
import static snw.kookbc.util.GsonUtil.getAsInt;

import java.util.Collection;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import org.jetbrains.annotations.Nullable;
import snw.jkook.entity.Guild;
import snw.jkook.entity.Guild.NotifyType;
import snw.jkook.entity.channel.Channel;
import snw.jkook.entity.channel.NonCategoryChannel;
import snw.kookbc.impl.KBCClient;
import snw.kookbc.impl.entity.channel.CategoryImpl;
import snw.kookbc.impl.entity.channel.NonCategoryChannelImpl;
import snw.kookbc.impl.entity.channel.TextChannelImpl;
import snw.kookbc.impl.entity.channel.VoiceChannelImpl;

public class EntityBuildUtil {

    public static Collection<Channel.RolePermissionOverwrite> parseRPO(JsonObject object) {
        JsonArray array = get(object, "permission_overwrites").getAsJsonArray();
        Collection<Channel.RolePermissionOverwrite> rpo = new ConcurrentLinkedQueue<>();
        for (JsonElement element : array) {
            JsonObject orpo = element.getAsJsonObject();
            rpo.add(
                    new Channel.RolePermissionOverwrite(
                            orpo.get("role_id").getAsInt(),
                            orpo.get("allow").getAsInt(),
                            orpo.get("deny").getAsInt()));
        }
        return rpo;
    }

    public static Collection<Channel.UserPermissionOverwrite> parseUPO(KBCClient client, JsonObject object) {
        JsonArray array = get(object, "permission_users").getAsJsonArray();
        Collection<Channel.UserPermissionOverwrite> upo = new ConcurrentLinkedQueue<>();
        for (JsonElement element : array) {
            JsonObject oupo = element.getAsJsonObject();
            JsonObject rawUser = oupo.getAsJsonObject("user");
            upo.add(
                    new Channel.UserPermissionOverwrite(
                            client.getStorage().getUser(rawUser.get("id").getAsString(), rawUser),
                            oupo.get("allow").getAsInt(),
                            oupo.get("deny").getAsInt()));
        }
        return upo;
    }

    public static NotifyType parseNotifyType(JsonObject object) {
        Guild.NotifyType type = null;
        int rawNotifyType = getAsInt(object, "notify_type");
        for (Guild.NotifyType value : Guild.NotifyType.values()) {
            if (value.getValue() == rawNotifyType) {
                type = value;
                break;
            }
        }
        notNull(type, String.format("Internal Error: Unexpected NotifyType from remote: %s", rawNotifyType));
        return type;
    }

    public static Guild parseEmojiGuild(String id, KBCClient client, JsonObject object) {
        Guild guild = null;
        if (id.contains("/")) {
            guild = client.getStorage().getGuild(id.substring(0, id.indexOf("/")));
        }
        return guild;
    }

    @Nullable
    public static Channel parseChannel(KBCClient client, String id, int type) {
        switch (type) {
            case 0:
                return new CategoryImpl(client, id);
            case 1:
                return new TextChannelImpl(client, id);
            case 2:
                return new VoiceChannelImpl(client, id);
            default:
                return null;
        }
    }

}
