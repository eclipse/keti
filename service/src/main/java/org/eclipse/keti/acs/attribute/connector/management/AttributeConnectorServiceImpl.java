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

package org.eclipse.keti.acs.attribute.connector.management;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import org.eclipse.keti.acs.attribute.readers.AttributeReaderFactory;
import org.eclipse.keti.acs.encryption.Encryptor;
import org.eclipse.keti.acs.rest.AttributeAdapterConnection;
import org.eclipse.keti.acs.rest.AttributeConnector;
import org.eclipse.keti.acs.zone.management.dao.ZoneEntity;
import org.eclipse.keti.acs.zone.management.dao.ZoneRepository;
import org.eclipse.keti.acs.zone.resolver.ZoneResolver;

@Component
public class AttributeConnectorServiceImpl implements AttributeConnectorService {

    private static final String HTTPS = "https";

    private static final int CACHED_INTERVAL_THRESHOLD_MINUTES = 30;

    @Autowired
    private ZoneRepository zoneRepository;
    @Autowired
    private ZoneResolver zoneResolver;
    @Autowired
    private AttributeReaderFactory attributeReaderFactory;

    @Value("${ENCRYPTION_KEY}")
    private String encryptionKey;

    private Encryptor encryptor;

    @PostConstruct
    public void postConstruct() {
        setEncryptionKey(this.encryptionKey);
    }

    void setEncryptionKey(final String encryptionKey) {
        this.encryptor = new Encryptor();
        this.encryptor.setEncryptionKey(encryptionKey);
    }

    @Override
    public boolean upsertResourceConnector(final AttributeConnector connector) {
        ZoneEntity zoneEntity = this.zoneResolver.getZoneEntityOrFail();
        validateConnectorConfigOrFail(connector);

        boolean isCreated = false;
        try {
            AttributeConnector existingConnector = zoneEntity.getResourceAttributeConnector();
            isCreated = null == existingConnector;
            connector.setAdapters(encryptAdapterClientSecrets(connector.getAdapters()));
            zoneEntity.setResourceAttributeConnector(connector);
            this.zoneRepository.save(zoneEntity);
            if (!isCreated) {
                this.attributeReaderFactory.removeResourceReader(zoneEntity.getName());
            }
        } catch (Exception e) {
            String message = String.format(
                    "Unable to persist connector configuration for resource attributes for zone '%s'",
                    zoneEntity.getName());
            throw new AttributeConnectorException(message, e);
        }
        return isCreated;
    }

    @Override
    public AttributeConnector retrieveResourceConnector() {
        ZoneEntity zoneEntity = this.zoneResolver.getZoneEntityOrFail();
        try {
            AttributeConnector connector = zoneEntity.getResourceAttributeConnector();
            if (null != connector) {
                // Deep copy the connector to prevent double-decryption of secrets
                connector = AttributeConnector.newInstance(connector);
                connector.setAdapters(decryptAdapterClientSecrets(connector.getAdapters()));
            }
            return connector;
        } catch (Exception e) {
            String message = String.format(
                    "Unable to retrieve connector configuration for resource attributes for zone '%s'",
                    zoneEntity.getName());
            throw new AttributeConnectorException(message, e);
        }
    }

    @Override
    public boolean deleteResourceConnector() {
        ZoneEntity zoneEntity = this.zoneResolver.getZoneEntityOrFail();
        try {
            if (null == zoneEntity.getResourceAttributeConnector()) {
                return false;
            }
            zoneEntity.setResourceAttributeConnector(null);
            this.zoneRepository.save(zoneEntity);
            this.attributeReaderFactory.removeResourceReader(zoneEntity.getName());
        } catch (Exception e) {
            String message = String.format(
                    "Unable to delete connector configuration for resource attributes for zone '%s'",
                    zoneEntity.getName());
            throw new AttributeConnectorException(message, e);
        }
        return true;
    }

    @Override
    public boolean upsertSubjectConnector(final AttributeConnector connector) {
        ZoneEntity zoneEntity = this.zoneResolver.getZoneEntityOrFail();
        validateConnectorConfigOrFail(connector);

        boolean isCreated = false;
        try {
            AttributeConnector existingConnector = zoneEntity.getSubjectAttributeConnector();
            isCreated = null == existingConnector;
            connector.setAdapters(encryptAdapterClientSecrets(connector.getAdapters()));
            zoneEntity.setSubjectAttributeConnector(connector);
            this.zoneRepository.save(zoneEntity);
            if (!isCreated) {
                this.attributeReaderFactory.removeSubjectReader(zoneEntity.getName());
            }
        } catch (Exception e) {
            String message = String.format(
                    "Unable to persist connector configuration for subject attributes for zone '%s'",
                    zoneEntity.getName());
            throw new AttributeConnectorException(message, e);
        }
        return isCreated;
    }

    @Override
    public AttributeConnector retrieveSubjectConnector() {
        ZoneEntity zoneEntity = this.zoneResolver.getZoneEntityOrFail();
        try {
            AttributeConnector connector = zoneEntity.getSubjectAttributeConnector();
            if (null != connector) {
                // Deep copy the connector to prevent double-decryption of secrets
                connector = AttributeConnector.newInstance(connector);
                connector.setAdapters(decryptAdapterClientSecrets(connector.getAdapters()));
            }
            return connector;
        } catch (Exception e) {
            String message = String.format(
                    "Unable to retrieve connector configuration for subject attributes for zone '%s'",
                    zoneEntity.getName());
            throw new AttributeConnectorException(message, e);
        }
    }

    @Override
    public boolean deleteSubjectConnector() {
        ZoneEntity zoneEntity = this.zoneResolver.getZoneEntityOrFail();
        try {
            if (null == zoneEntity.getSubjectAttributeConnector()) {
                return false;
            }
            zoneEntity.setSubjectAttributeConnector(null);
            this.zoneRepository.save(zoneEntity);
            this.attributeReaderFactory.removeSubjectReader(zoneEntity.getName());
        } catch (Exception e) {
            String message = String.format(
                    "Unable to delete connector configuration for subject attributes for zone '%s'",
                    zoneEntity.getName());
            throw new AttributeConnectorException(message, e);
        }
        return true;
    }

    private void validateAdapterEntityOrFail(final AttributeAdapterConnection adapter) {
        if (adapter == null) {
            throw new AttributeConnectorException("Attribute connector configuration requires at least one adapter");
        }
        try {
            if (!new URL(adapter.getAdapterEndpoint()).getProtocol().equalsIgnoreCase(HTTPS)) {
                throw new AttributeConnectorException("Attribute adapter endpoint must use the HTTPS protocol");
            }
        } catch (MalformedURLException e) {
            throw new AttributeConnectorException(
                    "Attribute adapter endpoint either has no protocol or is not a valid URL", e);
        }
        try {
            if (!new URL(adapter.getUaaTokenUrl()).getProtocol().equalsIgnoreCase(HTTPS)) {
                throw new AttributeConnectorException("Attribute adapter UAA token URL must use the HTTPS protocol");
            }
        } catch (MalformedURLException e) {
            throw new AttributeConnectorException(
                    "Attribute adapter UAA token URL either has no protocol or is not a valid URL", e);
        }
        if (adapter.getUaaClientId() == null || adapter.getUaaClientId().isEmpty()) {
            throw new AttributeConnectorException("Attribute adapter configuration requires a nonempty client ID");
        }
        if (adapter.getUaaClientSecret() == null || adapter.getUaaClientSecret().isEmpty()) {
            throw new AttributeConnectorException("Attribute adapter configuration requires a nonempty client secret");
        }
    }

    private void validateConnectorConfigOrFail(final AttributeConnector connector) {
        if (connector == null) {
            throw new AttributeConnectorException("Attribute connector configuration requires a valid connector");
        }
        if (connector.getAdapters() == null || connector.getAdapters().isEmpty()
                || connector.getAdapters().size() > 1) {
            throw new AttributeConnectorException("Attribute connector configuration requires one adapter");
        }
        if (connector.getMaxCachedIntervalMinutes() < CACHED_INTERVAL_THRESHOLD_MINUTES) {
            throw new AttributeConnectorException(
                    "Minimum value for maxCachedIntervalMinutes is " + CACHED_INTERVAL_THRESHOLD_MINUTES);
        }
        connector.getAdapters().parallelStream().forEach(this::validateAdapterEntityOrFail);
    }

    private Set<AttributeAdapterConnection> encryptAdapterClientSecrets(
            final Set<AttributeAdapterConnection> adapters) {
        if (CollectionUtils.isEmpty(adapters)) {
            return Collections.emptySet();
        }
        adapters.forEach(adapter -> adapter.setUaaClientSecret(this.encryptor.encrypt(adapter.getUaaClientSecret())));
        return adapters;
    }

    private Set<AttributeAdapterConnection> decryptAdapterClientSecrets(
            final Set<AttributeAdapterConnection> adapters) {
        if (CollectionUtils.isEmpty(adapters)) {
            return Collections.emptySet();
        }
        adapters.forEach(adapter -> adapter.setUaaClientSecret(this.encryptor.decrypt(adapter.getUaaClientSecret())));
        return adapters;
    }

    @Override
    public AttributeConnector getResourceAttributeConnector() {
        return retrieveResourceConnector();
    }

    @Override
    public AttributeConnector getSubjectAttributeConnector() {
        return retrieveSubjectConnector();
    }

    @Override
    public boolean isResourceAttributeConnectorConfigured() {
        return this.getResourceAttributeConnector() != null && this.getResourceAttributeConnector().getIsActive();
    }

    @Override
    public boolean isSubjectAttributeConnectorConfigured() {
        return this.getSubjectAttributeConnector() != null && this.getSubjectAttributeConnector().getIsActive();
    }

}
