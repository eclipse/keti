package com.ge.predix.acs.attribute.cache;

import com.ge.predix.acs.attribute.readers.CachedAttributes;

public interface AttributeCache {

    default void setAttributes(final String identifier, final CachedAttributes value) {
        this.set(identifier, value);
    }

    default CachedAttributes getAttributes(final String identifier) {
        return this.get(identifier);
    }

    void set(String key, CachedAttributes value);

    // get should return null if value is not found in the cache
    CachedAttributes get(String key);

    void flushAll();
}
