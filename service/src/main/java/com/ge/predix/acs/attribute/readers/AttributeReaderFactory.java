package com.ge.predix.acs.attribute.readers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AttributeReaderFactory {
    @Autowired
    private ResourceAttributeReader resourceAttributeReader;
    @Autowired
    private SubjectAttributeReader subjectAttributeReader;

    public ResourceAttributeReader getResourceAttributeReader() {
        return this.resourceAttributeReader;
    }

    public SubjectAttributeReader getSubjectAttributeReader() {
        return this.subjectAttributeReader;
    }
}
