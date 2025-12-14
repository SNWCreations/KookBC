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

import static snw.kookbc.util.JacksonUtil.getIntOrDefault;

import java.util.Collection;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.fasterxml.jackson.databind.JsonNode;

import snw.jkook.entity.Guild;
import snw.jkook.entity.User;
import snw.jkook.entity.channel.Category;
import snw.jkook.entity.channel.Channel.RolePermissionOverwrite;
import snw.jkook.entity.channel.Channel.UserPermissionOverwrite;
import snw.jkook.entity.channel.ThreadChannel;
import snw.jkook.entity.thread.ThreadPost;
import snw.jkook.util.PageIterator;
import snw.kookbc.impl.KBCClient;
import snw.kookbc.impl.entity.thread.ThreadCategoryImpl;
import snw.kookbc.impl.entity.thread.ThreadPostImpl;
import snw.kookbc.impl.network.HttpAPIRoute;
import snw.kookbc.impl.pageiter.ThreadPostIterator;
import snw.kookbc.util.MapBuilder;

/**
 * 帖子频道实现 (Thread Channel - Type 4)
 *
 * <p>帖子频道是 Kook 提供的内容型频道,允许用户生成结构化内容,
 * 支持知识分享和经验交流。帖子支持富文本内容(文字+图片)。
 *
 * <p>帖子频道特性:
 * <ul>
 *   <li>支持分类管理</li>
 *   <li>支持主贴、回复和楼中楼</li>
 *   <li>主贴和回复支持富媒体(文字+图片)</li>
 *   <li>楼中楼仅支持 KMD 和表情</li>
 *   <li>支持 @提及功能</li>
 * </ul>
 *
 *
 * @see <a href="https://developer.kookapp.cn/doc/http/thread">Kook 帖子频道文档</a>
 * @since KookBC 0.33.0
 */
public class ThreadChannelImpl extends NonCategoryChannelImpl implements ThreadChannel {

    /**
     * 慢速模式时间限制 (秒)
     */
    private int chatLimitTime;

    /**
     * 构造一个未完全初始化的帖子频道对象
     *
     * @param client KBCClient 实例
     * @param id 频道 ID
     */
    public ThreadChannelImpl(KBCClient client, String id) {
        super(client, id);
    }

    /**
     * 构造一个完全初始化的帖子频道对象
     *
     * @param client KBCClient 实例
     * @param id 频道 ID
     * @param master 频道创建者
     * @param guild 所属服务器
     * @param permSync 权限是否与父分类同步
     * @param parent 父分类频道
     * @param name 频道名称
     * @param rpo 角色权限覆写集合
     * @param upo 用户权限覆写集合
     * @param level 频道排序等级
     * @param chatLimitTime 慢速模式时间限制
     */
    public ThreadChannelImpl(KBCClient client, String id, User master, Guild guild, boolean permSync, Category parent,
            String name, Collection<RolePermissionOverwrite> rpo, Collection<UserPermissionOverwrite> upo, int level,
            int chatLimitTime) {
        super(client, id, master, guild, permSync, parent, name, rpo, upo, level, chatLimitTime);
        this.chatLimitTime = chatLimitTime;
        this.completed = true;
    }

    @Override
    public ThreadPost createThread(String title, String content, @Nullable String categoryId) {
        // 将纯文本内容转换为简单的卡片消息格式
        // Kook 帖子 API 要求 content 必须是卡片消息的 JSON 字符串
        String cardContent = String.format(
            "[{\"type\":\"card\",\"theme\":\"invisible\",\"size\":\"lg\",\"modules\":[{\"type\":\"section\",\"text\":{\"type\":\"plain-text\",\"content\":\"%s\"}}]}]",
            content.replace("\"", "\\\"").replace("\n", "\\n")
        );

        Map<String, Object> body = new MapBuilder()
                .put("channel_id", getId())
                .put("guild_id", getGuild().getId())
                .put("title", title)
                .put("content", cardContent)
                .build();

        if (categoryId != null && !categoryId.isEmpty()) {
            body.put("category_id", categoryId);
        }

        JsonNode response = client.getNetworkClient().post(
                HttpAPIRoute.THREAD_CREATE.toFullURL(),
                body
        );

        // 从响应中构建 ThreadPost 对象
        return new ThreadPostImpl(client, this, response);
    }

    @Override
    @Nullable
    public ThreadPost getThreadPost(String threadId) {
        Map<String, Object> queryParams = new MapBuilder()
                .put("channel_id", getId())
                .put("thread_id", threadId)
                .build();

        try {
            JsonNode response = client.getNetworkClient().get(
                    HttpAPIRoute.THREAD_VIEW.toFullURL() +
                            "?channel_id=" + getId() +
                            "&thread_id=" + threadId
            );

            return new ThreadPostImpl(client, this, response);
        } catch (Exception e) {
            // 帖子不存在或访问出错
            client.getCore().getLogger().warn("获取帖子失败: " + threadId, e);
            return null;
        }
    }

    @Override
    public PageIterator<Collection<ThreadPost>> getThreadPosts(@Nullable String categoryId) {
        return new ThreadPostIterator(client, this, categoryId);
    }

    @Override
    public Collection<ThreadCategory> getCategories() {
        try {
            String url = HttpAPIRoute.THREAD_CATEGORY_LIST.toFullURL() + "?channel_id=" + getId();
            JsonNode response = client.getNetworkClient().get(url);

            Collection<ThreadCategory> categories = new java.util.ArrayList<>();

            // API 返回的数据结构: { "list": [...] }
            // NetworkClient.get() 已经提取了 "data" 字段,所以需要再获取 "list"
            JsonNode listNode = response.get("list");
            if (listNode != null && listNode.isArray()) {
                for (JsonNode categoryNode : listNode) {
                    ThreadCategory category = new ThreadCategoryImpl(client, categoryNode);
                    categories.add(category);
                }
            }

            return categories;
        } catch (Exception e) {
            client.getCore().getLogger().warn("获取帖子分类失败", e);
            return java.util.Collections.emptyList();
        }
    }

    /**
     * 获取慢速模式时间限制
     *
     * @return 时间限制(秒)
     */
    public int getChatLimitTime() {
        initIfNeeded();
        return chatLimitTime;
    }

    /**
     * 设置慢速模式时间限制
     *
     * @param chatLimitTime 时间限制(秒)
     */
    public void setChatLimitTime(int chatLimitTime) {
        this.chatLimitTime = chatLimitTime;
    }

    /**
     * 从 Jackson JsonNode 更新频道信息
     *
     * <p>此方法会安全地提取以下字段:
     * <ul>
     *   <li>slow_mode - 慢速模式时间限制</li>
     * </ul>
     *
     * @param data JSON 数据节点
     */
    @Override
    public synchronized void update(JsonNode data) {
        super.update(data);
        this.chatLimitTime = getIntOrDefault(data, "slow_mode", 0);
    }

    /**
     * 返回帖子频道的字符串表示
     *
     * @return 包含频道ID和名称的字符串
     */
    @Override
    public String toString() {
        return String.format("ThreadChannel{id=%s, name=%s}", getId(), getName());
    }
}
