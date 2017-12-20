/*******************************************************************************
 * Copyright 2017 General Electric Company
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 *******************************************************************************/

package com.ge.predix.acs.policy.evaluation.cache;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import org.codehaus.jackson.map.ObjectMapper;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Minutes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.ge.predix.acs.attribute.connector.management.AttributeConnectorService;
import com.ge.predix.acs.privilege.management.dao.ResourceEntity;
import com.ge.predix.acs.privilege.management.dao.SubjectEntity;
import com.ge.predix.acs.rest.PolicyEvaluationResult;

enum EntityType {

    RESOURCE("resource"),
    SUBJECT("subject"),
    POLICY_SET("policy set");

    private final String name;

    EntityType(final String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }
}

@Component
public abstract class AbstractPolicyEvaluationCache implements PolicyEvaluationCache {

    @Autowired
    private AttributeConnectorService connectorService;

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractPolicyEvaluationCache.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * This method will get the Policy Evaluation Result from the cache. It will check if any of the subject, policy
     * sets or resolved resource URI's have a timestamp in the cache after the timestamp of the Policy Evaluation
     * Result. If the key is not in the cache or the result is invalidated, it will return null. Also it will remove
     * the Policy EvaluationResult so that subsequent evaluations won't find the key in the cache.
     *
     * @param evalRequestkey The Policy Evaluation key to retrieve.
     * @return The Policy Evaluation Result if the key is in the cache and the result isn't invalidated, or null
     */
    @Override
    public PolicyEvaluationResult get(final PolicyEvaluationRequestCacheKey evalRequestkey) {
        //Get all result related entries
        DecisionCacheEntries cachedEntries = new DecisionCacheEntries(evalRequestkey);

        String cachedEvalResultString = cachedEntries.getDecisionString();
        if (null == cachedEvalResultString) {
            return null;
        }
        PolicyEvaluationResult cachedEvalResult = toPolicyEvaluationResult(cachedEvalResultString);

        List<String> attributeInvalidationTimeStamps = new ArrayList<>();
        List<String> policyInvalidationTimeStamps = new ArrayList<>();
        attributeInvalidationTimeStamps.add(cachedEntries.getSubjectLastModified());
        policyInvalidationTimeStamps.addAll(cachedEntries.getPolicySetsLastModified());

        Set<String> cachedResolvedResourceUris = cachedEvalResult.getResolvedResourceUris();

        //is requested resource id same as resolved resource uri ?
        if (cachedResolvedResourceUris.size() == 1 && cachedResolvedResourceUris.iterator().next()
                .equals(evalRequestkey.getResourceId())) {
            attributeInvalidationTimeStamps.add(cachedEntries.getRequestedResourceLastModified());
        } else {
            List<String> cacheResolvedResourceKeys = cachedResolvedResourceUris.stream()
                    .map(resolvedResourceUri -> resourceKey(evalRequestkey.getZoneId(), resolvedResourceUri))
                    .collect(Collectors.toList());
            attributeInvalidationTimeStamps.addAll(multiGet(cacheResolvedResourceKeys));
        }

        if (isCachedRequestInvalid(attributeInvalidationTimeStamps, policyInvalidationTimeStamps,
                timestampToDateUTC(cachedEvalResult.getTimestamp()))) {
            delete(cachedEntries.getDecisionKey());
            LOGGER.debug("Cached decision for key '{}' is not valid.", cachedEntries.getDecisionKey());
            return null;
        }

        return cachedEvalResult;
    }

    private PolicyEvaluationResult toPolicyEvaluationResult(final String cachedDecisionString) {
        try {
            return OBJECT_MAPPER.readValue(cachedDecisionString, PolicyEvaluationResult.class);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read policy evaluation result as JSON.", e);
        }
    }

    private final class DecisionCacheEntries {
        private final List<String> entryValues;
        private final List<String> entryKeys;
        private final List<String> policySetTimestamps = new ArrayList<>();
        private final int lastValueIndex;
        private final String decisionKey;

        /**
         * Execute a multi-get operation on all entries related to a cached result,
         * and provides a immutable object for the values fetched.
         */
        DecisionCacheEntries(final PolicyEvaluationRequestCacheKey evalRequestKey) {
            //Get all values with a batch get
            this.decisionKey = evalRequestKey.toDecisionKey();
            this.entryKeys = prepareKeys(evalRequestKey);
            this.entryValues = multiGet(this.entryKeys);
            this.lastValueIndex = this.entryValues.size() - 1;

            logCacheGetDebugMessages(evalRequestKey, this.decisionKey, this.entryKeys, this.entryValues);

            //create separate list of policySetTimes to prevent mutation on entryValues
            for (int i = 0; i < evalRequestKey.getPolicySetIds().size(); i++) {
                this.policySetTimestamps.add(this.entryValues.get(i));
            }
        }

        //Prepare keys to fetch in one batch
        private List<String> prepareKeys(final PolicyEvaluationRequestCacheKey evalRequestKey) {
            List<String> keys = new ArrayList<>();

            //Add 'n' Policy Set keys
            LinkedHashSet<String> policySetIds = evalRequestKey.getPolicySetIds();
            policySetIds.forEach(policySetId -> keys.add(policySetKey(evalRequestKey.getZoneId(), policySetId)));

            // n+1
            keys.add(subjectKey(evalRequestKey.getZoneId(), evalRequestKey.getSubjectId()));

            //n+2
            keys.add(resourceKey(evalRequestKey.getZoneId(), evalRequestKey.getResourceId()));

            //n+3
            keys.add(this.decisionKey);

            return keys;
        }

        /**
         * (eval result, eval time, resolved resource uri(s)).
         */
        String getDecisionString() {
            return entryValues.get(this.lastValueIndex);
        }

        String getDecisionKey() {
            return decisionKey;
        }

        String getSubjectLastModified() {
            return entryValues.get(lastValueIndex - 2);
        }

        String getRequestedResourceLastModified() {
            return entryValues.get(lastValueIndex - 1);
        }

        List<String> getPolicySetsLastModified() {
            return this.policySetTimestamps;
        }
    }

    private void logCacheGetDebugMessages(final PolicyEvaluationRequestCacheKey key, final String redisKey,
            final List<String> keys, final List<String> values) {
        LinkedHashSet<String> policySetIds = key.getPolicySetIds();
        int idx = 0;
        for (String policySetId : policySetIds) {
            LOGGER.debug("Getting timestamp for policy set: '{}', key: '{}', timestamp:'{}'.", policySetId,
                    keys.get(idx), values.get(idx++));
        }
        LOGGER.debug("Getting timestamp for subject: '{}', key: '{}', timestamp:'{}'.", key.getSubjectId(),
                keys.get(idx), values.get(idx++));
        LOGGER.debug("Getting timestamp for resource: '{}', key: '{}', timestamp:'{}'.", key.getResourceId(),
                keys.get(idx), values.get(idx++));
        LOGGER.debug("Getting policy evaluation from cache; key: '{}', value: '{}'.", redisKey, values.get(idx));
    }

    // Set's the policy evaluation key to the policy evaluation result in the cache
    @Override
    public void set(final PolicyEvaluationRequestCacheKey key, final PolicyEvaluationResult result) {
        try {
            setEntityTimestamps(key, result);
            result.setTimestamp(currentDateUTC().getMillis());
            String value = OBJECT_MAPPER.writeValueAsString(result);
            set(key.toDecisionKey(), value);
            LOGGER.debug("Setting policy evaluation to cache; key: '{}', value: '{}'.", key.toDecisionKey(), value);
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to write policy evaluation result as JSON.", e);
        }
    }

    private void setEntityTimestamps(final PolicyEvaluationRequestCacheKey key, final PolicyEvaluationResult result) {
        // This ensures that if the timestamp for any entity involved in this decision is not in the cache at the time
        // of this evaluation, it will be put there so that in subsequent evaluations, we will use the cached
        // decision.
        // We reset the timestamp to now for entities only if they do not exist in the cache so that we don't
        // invalidate previous cached decisions.
        LOGGER.debug("Setting timestamp to now for entities if they do not exist in the cache"); 

        String zoneId = key.getZoneId();
        setSubjectIfNotExists(zoneId, key.getSubjectId());

        Set<String> resolvedResourceUris = result.getResolvedResourceUris();
        for (String resolvedResourceUri : resolvedResourceUris) {
            setResourceIfNotExists(zoneId, resolvedResourceUri);
        }

        Set<String> policySetIds = key.getPolicySetIds();
        for (String policySetId : policySetIds) {
            setPolicySetIfNotExists(zoneId, policySetId);
        }
    }

    @Override
    public void reset() {
        flushAll();
    }

    @Override
    public void reset(final PolicyEvaluationRequestCacheKey key) {
        Set<String> keys = keys(key.toDecisionKey());
        delete(keys);
    }

    // Method which resets the timestamp for the given entity in the policy evaluation cache.
    private void resetForEntity(final String zoneId, final String entityId, final EntityType entityType,
            final BiFunction<String, String, String> getKey) {
        String key = getKey.apply(zoneId, entityId);
        String timestamp = timestampUTC();
        logSetEntityTimestampsDebugMessage(timestamp, key, entityId, entityType);
        set(key, timestamp);
    }

    private void setEntityIfNotExists(final String zoneId, final String entityId,
            final BiFunction<String, String, String> getKey) {
        String key = getKey.apply(zoneId, entityId);
        setIfNotExists(key, timestampUTC());
    }

    private void logSetEntityTimestampsDebugMessage(final String timestamp, final String key, final String entityId,
            final EntityType entityType) {
        LOGGER.debug("Setting timestamp for {} '{}'; key: '{}', value: '{}'", entityType, entityId, key, timestamp);
    }

    @Override
    public void resetForPolicySet(final String zoneId, final String policySetId) {
        resetForEntity(zoneId, policySetId, EntityType.POLICY_SET, AbstractPolicyEvaluationCache::policySetKey);
    }

    private void setPolicySetIfNotExists(final String zoneId, final String policySetId) {
        setEntityIfNotExists(zoneId, policySetId, AbstractPolicyEvaluationCache::policySetKey);
    }

    @Override
    public void resetForResource(final String zoneId, final String resourceId) {
        resetForEntity(zoneId, resourceId, EntityType.RESOURCE, AbstractPolicyEvaluationCache::resourceKey);
    }

    private void setResourceIfNotExists(final String zoneId, final String resourceId) {
        setEntityIfNotExists(zoneId, resourceId, AbstractPolicyEvaluationCache::resourceKey);
    }

    @Override
    public void resetForResourcesByIds(final String zoneId, final Set<String> resourceIds) {
        Map<String, String> map = new HashMap<>();
        for (String resourceId : resourceIds) {
            createMutliSetEntityMap(zoneId, map, resourceId, EntityType.RESOURCE,
                    AbstractPolicyEvaluationCache::resourceKey);
        }
        multiSet(map);
    }

    @Override
    public void resetForResources(final String zoneId, final List<ResourceEntity> resourceEntities) {
        Map<String, String> map = new HashMap<>();
        for (ResourceEntity resourceEntity : resourceEntities) {
            createMutliSetEntityMap(zoneId, map, resourceEntity.getResourceIdentifier(), EntityType.RESOURCE,
                    AbstractPolicyEvaluationCache::resourceKey);
        }
        multiSet(map);
    }

    @Override
    public void resetForSubject(final String zoneId, final String subjectId) {
        resetForEntity(zoneId, subjectId, EntityType.SUBJECT, AbstractPolicyEvaluationCache::subjectKey);
    }

    private void setSubjectIfNotExists(final String zoneId, final String subjectId) {
        setEntityIfNotExists(zoneId, subjectId, AbstractPolicyEvaluationCache::subjectKey);
    }

    @Override
    public void resetForSubjectsByIds(final String zoneId, final Set<String> subjectIds) {
        Map<String, String> map = new HashMap<>();
        for (String subjectId : subjectIds) {
            createMutliSetEntityMap(zoneId, map, subjectId, EntityType.SUBJECT,
                    AbstractPolicyEvaluationCache::subjectKey);
        }
        multiSet(map);
    }

    @Override
    public void resetForSubjects(final String zoneId, final List<SubjectEntity> subjectEntities) {
        Map<String, String> map = new HashMap<>();
        for (SubjectEntity subjectEntity : subjectEntities) {
            createMutliSetEntityMap(zoneId, map, subjectEntity.getSubjectIdentifier(), EntityType.SUBJECT,
                    AbstractPolicyEvaluationCache::subjectKey);
        }
        multiSet(map);
    }

    private void createMutliSetEntityMap(final String zoneId, final Map<String, String> map, final String subjectId,
            final EntityType entityType, final BiFunction<String, String, String> getKey) {
        String key = getKey.apply(zoneId, subjectId);
        String timestamp = timestampUTC();
        logSetEntityTimestampsDebugMessage(key, timestamp, subjectId, entityType);
        map.put(key, timestamp);
    }

    private boolean isCachedRequestInvalid(final List<String> attributeInvalidationTimeStamps,
            final List<String> policyInvalidationTimeStamps, final DateTime policyEvalTimestampUTC) {
        if (haveEntitiesChanged(policyInvalidationTimeStamps, policyEvalTimestampUTC)) {
            return true;
        }

        if (this.connectorService.isResourceAttributeConnectorConfigured() || this.connectorService
                .isSubjectAttributeConnectorConfigured()) {
            return haveConnectorCacheIntervalsLapsed(this.connectorService, policyEvalTimestampUTC);
        } else {
            return haveEntitiesChanged(attributeInvalidationTimeStamps, policyEvalTimestampUTC);
        }
    }

    /**
     * This method checks to see if any objects related to the policy evaluation have been changed since the Policy
     * Evaluation Result was cached.
     *
     * @param values                 List of values which contain subject, policy sets and resolved resource URI's.
     * @param policyEvalTimestampUTC The timestamp to compare against.
     * @return true or false depending on whether any of the objects in values has a timestamp after
     * policyEvalTimestampUTC.
     */
    boolean haveEntitiesChanged(final List<String> values, final DateTime policyEvalTimestampUTC) {
        for (String value : values) {
            if (null == value) {
                return true;
            }
            DateTime invalidationTimestampUTC = timestampToDateUTC(value);
            if (invalidationTimestampUTC.isAfter(policyEvalTimestampUTC)) {
                LOGGER.debug("Privilege service attributes have timestamp '{}' which is after "
                        + "policy evaluation timestamp '{}'", invalidationTimestampUTC, policyEvalTimestampUTC);
                return true;
            }
        }
        return false;
    }

    boolean haveConnectorCacheIntervalsLapsed(final AttributeConnectorService localConnectorService,
            final DateTime policyEvalTimestampUTC) {
        DateTime nowUTC = currentDateUTC();

        int decisionAgeMinutes = Minutes.minutesBetween(policyEvalTimestampUTC, nowUTC).getMinutes();

        boolean hasResourceConnectorIntervalLapsed = localConnectorService.isResourceAttributeConnectorConfigured()
                && decisionAgeMinutes >= localConnectorService.getResourceAttributeConnector()
                .getMaxCachedIntervalMinutes();

        boolean hasSubjectConnectorIntervalLapsed = localConnectorService.isSubjectAttributeConnectorConfigured()
                && decisionAgeMinutes >= localConnectorService.getSubjectAttributeConnector()
                .getMaxCachedIntervalMinutes();

        return hasResourceConnectorIntervalLapsed || hasSubjectConnectorIntervalLapsed;
    }

    static String policySetKey(final String zoneId, final String policySetId) {
        return zoneId + ":set-id:" + Integer.toHexString(policySetId.hashCode());
    }

    static String resourceKey(final String zoneId, final String resourceId) {
        return zoneId + ":res-id:" + Integer.toHexString(resourceId.hashCode());
    }

    static String subjectKey(final String zoneId, final String subjectId) {
        return zoneId + ":sub-id:" + Integer.toHexString(subjectId.hashCode());
    }

    static boolean isPolicyEvalResultKey(final String key) {
        return key.matches("^[^:]*:[^:]*:[^:]*:[^:]*$");
    }

    static boolean isPolicySetChangedKey(final String key) {
        return key.matches("^[^:]*:set-id:[^:]*$");
    }

    static boolean isResourceChangedKey(final String key) {
        return key.matches("^[^:]*:res-id:[^:]*$");
    }

    static boolean isSubjectChangedKey(final String key) {
        return key.matches("^[^:]*:sub-id:[^:]*$");
    }

    private static DateTime currentDateUTC() {
        return new DateTime().withZone(DateTimeZone.UTC);
    }

    private static String timestampUTC() {
        try {
            return OBJECT_MAPPER.writeValueAsString(currentDateUTC().getMillis());
        } catch (IOException e) {
            throw new IllegalStateException("Failed to write timestamp as JSON.", e);
        }
    }

    private static DateTime timestampToDateUTC(final long timestamp) {
        return new DateTime(timestamp).withZone(DateTimeZone.UTC);
    }

    private static DateTime timestampToDateUTC(final String timestamp) {
        return new DateTime(Long.valueOf(timestamp)).withZone(DateTimeZone.UTC);
    }

    abstract void delete(String key);

    abstract void delete(Collection<String> keys);

    abstract void flushAll();

    abstract Set<String> keys(String key);

    abstract List<String> multiGet(List<String> keys);

    abstract void multiSet(Map<String, String> map);

    abstract void set(String key, String value);

    abstract void setIfNotExists(String key, String value);
}
