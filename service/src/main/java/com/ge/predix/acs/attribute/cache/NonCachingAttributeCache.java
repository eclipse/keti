package com.ge.predix.acs.attribute.cache;

import com.ge.predix.acs.attribute.readers.CachedAttributes;

public class NonCachingAttributeCache implements AttributeCache {

    @Override
    public void set(final String key, final CachedAttributes value) {
        //Intentionally left empty to satisfy interface
    }

    @Override
    public CachedAttributes get(final String key) {
        return null;
    }

    @Override
    public void flushAll() {
        //Intentionally left empty to satisfy interface
    }

}