package com.ge.predix.acs.privilege.management.dao;

import org.apache.commons.lang.StringUtils;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.springframework.util.Assert;

import com.ge.predix.acs.zone.management.dao.ZoneEntity;

public class GraphSubjectRepository extends GraphGenericRepository<SubjectEntity> implements SubjectRepository {
    private static final String ATTRIBUTES_PROPERTY_KEY = "attributes";
    private static final String EMPTY_ATTRIBUTES = "{}";
    private static final String HAS_SUBJECT_RELATIONSHIP_KEY = "hasSubject";
    private static final String SUBJECT_LABEL = "subject";

    public static final String SUBJECT_ID_KEY = "subjectId";

    @Override
    public SubjectEntity getByZoneAndSubjectIdentifier(final ZoneEntity zone, final String subjectIdentifier) {
        try {
            GraphTraversal<Vertex, Vertex> traversal = getGraph().traversal().V().has(ZONE_ID_KEY, zone.getName())
                    .has(SUBJECT_ID_KEY, subjectIdentifier);
            if (!traversal.hasNext()) {
                return null;
            }
            SubjectEntity subjectEntity = vertexToEntity(traversal.next());

            // There should be only one subject with a given subject id.
            Assert.isTrue(!traversal.hasNext(), "There are two subjects with the same subject id.");
            return subjectEntity;
        } finally {
            getGraph().tx().commit();
        }
    }

    @Override
    String computeId(final SubjectEntity entity) {
        return entity.getSubjectIdentifier();
    }

    @Override
    String getEntityIdKey() {
        return SUBJECT_ID_KEY;
    }

    @Override
    String getEntityLabel() {
        return SUBJECT_LABEL;
    }

    @Override
    String getRelationshipKey() {
        return HAS_SUBJECT_RELATIONSHIP_KEY;
    }

    @Override
    void upsertEntityVertex(final SubjectEntity entity, final Vertex vertex) {
        String subjectAttributesJson = entity.getAttributesAsJson();
        if (StringUtils.isEmpty(subjectAttributesJson)) {
            subjectAttributesJson = EMPTY_ATTRIBUTES;
        }
        vertex.property(ATTRIBUTES_PROPERTY_KEY, subjectAttributesJson);
    }

    @Override
    SubjectEntity vertexToEntity(final Vertex vertex) {
        String subjectIdentifier = getVertexStringPropertyOrFail(vertex, SUBJECT_ID_KEY);
        String zoneName = getVertexStringPropertyOrFail(vertex, ZONE_ID_KEY);
        ZoneEntity zoneEntity = new ZoneEntity();
        zoneEntity.setName(zoneName);
        SubjectEntity subjectEntity = new SubjectEntity(zoneEntity, subjectIdentifier);
        subjectEntity.setId((long) vertex.id());
        subjectEntity.setAttributesAsJson(getVertexStringPropertyOrEmpty(vertex, ATTRIBUTES_PROPERTY_KEY));
        return subjectEntity;
    }
}
