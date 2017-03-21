package com.ge.predix.acs.attribute.connector.management;

import com.ge.predix.acs.rest.AttributeConnector;

public interface AttributeConnectorService {

    boolean upsertResourceConnector(AttributeConnector connector);

    AttributeConnector retrieveResourceConnector();

    boolean deleteResourceConnector();

    AttributeConnector getResourceAttributeConnector();

    boolean isResourceAttributeConnectorConfigured();

    boolean upsertSubjectConnector(AttributeConnector connector);

    AttributeConnector retrieveSubjectConnector();

    boolean deleteSubjectConnector();

    AttributeConnector getSubjectAttributeConnector();

    boolean isSubjectAttributeConnectorConfigured();
}
