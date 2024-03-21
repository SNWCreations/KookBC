package snw.kookbc.impl.pageiter;

import snw.jkook.util.Meta;

public class MetaImpl implements Meta {

    private final int page;
    private final int pageTotal;
    private final int pageSize;
    private final int total;

    public MetaImpl(int page, int pageTotal, int pageSize, int total) {
        this.page = page;
        this.pageTotal = pageTotal;
        this.pageSize = pageSize;
        this.total = total;
    }

    @Override
    public int getPage() {
        return page;
    }

    @Override
    public int getPageTotal() {
        return pageTotal;
    }

    @Override
    public int getPageSize() {
        return pageSize;
    }

    @Override
    public int getTotal() {
        return total;
    }
}
