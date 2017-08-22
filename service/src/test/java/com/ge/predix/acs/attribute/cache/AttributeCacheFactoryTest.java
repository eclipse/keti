package com.ge.predix.acs.attribute.cache;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsInstanceOf.instanceOf;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.core.env.Environment;
import org.springframework.test.util.ReflectionTestUtils;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class AttributeCacheFactoryTest {
    @Mock
    private Environment mockEnvironment;

    @InjectMocks
    private AttributeCacheFactory cacheFactory;

    @BeforeClass
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testAttributeCacheDisabled() {
        setupEnvironment(false, null);
        assertThat(cacheFactory.createResourceAttributeCache(180, "myzone", null),
                instanceOf(NonCachingAttributeCache.class));
        assertThat(cacheFactory.createSubjectAttributeCache(180, "myzone", null),
                instanceOf(NonCachingAttributeCache.class));
    }

    @Test
    public void testAttributeCacheRedis() {
        setupEnvironment(true, "redis");
        assertThat(cacheFactory.createResourceAttributeCache(180, "myzone", null),
                instanceOf(RedisAttributeCache.class));
        assertThat(cacheFactory.createSubjectAttributeCache(180, "myzone", null),
                instanceOf(RedisAttributeCache.class));
    }

    @Test
    public void testAttributeCacheCloudRedis() {
        setupEnvironment(true, "cloud-redis");
        assertThat(cacheFactory.createResourceAttributeCache(180, "myzone", null),
                instanceOf(RedisAttributeCache.class));
        assertThat(cacheFactory.createSubjectAttributeCache(180, "myzone", null),
                instanceOf(RedisAttributeCache.class));
    }

    @Test
    public void testAttributeCacheInMemoty() {
        setupEnvironment(true, null);
        assertThat(cacheFactory.createResourceAttributeCache(180, "myzone", null),
                instanceOf(InMemoryAttributeCache.class));
        assertThat(cacheFactory.createSubjectAttributeCache(180, "myzone", null),
                instanceOf(InMemoryAttributeCache.class));
    }

    private void setupEnvironment(final boolean cachingEnabled, final String springProfileActive) {
        ReflectionTestUtils.setField(cacheFactory, "resourceCachingEnabled", cachingEnabled);
        ReflectionTestUtils.setField(cacheFactory, "subjectCachingEnabled", cachingEnabled);
        String[] redisEnvironment = {springProfileActive};
        Mockito.doReturn(redisEnvironment).when(mockEnvironment).getActiveProfiles();
    }
}
