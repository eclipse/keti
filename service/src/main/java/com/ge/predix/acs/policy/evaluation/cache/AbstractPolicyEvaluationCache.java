/*******************************************************************************
 * Copyright 2016 General Electric Company.
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
 *******************************************************************************/

package com.ge.predix.acs.policy.evaluation.cache;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

@Component
public abstract class AbstractPolicyEvaluationCache implements PolicyEvaluationCache {

    @Autowired
    private AttributeConnectorService connectorService;

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractPolicyEvaluationCache.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    public PolicyEvaluationResult get(final PolicyEvaluationRequestCacheKey key) {
        String redisKey = key.toRedisKey();
        List<String> keys = assembleKeys(key);
        List<String> values = multiGet(keys);

        logCacheGetDebugMessages(key, redisKey, keys, values);
        if (!isRequestCached(values)) {
            return null;
        }

        PolicyEvaluationResult cachedResult;
        try {
            cachedResult = OBJECT_MAPPER.readValue(values.get(values.size() - 1), PolicyEvaluationResult.class);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read policy evaluation result as JSON.", e);
        }

        if (isCachedRequestInvalid(values, new DateTime(cachedResult.getTimestamp()))) {
            delete(keys.get(keys.size() - 1));
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(String.format("Cached decision for key '%s' is not valid.", redisKey));
            }
            return null;
        }
        return cachedResult;
    }

    private List<String> assembleKeys(final PolicyEvaluationRequestCacheKey key) {
        List<String> keys = new ArrayList<>();
        LinkedHashSet<String> policySetIds = key.getPolicySetIds();
        policySetIds.forEach(policySetId -> keys.add(policySetKey(key.getZoneId(), policySetId)));
        keys.add(resourceKey(key.getZoneId(), key.getResourceId()));
        keys.add(subjectKey(key.getZoneId(), key.getSubjectId()));
        keys.add(key.toRedisKey());
        return keys;
    }

    private void logCacheGetDebugMessages(final PolicyEvaluationRequestCacheKey key, final String redisKey,
            final List<String> keys, final List<String> values) {
        if (LOGGER.isDebugEnabled()) {
            LinkedHashSet<String> policySetIds = key.getPolicySetIds();
            policySetIds.forEach(policySetId -> LOGGER
                    .debug(String.format("Getting timestamp for policy set: '%s', key: '%s', timestamp:'%s'.",
                            policySetId, keys.get(0), values.get(0))));
            LOGGER.debug(String.format("Getting timestamp for resource: '%s', key: '%s', timestamp:'%s'.",
                    key.getResourceId(), keys.get(1), values.get(1)));
            LOGGER.debug(String.format("Getting timestamp for subject: '%s', key: '%s', timestamp:'%s'.",
                    key.getSubjectId(), keys.get(2), values.get(2)));
            LOGGER.debug(String.format("Getting policy evaluation from cache; key: '%s', value: '%s'.", redisKey,
                    values.get(3)));
        }
    }

    @Override
    public void set(final PolicyEvaluationRequestCacheKey key, final PolicyEvaluationResult result) {
        try {
            DateTime now = new DateTime();
            result.setTimestamp(now.getMillis());
            String value = OBJECT_MAPPER.writeValueAsString(result);
            set(key.toRedisKey(), value);
            setResourceTranslations(key.getZoneId(), result.getResolvedResourceUris(), key.getResourceId());
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(String.format("Setting policy evaluation to cache; key: '%s', value: '%s'.",
                        key.toRedisKey(), value));
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to write policy evaluation result as JSON.", e);
        }
    }

    @Override
    public void setResourceTranslation(final String zoneId, final String fromResourceId, final String toResourceId) {
        String fromKey = resourceTranslationKey(zoneId, fromResourceId);
        String toKey = resourceKey(zoneId, toResourceId);
        setResourceTranslation(fromKey, toKey);
    }

    @Override
    public void setResourceTranslations(final String zoneId, final Set<String> fromResourceIds,
            final String toResourceId) {
        Set<String> fromKeys = new HashSet<>();
        for (String fromResourceId : fromResourceIds) {
            fromKeys.add(resourceTranslationKey(zoneId, fromResourceId));
        }
        String toKey = resourceKey(zoneId, toResourceId);
        setResourceTranslations(fromKeys, toKey);
    }

    @Override
    public void reset() {
        flushAll();
    }

    @Override
    public void reset(final PolicyEvaluationRequestCacheKey key) {
        Set<String> keys = keys(key.toRedisKey());
        delete(keys);
    }

    @Override
    public void resetForPolicySet(final String zoneId, final String policySetId) {
        String key = policySetKey(zoneId, policySetId);
        String timestamp = timestampValue();
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(String.format("Setting timestamp for poliy set '%s'; key: '%s', value: '%s' ", policySetId,
                    key, timestamp));
        }
        set(key, timestamp);
    }

    @Override
    public void resetForResource(final String zoneId, final String resourceId) {
        String timestamp = timestampValue();
        Set<String> toKeys = getResourceTranslations(resourceTranslationKey(zoneId, resourceId));
        Map<String, String> map = new HashMap<>();
        for (String toKey : toKeys) {
            map.put(toKey, timestamp);
            logSetResourceTimestampsDebugMessage(timestamp, toKey, resourceId);
        }
        multiSet(map);
    }

    @Override
    public void resetForResourcesByIds(final String zoneId, final Set<String> resourceIds) {
        multisetForResources(zoneId, resourceIds.stream().collect(Collectors.toList()));
    }

    @Override
    public void resetForResources(final String zoneId, final List<ResourceEntity> resourceEntities) {
        multisetForResources(zoneId, resourceEntities.stream().map(resource -> resource.getResourceIdentifier())
                .collect(Collectors.toList()));
    }

    private void multisetForResources(final String zoneId, final List<String> resourceIds) {
        String timestamp = timestampValue();

        List<String> fromKeys = resourceIds.stream().map(resource -> resourceTranslationKey(zoneId, resource))
                .collect(Collectors.toList());

        List<Object> toKeys = multiGetResourceTranslations(fromKeys);

        Map<String, String> map = new HashMap<>();
        for (int i = 0; i < toKeys.size(); i++) {
            @SuppressWarnings("unchecked")
            Set<String> toKeySet = (Set<String>) toKeys.get(i);
            for (String toKey : toKeySet) {
                map.put(toKey, timestamp);
                String resourceId = resourceIds.get(i);
                logSetResourceTimestampsDebugMessage(timestamp, toKey, resourceId);
            }
        }
        multiSet(map);
    }

    private void logSetResourceTimestampsDebugMessage(final String timestamp, final String toKey,
            final String resourceId) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(String.format("Setting timestamp for resource '%s'; key: '%s', value: '%s' ", resourceId,
                    toKey, timestamp));
        }
    }

    @Override
    public void resetForSubject(final String zoneId, final String subjectId) {
        String key = subjectKey(zoneId, subjectId);
        String timestamp = timestampValue();
        logSetSubjectTimestampDebugMessage(key, timestamp, subjectId);
        set(key, timestamp);
    }

    @Override
    public void resetForSubjectsByIds(final String zoneId, final Set<String> subjectIds) {
        multisetForSubjects(zoneId, subjectIds.stream().collect(Collectors.toList()));
    }

    @Override
    public void resetForSubjects(final String zoneId, final List<SubjectEntity> subjectEntities) {
        multisetForSubjects(zoneId,
                subjectEntities.stream().map(subject -> subject.getSubjectIdentifier()).collect(Collectors.toList()));
    }

    private void multisetForSubjects(final String zoneId, final List<String> subjectIds) {
        Map<String, String> map = new HashMap<>();
        subjectIds.forEach(subjectId -> {
            String key = subjectKey(zoneId, subjectId);
            String timestamp = timestampValue();
            logSetSubjectTimestampDebugMessage(key, timestamp, subjectId);
            map.put(key, timestamp);
        });
        multiSet(map);
    }

    private void logSetSubjectTimestampDebugMessage(final String key, final String timestamp, final String subjectId) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(String.format("Setting timestamp for subject '%s'; key: '%s', value: '%s' ", subjectId, key,
                    timestamp));
        }
    }

    private boolean isRequestCached(final List<String> values) {
        if (null != values.get(values.size() - 1)) {
            return true;
        }
        return false;
    }

    private boolean isCachedRequestInvalid(final List<String> values, final DateTime policyEvalTimestamp) {
        DateTime policyEvalTimestampUTC = policyEvalTimestamp.withZone(DateTimeZone.UTC);

        if (connectorService.isResourceAttributeConnectorConfigured()
                || connectorService.isSubjectAttributeConnectorConfigured()) {
            return haveConnectorCacheIntervalsLapsed(connectorService, policyEvalTimestampUTC);
        } else {
            return havePrivilegeServiceAttributesChanged(values, policyEvalTimestampUTC);
        }
    }

    boolean havePrivilegeServiceAttributesChanged(final List<String> values,
            final DateTime policyEvalTimestampUTC) {
        for (int i = 0; i < values.size() - 1; i++) {
            if (null == values.get(i)) {
                continue;
            }
            DateTime invalidationTimestampUTC;
            try {
                invalidationTimestampUTC = (OBJECT_MAPPER.readValue(values.get(i), DateTime.class))
                        .withZone(DateTimeZone.UTC);
            } catch (IOException e) {
                throw new IllegalStateException("Failed to read timestamp from JSON.", e);
            }

            if (invalidationTimestampUTC.isAfter(policyEvalTimestampUTC)) {
                return true;
            }
        }
        return false;
    }

    boolean haveConnectorCacheIntervalsLapsed(final AttributeConnectorService localConnectorService,
            final DateTime policyEvalTimestampUTC) {
        DateTime nowUTC = new DateTime().withZone(DateTimeZone.UTC);

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

    static String resourceTranslationKey(final String zoneId, final String resourceId) {
        return zoneId + ":rtr-id:" + Integer.toHexString(resourceId.hashCode());
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

    private String timestampValue() {
        try {
            return OBJECT_MAPPER.writeValueAsString(new DateTime());
        } catch (IOException e) {
            throw new IllegalStateException("Failed to write timestamp as JSON.", e);
        }
    }

    abstract void delete(String key);

    abstract void delete(Collection<String> keys);

    abstract void flushAll();

    abstract Set<String> getResourceTranslations(String fromKey);

    abstract Set<String> keys(String key);

    abstract List<String> multiGet(List<String> keys);

    abstract void multiSet(Map<String, String> map);

    abstract List<Object> multiGetResourceTranslations(List<String> fromKeys);

    abstract void set(String key, String value);

    abstract void setResourceTranslation(String fromKey, String toKey);

    abstract void setResourceTranslations(Set<String> fromKeys, String toKey);
}
