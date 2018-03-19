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

public class PredixAuditRecord {

    private String messageId;
    private String version;
    private String auditServiceId;
    private long timestamp;
    private String classifier;
    private String publisherType;
    private String categoryType;
    private String eventType;
    private String payload;
    private String correlationId;
    private String tenantUuid;

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(final String messageId) {
        this.messageId = messageId;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(final String version) {
        this.version = version;
    }

    public String getAuditServiceId() {
        return auditServiceId;
    }

    public void setAuditServiceId(final String auditServiceId) {
        this.auditServiceId = auditServiceId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(final long timestamp) {
        this.timestamp = timestamp;
    }

    public String getClassifier() {
        return classifier;
    }

    public void setClassifier(final String classifier) {
        this.classifier = classifier;
    }

    public String getPublisherType() {
        return publisherType;
    }

    public void setPublisherType(final String publisherType) {
        this.publisherType = publisherType;
    }

    public String getCategoryType() {
        return categoryType;
    }

    public void setCategoryType(final String categoryType) {
        this.categoryType = categoryType;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(final String eventType) {
        this.eventType = eventType;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(final String payload) {
        this.payload = payload;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(final String correlationId) {
        this.correlationId = correlationId;
    }

    public String getTenantUuid() {
        return tenantUuid;
    }

    public void setTenantUuid(final String tenantUuid) {
        this.tenantUuid = tenantUuid;
    }

}
