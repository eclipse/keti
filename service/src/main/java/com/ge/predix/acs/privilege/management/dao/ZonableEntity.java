package com.ge.predix.acs.privilege.management.dao;

import java.util.Set;

import com.ge.predix.acs.model.Attribute;
import com.ge.predix.acs.rest.Parent;
import com.ge.predix.acs.zone.management.dao.ZoneEntity;

public interface ZonableEntity {
    Long getId();

    void setId(Long id);

    ZoneEntity getZone();

    void setZone(ZoneEntity zone);

    Set<Attribute> getAttributes();

    void setAttributes(final Set<Attribute> attributes);

    String getAttributesAsJson();

    void setAttributesAsJson(final String attributesAsJson);

    Set<Parent> getParents();

    void setParents(final Set<Parent> parentIdentifiers);
}
