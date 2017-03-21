package com.ge.predix.acs.attribute.readers;

import java.util.Set;

import com.ge.predix.acs.attribute.cache.AttributeCache;
import com.ge.predix.acs.attribute.connector.management.AttributeConnectorService;
import com.ge.predix.acs.model.Attribute;
import com.ge.predix.acs.rest.AttributeAdapterConnection;

public class ExternalSubjectAttributeReader extends ExternalAttributeReader implements SubjectAttributeReader {

    public ExternalSubjectAttributeReader(final AttributeConnectorService connectorService,
            final AttributeCache subjectAttributeCache, final int adapterTimeoutMillis) {
        super(connectorService, subjectAttributeCache, adapterTimeoutMillis);
    }

    @Override
    Set<AttributeAdapterConnection> getAttributeAdapterConnections() {
        return this.getConnectorService().getSubjectAttributeConnector().getAdapters();
    }

    @Override
    public Set<Attribute> getAttributesByScope(final String subjectId, final Set<Attribute> scopes) {
        // Connectors have no notion of scoped attributes
        return this.getAttributes(subjectId);
    }
}
