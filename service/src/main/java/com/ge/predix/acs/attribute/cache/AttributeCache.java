package com.ge.predix.acs.attribute.cache;

import java.util.Set;

import com.ge.predix.acs.model.Attribute;

public interface AttributeCache {

    default void setAttributes(final String identifier, final Set<Attribute> value) {
        this.set(identifier, value);
    }

    default Set<Attribute> getAttributes(final String identifier) {
        return this.get(identifier);
    }

    @SuppressWarnings("squid:RedundantThrowsDeclarationCheck")
    void set(String key, Set<Attribute> value) throws StorageLimitExceededException;

    Set<Attribute> get(String key);
    void flushAll();
    int getTtlInSeconds();
}
