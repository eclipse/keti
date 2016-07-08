package com.ge.predix.acs.privilege.management.dao;

import com.ge.predix.acs.zone.management.dao.ZoneEntity;

public interface ResourceHierarchicalRepository {

    ResourceEntity getByZoneAndResourceIdentifierWithInheritedAttributes(final ZoneEntity zone,
            final String resourceIdentifier);

}
