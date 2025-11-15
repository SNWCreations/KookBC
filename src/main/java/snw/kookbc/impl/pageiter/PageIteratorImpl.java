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

import com.fasterxml.jackson.databind.JsonNode;
import org.jetbrains.annotations.Range;
import snw.jkook.util.Meta;
import snw.jkook.util.PageIterator;
import snw.jkook.util.Validate;
import snw.kookbc.impl.KBCClient;

import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class PageIteratorImpl<E> implements PageIterator<E> {
    protected final KBCClient client;
    protected E object;
    protected final AtomicInteger currentPage = new AtomicInteger(1);
    private Optional<Meta> optionalMeta = Optional.empty();
    private int pageSizePerRequest = 20;
    private boolean executedOnce = false;
    private boolean next = true;

    protected PageIteratorImpl(KBCClient client) {
        this.client = client;
    }

    @Override
    public boolean hasNext() {
        if (!next) {
            return false; // make sure we won't execute useless request
        }

        if (!executedOnce) {
            executedOnce = true;
        }
        String reqUrl = getRequestURL();
        // 使用Jackson API获得更好的性能
        JsonNode object = client.getNetworkClient().get(
                reqUrl + (reqUrl.contains("?") ? "&" : "?") + "page=" + currentPage.get() + "&page_size=" + getPageSize()
        );

        JsonNode meta = object.get("meta");
        JsonNode items = object.get("items");

        // 先处理返回的数据项
        boolean hasData = false;
        if (items != null && items.isArray() && items.size() > 0) {
            processElements(items);
            hasData = true;
        }

        // 然后判断是否还有下一页
        if (meta != null && !meta.isNull()) {
            // 有 meta 字段：使用分页信息判断是否有下一页
            optionalMeta = Optional.of(new MetaImpl(meta.get("page").asInt(),
                    meta.get("page_total").asInt(),
                    meta.get("page_size").asInt(),
                    meta.get("total").asInt()));
            next = currentPage.getAndAdd(1) <= optionalMeta.get().getPageTotal();
        } else {
            // 无 meta 字段：根据返回的 items 数量判断是否有下一页
            // 如果返回的 items 数量等于 page_size，可能还有下一页
            if (items != null && items.isArray()) {
                int itemCount = items.size();
                next = itemCount >= pageSizePerRequest;
                currentPage.incrementAndGet();
            } else {
                next = false;
            }
        }

        // 返回当前是否有数据（不是下一页是否有数据）
        return hasData;
    }

    @Override
    public E next() {
        if (object == null) {
            throw new NoSuchElementException();
        }
        E var1 = object;
        object = null;
        return var1;
    }

    @Override
    public int getPageSize() {
        return pageSizePerRequest;
    }

    @Override
    public void setPageSize(@Range(from = 50L, to = 100L) int size) {
        Validate.isTrue(!executedOnce, "You can't set the page size for this iterator again.");
        this.pageSizePerRequest = size;
    }

    @Override
    public Optional<Meta> getMeta() {
        return optionalMeta;
    }

    protected abstract String getRequestURL();

    protected abstract void processElements(JsonNode node);

    // 向后兼容方法：支持GSON JsonArray（已弃用）
    protected void processElements(com.google.gson.JsonArray array) {
        // 将JsonArray转换为JsonNode供新方法使用，确保兼容性
        processElements(snw.kookbc.util.JacksonUtil.parse(array.toString()));
    }
}
