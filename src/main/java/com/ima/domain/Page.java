package com.ima.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Bounded offset page for administrative metadata, not large event exports. */
public final class Page<T> {
    private final List<T> items;
    private final int offset;
    private final int limit;
    private final boolean hasMore;

    public Page(List<T> items, int offset, int limit, boolean hasMore) {
        this.offset = Checks.range(offset, "offset", 0, 1000000);
        this.limit = Checks.range(limit, "limit", 1, 1000);
        Checks.bounded(items, "items", 0, limit);
        this.items = Collections.unmodifiableList(new ArrayList<T>(items));
        this.hasMore = hasMore;
    }

    public List<T> getItems() {
        return items;
    }

    public int getOffset() {
        return offset;
    }

    public int getLimit() {
        return limit;
    }

    public boolean isHasMore() {
        return hasMore;
    }

    public int nextOffset() {
        if (!hasMore) {
            throw new IllegalStateException("there is no next page");
        }
        return offset + items.size();
    }
}
