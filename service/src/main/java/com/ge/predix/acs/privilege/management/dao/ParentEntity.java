package com.ge.predix.acs.privilege.management.dao;

import java.util.Set;

import com.ge.predix.acs.model.Attribute;

public class ParentEntity {

    private ZonableEntity entity;
    private Set<Attribute> scopes;

    ParentEntity() {
        // Default constructor.
    }

    public ParentEntity(final ZonableEntity entity) {
        this.entity = entity;
    }

    public ParentEntity(final ZonableEntity entity, final Set<Attribute> scopes) {
        this.entity = entity;
        this.scopes = scopes;
    }

    public ZonableEntity getEntity() {
        return this.entity;
    }

    public void setEntity(final ZonableEntity entity) {
        this.entity = entity;
    }

    public Set<Attribute> getScopes() {
        return this.scopes;
    }

    public void setScopes(final Set<Attribute> scopes) {
        this.scopes = scopes;
    }

}
