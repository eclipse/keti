package com.ge.predix.audit.rest;

import java.util.Set;

public class PredixAuditResponse {

    private Set<PredixAuditRecord> content;

    public Set<PredixAuditRecord> getContent() {
        return content;
    }

    public void setContent(final Set<PredixAuditRecord> content) {
        this.content = content;
    }

}