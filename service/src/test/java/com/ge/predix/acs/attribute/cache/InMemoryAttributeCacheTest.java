package com.ge.predix.acs.attribute.cache;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.mockito.internal.util.reflection.Whitebox;
import org.springframework.test.util.ReflectionTestUtils;
import org.testng.Assert;
import org.testng.annotations.Test;

import com.ge.predix.acs.attribute.readers.CachedAttributes;
import com.ge.predix.acs.model.Attribute;

public class InMemoryAttributeCacheTest {

    private static final String TEST_ZONE = "test-zone";

    private static final String TEST_KEY = "test-key";

    @SuppressWarnings("unchecked")
    @Test
    public void testSetCachedAttributes() {
        InMemoryAttributeCache inMemoryAttributeCache = new InMemoryAttributeCache(5, TEST_ZONE,
                AbstractAttributeCache::resourceKey);
        Set<Attribute> attributes = Collections.singleton(new Attribute("https://test.com", "attribute1", "value1"));
        CachedAttributes cachedAttributes = new CachedAttributes(attributes);
        inMemoryAttributeCache.set(TEST_KEY, cachedAttributes);

        Map<String, CachedAttributes> resourceCache = (Map<String, CachedAttributes>) Whitebox
                .getInternalState(inMemoryAttributeCache, "attributeCache");
        String cacheKey = AbstractAttributeCache.resourceKey(TEST_ZONE, TEST_KEY);
        Assert.assertEquals(resourceCache.get(cacheKey), cachedAttributes);
    }

    @Test
    public void testGetCachedAttributes() {
        InMemoryAttributeCache inMemoryAttributeCache = new InMemoryAttributeCache(5, TEST_ZONE,
                AbstractAttributeCache::resourceKey);
        Set<Attribute> attributes = Collections.singleton(new Attribute("https://test.com", "attribute1", "value1"));
        CachedAttributes cachedAttributes = new CachedAttributes(attributes);

        Map<String, CachedAttributes> cachedAttributesMap = new HashMap<>();
        String cacheKey = AbstractAttributeCache.resourceKey(TEST_ZONE, TEST_KEY);
        cachedAttributesMap.put(cacheKey, cachedAttributes);
        ReflectionTestUtils.setField(inMemoryAttributeCache, "attributeCache", cachedAttributesMap);

        Assert.assertEquals(inMemoryAttributeCache.get(TEST_KEY), cachedAttributes);

    }

    @SuppressWarnings("unchecked")
    @Test
    public void testFlushAll() {
        InMemoryAttributeCache inMemoryAttributeCache = new InMemoryAttributeCache(5, TEST_ZONE,
                AbstractAttributeCache::subjectKey);
        Set<Attribute> attributes = Collections.singleton(new Attribute("https://test.com", "attribute1", "value1"));
        CachedAttributes cachedAttributes = new CachedAttributes(attributes);

        Map<String, CachedAttributes> cachedAttributesMap = new HashMap<>();
        String cacheKey = AbstractAttributeCache.subjectKey(TEST_ZONE, TEST_KEY);
        cachedAttributesMap.put(cacheKey, cachedAttributes);
        ReflectionTestUtils.setField(inMemoryAttributeCache, "attributeCache", cachedAttributesMap);
        Assert.assertEquals(inMemoryAttributeCache.get(TEST_KEY), cachedAttributes);
        inMemoryAttributeCache.flushAll();
        Assert.assertTrue(
                ((Map<String, CachedAttributes>) ReflectionTestUtils.getField(inMemoryAttributeCache, "attributeCache"))
                        .isEmpty());

    }
}
