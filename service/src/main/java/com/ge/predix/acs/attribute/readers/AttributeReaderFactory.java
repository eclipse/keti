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

package com.ge.predix.acs.attribute.readers;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.ConcurrentReferenceHashMap;

import com.ge.predix.acs.attribute.cache.AttributeCacheFactory;
import com.ge.predix.acs.attribute.connector.management.AttributeConnectorService;
import com.ge.predix.acs.zone.resolver.SpringSecurityZoneResolver;

// CHECKSTYLE:OFF: FinalClass
@Component
public class AttributeReaderFactory {

    @Value("${ADAPTER_TIMEOUT_MILLIS:3000}")
    private int adapterTimeoutMillis;

    @Autowired
    private PrivilegeServiceResourceAttributeReader privilegeServiceResourceAttributeReader;

    @Autowired
    private PrivilegeServiceSubjectAttributeReader privilegeServiceSubjectAttributeReader;

    @Autowired
    private AttributeCacheFactory attributeCacheFactory;

    private AttributeConnectorService connectorService;
    private RedisTemplate<String, String> resourceCacheRedisTemplate;
    private RedisTemplate<String, String> subjectCacheRedisTemplate;

    // Caches that use the multiton design pattern (keyed off the zone name)
    private final Map<String, ExternalResourceAttributeReader> externalResourceAttributeReaderCache = new
            ConcurrentReferenceHashMap<>();
    private final Map<String, ExternalSubjectAttributeReader> externalSubjectAttributeReaderCache = new
            ConcurrentReferenceHashMap<>();

    public ResourceAttributeReader getResourceAttributeReader() {
        if (!this.connectorService.isResourceAttributeConnectorConfigured()) {
            return this.privilegeServiceResourceAttributeReader;
        }

        String zoneName = SpringSecurityZoneResolver.getZoneName();
        ExternalResourceAttributeReader externalResourceAttributeReader = this.externalResourceAttributeReaderCache
                .get(zoneName);
        if (externalResourceAttributeReader != null) {
            return externalResourceAttributeReader;
        }

        externalResourceAttributeReader = new ExternalResourceAttributeReader(this.connectorService,
                this.attributeCacheFactory.createResourceAttributeCache(
                        this.connectorService.getResourceAttributeConnector().getMaxCachedIntervalMinutes(), zoneName,
                        this.resourceCacheRedisTemplate), adapterTimeoutMillis);
        this.externalResourceAttributeReaderCache.put(zoneName, externalResourceAttributeReader);
        return externalResourceAttributeReader;
    }

    public SubjectAttributeReader getSubjectAttributeReader() {
        if (!connectorService.isSubjectAttributeConnectorConfigured()) {
            return this.privilegeServiceSubjectAttributeReader;
        }

        String zoneName = SpringSecurityZoneResolver.getZoneName();
        ExternalSubjectAttributeReader externalSubjectAttributeReader = this.externalSubjectAttributeReaderCache
                .get(zoneName);
        if (externalSubjectAttributeReader != null) {
            return externalSubjectAttributeReader;
        }

        externalSubjectAttributeReader = new ExternalSubjectAttributeReader(connectorService, this.attributeCacheFactory
                .createSubjectAttributeCache(
                        this.connectorService.getSubjectAttributeConnector().getMaxCachedIntervalMinutes(), zoneName,
                        this.subjectCacheRedisTemplate), adapterTimeoutMillis);
        this.externalSubjectAttributeReaderCache.put(zoneName, externalSubjectAttributeReader);
        return externalSubjectAttributeReader;
    }

    public void removeResourceReader(final String zoneName) {
        this.externalResourceAttributeReaderCache.remove(zoneName);
    }

    public void removeSubjectReader(final String zoneName) {
        this.externalSubjectAttributeReaderCache.remove(zoneName);
    }

    @Autowired(required = false)
    public void setResourceCacheRedisTemplate(final RedisTemplate<String, String> resourceCacheRedisTemplate) {
        this.resourceCacheRedisTemplate = resourceCacheRedisTemplate;
    }

    @Autowired(required = false)
    public void setSubjectCacheRedisTemplate(final RedisTemplate<String, String> subjectCacheRedisTemplate) {
        this.subjectCacheRedisTemplate = subjectCacheRedisTemplate;
    }

    @Autowired
    public void setConnectorService(final AttributeConnectorService connectorService) {
        this.connectorService = connectorService;
    }
}
// CHECKSTYLE:ON: FinalClass
