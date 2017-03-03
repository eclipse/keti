package com.ge.predix.acs.rest.attribute.adapter;

import java.util.Set;

import com.ge.predix.acs.model.Attribute;

public class AttributesResponse {

    private Set<Attribute> attributes;
    private String id;

    public AttributesResponse() {
        // Default constructor necessary for Jackson
    }

    public AttributesResponse(final Set<Attribute> attributes, final String id) {
        this.attributes = attributes;
        this.id = id;
    }

    public Set<Attribute> getAttributes() {
        return this.attributes;
    }

    public void setAttributes(final Set<Attribute> attributes) {
        this.attributes = attributes;
    }

    public String getId() {
        return this.id;
    }

    public void setId(final String id) {
        this.id = id;
    }
}
