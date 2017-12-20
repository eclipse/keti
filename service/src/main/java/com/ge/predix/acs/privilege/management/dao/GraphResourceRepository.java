/*******************************************************************************
 * Copyright 2017 General Electric Company
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 *******************************************************************************/

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

public class GraphResourceRepository extends GraphGenericRepository<ResourceEntity>
        implements ResourceRepository, ResourceHierarchicalRepository {

    private static final JsonUtils JSON_UTILS = new JsonUtils();

    private static final String EMPTY_ATTRIBUTES = "{}";
    private static final String HAS_RESOURCE_RELATIONSHIP_KEY = "hasResource";

    public static final String RESOURCE_LABEL = "resource";
    public static final String RESOURCE_ID_KEY = "resourceId";
    private static final String MESSAGE = "method not supported";

    @Override
    public ResourceEntity getByZoneAndResourceIdentifier(final ZoneEntity zone, final String resourceIdentifier) {
        return getEntity(zone, resourceIdentifier);
    }

    @Override
    public ResourceEntity getResourceWithInheritedAttributes(final ZoneEntity zone, final String resourceIdentifier) {
        return getEntityWithInheritedAttributes(zone, resourceIdentifier, Collections.emptySet());
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

    @SuppressWarnings("unchecked")
    @Override
    ResourceEntity vertexToEntity(final Vertex vertex) {
        String resourceIdentifier = getPropertyOrFail(vertex, RESOURCE_ID_KEY);
        String zoneName = getPropertyOrFail(vertex, ZONE_ID_KEY);
        ZoneEntity zoneEntity = new ZoneEntity();
        zoneEntity.setName(zoneName);
        ResourceEntity resourceEntity = new ResourceEntity(zoneEntity, resourceIdentifier);
        resourceEntity.setId((Long) vertex.id());
        String attributesAsJson = getPropertyOrEmptyString(vertex, ATTRIBUTES_PROPERTY_KEY);
        resourceEntity.setAttributesAsJson(attributesAsJson);
        resourceEntity.setAttributes(JSON_UTILS.deserialize(attributesAsJson, Set.class, Attribute.class));
        Set<Parent> parentSet = getParents(vertex, RESOURCE_ID_KEY);
        resourceEntity.setParents(parentSet);
        return resourceEntity;
    }

    @Override
    public Set<String> getResourceEntityAndDescendantsIds(final ResourceEntity entity) {
        return getEntityAndDescendantsIds(entity);
    }

    @Override
    public <S extends ResourceEntity> List<S> findAll(final Example<S> example) {
        throw new UnsupportedOperationException(MESSAGE);
    }

    @Override
    public <S extends ResourceEntity> List<S> findAll(final Example<S> example, final Sort sort) {
        throw new UnsupportedOperationException(MESSAGE);
    }

    @Override
    public <S extends ResourceEntity> S findOne(final Example<S> example) {
        throw new UnsupportedOperationException(MESSAGE);
    }

    @Override
    public <S extends ResourceEntity> Page<S> findAll(final Example<S> example, final Pageable pageable) {
        throw new UnsupportedOperationException(MESSAGE);
    }

    @Override
    public <S extends ResourceEntity> long count(final Example<S> example) {
        throw new UnsupportedOperationException(MESSAGE);
    }

    @Override
    public <S extends ResourceEntity> boolean exists(final Example<S> example) {
        throw new UnsupportedOperationException(MESSAGE);
    }
}
