package com.ge.predix.acs.attribute.cache;

import java.util.Map;
import java.util.function.BiFunction;

import org.apache.commons.collections4.map.PassiveExpiringMap;

import com.ge.predix.acs.attribute.readers.CachedAttributes;

public class InMemoryAttributeCache extends AbstractAttributeCache {

    private String zoneName;
    private BiFunction<String, String, String> getKey;
    private Map<String, CachedAttributes> attributeCache;

    InMemoryAttributeCache(final long maxCachedIntervalMinutes, final String zoneName,
            final BiFunction<String, String, String> getKey) {
        this.zoneName = zoneName;
        this.getKey = getKey;
        this.attributeCache = new PassiveExpiringMap<>(maxCachedIntervalMinutes);
    }

    @Override
    public void set(final String key, final CachedAttributes value) {
        this.attributeCache.put(this.getKey.apply(this.zoneName, key), value);
    }

    @Override
    public CachedAttributes get(final String key) {
        return this.attributeCache.get(this.getKey.apply(this.zoneName, key));

    }

    @Override
    public void flushAll() {
        this.attributeCache.clear();

    }

}
