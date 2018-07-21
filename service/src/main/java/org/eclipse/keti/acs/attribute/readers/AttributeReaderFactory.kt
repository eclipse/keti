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

package org.eclipse.keti.acs.attribute.readers

import org.eclipse.keti.acs.attribute.cache.AttributeCacheFactory
import org.eclipse.keti.acs.attribute.connector.management.AttributeConnectorService
import org.eclipse.keti.acs.zone.resolver.zoneName
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component
import org.springframework.util.ConcurrentReferenceHashMap

@Component
class AttributeReaderFactory {

    @Value("\${ADAPTER_TIMEOUT_MILLIS:3000}")
    private var adapterTimeoutMillis: Int = 3000

    @Autowired
    private lateinit var privilegeServiceResourceAttributeReader: PrivilegeServiceResourceAttributeReader

    @Autowired
    private lateinit var privilegeServiceSubjectAttributeReader: PrivilegeServiceSubjectAttributeReader

    @Autowired
    private lateinit var attributeCacheFactory: AttributeCacheFactory

    private lateinit var connectorService: AttributeConnectorService
    private lateinit var resourceCacheRedisTemplate: RedisTemplate<String, String>
    private lateinit var subjectCacheRedisTemplate: RedisTemplate<String, String>

    // Caches that use the multiton design pattern (keyed off the zone name)
    private val externalResourceAttributeReaderCache = ConcurrentReferenceHashMap<String, ExternalResourceAttributeReader>()
    private val externalSubjectAttributeReaderCache = ConcurrentReferenceHashMap<String, ExternalSubjectAttributeReader>()

    val resourceAttributeReader: ResourceAttributeReader?
        get() {
            if (!this.connectorService.isResourceAttributeConnectorConfigured) {
                return this.privilegeServiceResourceAttributeReader
            }

            val zoneName = zoneName
            var externalResourceAttributeReader: ExternalResourceAttributeReader? = this.externalResourceAttributeReaderCache[zoneName]
            if (externalResourceAttributeReader != null) {
                return externalResourceAttributeReader
            }

            externalResourceAttributeReader = ExternalResourceAttributeReader(
                this.connectorService,
                this.attributeCacheFactory.createResourceAttributeCache(
                    this.connectorService.resourceAttributeConnector!!.maxCachedIntervalMinutes.toLong(), zoneName,
                    this.resourceCacheRedisTemplate
                ), adapterTimeoutMillis
            )
            this.externalResourceAttributeReaderCache[zoneName] = externalResourceAttributeReader
            return externalResourceAttributeReader
        }

    val subjectAttributeReader: SubjectAttributeReader?
        get() {
            if (!connectorService.isSubjectAttributeConnectorConfigured) {
                return this.privilegeServiceSubjectAttributeReader
            }

            val zoneName = zoneName
            var externalSubjectAttributeReader: ExternalSubjectAttributeReader? = this.externalSubjectAttributeReaderCache[zoneName]
            if (externalSubjectAttributeReader != null) {
                return externalSubjectAttributeReader
            }

            externalSubjectAttributeReader = ExternalSubjectAttributeReader(
                connectorService, this.attributeCacheFactory
                    .createSubjectAttributeCache(
                        this.connectorService.subjectAttributeConnector!!.maxCachedIntervalMinutes.toLong(), zoneName,
                        this.subjectCacheRedisTemplate
                    ), adapterTimeoutMillis
            )
            this.externalSubjectAttributeReaderCache[zoneName] = externalSubjectAttributeReader
            return externalSubjectAttributeReader
        }

    fun removeResourceReader(zoneName: String?) {
        this.externalResourceAttributeReaderCache.remove(zoneName)
    }

    fun removeSubjectReader(zoneName: String?) {
        this.externalSubjectAttributeReaderCache.remove(zoneName)
    }

    @Autowired(required = false)
    fun setResourceCacheRedisTemplate(resourceCacheRedisTemplate: RedisTemplate<String, String>) {
        this.resourceCacheRedisTemplate = resourceCacheRedisTemplate
    }

    @Autowired(required = false)
    fun setSubjectCacheRedisTemplate(subjectCacheRedisTemplate: RedisTemplate<String, String>) {
        this.subjectCacheRedisTemplate = subjectCacheRedisTemplate
    }

    @Autowired
    fun setConnectorService(connectorService: AttributeConnectorService) {
        this.connectorService = connectorService
    }
}
