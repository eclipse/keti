package com.ge.predix.acs.attribute.connectors;

import java.util.Set;

import com.ge.predix.acs.model.Attribute;

public interface AttributeReader {
    Set<Attribute> getAttributes(String identifier);
}
