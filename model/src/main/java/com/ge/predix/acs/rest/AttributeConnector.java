package com.ge.predix.acs.rest;

import java.util.Set;

public class AttributeConnector {
    private boolean isActive;
    
    private int maxCachedIntervalMinutes;
    
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
        final int prime = 31;
        int result = 1;
        result = prime * result + ((this.adapters == null) ? 0 : this.adapters.hashCode());
        result = prime * result + (this.isActive ? 1231 : 1237);
        result = prime * result + this.maxCachedIntervalMinutes;
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        AttributeConnector other = (AttributeConnector) obj;
        if (this.adapters == null) {
            if (other.adapters != null) {
                return false;
            }
        } else if (!this.adapters.equals(other.adapters)) {
            return false;
        }
        if (this.isActive != other.isActive) {
            return false;
        }
        if (this.maxCachedIntervalMinutes != other.maxCachedIntervalMinutes) {
            return false;
        }
        return true;
    }
}
