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

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.jetbrains.annotations.Range;
import snw.jkook.util.PageIterator;
import snw.jkook.util.Validate;
import snw.kookbc.impl.KBCClient;

import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class PageIteratorImpl<E> implements PageIterator<E> {
    protected final KBCClient client;
    protected E object;
    protected final AtomicInteger currentPage = new AtomicInteger(1);
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
        JsonObject object = client.getNetworkClient().get(
                reqUrl + (reqUrl.contains("?") ? "&" : "?") + "page=" + currentPage.get() + "&page_size=" + getPageSize()
        );
        JsonObject meta = object.getAsJsonObject("meta");
        next = currentPage.getAndAdd(1) <= meta.get("page_total").getAsInt();
        if (next) {
            processElements(object.getAsJsonArray("items"));
        }
        return next;
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

    protected abstract String getRequestURL();

    protected abstract void processElements(JsonArray array);
}
