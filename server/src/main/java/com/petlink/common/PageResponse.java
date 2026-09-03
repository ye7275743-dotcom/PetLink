package com.petlink.common;

import java.util.List;

public class PageResponse<T> {
    private List<T> records;
    private int page;
    private int size;
    private long total;
    private long pages;

    public PageResponse() {}

    public PageResponse(List<T> records, int page, int size, long total) {
        this.records = records;
        this.page = page;
        this.size = size;
        this.total = total;
        this.pages = total == 0 ? 0 : (total + size - 1) / size;
    }

    public List<T> getRecords() { return records; }
    public void setRecords(List<T> records) { this.records = records; }
    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }
    public int getSize() { return size; }
    public void setSize(int size) { this.size = size; }
    public long getTotal() { return total; }
    public void setTotal(long total) { this.total = total; }
    public long getPages() { return pages; }
    public void setPages(long pages) { this.pages = pages; }
}
