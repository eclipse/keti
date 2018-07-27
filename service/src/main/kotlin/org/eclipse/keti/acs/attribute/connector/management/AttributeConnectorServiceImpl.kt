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

package org.eclipse.keti.acs.attribute.connector.management

import org.apache.commons.collections4.CollectionUtils
import org.eclipse.keti.acs.attribute.readers.AttributeReaderFactory
import org.eclipse.keti.acs.encryption.Encryptor
import org.eclipse.keti.acs.rest.AttributeAdapterConnection
import org.eclipse.keti.acs.rest.AttributeConnector
import org.eclipse.keti.acs.rest.newInstance
import org.eclipse.keti.acs.zone.management.dao.ZoneRepository
import org.eclipse.keti.acs.zone.resolver.ZoneResolver
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.net.MalformedURLException
import java.net.URL
import javax.annotation.PostConstruct

private const val HTTPS = "https"

private const val CACHED_INTERVAL_THRESHOLD_MINUTES = 30

@Component
class AttributeConnectorServiceImpl : AttributeConnectorService {

    @Autowired
    private lateinit var zoneRepository: ZoneRepository
    @Autowired
    private lateinit var zoneResolver: ZoneResolver
    @Autowired
    private lateinit var attributeReaderFactory: AttributeReaderFactory

    @Value("\${ENCRYPTION_KEY}")
    private lateinit var encryptionKey: String

    private var encryptor: Encryptor? = null

    override val resourceAttributeConnector: AttributeConnector?
        get() = retrieveResourceConnector()

    override val subjectAttributeConnector: AttributeConnector?
        get() = retrieveSubjectConnector()

    override val isResourceAttributeConnectorConfigured: Boolean
        get() = this.resourceAttributeConnector != null && this.resourceAttributeConnector!!.isActive

    override val isSubjectAttributeConnectorConfigured: Boolean
        get() = this.subjectAttributeConnector != null && this.subjectAttributeConnector!!.isActive

    @PostConstruct
    fun postConstruct() {
        setEncryptionKey(this.encryptionKey)
    }

    fun setEncryptionKey(encryptionKey: String) {
        this.encryptor = Encryptor()
        this.encryptor!!.setEncryptionKey(encryptionKey)
    }

    override fun upsertResourceConnector(connector: AttributeConnector?): Boolean {
        val zoneEntity = this.zoneResolver.zoneEntityOrFail
        validateConnectorConfigOrFail(connector)

        val isCreated: Boolean
        try {
            val existingConnector = zoneEntity.resourceAttributeConnector
            isCreated = null == existingConnector
            connector!!.adapters = encryptAdapterClientSecrets(connector.adapters!!)
            zoneEntity.resourceAttributeConnector = connector
            this.zoneRepository.save(zoneEntity)
            if (!isCreated) {
                this.attributeReaderFactory.removeResourceReader(zoneEntity.name)
            }
        } catch (e: Exception) {
            val message = String.format(
                "Unable to persist connector configuration for resource attributes for zone '%s'",
                zoneEntity.name
            )
            throw AttributeConnectorException(message, e)
        }

        return isCreated
    }

    override fun retrieveResourceConnector(): AttributeConnector? {
        val zoneEntity = this.zoneResolver.zoneEntityOrFail
        try {
            var connector = zoneEntity.resourceAttributeConnector
            if (null != connector) {
                // Deep copy the connector to prevent double-decryption of secrets
                connector = newInstance(connector)
                connector.adapters = decryptAdapterClientSecrets(connector.adapters!!)
            }
            return connector
        } catch (e: Exception) {
            val message = String.format(
                "Unable to retrieve connector configuration for resource attributes for zone '%s'",
                zoneEntity.name
            )
            throw AttributeConnectorException(message, e)
        }
    }

    override fun deleteResourceConnector(): Boolean {
        val zoneEntity = this.zoneResolver.zoneEntityOrFail
        try {
            if (null == zoneEntity.resourceAttributeConnector) {
                return false
            }
            zoneEntity.resourceAttributeConnector = null
            this.zoneRepository.save(zoneEntity)
            this.attributeReaderFactory.removeResourceReader(zoneEntity.name)
        } catch (e: Exception) {
            val message = String.format(
                "Unable to delete connector configuration for resource attributes for zone '%s'",
                zoneEntity.name
            )
            throw AttributeConnectorException(message, e)
        }

        return true
    }

    override fun upsertSubjectConnector(connector: AttributeConnector?): Boolean {
        val zoneEntity = this.zoneResolver.zoneEntityOrFail
        validateConnectorConfigOrFail(connector)

        val isCreated: Boolean
        try {
            val existingConnector = zoneEntity.subjectAttributeConnector
            isCreated = null == existingConnector
            connector!!.adapters = encryptAdapterClientSecrets(connector.adapters!!)
            zoneEntity.subjectAttributeConnector = connector
            this.zoneRepository.save(zoneEntity)
            if (!isCreated) {
                this.attributeReaderFactory.removeSubjectReader(zoneEntity.name)
            }
        } catch (e: Exception) {
            val message = String.format(
                "Unable to persist connector configuration for subject attributes for zone '%s'",
                zoneEntity.name
            )
            throw AttributeConnectorException(message, e)
        }

        return isCreated
    }

    override fun retrieveSubjectConnector(): AttributeConnector? {
        val zoneEntity = this.zoneResolver.zoneEntityOrFail
        try {
            var connector = zoneEntity.subjectAttributeConnector
            if (null != connector) {
                // Deep copy the connector to prevent double-decryption of secrets
                connector = newInstance(connector)
                connector.adapters = decryptAdapterClientSecrets(connector.adapters!!)
            }
            return connector
        } catch (e: Exception) {
            val message = String.format(
                "Unable to retrieve connector configuration for subject attributes for zone '%s'",
                zoneEntity.name
            )
            throw AttributeConnectorException(message, e)
        }
    }

    override fun deleteSubjectConnector(): Boolean {
        val zoneEntity = this.zoneResolver.zoneEntityOrFail
        try {
            if (null == zoneEntity.subjectAttributeConnector) {
                return false
            }
            zoneEntity.subjectAttributeConnector = null
            this.zoneRepository.save(zoneEntity)
            this.attributeReaderFactory.removeSubjectReader(zoneEntity.name)
        } catch (e: Exception) {
            val message = String.format(
                "Unable to delete connector configuration for subject attributes for zone '%s'",
                zoneEntity.name
            )
            throw AttributeConnectorException(message, e)
        }

        return true
    }

    private fun validateAdapterEntityOrFail(adapter: AttributeAdapterConnection?) {
        if (adapter == null) {
            throw AttributeConnectorException("Attribute connector configuration requires at least one adapter")
        }
        try {
            if (!URL(adapter.adapterEndpoint).protocol.equals(HTTPS, ignoreCase = true)) {
                throw AttributeConnectorException("Attribute adapter endpoint must use the HTTPS protocol")
            }
        } catch (e: MalformedURLException) {
            throw AttributeConnectorException(
                "Attribute adapter endpoint either has no protocol or is not a valid URL", e
            )
        }

        try {
            if (!URL(adapter.uaaTokenUrl).protocol.equals(HTTPS, ignoreCase = true)) {
                throw AttributeConnectorException("Attribute adapter UAA token URL must use the HTTPS protocol")
            }
        } catch (e: MalformedURLException) {
            throw AttributeConnectorException(
                "Attribute adapter UAA token URL either has no protocol or is not a valid URL", e
            )
        }

        if (adapter.uaaClientId == null || adapter.uaaClientId!!.isEmpty()) {
            throw AttributeConnectorException("Attribute adapter configuration requires a nonempty client ID")
        }
        if (adapter.uaaClientSecret == null || adapter.uaaClientSecret!!.isEmpty()) {
            throw AttributeConnectorException("Attribute adapter configuration requires a nonempty client secret")
        }
    }

    private fun validateConnectorConfigOrFail(connector: AttributeConnector?) {
        if (connector == null) {
            throw AttributeConnectorException("Attribute connector configuration requires a valid connector")
        }
        if (connector.adapters == null || connector.adapters!!.isEmpty()
            || connector.adapters!!.size > 1
        ) {
            throw AttributeConnectorException("Attribute connector configuration requires one adapter")
        }
        if (connector.maxCachedIntervalMinutes < CACHED_INTERVAL_THRESHOLD_MINUTES) {
            throw AttributeConnectorException(
                "Minimum value for maxCachedIntervalMinutes is $CACHED_INTERVAL_THRESHOLD_MINUTES"
            )
        }
        connector.adapters!!.parallelStream()
            .forEach { this.validateAdapterEntityOrFail(it) }
    }

    private fun encryptAdapterClientSecrets(
        adapters: Set<AttributeAdapterConnection>
    ): Set<AttributeAdapterConnection> {
        if (CollectionUtils.isEmpty(adapters)) {
            return emptySet()
        }
        adapters.forEach { adapter -> adapter.uaaClientSecret = this.encryptor!!.encrypt(adapter.uaaClientSecret!!) }
        return adapters
    }

    private fun decryptAdapterClientSecrets(
        adapters: Set<AttributeAdapterConnection>
    ): Set<AttributeAdapterConnection> {
        if (CollectionUtils.isEmpty(adapters)) {
            return emptySet()
        }
        adapters.forEach { adapter -> adapter.uaaClientSecret = this.encryptor!!.decrypt(adapter.uaaClientSecret!!) }
        return adapters
    }
}
