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
import snw.jkook.entity.thread.ThreadPost;
import snw.jkook.entity.thread.ThreadReply;
import snw.jkook.message.component.BaseComponent;
import snw.jkook.message.component.card.CardComponent;
import snw.jkook.message.component.card.MultipleCardComponent;
import snw.kookbc.impl.KBCClient;
import snw.kookbc.impl.entity.builder.MessageBuilder;
import snw.kookbc.impl.network.HttpAPIRoute;
import snw.kookbc.util.MapBuilder;

/**
 * ThreadReply 实体实现
 *
 * <p>代表帖子频道中对主贴的回复,支持富文本内容和嵌套回复(楼中楼)
 *
 * @see ThreadReply
 * @since KookBC 0.32.2
 */
public class ThreadReplyImpl implements ThreadReply {

    private final KBCClient client;
    private final String id;
    private final ThreadPost threadPost;
    private User author;
    private MultipleCardComponent content;
    private long createTime;
    private String belongToPostId;
    private String replyToPostId;
    private Collection<ThreadReply> replies;
    private boolean updated;

    /**
     * 从 Jackson JsonNode 构建 ThreadReply
     *
     * @param client KBCClient 实例
     * @param threadPost 所属的主贴
     * @param data JSON 数据节点
     */
    public ThreadReplyImpl(KBCClient client, ThreadPost threadPost, JsonNode data) {
        this.client = client;
        this.threadPost = threadPost;
        this.id = getAsString(data, "reply_id");
        update(data);
    }

    /**
     * 从 JSON 数据更新回复信息
     *
     * @param data JSON 数据节点
     */
    public synchronized void update(JsonNode data) {
        // 时间信息
        this.createTime = getLongOrDefault(data, "create_time", 0L);

        // 状态标志
        this.updated = getBooleanOrDefault(data, "is_updated", false);

        // 关系信息
        this.belongToPostId = getStringOrDefault(data, "belong_to_post_id", null);
        this.replyToPostId = getStringOrDefault(data, "reply_id", null);

        // 作者信息
        String authorId = getStringOrDefault(data, "author_id", null);
        if (authorId != null) {
            this.author = client.getStorage().getUser(authorId);
        }

        // 嵌套回复列表
        JsonNode repliesNode = data.get("replies");
        if (repliesNode != null && repliesNode.isArray()) {
            Collection<ThreadReply> replyList = new ArrayList<>();
            for (JsonNode replyNode : repliesNode) {
                ThreadReply nestedReply = new ThreadReplyImpl(client, threadPost, replyNode);
                replyList.add(nestedReply);
            }
            this.replies = Collections.unmodifiableCollection(replyList);
        } else {
            this.replies = Collections.emptyList();
        }

        // 解析卡片消息组件
        JsonNode contentNode = data.get("content");
        if (contentNode != null && !contentNode.isNull() && !contentNode.asText().isEmpty()) {
            try {
                MessageBuilder messageBuilder = new MessageBuilder(client);
                BaseComponent component = messageBuilder.buildComponent(data);

                // 如果是 MultipleCardComponent 或者可以转换为 MultipleCardComponent
                if (component instanceof MultipleCardComponent) {
                    this.content = (MultipleCardComponent) component;
                } else if (component instanceof CardComponent) {
                    // 单个卡片包装成 MultipleCardComponent
                    this.content = new MultipleCardComponent(
                            Collections.singletonList((CardComponent) component)
                    );
                } else {
                    // 其他类型的消息组件暂不支持
                    this.content = null;
                }
            } catch (Exception e) {
                // 解析失败时记录日志并设置为 null
                client.getCore().getLogger().warn("Failed to parse thread reply content: {}", e.getMessage());
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
    public ThreadPost getThreadPost() {
        return threadPost;
    }

    @Override
    public User getAuthor() {
        return author;
    }

    @Override
    @Nullable
    public MultipleCardComponent getContent() {
        return content;
    }

    @Override
    public long getCreateTime() {
        return createTime;
    }

    @Override
    @Nullable
    public String getBelongToPostId() {
        return belongToPostId;
    }

    @Override
    @Nullable
    public String getReplyToPostId() {
        return replyToPostId;
    }

    @Override
    public Collection<ThreadReply> getReplies() {
        return replies;
    }

    @Override
    public boolean isUpdated() {
        return updated;
    }

    @Override
    public ThreadReply reply(String content) {
        Map<String, Object> body = new MapBuilder()
                .put("channel_id", threadPost.getChannel().getId())
                .put("thread_id", threadPost.getId())
                .put("reply_id", id)
                .put("content", content)
                .build();

        JsonNode response = client.getNetworkClient().post(
                HttpAPIRoute.THREAD_REPLY.toFullURL(),
                body
        );

        // 从响应中构建嵌套回复对象
        return new ThreadReplyImpl(client, threadPost, response);
    }

    @Override
    public void delete() {
        Map<String, Object> body = new MapBuilder()
                .put("channel_id", threadPost.getChannel().getId())
                .put("reply_id", id)
                .build();

        client.getNetworkClient().post(
                HttpAPIRoute.THREAD_DELETE.toFullURL(),
                body
        );
    }

    @Override
    public String toString() {
        return String.format("ThreadReply{id=%s, author=%s, createTime=%d}",
                id, author != null ? author.getName() : "unknown", createTime);
    }
}
