/*******************************************************************************
 * Copyright 2017 General Electric Company
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/

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
