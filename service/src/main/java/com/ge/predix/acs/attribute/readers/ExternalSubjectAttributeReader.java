package com.ge.predix.acs.attribute.readers;

import java.util.Set;

import com.ge.predix.acs.attribute.cache.AttributeCache;
import com.ge.predix.acs.model.Attribute;

public class ExternalSubjectAttributeReader extends ExternalAttributeReader implements SubjectAttributeReader {

    public ExternalSubjectAttributeReader(final AttributeCache subjectAttributeCache) {
        super(subjectAttributeCache);
    }

    @Override
    public Set<Attribute> getAttributesByScope(final String subjectId, final Set<Attribute> scopes) {
        // Connectors have no notion of scoped attributes
        return this.getAttributes(subjectId);
    }
}
