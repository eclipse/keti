package com.ge.predix.acs.attribute.readers;

import java.util.Set;

import com.ge.predix.acs.model.Attribute;

public class CachedAttributes {

    private State state;
    private Set<Attribute> attributes;

    public CachedAttributes() {
        // Needed for jackson serialization
    }

    public CachedAttributes(final Set<Attribute> attributes) {
        this.attributes = attributes;
        this.state = State.SUCCESS;
    }

    public CachedAttributes(final State state) {
        this.state = state;
    }

    public State getState() {
        return this.state;
    }

    public void setState(final State state) {
        this.state = state;
    }

    public Set<Attribute> getAttributes() {
        return attributes;
    }

    public void setAttributes(final Set<Attribute> attributes) {
        this.attributes = attributes;
    }

    public enum State {

        SUCCESS,
        DO_NOT_RETRY
    }
}
