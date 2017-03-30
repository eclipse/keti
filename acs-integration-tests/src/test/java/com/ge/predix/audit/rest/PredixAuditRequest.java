package com.ge.predix.audit.rest;

public class PredixAuditRequest {

    private int page;
    private int pageSize;
    private long startDate;
    private long endDate;

    public PredixAuditRequest(final int page, final int pageSize, final long startDate, final long endDate) {
        this.page = page;
        this.pageSize = pageSize;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public int getPage() {
        return this.page;
    }

    public void setPage(final int page) {
        this.page = page;
    }

    public int getPageSize() {
        return this.pageSize;
    }

    public void setPageSize(final int pageSize) {
        this.pageSize = pageSize;
    }

    public long getStartDate() {
        return this.startDate;
    }

    public void setStartDate(final long startDate) {
        this.startDate = startDate;
    }

    public long getEndDate() {
        return this.endDate;
    }

    public void setEndDate(final long endDate) {
        this.endDate = endDate;
    }

}
