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

package snw.kookbc.impl.pageiter;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.jetbrains.annotations.Range;
import snw.jkook.util.PageIterator;
import snw.kookbc.impl.KBCClient;
import snw.jkook.util.Validate;

import java.util.concurrent.atomic.AtomicInteger;

public abstract class PageIteratorImpl<E> implements PageIterator<E> {
    protected final AtomicInteger currentPage = new AtomicInteger(1);
    private int pageSizePerRequest;
    private boolean executedOnce = false;

    @Override
    public boolean hasNext() {
        if (!executedOnce) {
            executedOnce = true;
        }
        JsonObject object = KBCClient.getInstance().getConnector().getClient().get(
                getRequestURL() + "&page=" + currentPage.getAndAdd(1) + "&page_size=" + getPageSize()
        );
        JsonObject meta = object.getAsJsonObject("meta");
        boolean res = meta.get("page").getAsInt() < meta.get("page_total").getAsInt();
        if (res) {
            processElements(object.getAsJsonArray("items"));
        } else {
            onHasNextButNoMoreElement();
        }
        return res;
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

    protected abstract String getRequestURL();

    protected abstract void processElements(JsonArray array);

    // just a hook, so no argument will be provided
    protected abstract void onHasNextButNoMoreElement();
}
