package org.eclipse.keti.acs.config;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsInstanceOf.instanceOf;

import org.eclipse.keti.acs.commons.policy.condition.groovy.InMemoryGroovyConditionCache;
import org.eclipse.keti.acs.commons.policy.condition.groovy.NonCachingGroovyConditionCache;
import org.testng.annotations.Test;

public class PolicyConditionCacheConfigTest {

    private PolicyConditionCacheConfig policyConditionCacheConfig = new PolicyConditionCacheConfig();

    @Test
    public void testPolicyConditionCacheConfigDisabled() {
        assertThat(this.policyConditionCacheConfig.conditionCache(true),
                instanceOf(NonCachingGroovyConditionCache.class));
    }

    @Test
    public void testPolicyConditionCacheConfigEnabled() {
        assertThat(this.policyConditionCacheConfig.conditionCache(false),
                instanceOf(InMemoryGroovyConditionCache.class));
    }
}
