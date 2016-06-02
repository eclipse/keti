package com.ge.predix.acs.privilege.management.dao;

import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.tinkerpop.gremlin.structure.Vertex;

import com.ge.predix.acs.model.Attribute;
import com.ge.predix.acs.zone.management.dao.ZoneEntity;

public class GraphResourceRepository extends GraphGenericRepository<ResourceEntity> implements ResourceRepository {
    private static final String EMPTY_ATTRIBUTES = "{}";
    private static final String HAS_RESOURCE_RELATIONSHIP_KEY = "hasResource";
    private static final String RESOURCE_LABEL = "resource";

    public static final String RESOURCE_ID_KEY = "resourceId";

    @Override
    public ResourceEntity getByZoneAndResourceIdentifier(final ZoneEntity zone, final String resourceIdentifier) {
        return getByZoneAndIdentifier(zone, resourceIdentifier);
    }

    public ResourceEntity getByZoneAndResourceIdentifierAndScopes(final ZoneEntity zone,
            final String resourceIdentifier, final Set<Attribute> scopes) {
        return getByZoneAndIdentifierAndScopes(zone, resourceIdentifier, scopes);
    }

    @Override
    String getEntityId(final ResourceEntity entity) {
        return entity.getResourceIdentifier();
    }

    @Override
    String getEntityIdKey() {
        return RESOURCE_ID_KEY;
    }

    @Override
    String getEntityLabel() {
        return RESOURCE_LABEL;
    }

    @Override
    String getRelationshipKey() {
        return HAS_RESOURCE_RELATIONSHIP_KEY;
    }

    @Override
    void updateVertexProperties(final ResourceEntity entity, final Vertex vertex) {
        String resourceAttributesJson = entity.getAttributesAsJson();
        if (StringUtils.isEmpty(resourceAttributesJson)) {
            resourceAttributesJson = EMPTY_ATTRIBUTES;
        }
        vertex.property(ATTRIBUTES_PROPERTY_KEY, resourceAttributesJson);
    }

    @Override
    ResourceEntity vertexToEntity(final Vertex vertex) {
        String resourceIdentifier = getVertexStringPropertyOrFail(vertex, RESOURCE_ID_KEY);
        String zoneName = getVertexStringPropertyOrFail(vertex, ZONE_ID_KEY);
        ZoneEntity zoneEntity = new ZoneEntity();
        zoneEntity.setName(zoneName);
        ResourceEntity resourceEntity = new ResourceEntity(zoneEntity, resourceIdentifier);
        resourceEntity.setId((long) vertex.id());
        resourceEntity.setAttributesAsJson(getVertexStringPropertyOrEmpty(vertex, ATTRIBUTES_PROPERTY_KEY));
        return resourceEntity;
    }
}
