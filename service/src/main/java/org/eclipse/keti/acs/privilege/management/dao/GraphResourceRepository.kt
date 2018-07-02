/*******************************************************************************
 * Copyright 2017 General Electric Company
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.eclipse.keti.acs.privilege.management.dao

import org.apache.commons.lang.StringUtils
import org.apache.tinkerpop.gremlin.structure.Vertex
import org.eclipse.keti.acs.model.Attribute
import org.eclipse.keti.acs.utils.JsonUtils
import org.eclipse.keti.acs.zone.management.dao.ZoneEntity
import org.springframework.data.domain.Example
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort

private val JSON_UTILS = JsonUtils()

private const val EMPTY_ATTRIBUTES = "{}"
private const val HAS_RESOURCE_RELATIONSHIP_KEY = "hasResource"

const val RESOURCE_LABEL = "resource"
const val RESOURCE_ID_KEY = "resourceId"
private const val MESSAGE = "method not supported"

class GraphResourceRepository : GraphGenericRepository<ResourceEntity>(), ResourceRepository,
    ResourceHierarchicalRepository {

    override val entityIdKey: String
        get() = RESOURCE_ID_KEY

    override val entityLabel: String
        get() = RESOURCE_LABEL

    override val relationshipKey: String
        get() = HAS_RESOURCE_RELATIONSHIP_KEY

    override fun getByZoneAndResourceIdentifier(zone: ZoneEntity, resourceIdentifier: String): ResourceEntity? {
        return getEntity(zone, resourceIdentifier)
    }

    override fun getResourceWithInheritedAttributes(zone: ZoneEntity, resourceIdentifier: String): ResourceEntity? {
        return getEntityWithInheritedAttributes(zone, resourceIdentifier, emptySet())
    }

    override fun getEntityId(entity: ResourceEntity): String? {
        return entity.resourceIdentifier
    }

    override fun updateVertexProperties(entity: ResourceEntity, vertex: Vertex) {
        var resourceAttributesJson = entity.attributesAsJson
        if (StringUtils.isEmpty(resourceAttributesJson)) {
            resourceAttributesJson = EMPTY_ATTRIBUTES
        }
        vertex.property(ATTRIBUTES_PROPERTY_KEY, resourceAttributesJson)
    }

    @Suppress("UNCHECKED_CAST")
    override fun vertexToEntity(vertex: Vertex): ResourceEntity {
        val resourceIdentifier = getPropertyOrFail(vertex, RESOURCE_ID_KEY)
        val zoneName = getPropertyOrFail(vertex, ZONE_ID_KEY)
        val zoneEntity = ZoneEntity()
        zoneEntity.name = zoneName
        val resourceEntity = ResourceEntity(zoneEntity, resourceIdentifier)
        resourceEntity.id = vertex.id() as Long
        val attributesAsJson =
            getPropertyOrEmptyString(vertex, ATTRIBUTES_PROPERTY_KEY)
        resourceEntity.attributesAsJson = attributesAsJson
        resourceEntity.attributes =
            JSON_UTILS.deserialize(attributesAsJson, Set::class.java as Class<Set<Attribute>>, Attribute::class.java)
        val parentSet = getParents(vertex, RESOURCE_ID_KEY)
        resourceEntity.parents = parentSet
        return resourceEntity
    }

    override fun getResourceEntityAndDescendantsIds(entity: ResourceEntity?): Set<String> {
        return getEntityAndDescendantsIds(entity)
    }

    override fun <S : ResourceEntity> findAll(example: Example<S>): List<S> {
        throw UnsupportedOperationException(MESSAGE)
    }

    override fun <S : ResourceEntity> findAll(example: Example<S>, sort: Sort): List<S> {
        throw UnsupportedOperationException(MESSAGE)
    }

    override fun <S : ResourceEntity> findOne(example: Example<S>): S {
        throw UnsupportedOperationException(MESSAGE)
    }

    override fun <S : ResourceEntity> findAll(example: Example<S>, pageable: Pageable): Page<S> {
        throw UnsupportedOperationException(MESSAGE)
    }

    override fun <S : ResourceEntity> count(example: Example<S>): Long {
        throw UnsupportedOperationException(MESSAGE)
    }

    override fun <S : ResourceEntity> exists(example: Example<S>): Boolean {
        throw UnsupportedOperationException(MESSAGE)
    }
}
