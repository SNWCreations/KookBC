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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;

import snw.jkook.entity.thread.ThreadPost;
import snw.jkook.entity.thread.ThreadReply;
import snw.kookbc.impl.KBCClient;
import snw.kookbc.impl.entity.thread.ThreadReplyImpl;
import snw.kookbc.impl.network.HttpAPIRoute;

/**
 * ThreadReply 分页迭代器
 *
 * <p>用于获取帖子的回复列表
 *
 * @since KookBC 0.32.2
 */
public class ThreadReplyIterator extends PageIteratorImpl<java.util.Collection<ThreadReply>> {

    private final ThreadPost threadPost;

    public ThreadReplyIterator(KBCClient client, ThreadPost threadPost) {
        super(client);
        this.threadPost = threadPost;
    }

    @Override
    protected String getRequestURL() {
        return String.format("%s?channel_id=%s&thread_id=%s",
                HttpAPIRoute.THREAD_POST_LIST.toFullURL(),
                threadPost.getChannel().getId(),
                threadPost.getId());
    }

    @Override
    protected void processElements(JsonNode array) {
        object = new java.util.ArrayList<>(array.size());

        for (JsonNode element : array) {
            ThreadReply reply = new ThreadReplyImpl(client, threadPost, element);
            object.add(reply);
        }
    }
}
