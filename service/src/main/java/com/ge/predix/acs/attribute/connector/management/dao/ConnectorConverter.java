package com.ge.predix.acs.attribute.connector.management.dao;

import java.util.HashSet;
import java.util.Set;

import com.ge.predix.acs.rest.AttributeAdapterConnection;
import com.ge.predix.acs.rest.AttributeConnector;

/**
 *
 * @author 212570782
 */
public class ConnectorConverter {

    public AttributeConnector toConnector(final AttributeConnectorEntity connectorEntity) {
        if (connectorEntity == null) {
            return null;
        }
        AttributeConnector connector = new AttributeConnector();
        connector.setMaxCachedIntervalMinutes(connectorEntity.getCachedIntervalMinutes());
        connector.setIsActive(connectorEntity.isActive());
        Set<AttributeAdapterConnection> adapters = new HashSet<>();
        connectorEntity.getAttributeAdapterConnections().parallelStream().forEach(adapterEntity -> {
            adapters.add(this.toAdapter(adapterEntity));
        });
        connector.setAdapters(adapters);
        return connector;
    }

    public AttributeConnectorEntity toConnectorEntity(final AttributeConnector connector) {
        if (connector == null) {
            return null;
        }
        AttributeConnectorEntity connectorEntity = new AttributeConnectorEntity();
        connectorEntity.setCachedIntervalMinutes(connector.getMaxCachedIntervalMinutes());
        connectorEntity.setActive(connector.getIsActive());
        Set<AttributeAdapterConnectionEntity> adapterEntities = new HashSet<>();
        connector.getAdapters().parallelStream().forEach(adapter -> {
            adapterEntities.add(this.toAdapterEntity(connectorEntity, adapter));
        });
        connectorEntity.setAttributeAdapterConnections(adapterEntities);
        return connectorEntity;
    }

    private AttributeAdapterConnectionEntity toAdapterEntity(final AttributeConnectorEntity connectorEntity,
            final AttributeAdapterConnection adapter) {
        if (adapter == null) {
            return null;
        }
        return new AttributeAdapterConnectionEntity(connectorEntity, adapter.getAdapterEndpoint(),
                adapter.getUaaTokenUrl(), adapter.getUaaClientId(), adapter.getUaaClientSecret());
    }

    public AttributeAdapterConnection toAdapter(final AttributeAdapterConnectionEntity adapterEntity) {
        if (adapterEntity == null) {
            return null;
        }
        return new AttributeAdapterConnection(adapterEntity.getAdapterEndpoint(), adapterEntity.getUaaTokenUrl(),
                adapterEntity.getUaaClientId(), adapterEntity.getUaaClientSecret());
    }
}
