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
// Jackson utils for safe field access
import static snw.kookbc.util.JacksonUtil.*;

import java.util.Collection;

import com.fasterxml.jackson.databind.JsonNode;

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
import snw.kookbc.impl.entity.channel.ThreadChannelImpl;
import snw.kookbc.impl.entity.channel.VoiceChannelImpl;

// The class for building entities.
public class EntityBuilder {
    private final KBCClient client;

    public EntityBuilder(KBCClient client) {
        this.client = client;
    }

    /**
     * 构建User对象 (Gson兼容版本)
     * 该方法保留用于向后兼容，内部委托给Jackson版本
     */
    public User buildUser(com.google.gson.JsonObject s) {
        JsonNode node = convertFromGsonJsonObject(s);
        return buildUser(node);
    }

    /**
     * 构建Guild对象 (Gson兼容版本)
     * 该方法保留用于向后兼容，内部委托给Jackson版本
     */
    public Guild buildGuild(com.google.gson.JsonObject object) {
        JsonNode node = convertFromGsonJsonObject(object);
        return buildGuild(node);
    }

    /**
     * 构建Channel对象 (Gson兼容版本)
     * 该方法保留用于向后兼容，内部委托给Jackson版本
     */
    public Channel buildChannel(com.google.gson.JsonObject object) {
        JsonNode node = convertFromGsonJsonObject(object);
        return buildChannel(node);
    }

    /**
     * 构建Role对象 (Gson兼容版本)
     * 该方法保留用于向后兼容，内部委托给Jackson版本
     */
    public Role buildRole(Guild guild, com.google.gson.JsonObject object) {
        JsonNode node = convertFromGsonJsonObject(object);
        return buildRole(guild, node);
    }

    /**
     * 构建CustomEmoji对象 (Gson兼容版本)
     * 该方法保留用于向后兼容，内部委托给Jackson版本
     */
    public CustomEmoji buildEmoji(com.google.gson.JsonObject object) {
        JsonNode node = convertFromGsonJsonObject(object);
        return buildEmoji(node);
    }

    /**
     * 构建Game对象 (Gson兼容版本)
     * 该方法保留用于向后兼容，内部委托给Jackson版本
     */
    public Game buildGame(com.google.gson.JsonObject object) {
        JsonNode node = convertFromGsonJsonObject(object);
        return buildGame(node);
    }

    // ===== Jackson API - 高性能版本（处理Kook不完整JSON数据）=====

    /**
     * 构建User对象 (Jackson版本，安全处理不完整JSON)
     * 处理Kook可能发送不完整JSON的情况
     */
    public User buildUser(JsonNode node) {
        // 必需字段 - 如果不存在会抛出异常
        final String id = getRequiredString(node, "id");

        // 可选字段 - 提供合适的默认值
        final boolean bot = getBooleanOrDefault(node, "bot", false);
        final String name = getStringOrDefault(node, "username", "Unknown User");
        final int identify = getIntOrDefault(node, "identify_num", 0);

        // 状态字段 - 默认为正常状态（非封禁）
        final int status = getIntOrDefault(node, "status", 0);
        final boolean ban = (status == 10);

        // VIP状态默认为false
        final boolean vip = getBooleanOrDefault(node, "is_vip", false);

        // 头像URL - 提供空字符串作为默认值
        final String avatarUrl = getStringOrDefault(node, "avatar", "");
        final String vipAvatarUrl = getStringOrDefault(node, "vip_avatar", "");

        return new UserImpl(client, id, bot, name, identify, ban, vip, avatarUrl, vipAvatarUrl);
    }

    /**
     * 构建Guild对象 (Jackson版本，安全处理不完整JSON)
     * 处理Kook可能发送不完整JSON的情况
     */
    public Guild buildGuild(JsonNode node) {
        // 必需字段
        final String id = getRequiredString(node, "id");

        // 通知类型 - 使用EntityBuildUtil解析
        final NotifyType notifyType = parseNotifyType(node);

        // 服务器主人 - 必需字段
        final String masterId = getRequiredString(node, "master_id");
        final User master = client.getStorage().getUser(masterId);

        // 服务器基本信息
        final String name = getStringOrDefault(node, "name", "Unknown Guild");
        final boolean public_ = getBooleanOrDefault(node, "enable_open", false);
        final String region = getStringOrDefault(node, "region", "unknown");
        final String avatarUrl = getStringOrDefault(node, "icon", "");

        return new GuildImpl(client, id, notifyType, master, name, public_, region, avatarUrl);
    }

    /**
     * 构建Channel对象 (Jackson版本，安全处理不完整JSON)
     * 处理Kook可能发送不完整JSON的情况
     */
    public Channel buildChannel(JsonNode node) {
        // 必需字段
        final String id = getRequiredString(node, "id");
        final String name = getStringOrDefault(node, "name", "Unknown Channel");

        // 所属服务器和创建者
        final String guildId = getRequiredString(node, "guild_id");
        final Guild guild = client.getStorage().getGuild(guildId);

        final String userId = getRequiredString(node, "user_id");
        final User master = client.getStorage().getUser(userId);

        // 权限同步和级别
        final boolean isPermSync = getIntOrDefault(node, "permission_sync", 0) != 0;
        final int level = getIntOrDefault(node, "level", 0);

        // 权限覆写 - 使用EntityBuildUtil安全解析
        final Collection<Channel.RolePermissionOverwrite> rpo = parseRPO(node);
        final Collection<Channel.UserPermissionOverwrite> upo = parseUPO(client, node);

        // 检查是否为分类频道
        if (getBooleanOrDefault(node, "is_category", false)) {
            return new CategoryImpl(client, id, master, guild, isPermSync, rpo, upo, level, name);
        }

        // 处理父级分类
        final String parentId = getStringOrDefault(node, "parent_id", "");
        final Boolean needCategory = "".equals(parentId) || "0".equals(parentId);
        final Category parent = needCategory ? null : new CategoryImpl(client, parentId);

        // 根据频道类型创建对应对象
        final int channelType = getIntOrDefault(node, "type", 1); // 默认为文本频道

        switch (channelType) {
            case 1: {
                // 文本频道
                final int chatLimitTime = getIntOrDefault(node, "slow_mode", 0);
                final String topic = getStringOrDefault(node, "topic", "");
                return new TextChannelImpl(client, id, master, guild, isPermSync, parent, name, rpo, upo, level,
                        chatLimitTime, topic);
            }
            case 2: {
                // 语音频道
                final int chatLimitTime = getIntOrDefault(node, "slow_mode", 0);
                final boolean hasPassword = hasNonNull(node, "has_password") && getBooleanOrDefault(node, "has_password", false);
                final int size = getIntOrDefault(node, "limit_amount", 99);
                final int quality = getIntOrDefault(node, "voice_quality", 1);
                return new VoiceChannelImpl(client, id, master, guild, isPermSync, parent, name, rpo, upo, level,
                        hasPassword, size, quality, chatLimitTime);
            }
            case 4: {
                // 帖子频道 (Thread Channel)
                final int chatLimitTime = getIntOrDefault(node, "slow_mode", 0);
                return new ThreadChannelImpl(client, id, master, guild, isPermSync, parent, name, rpo, upo, level,
                        chatLimitTime);
            }
            default: {
                final String msg = "We can't construct the Channel using given information. Unknown channel type: " + channelType;
                throw new IllegalArgumentException(msg);
            }
        }
    }

    /**
     * 构建Role对象 (Jackson版本，安全处理不完整JSON)
     * 处理Kook可能发送不完整JSON的情况
     */
    public Role buildRole(Guild guild, JsonNode node) {
        // 必需字段
        final int id = getRequiredInt(node, "role_id");
        final String name = getStringOrDefault(node, "name", "Unknown Role");

        // 外观属性
        final int color = getIntOrDefault(node, "color", 0); // 默认无颜色
        final int pos = getIntOrDefault(node, "position", 0); // 默认位置0

        // 显示设置 (0/1值转换为布尔值)
        final boolean hoist = getIntOrDefault(node, "hoist", 0) == 1;
        final boolean mentionable = getIntOrDefault(node, "mentionable", 0) == 1;

        // 权限位掩码
        final int permissions = getIntOrDefault(node, "permissions", 0);

        return new RoleImpl(client, guild, id, color, pos, permissions, mentionable, hoist, name);
    }

    /**
     * 构建CustomEmoji对象 (Jackson版本，安全处理不完整JSON)
     * 处理Kook可能发送不完整JSON的情况
     */
    public CustomEmoji buildEmoji(JsonNode node) {
        // 必需字段
        final String id = getRequiredString(node, "id");
        final String name = getStringOrDefault(node, "name", "Unknown Emoji");

        // 解析表情所属服务器 - 使用EntityBuildUtil
        final Guild guild = parseEmojiGuild(id, client, node);

        return new CustomEmojiImpl(client, id, guild, name);
    }

    /**
     * 构建Game对象 (Jackson版本，安全处理不完整JSON)
     * 处理Kook可能发送不完整JSON的情况
     */
    public Game buildGame(JsonNode node) {
        // 必需字段
        final int id = getRequiredInt(node, "id");
        final String name = getStringOrDefault(node, "name", "Unknown Game");
        final String icon = getStringOrDefault(node, "icon", "");

        return new GameImpl(client, id, name, icon);
    }
}
