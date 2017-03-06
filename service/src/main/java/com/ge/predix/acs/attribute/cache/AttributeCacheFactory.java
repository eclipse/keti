package com.ge.predix.acs.attribute.cache;

public interface AttributeCacheFactory {
    AttributeCache createResourceAttributeCache(String zoneId, long maxStorageInMegabytes);
    AttributeCache createSubjectAttributeCache(String zoneId, long maxStorageInMegabytes);
}
