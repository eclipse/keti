package com.ge.predix.acs.attribute.cache;

// CHECKSTYLE:OFF: FinalClass
public class AttributeCacheFactory {

    private AttributeCacheFactory() {
        throw new AssertionError();
    }

    public static AttributeCache createResourceAttributeCache(final long maxStorageInMegabytes) {
        return new NonCachingAttributeCache();
    }

    public static AttributeCache createSubjectAttributeCache(final long maxStorageInMegabytes) {
        return new NonCachingAttributeCache();
    }
}
// CHECKSTYLE:ON: FinalClass
