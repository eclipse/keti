package org.eclipse.keti.acs.config

import org.eclipse.keti.acs.commons.policy.condition.groovy.GroovyConditionCache
import org.eclipse.keti.acs.commons.policy.condition.groovy.InMemoryGroovyConditionCache
import org.eclipse.keti.acs.commons.policy.condition.groovy.NonCachingGroovyConditionCache
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

private val LOGGER = LoggerFactory.getLogger(PolicyConditionCacheConfig::class.java)

@Configuration
class PolicyConditionCacheConfig {

    @Bean
    fun conditionCache(
        @Value("\${ENABLE_DECISION_CACHING:true}") decisionCachingEnabled: Boolean
    ): GroovyConditionCache {
        if (decisionCachingEnabled) {
            LOGGER.info(
                "In-memory condition caching disabled for policy evaluation (superseded by decision caching)."
            )
            return NonCachingGroovyConditionCache()
        }

        LOGGER.info("In-memory condition caching enabled for policy evaluation.")
        return InMemoryGroovyConditionCache()
    }
}
