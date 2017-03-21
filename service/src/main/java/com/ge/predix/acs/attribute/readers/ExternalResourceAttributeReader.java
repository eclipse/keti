package com.ge.predix.acs.attribute.readers;

import java.util.Set;

import com.ge.predix.acs.attribute.cache.AttributeCache;
import com.ge.predix.acs.attribute.connector.management.AttributeConnectorService;
import com.ge.predix.acs.rest.AttributeAdapterConnection;

public class ExternalResourceAttributeReader extends ExternalAttributeReader implements ResourceAttributeReader {

    public ExternalResourceAttributeReader(final AttributeConnectorService connectorService,
            final AttributeCache resourceAttributeCache, final int adapterTimeoutMillis) {
        super(connectorService, resourceAttributeCache, adapterTimeoutMillis);
    }

    @Override
    Set<AttributeAdapterConnection> getAttributeAdapterConnections() {
        return this.getConnectorService().getResourceAttributeConnector().getAdapters();
    }
}
