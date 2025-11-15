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

import java.util.Collection;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.fasterxml.jackson.databind.JsonNode;

import org.jetbrains.annotations.Nullable;
import snw.jkook.entity.Guild;
import snw.jkook.entity.Guild.NotifyType;
import snw.jkook.entity.channel.Channel;
import snw.jkook.entity.channel.NonCategoryChannel;
import snw.kookbc.impl.KBCClient;
import snw.kookbc.util.JacksonUtil;
import snw.kookbc.impl.entity.channel.CategoryImpl;
import snw.kookbc.impl.entity.channel.NonCategoryChannelImpl;
import snw.kookbc.impl.entity.channel.TextChannelImpl;
import snw.kookbc.impl.entity.channel.ThreadChannelImpl;
import snw.kookbc.impl.entity.channel.VoiceChannelImpl;

public class EntityBuildUtil {

    @Nullable
    public static Channel parseChannel(KBCClient client, String id, int type) {
        switch (type) {
            case 0:
                return new CategoryImpl(client, id);
            case 1:
                return new TextChannelImpl(client, id);
            case 2:
                return new VoiceChannelImpl(client, id);
            case 4:
                // 帖子频道 (Thread Channel)
                return new ThreadChannelImpl(client, id);
            default:
                return null;
        }
    }

    // ===== Jackson API - 安全版本（处理不完整JSON数据） =====

    /**
     * 解析角色权限覆写 (Jackson版本，安全处理不完整JSON)
     */
    public static Collection<Channel.RolePermissionOverwrite> parseRPO(JsonNode node) {
        JsonNode array = JacksonUtil.getArrayOrNull(node, "permission_overwrites");
        Collection<Channel.RolePermissionOverwrite> rpo = new ConcurrentLinkedQueue<>();

        if (array != null && array.isArray()) {
            for (JsonNode element : array) {
                if (element != null && element.isObject()) {
                    int roleId = JacksonUtil.getIntOrDefault(element, "role_id", 0);
                    int allow = JacksonUtil.getIntOrDefault(element, "allow", 0);
                    int deny = JacksonUtil.getIntOrDefault(element, "deny", 0);

                    rpo.add(new Channel.RolePermissionOverwrite(roleId, allow, deny));
                }
            }
        }
        return rpo;
    }

    /**
     * 解析用户权限覆写 (Jackson版本，安全处理不完整JSON)
     */
    public static Collection<Channel.UserPermissionOverwrite> parseUPO(KBCClient client, JsonNode node) {
        JsonNode array = JacksonUtil.getArrayOrNull(node, "permission_users");
        Collection<Channel.UserPermissionOverwrite> upo = new ConcurrentLinkedQueue<>();

        if (array != null && array.isArray()) {
            for (JsonNode element : array) {
                if (element != null && element.isObject()) {
                    JsonNode rawUser = JacksonUtil.getObjectOrNull(element, "user");
                    if (rawUser != null) {
                        String userId = JacksonUtil.getRequiredString(rawUser, "id");
                        int allow = JacksonUtil.getIntOrDefault(element, "allow", 0);
                        int deny = JacksonUtil.getIntOrDefault(element, "deny", 0);

                        // 直接使用Jackson JsonNode
                        upo.add(new Channel.UserPermissionOverwrite(
                                client.getStorage().getUser(userId, rawUser),
                                allow, deny));
                    }
                }
            }
        }
        return upo;
    }

    /**
     * 解析通知类型 (Jackson版本，安全处理不完整JSON)
     */
    public static NotifyType parseNotifyType(JsonNode node) {
        int rawNotifyType = JacksonUtil.getIntOrDefault(node, "notify_type", 0);

        for (Guild.NotifyType value : Guild.NotifyType.values()) {
            if (value.getValue() == rawNotifyType) {
                return value;
            }
        }

        // 如果找不到匹配的通知类型，使用默认值而不是抛出异常
        return Guild.NotifyType.values()[0]; // 使用第一个枚举值作为默认
    }

    /**
     * 解析表情所属服务器 (Jackson版本，安全处理不完整JSON)
     */
    public static Guild parseEmojiGuild(String id, KBCClient client, JsonNode node) {
        Guild guild = null;
        if (id != null && id.contains("/")) {
            String guildId = id.substring(0, id.indexOf("/"));
            if (!guildId.isEmpty()) {
                guild = client.getStorage().getGuild(guildId);
            }
        }
        return guild;
    }

}