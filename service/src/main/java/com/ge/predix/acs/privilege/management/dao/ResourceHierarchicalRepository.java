package com.ge.predix.acs.privilege.management.dao;

import com.ge.predix.acs.zone.management.dao.ZoneEntity;

import java.util.Set;

public interface ResourceHierarchicalRepository {

    ResourceEntity getResourceWithInheritedAttributes(ZoneEntity zone, String resourceIdentifier);

    Set<String> getResourceEntityAndDescendantsIds(ResourceEntity entity); 
}