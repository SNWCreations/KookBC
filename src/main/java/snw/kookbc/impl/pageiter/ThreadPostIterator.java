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

package snw.kookbc.impl.pageiter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.fasterxml.jackson.databind.JsonNode;

import snw.jkook.entity.channel.ThreadChannel;
import snw.jkook.entity.thread.ThreadPost;
import snw.kookbc.impl.KBCClient;
import snw.kookbc.impl.entity.thread.ThreadPostImpl;
import snw.kookbc.impl.network.HttpAPIRoute;

/**
 * ThreadPost 分页迭代器
 *
 * <p>用于获取帖子频道中的帖子列表
 *
 * @since KookBC 0.33.0
 */
public class ThreadPostIterator extends PageIteratorImpl<Collection<ThreadPost>> {

    private final ThreadChannel channel;
    private final String categoryId;

    /**
     * 构造 ThreadPost 迭代器
     *
     * @param client KBCClient 实例
     * @param channel 帖子频道
     * @param categoryId 分类 ID (可为 null，表示获取所有分类)
     */
    public ThreadPostIterator(KBCClient client, ThreadChannel channel, @Nullable String categoryId) {
        super(client);
        this.channel = channel;
        this.categoryId = categoryId;
    }

    @Override
    protected String getRequestURL() {
        String url = String.format("%s?channel_id=%s",
                HttpAPIRoute.THREAD_LIST.toFullURL(),
                channel.getId());

        if (categoryId != null && !categoryId.isEmpty()) {
            url += "&category_id=" + categoryId;
        }

        return url;
    }

    @Override
    protected void processElements(JsonNode array) {
        object = new ArrayList<>(array.size());

        for (JsonNode element : array) {
            ThreadPost threadPost = new ThreadPostImpl(client, channel, element);
            object.add(threadPost);
        }
    }
}
