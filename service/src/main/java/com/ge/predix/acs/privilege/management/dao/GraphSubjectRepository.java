package com.ge.predix.acs.privilege.management.dao;

import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.tinkerpop.gremlin.structure.Vertex;

import com.ge.predix.acs.model.Attribute;
import com.ge.predix.acs.zone.management.dao.ZoneEntity;

public class GraphSubjectRepository extends GraphGenericRepository<SubjectEntity>
        implements SubjectRepository, SubjectScopedAccessRepository {
    private static final String EMPTY_ATTRIBUTES = "{}";
    private static final String HAS_SUBJECT_RELATIONSHIP_KEY = "hasSubject";
    private static final String SUBJECT_LABEL = "subject";

    public static final String SUBJECT_ID_KEY = "subjectId";

    @Override
    public SubjectEntity getByZoneAndSubjectIdentifier(final ZoneEntity zone, final String subjectIdentifier) {
        return getByZoneAndIdentifier(zone, subjectIdentifier);
    }

    @Override
    public SubjectEntity getByZoneAndSubjectIdentifierAndScopes(final ZoneEntity zone, final String subjectIdentifier,
            final Set<Attribute> scopes) {
        return getByZoneAndIdentifierAndScopes(zone, subjectIdentifier, scopes);
    }

    @Override
    String getEntityId(final SubjectEntity entity) {
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
    void updateVertexProperties(final SubjectEntity entity, final Vertex vertex) {
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
