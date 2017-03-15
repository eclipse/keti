package com.ge.predix.acs.attribute.connector.management;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.ge.predix.acs.attribute.connector.management.dao.AttributeConnectorEntity;
import com.ge.predix.acs.attribute.connector.management.dao.ConnectorConverter;
import com.ge.predix.acs.rest.AttributeAdapterConnection;
import com.ge.predix.acs.rest.AttributeConnector;
import com.ge.predix.acs.zone.management.dao.ZoneEntity;
import com.ge.predix.acs.zone.management.dao.ZoneRepository;
import com.ge.predix.acs.zone.resolver.ZoneResolver;

@Component
public class AttributeConnectorServiceImpl implements AttributeConnectorService {
    private static final int CACHED_INTERVAL_THRESHOLD_MINUTES = 30;

    @Autowired
    private ZoneRepository zoneRepository;
    @Autowired
    private ZoneResolver zoneResolver;

    private final ConnectorConverter connectorConverter = new ConnectorConverter();

    @Override
    public boolean upsertResourceConnector(final AttributeConnector connector) {
        ZoneEntity zoneEntity = this.zoneResolver.getZoneEntityOrFail();
        validateConnectorConfigOrFail(connector);

        AttributeConnectorEntity connectorEntity = zoneEntity.getResourceAttributeConnector();
        boolean isCreated = null == connectorEntity;
        
        connectorEntity = connectorConverter.toConnectorEntity(connector);
        zoneEntity.setResourceAttributeConnector(connectorEntity);
        try {
            this.zoneRepository.save(zoneEntity);
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
        return this.connectorConverter.toConnector(zoneEntity.getResourceAttributeConnector());
    }

    @Override
    public Boolean deleteResourceConnector() {
        ZoneEntity zoneEntity = this.zoneResolver.getZoneEntityOrFail();
        boolean isDeleted = false;
        if (null == zoneEntity.getResourceAttributeConnector()) {
            return isDeleted;
        }
        zoneEntity.setResourceAttributeConnector(null);
        try {
            this.zoneRepository.save(zoneEntity);
            isDeleted = true;
        } catch (Exception e) {
            String message = String.format(
                    "Unable to delete connector configuration for resource attributes for zone '%s'",
                    zoneEntity.getName());
            throw new AttributeConnectorException(message, e);
        }
        return isDeleted;

    }

    private void validateAdapterEntityOrFail(final AttributeAdapterConnection adapter) {
        if (adapter == null) {
            throw new AttributeConnectorException("Attribute connector configuration requires at least one adapter");
        }
        if (adapter.getAdapterEndpoint() == null || adapter.getAdapterEndpoint().isEmpty()) {
            throw new AttributeConnectorException("Attribute adapter configuration requires a nonempty endpoint URL");
        }
        if (adapter.getUaaTokenUrl() == null || adapter.getUaaTokenUrl().isEmpty()) {
            throw new AttributeConnectorException("Attribute adapter configuration requires a nonempty UAA token URL");
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
        if (connector.getAdapters() == null || connector.getAdapters().isEmpty()) {
            throw new AttributeConnectorException("Attribute connector configuration requires at least one adapter");
        }
        if (connector.getMaxCachedIntervalMinutes() < CACHED_INTERVAL_THRESHOLD_MINUTES) {
            throw new AttributeConnectorException(
                    "Minimum value for maxCachedIntervalMinutes is " + CACHED_INTERVAL_THRESHOLD_MINUTES);
        }
        connector.getAdapters().parallelStream().forEach(adapter -> {
            validateAdapterEntityOrFail(adapter);
        });
    }
}
