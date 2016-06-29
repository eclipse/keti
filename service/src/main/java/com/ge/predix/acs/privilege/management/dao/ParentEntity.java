package com.ge.predix.acs.privilege.management.dao;

import java.util.Set;

import com.ge.predix.acs.model.Attribute;

public class ParentEntity {

    private ZonableEntity childEntity;
    private Set<Attribute> scopes;

    ParentEntity() {
        // Default constructor.
    }

    public ParentEntity(final ZonableEntity entity) {
        this.childEntity = entity;
    }

    public ParentEntity(final ZonableEntity entity, final Set<Attribute> scopes) {
        this.childEntity = entity;
        this.scopes = scopes;
    }

    public ZonableEntity getChildEntity() {
        return this.childEntity;
    }

    public void setChildEntity(final ZonableEntity entity) {
        this.childEntity = entity;
    }

    public Set<Attribute> getScopes() {
        return this.scopes;
    }

    public void setScopes(final Set<Attribute> scopes) {
        this.scopes = scopes;
    }

}
