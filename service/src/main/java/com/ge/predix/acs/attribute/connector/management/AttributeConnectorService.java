package com.ge.predix.acs.attribute.connector.management;

import com.ge.predix.acs.rest.AttributeConnector;

public interface AttributeConnectorService {
    boolean upsertResourceConnector(AttributeConnector connector);

    AttributeConnector retrieveResourceConnector();

    Boolean deleteResourceConnector();
}
