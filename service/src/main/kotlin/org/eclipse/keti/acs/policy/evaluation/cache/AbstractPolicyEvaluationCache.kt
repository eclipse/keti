/*******************************************************************************
 * Copyright 2017 General Electric Company
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.eclipse.keti.acs.policy.evaluation.cache

import org.codehaus.jackson.map.ObjectMapper
import org.eclipse.keti.acs.attribute.connector.management.AttributeConnectorService
import org.eclipse.keti.acs.privilege.management.dao.ResourceEntity
import org.eclipse.keti.acs.privilege.management.dao.SubjectEntity
import org.eclipse.keti.acs.rest.PolicyEvaluationResult
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.joda.time.Minutes
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.io.IOException
import java.util.ArrayList
import java.util.HashMap

internal enum class EntityType constructor(val value: String) {

    RESOURCE("resource"),
    SUBJECT("subject"),
    POLICY_SET("policy set");

    override fun toString(): String {
        return value
    }
}

private val LOGGER = LoggerFactory.getLogger(AbstractPolicyEvaluationCache::class.java)
private val OBJECT_MAPPER = ObjectMapper()

fun policySetKey(
    zoneId: String,
    policySetId: String
): String {
    return zoneId + ":set-id:" + Integer.toHexString(policySetId.hashCode())
}

fun resourceKey(
    zoneId: String,
    resourceId: String
): String {
    return zoneId + ":res-id:" + Integer.toHexString(resourceId.hashCode())
}

fun subjectKey(
    zoneId: String,
    subjectId: String
): String {
    return zoneId + ":sub-id:" + Integer.toHexString(subjectId.hashCode())
}

internal fun isPolicyEvalResultKey(key: String): Boolean {
    return key.matches("^[^:]*:[^:]*:[^:]*:[^:]*$".toRegex())
}

internal fun isPolicySetChangedKey(key: String): Boolean {
    return key.matches("^[^:]*:set-id:[^:]*$".toRegex())
}

internal fun isResourceChangedKey(key: String): Boolean {
    return key.matches("^[^:]*:res-id:[^:]*$".toRegex())
}

internal fun isSubjectChangedKey(key: String): Boolean {
    return key.matches("^[^:]*:sub-id:[^:]*$".toRegex())
}

private fun currentDateUTC(): DateTime {
    return DateTime().withZone(DateTimeZone.UTC)
}

private fun timestampUTC(): String {
    try {
        return OBJECT_MAPPER.writeValueAsString(currentDateUTC().millis)
    } catch (e: IOException) {
        throw IllegalStateException("Failed to write timestamp as JSON.", e)
    }
}

private fun timestampToDateUTC(timestamp: Long): DateTime {
    return DateTime(timestamp).withZone(DateTimeZone.UTC)
}

private fun timestampToDateUTC(timestamp: String): DateTime {
    return DateTime(java.lang.Long.valueOf(timestamp)).withZone(DateTimeZone.UTC)
}

@Component
abstract class AbstractPolicyEvaluationCache : PolicyEvaluationCache {

    @Autowired
    private lateinit var connectorService: AttributeConnectorService

    /**
     * This method will get the Policy Evaluation Result from the cache. It will check if any of the subject, policy
     * sets or resolved resource URI's have a timestamp in the cache after the timestamp of the Policy Evaluation
     * Result. If the key is not in the cache or the result is invalidated, it will return null. Also it will remove
     * the Policy EvaluationResult so that subsequent evaluations won't find the key in the cache.
     *
     * @param key The Policy Evaluation key to retrieve.
     * @return The Policy Evaluation Result if the key is in the cache and the result isn't invalidated, or null
     */
    override fun get(key: PolicyEvaluationRequestCacheKey): PolicyEvaluationResult? {
        // Get all result related entries
        val cachedEntries = DecisionCacheEntries(key)

        val cachedEvalResultString = cachedEntries.decisionString ?: return null
        val cachedEvalResult = toPolicyEvaluationResult(cachedEvalResultString)

        val attributeInvalidationTimeStamps = ArrayList<String?>()
        val policyInvalidationTimeStamps = ArrayList<String?>()
        attributeInvalidationTimeStamps.add(cachedEntries.subjectLastModified)
        policyInvalidationTimeStamps.addAll(cachedEntries.policySetsLastModified)

        val cachedResolvedResourceUris = cachedEvalResult.resolvedResourceUris

        // is requested resource id same as resolved resource uri ?
        if (cachedResolvedResourceUris.size == 1 && cachedResolvedResourceUris.iterator().next() == key.resourceId) {
            attributeInvalidationTimeStamps.add(cachedEntries.requestedResourceLastModified)
        } else {
            val cacheResolvedResourceKeys = cachedResolvedResourceUris
                .map { resolvedResourceUri -> resourceKey(key.zoneId!!, resolvedResourceUri) }
                .toList()
            attributeInvalidationTimeStamps.addAll(multiGet(cacheResolvedResourceKeys))
        }

        if (isCachedRequestInvalid(
                attributeInvalidationTimeStamps,
                policyInvalidationTimeStamps,
                timestampToDateUTC(cachedEvalResult.timestamp)
            )
        ) {
            delete(cachedEntries.decisionKey)
            LOGGER.debug("Cached decision for key '{}' is not valid.", cachedEntries.decisionKey)
            return null
        }

        return cachedEvalResult
    }

    private fun toPolicyEvaluationResult(cachedDecisionString: String): PolicyEvaluationResult {
        try {
            return OBJECT_MAPPER.readValue(cachedDecisionString, PolicyEvaluationResult::class.java)
        } catch (e: IOException) {
            throw IllegalStateException("Failed to read policy evaluation result as JSON.", e)
        }
    }

    private inner class DecisionCacheEntries
    /**
     * Execute a multi-get operation on all entries related to a cached result,
     * and provides a immutable object for the values fetched.
     */
    internal constructor(evalRequestKey: PolicyEvaluationRequestCacheKey) {

        private val entryValues: List<String?>
        private val entryKeys: List<String>
        private val policySetTimestamps = ArrayList<String?>()
        private val lastValueIndex: Int
        internal val decisionKey: String = evalRequestKey.toDecisionKey()

        /**
         * (eval result, eval time, resolved resource uri(s)).
         */
        internal val decisionString: String?
            get() = entryValues[this.lastValueIndex]

        internal val subjectLastModified: String?
            get() = entryValues[lastValueIndex - 2]

        internal val requestedResourceLastModified: String?
            get() = entryValues[lastValueIndex - 1]

        internal val policySetsLastModified: List<String?>
            get() = this.policySetTimestamps

        init {
            // Get all values with a batch get
            this.entryKeys = prepareKeys(evalRequestKey)
            this.entryValues = multiGet(this.entryKeys)
            this.lastValueIndex = this.entryValues.size - 1

            logCacheGetDebugMessages(evalRequestKey, this.decisionKey, this.entryKeys, this.entryValues)

            // create separate list of policySetTimes to prevent mutation on entryValues
            for (i in 0 until evalRequestKey.policySetIds.size) {
                this.policySetTimestamps.add(this.entryValues[i])
            }
        }

        // Prepare keys to fetch in one batch
        private fun prepareKeys(evalRequestKey: PolicyEvaluationRequestCacheKey): List<String> {
            val keys = ArrayList<String>()

            // Add 'n' Policy Set keys
            val policySetIds = evalRequestKey.policySetIds
            policySetIds.forEach { policySetId -> keys.add(policySetKey(evalRequestKey.zoneId!!, policySetId!!)) }

            // n+1
            keys.add(subjectKey(evalRequestKey.zoneId!!, evalRequestKey.subjectId!!))

            // n+2
            keys.add(resourceKey(evalRequestKey.zoneId, evalRequestKey.resourceId!!))

            // n+3
            keys.add(this.decisionKey)

            return keys
        }
    }

    private fun logCacheGetDebugMessages(
        key: PolicyEvaluationRequestCacheKey,
        redisKey: String,
        keys: List<String>,
        values: List<String?>
    ) {
        val policySetIds = key.policySetIds
        var idx = 0
        for (policySetId in policySetIds) {
            LOGGER.debug(
                "Getting timestamp for policy set: '{}', key: '{}', timestamp:'{}'.",
                policySetId,
                keys[idx],
                values[idx++]
            )
        }
        LOGGER.debug(
            "Getting timestamp for subject: '{}', key: '{}', timestamp:'{}'.", key.subjectId, keys[idx], values[idx++]
        )
        LOGGER.debug(
            "Getting timestamp for resource: '{}', key: '{}', timestamp:'{}'.", key.resourceId, keys[idx], values[idx++]
        )
        LOGGER.debug("Getting policy evaluation from cache; key: '{}', value: '{}'.", redisKey, values[idx])
    }

    // Set's the policy evaluation key to the policy evaluation result in the cache
    override fun set(
        key: PolicyEvaluationRequestCacheKey,
        value: PolicyEvaluationResult
    ) {
        try {
            setEntityTimestamps(key, value)
            value.timestamp = currentDateUTC().millis
            val result = OBJECT_MAPPER.writeValueAsString(value)
            set(key.toDecisionKey(), result)
            LOGGER.debug("Setting policy evaluation to cache; key: '{}', value: '{}'.", key.toDecisionKey(), result)
        } catch (e: IOException) {
            throw IllegalArgumentException("Failed to write policy evaluation result as JSON.", e)
        }
    }

    private fun setEntityTimestamps(
        key: PolicyEvaluationRequestCacheKey,
        result: PolicyEvaluationResult
    ) {
        // This ensures that if the timestamp for any entity involved in this decision is not in the cache at the time
        // of this evaluation, it will be put there so that in subsequent evaluations, we will use the cached
        // decision.
        // We reset the timestamp to now for entities only if they do not exist in the cache so that we don't
        // invalidate previous cached decisions.
        LOGGER.debug("Setting timestamp to now for entities if they do not exist in the cache")

        val zoneId = key.zoneId!!
        setSubjectIfNotExists(zoneId, key.subjectId!!)

        val resolvedResourceUris = result.resolvedResourceUris
        for (resolvedResourceUri in resolvedResourceUris) {
            setResourceIfNotExists(zoneId, resolvedResourceUri)
        }

        val policySetIds = key.policySetIds
        for (policySetId in policySetIds) {
            setPolicySetIfNotExists(zoneId, policySetId!!)
        }
    }

    override fun reset() {
        flushAll()
    }

    override fun reset(key: PolicyEvaluationRequestCacheKey) {
        val keys = keys(key.toDecisionKey())
        delete(keys)
    }

    // Method which resets the timestamp for the given entity in the policy evaluation cache.
    private fun resetForEntity(
        zoneId: String,
        entityId: String,
        entityType: EntityType,
        getKey: (String, String) -> String
    ) {
        val key = getKey(zoneId, entityId)
        val timestamp = timestampUTC()
        logSetEntityTimestampsDebugMessage(timestamp, key, entityId, entityType)
        set(key, timestamp)
    }

    private fun setEntityIfNotExists(
        zoneId: String,
        entityId: String,
        getKey: (String, String) -> String
    ) {
        val key = getKey(zoneId, entityId)
        setIfNotExists(key, timestampUTC())
    }

    private fun logSetEntityTimestampsDebugMessage(
        timestamp: String,
        key: String,
        entityId: String?,
        entityType: EntityType
    ) {
        LOGGER.debug("Setting timestamp for {} '{}'; key: '{}', value: '{}'", entityType, entityId, key, timestamp)
    }

    override fun resetForPolicySet(
        zoneId: String,
        policySetId: String
    ) {
        resetForEntity(
            zoneId,
            policySetId,
            EntityType.POLICY_SET
        ) { _, _ -> policySetKey(zoneId, policySetId) }
    }

    private fun setPolicySetIfNotExists(
        zoneId: String,
        policySetId: String
    ) {
        setEntityIfNotExists(
            zoneId,
            policySetId
        ) { _, _ -> policySetKey(zoneId, policySetId) }
    }

    override fun resetForResource(
        zoneId: String,
        resourceId: String
    ) {
        resetForEntity(
            zoneId,
            resourceId,
            EntityType.RESOURCE
        ) { _, _ -> resourceKey(zoneId, resourceId) }
    }

    private fun setResourceIfNotExists(
        zoneId: String,
        resourceId: String
    ) {
        setEntityIfNotExists(zoneId, resourceId) { _, _ -> resourceKey(zoneId, resourceId) }
    }

    override fun resetForResourcesByIds(
        zoneId: String,
        resourceIds: Set<String>
    ) {
        val map = HashMap<String, String>()
        for (resourceId in resourceIds) {
            createMutliSetEntityMap(
                zoneId,
                map,
                resourceId,
                EntityType.RESOURCE
            ) { _, _ -> resourceKey(zoneId, resourceId) }
        }
        multiSet(map)
    }

    override fun resetForResources(
        zoneId: String,
        entities: List<ResourceEntity>
    ) {
        val map = HashMap<String, String>()
        for (resourceEntity in entities) {
            createMutliSetEntityMap(
                zoneId,
                map,
                resourceEntity.resourceIdentifier!!,
                EntityType.RESOURCE
            ) { _, resourceId -> resourceKey(zoneId, resourceId) }
        }
        multiSet(map)
    }

    override fun resetForSubject(
        zoneId: String,
        subjectId: String
    ) {
        resetForEntity(
            zoneId,
            subjectId,
            EntityType.SUBJECT
        ) { _, _ -> subjectKey(zoneId, subjectId) }
    }

    private fun setSubjectIfNotExists(
        zoneId: String,
        subjectId: String
    ) {
        setEntityIfNotExists(zoneId, subjectId) { _, _ -> subjectKey(zoneId, subjectId) }
    }

    override fun resetForSubjectsByIds(
        zoneId: String,
        subjectIds: Set<String>
    ) {
        val map = HashMap<String, String>()
        for (subjectId in subjectIds) {
            createMutliSetEntityMap(
                zoneId,
                map,
                subjectId,
                EntityType.SUBJECT
            ) { _, _ -> subjectKey(zoneId, subjectId) }
        }
        multiSet(map)
    }

    override fun resetForSubjects(
        zoneId: String,
        subjectEntities: List<SubjectEntity>
    ) {
        val map = HashMap<String, String>()
        for (subjectEntity in subjectEntities) {
            createMutliSetEntityMap(
                zoneId,
                map,
                subjectEntity.subjectIdentifier!!,
                EntityType.SUBJECT
            ) { _, subjectId -> subjectKey(zoneId, subjectId) }
        }
        multiSet(map)
    }

    private fun createMutliSetEntityMap(
        zoneId: String,
        map: MutableMap<String, String>,
        subjectId: String,
        entityType: EntityType,
        getKey: (String, String) -> String
    ) {
        val key = getKey(zoneId, subjectId)
        val timestamp = timestampUTC()
        logSetEntityTimestampsDebugMessage(key, timestamp, subjectId, entityType)
        map[key] = timestamp
    }

    private fun isCachedRequestInvalid(
        attributeInvalidationTimeStamps: List<String?>,
        policyInvalidationTimeStamps: List<String?>,
        policyEvalTimestampUTC: DateTime
    ): Boolean {
        if (haveEntitiesChanged(policyInvalidationTimeStamps, policyEvalTimestampUTC)) {
            return true
        }

        return if (this.connectorService.isResourceAttributeConnectorConfigured || this.connectorService.isSubjectAttributeConnectorConfigured) {
            haveConnectorCacheIntervalsLapsed(this.connectorService, policyEvalTimestampUTC)
        } else {
            haveEntitiesChanged(attributeInvalidationTimeStamps, policyEvalTimestampUTC)
        }
    }

    /**
     * This method checks to see if any objects related to the policy evaluation have been changed since the Policy
     * Evaluation Result was cached.
     *
     * @param values List of values which contain subject, policy sets and resolved resource URI's.
     * @param policyEvalTimestampUTC The timestamp to compare against.
     * @return true or false depending on whether any of the objects in values has a timestamp after
     * policyEvalTimestampUTC.
     */
    fun haveEntitiesChanged(
        values: List<String?>,
        policyEvalTimestampUTC: DateTime
    ): Boolean {
        for (value in values) {
            if (null == value) {
                return true
            }
            val invalidationTimestampUTC = timestampToDateUTC(value)
            if (invalidationTimestampUTC.isAfter(policyEvalTimestampUTC)) {
                LOGGER.debug(
                    "Privilege service attributes have timestamp '{}' which is after " + "policy evaluation timestamp '{}'",
                    invalidationTimestampUTC,
                    policyEvalTimestampUTC
                )
                return true
            }
        }
        return false
    }

    fun haveConnectorCacheIntervalsLapsed(
        localConnectorService: AttributeConnectorService,
        policyEvalTimestampUTC: DateTime
    ): Boolean {
        val nowUTC = currentDateUTC()

        val decisionAgeMinutes = Minutes.minutesBetween(policyEvalTimestampUTC, nowUTC).minutes

        val hasResourceConnectorIntervalLapsed =
            localConnectorService.isResourceAttributeConnectorConfigured && decisionAgeMinutes >= localConnectorService.resourceAttributeConnector!!.maxCachedIntervalMinutes

        val hasSubjectConnectorIntervalLapsed =
            localConnectorService.isSubjectAttributeConnectorConfigured && decisionAgeMinutes >= localConnectorService.subjectAttributeConnector!!.maxCachedIntervalMinutes

        return hasResourceConnectorIntervalLapsed || hasSubjectConnectorIntervalLapsed
    }

    abstract fun delete(key: String)

    abstract fun delete(keys: Collection<String>)

    abstract fun flushAll()

    abstract fun keys(key: String): Set<String>

    abstract fun multiGet(keys: List<String>): List<String?>

    abstract fun multiSet(map: Map<String, String>)

    abstract operator fun set(
        key: String,
        value: String
    )

    abstract fun setIfNotExists(
        key: String,
        value: String
    )
}
