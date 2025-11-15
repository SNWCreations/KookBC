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

package snw.kookbc.impl.entity.thread;

import static snw.kookbc.util.JacksonUtil.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.fasterxml.jackson.databind.JsonNode;

import snw.jkook.entity.User;
import snw.jkook.entity.channel.ThreadChannel;
import snw.jkook.entity.thread.ThreadPost;
import snw.jkook.entity.thread.ThreadReply;
import snw.jkook.message.component.BaseComponent;
import snw.jkook.message.component.card.CardComponent;
import snw.jkook.message.component.card.MultipleCardComponent;
import snw.jkook.util.PageIterator;
import snw.kookbc.impl.KBCClient;
import snw.kookbc.impl.entity.builder.CardBuilder;
import snw.kookbc.impl.entity.builder.MessageBuilder;
import snw.kookbc.impl.network.HttpAPIRoute;
import snw.kookbc.impl.pageiter.ThreadReplyIterator;
import snw.kookbc.util.MapBuilder;

/**
 * ThreadPost 实体实现
 *
 * <p>代表帖子频道中的一个帖子,支持富文本内容、回复和统计信息
 *
 * @see ThreadPost
 * @since KookBC 0.32.2
 */
public class ThreadPostImpl implements ThreadPost {

    private final KBCClient client;
    private final String id;
    private final ThreadChannel channel;
    private User author;
    private String title;
    private MultipleCardComponent content;
    private String previewContent;
    private String cover;
    private int status;
    private String categoryId;
    private long createTime;
    private long latestActiveTime;
    private boolean updated;
    private boolean contentDeleted;
    private int contentDeletedType;
    private int collectNum;
    private Collection<String> tags;
    private int replyCount;
    private int viewCount;

    /**
     * 从 Jackson JsonNode 构建 ThreadPost
     *
     * @param client KBCClient 实例
     * @param channel 所属频道
     * @param data JSON 数据节点
     */
    public ThreadPostImpl(KBCClient client, ThreadChannel channel, JsonNode data) {
        this.client = client;
        this.channel = channel;
        // Kook API 统一使用 "id" 字段表示帖子 ID
        this.id = getAsString(data, "id");
        update(data);
    }

    /**
     * 从 JSON 数据更新帖子信息
     *
     * @param data JSON 数据节点
     */
    public synchronized void update(JsonNode data) {
        // 基础信息
        this.title = getStringOrDefault(data, "title", "");
        this.previewContent = getStringOrDefault(data, "preview_content", "");
        this.cover = getStringOrDefault(data, "cover", "");
        this.status = getIntOrDefault(data, "status", 0);
        this.categoryId = getStringOrDefault(data, "category_id", "");

        // 时间信息
        this.createTime = getLongOrDefault(data, "create_time", 0L);
        this.latestActiveTime = getLongOrDefault(data, "latest_active_time", this.createTime);

        // 状态标志
        this.updated = getBooleanOrDefault(data, "is_updated", false);
        this.contentDeleted = getBooleanOrDefault(data, "content_deleted", false);
        this.contentDeletedType = getIntOrDefault(data, "content_deleted_type", 0);

        // 统计信息
        this.collectNum = getIntOrDefault(data, "collect_num", 0);
        this.replyCount = getIntOrDefault(data, "reply_count", 0);
        this.viewCount = getIntOrDefault(data, "view_count", 0);

        // 作者信息
        String authorId = getStringOrDefault(data, "author_id", null);
        if (authorId != null) {
            this.author = client.getStorage().getUser(authorId);
        }

        // 标签列表
        JsonNode tagsNode = data.get("tags");
        if (tagsNode != null && tagsNode.isArray()) {
            Collection<String> tagList = new ArrayList<>();
            for (JsonNode tag : tagsNode) {
                tagList.add(tag.asText());
            }
            this.tags = Collections.unmodifiableCollection(tagList);
        } else {
            this.tags = Collections.emptyList();
        }

        // 解析卡片消息组件
        // API 返回的 content 是一个 JSON 字符串，需要先解析为 JsonNode
        JsonNode contentNode = data.get("content");
        if (contentNode != null && !contentNode.isNull() && !contentNode.asText().isEmpty()) {
            try {
                // content 是 JSON 字符串，需要先解析
                String contentStr = contentNode.asText();
                JsonNode contentJson = parse(contentStr);

                // 使用 CardBuilder 构建卡片组件
                if (contentJson.isArray()) {
                    // 多个卡片
                    this.content = CardBuilder.buildCardArray(contentJson);
                } else if (contentJson.isObject()) {
                    // 单个卡片，包装成 MultipleCardComponent
                    CardComponent card = CardBuilder.buildCardObject(contentJson);
                    this.content = new MultipleCardComponent(Collections.singletonList(card));
                } else {
                    this.content = null;
                }
            } catch (Exception e) {
                // 解析失败时记录日志并设置为 null
                client.getCore().getLogger().warn("Failed to parse thread post content: {}", e.getMessage());
                this.content = null;
            }
        } else {
            this.content = null;
        }
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public ThreadChannel getChannel() {
        return channel;
    }

    @Override
    public User getAuthor() {
        return author;
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    @Nullable
    public MultipleCardComponent getContent() {
        return content;
    }

    @Override
    public String getPreviewContent() {
        return previewContent;
    }

    @Override
    public String getCover() {
        return cover;
    }

    @Override
    public int getStatus() {
        return status;
    }

    @Override
    public String getCategoryId() {
        return categoryId;
    }

    @Override
    public long getCreateTime() {
        return createTime;
    }

    @Override
    public long getLatestActiveTime() {
        return latestActiveTime;
    }

    @Override
    public boolean isUpdated() {
        return updated;
    }

    @Override
    public boolean isContentDeleted() {
        return contentDeleted;
    }

    @Override
    public int getContentDeletedType() {
        return contentDeletedType;
    }

    @Override
    public int getCollectNum() {
        return collectNum;
    }

    @Override
    public Collection<String> getTags() {
        return tags;
    }

    @Override
    public int getReplyCount() {
        return replyCount;
    }

    @Override
    public int getViewCount() {
        return viewCount;
    }

    @Override
    public PageIterator<Collection<ThreadReply>> getReplies() {
        return new ThreadReplyIterator(client, this);
    }

    @Override
    public ThreadReply reply(String content) {
        Map<String, Object> body = new MapBuilder()
                .put("channel_id", channel.getId())
                .put("thread_id", id)
                .put("content", content)
                .build();

        JsonNode response = client.getNetworkClient().post(
                HttpAPIRoute.THREAD_REPLY.toFullURL(),
                body
        );

        // 从响应中构建 ThreadReply 对象
        return new ThreadReplyImpl(client, this, response);
    }

    @Override
    public void delete() {
        Map<String, Object> body = new MapBuilder()
                .put("channel_id", channel.getId())
                .put("thread_id", id)
                .build();

        client.getNetworkClient().post(
                HttpAPIRoute.THREAD_DELETE.toFullURL(),
                body
        );
    }

    @Override
    public String toString() {
        return String.format("ThreadPost{id=%s, title=%s, author=%s}",
                id, title, author != null ? author.getName() : "unknown");
    }
}
