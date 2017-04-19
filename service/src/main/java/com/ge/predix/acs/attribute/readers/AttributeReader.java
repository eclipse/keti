package com.ge.predix.acs.attribute.readers;

import java.util.Set;

import com.ge.predix.acs.model.Attribute;

@FunctionalInterface
public interface AttributeReader {
    Set<Attribute> getAttributes(String identifier);
}
