package com.ge.predix.acs.privilege.management.dao;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import com.ge.predix.acs.model.Attribute;
import com.ge.predix.acs.rest.Parent;
import com.ge.predix.acs.utils.JsonUtils;
import com.ge.predix.acs.zone.management.dao.ZoneEntity;

public class GraphSubjectRepository extends GraphGenericRepository<SubjectEntity>
        implements SubjectRepository, SubjectHierarchicalRepository {
    private static final JsonUtils JSON_UTILS = new JsonUtils();

    private static final String EMPTY_ATTRIBUTES = "{}";
    private static final String HAS_SUBJECT_RELATIONSHIP_KEY = "hasSubject";

    public static final String SUBJECT_LABEL = "subject";
    public static final String SUBJECT_ID_KEY = "subjectId";
    private static final String MESSAGE = "method not supported";

    @Override
    public SubjectEntity getByZoneAndSubjectIdentifier(final ZoneEntity zone, final String subjectIdentifier) {
        return getEntity(zone, subjectIdentifier);
    }

    @Override
    public SubjectEntity getSubjectWithInheritedAttributes(final ZoneEntity zone, final String subjectIdentifier) {
        return getEntityWithInheritedAttributes(zone, subjectIdentifier, Collections.emptySet());
    }

    @Override
    public SubjectEntity getSubjectWithInheritedAttributesForScopes(final ZoneEntity zone,
            final String subjectIdentifier, final Set<Attribute> scopes) {
        return getEntityWithInheritedAttributes(zone, subjectIdentifier, scopes);
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

    @SuppressWarnings("unchecked")
    @Override
    SubjectEntity vertexToEntity(final Vertex vertex) {
        String subjectIdentifier = getPropertyOrFail(vertex, SUBJECT_ID_KEY);
        String zoneName = getPropertyOrFail(vertex, ZONE_ID_KEY);
        ZoneEntity zoneEntity = new ZoneEntity();
        zoneEntity.setName(zoneName);
        SubjectEntity subjectEntity = new SubjectEntity(zoneEntity, subjectIdentifier);
        subjectEntity.setId((long) vertex.id());
        String attributesAsJson = getPropertyOrEmptyString(vertex, ATTRIBUTES_PROPERTY_KEY);
        subjectEntity.setAttributesAsJson(attributesAsJson);
        subjectEntity.setAttributes(JSON_UTILS.deserialize(attributesAsJson, Set.class, Attribute.class));
        Set<Parent> parentSet = getParents(vertex, SUBJECT_ID_KEY);
        subjectEntity.setParents(parentSet);
        return subjectEntity;
    }

    @Override
    public Set<String> getSubjectEntityAndDescendantsIds(final SubjectEntity entity) {
        return getEntityAndDescendantsIds(entity);
    }

    @Override
    public <S extends SubjectEntity> List<S> findAll(final Example<S> example) {
        throw new UnsupportedOperationException(MESSAGE);
    }

    @Override
    public <S extends SubjectEntity> List<S> findAll(final Example<S> example, final Sort sort) {
        throw new UnsupportedOperationException(MESSAGE);
    }

    @Override
    public <S extends SubjectEntity> S findOne(final Example<S> example) {
        throw new UnsupportedOperationException(MESSAGE);
    }

    @Override
    public <S extends SubjectEntity> Page<S> findAll(final Example<S> example, final Pageable pageable) {
        throw new UnsupportedOperationException(MESSAGE);
    }

    @Override
    public <S extends SubjectEntity> long count(final Example<S> example) {
        throw new UnsupportedOperationException(MESSAGE);
    }

    @Override
    public <S extends SubjectEntity> boolean exists(final Example<S> example) {
        throw new UnsupportedOperationException(MESSAGE);
    }
}
