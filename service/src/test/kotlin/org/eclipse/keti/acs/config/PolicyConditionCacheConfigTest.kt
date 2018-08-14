package org.eclipse.keti.acs.config

import org.eclipse.keti.acs.commons.policy.condition.groovy.InMemoryGroovyConditionCache
import org.eclipse.keti.acs.commons.policy.condition.groovy.NonCachingGroovyConditionCache
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.core.IsInstanceOf.instanceOf
import org.testng.annotations.Test

class PolicyConditionCacheConfigTest {

    private val policyConditionCacheConfig = PolicyConditionCacheConfig()

    @Test
    fun testPolicyConditionCacheConfigDisabled() {
        assertThat(
            this.policyConditionCacheConfig.conditionCache(true),
            instanceOf(NonCachingGroovyConditionCache::class.java)
        )
    }

    @Test
    fun testPolicyConditionCacheConfigEnabled() {
        assertThat(
            this.policyConditionCacheConfig.conditionCache(false),
            instanceOf(InMemoryGroovyConditionCache::class.java)
        )
    }
}
