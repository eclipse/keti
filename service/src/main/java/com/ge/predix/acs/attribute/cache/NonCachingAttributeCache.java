package com.ge.predix.acs.attribute.cache;

import java.util.Set;

import com.ge.predix.acs.model.Attribute;

public class NonCachingAttributeCache implements AttributeCache {

    @Override
    public void set(final String key, final Set<Attribute> value) throws StorageLimitExceededException {
    }

    @Override
    public Set<Attribute> get(final String key) {
        return null;
    }

    @Override
    public void flushAll() {

    }

    @Override
    public int getTtlInSeconds() {
        return 0;
    }

}