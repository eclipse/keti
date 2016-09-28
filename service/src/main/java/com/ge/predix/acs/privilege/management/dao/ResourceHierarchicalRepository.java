package com.ge.predix.acs.privilege.management.dao;

import java.util.Set;

import com.ge.predix.acs.zone.management.dao.ZoneEntity;

public interface ResourceHierarchicalRepository {

    ResourceEntity getResourceWithInheritedAttributes(final ZoneEntity zone, final String resourceIdentifier);

    Set<String> getResourceEntityAndDescendantsIds(final ResourceEntity entity); 
}