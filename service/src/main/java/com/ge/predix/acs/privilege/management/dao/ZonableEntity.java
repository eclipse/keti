package com.ge.predix.acs.privilege.management.dao;

import com.ge.predix.acs.model.Attribute;
import com.ge.predix.acs.rest.Parent;
import com.ge.predix.acs.zone.management.dao.ZoneEntity;

import java.util.Set;

public interface ZonableEntity {
    Long getId();

    String getEntityId();

    String getEntityType();

    void setId(Long id);

    ZoneEntity getZone();

    void setZone(ZoneEntity zone);

    Set<Attribute> getAttributes();

    void setAttributes(Set<Attribute> attributes);

    String getAttributesAsJson();

    void setAttributesAsJson(String attributesAsJson);

    Set<Parent> getParents();

    void setParents(Set<Parent> parentIdentifiers);
}
