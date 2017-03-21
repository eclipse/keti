package com.ge.predix.acs.rest;

import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AttributeConnector {
    private boolean isActive;

    @JsonProperty(required = false)
    private int maxCachedIntervalMinutes = 480; //default value

    private Set<AttributeAdapterConnection> adapters;

    public boolean getIsActive() {
        return this.isActive;
    }

    public void setIsActive(final boolean isActive) {
        this.isActive = isActive;
    }

    public int getMaxCachedIntervalMinutes() {
        return this.maxCachedIntervalMinutes;
    }

    public void setMaxCachedIntervalMinutes(final int maxCachedIntervalMinutes) {
        this.maxCachedIntervalMinutes = maxCachedIntervalMinutes;
    }

    public Set<AttributeAdapterConnection> getAdapters() {
        return this.adapters;
    }

    public void setAdapters(final Set<AttributeAdapterConnection> adapters) {
        this.adapters = adapters;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(this.isActive).append(this.maxCachedIntervalMinutes).append(this.adapters)
                .toHashCode();

    }

    @Override
    public boolean equals(final Object obj) {
        if (obj instanceof AttributeConnector) {
            AttributeConnector other = (AttributeConnector) obj;
            return new EqualsBuilder().append(this.isActive, other.isActive)
                    .append(this.maxCachedIntervalMinutes, other.maxCachedIntervalMinutes)
                    .append(this.adapters, other.adapters).isEquals();
        }
        return false;
    }

    public static AttributeConnector newInstance(final AttributeConnector other) {
        AttributeConnector attributeConnector = new AttributeConnector();
        attributeConnector.isActive = other.isActive;
        attributeConnector.maxCachedIntervalMinutes = other.maxCachedIntervalMinutes;
        attributeConnector.adapters = other.adapters.stream().map(AttributeAdapterConnection::new)
                .collect(Collectors.toSet());
        return attributeConnector;
    }
}
