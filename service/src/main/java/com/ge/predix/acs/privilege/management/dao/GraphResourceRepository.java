package com.ge.predix.acs.privilege.management.dao;

import org.apache.commons.lang.StringUtils;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.springframework.util.Assert;

import com.ge.predix.acs.zone.management.dao.ZoneEntity;

public class GraphResourceRepository extends GraphGenericRepository<ResourceEntity> implements ResourceRepository {
    private static final String ATTRIBUTES_PROPERTY_KEY = "attributes";
    private static final String EMPTY_ATTRIBUTES = "{}";
    private static final String HAS_RESOURCE_RELATIONSHIP_KEY = "hasResource";
    private static final String RESOURCE_LABEL = "resource";

    public static final String RESOURCE_ID_KEY = "resourceId";

    @Override
    public ResourceEntity getByZoneAndResourceIdentifier(final ZoneEntity zone, final String resourceIdentifier) {
        try {
            GraphTraversal<Vertex, Vertex> traversal = getGraph().traversal().V().has(ZONE_ID_KEY, zone.getName())
                    .has(RESOURCE_ID_KEY, resourceIdentifier);

            if (!traversal.hasNext()) {
                return null;
            }
            ResourceEntity resourceEntity = vertexToEntity(traversal.next());

            // There should be only one resource with a given resource id.
            Assert.isTrue(!traversal.hasNext(), "There are two resources with the same resource id.");
            return resourceEntity;
        } finally {
            getGraph().tx().commit();
        }
    }

    @Override
    String computeId(final ResourceEntity entity) {
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
    void upsertEntityVertex(final ResourceEntity entity, final Vertex vertex) {
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
