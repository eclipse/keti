package com.ge.predix.acs.attribute.connectors;

import java.util.Collections;
import java.util.Set;

import com.ge.predix.acs.attribute.readers.ResourceAttributeReader;
import com.ge.predix.acs.model.Attribute;

public class ResourceAttributeConnector implements ResourceAttributeReader {
    @Override
    public Set<Attribute> getAttributes(final String identifier) {
        return Collections.emptySet();
    }
}
