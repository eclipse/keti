package com.ge.predix.acs.attribute.connectors;

import java.util.Collections;
import java.util.Set;

import com.ge.predix.acs.attribute.readers.SubjectAttributeReader;
import com.ge.predix.acs.model.Attribute;

public class SubjectAttributeConnector implements SubjectAttributeReader {
    @Override
    public Set<Attribute> getAttributes(final String identifier) {
        return Collections.emptySet();
    }

    @Override
    public Set<Attribute> getAttributesByScope(final String identifier, final Set<Attribute> scopes) {
        return Collections.emptySet();
    }
}
