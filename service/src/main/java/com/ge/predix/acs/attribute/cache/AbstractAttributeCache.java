package com.ge.predix.acs.attribute.cache;

public abstract class AbstractAttributeCache implements AttributeCache {

    static String resourceKey(final String zoneId, final String identifier) {
        return zoneId + ":attr-res-id:" + Integer.toHexString(identifier.hashCode());
    }

    static String subjectKey(final String zoneId, final String identifier) {
        return zoneId + ":attr-sub-id:" + Integer.toHexString(identifier.hashCode());
    }
}
