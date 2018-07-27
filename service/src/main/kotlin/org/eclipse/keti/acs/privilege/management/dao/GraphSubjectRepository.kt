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
private const val HAS_SUBJECT_RELATIONSHIP_KEY = "hasSubject"

const val SUBJECT_LABEL = "subject"
const val SUBJECT_ID_KEY = "subjectId"
private const val MESSAGE = "method not supported"

class GraphSubjectRepository : GraphGenericRepository<SubjectEntity>(), SubjectRepository,
    SubjectHierarchicalRepository {

    override val entityIdKey: String
        get() = SUBJECT_ID_KEY

    override val entityLabel: String
        get() = SUBJECT_LABEL

    override val relationshipKey: String
        get() = HAS_SUBJECT_RELATIONSHIP_KEY

    override fun getByZoneAndSubjectIdentifier(
        zone: ZoneEntity,
        subjectIdentifier: String
    ): SubjectEntity? {
        return getEntity(zone, subjectIdentifier)
    }

    override fun getSubjectWithInheritedAttributes(
        zone: ZoneEntity,
        subjectIdentifier: String
    ): SubjectEntity? {
        return getEntityWithInheritedAttributes(zone, subjectIdentifier, emptySet())
    }

    override fun getSubjectWithInheritedAttributesForScopes(
        zone: ZoneEntity,
        subjectIdentifier: String,
        scopes: Set<Attribute>?
    ): SubjectEntity? {
        return getEntityWithInheritedAttributes(zone, subjectIdentifier, scopes)
    }

    override fun getEntityId(entity: SubjectEntity): String? {
        return entity.subjectIdentifier
    }

    override fun updateVertexProperties(
        entity: SubjectEntity,
        vertex: Vertex
    ) {
        var subjectAttributesJson = entity.attributesAsJson
        if (StringUtils.isEmpty(subjectAttributesJson)) {
            subjectAttributesJson = EMPTY_ATTRIBUTES
        }
        vertex.property(ATTRIBUTES_PROPERTY_KEY, subjectAttributesJson)
    }

    @Suppress("UNCHECKED_CAST")
    override fun vertexToEntity(vertex: Vertex): SubjectEntity {
        val subjectIdentifier = getPropertyOrFail(vertex, SUBJECT_ID_KEY)
        val zoneName = getPropertyOrFail(vertex, ZONE_ID_KEY)
        val zoneEntity = ZoneEntity()
        zoneEntity.name = zoneName
        val subjectEntity = SubjectEntity(zoneEntity, subjectIdentifier)
        subjectEntity.id = vertex.id() as Long
        val attributesAsJson =
            getPropertyOrEmptyString(vertex, ATTRIBUTES_PROPERTY_KEY)
        subjectEntity.attributesAsJson = attributesAsJson
        subjectEntity.attributes =
            JSON_UTILS.deserialize(attributesAsJson, Set::class.java as Class<Set<Attribute>>, Attribute::class.java)
        val parentSet = getParents(vertex, SUBJECT_ID_KEY)
        subjectEntity.parents = parentSet
        return subjectEntity
    }

    override fun getSubjectEntityAndDescendantsIds(entity: SubjectEntity?): Set<String> {
        return getEntityAndDescendantsIds(entity)
    }

    override fun <S : SubjectEntity> findAll(example: Example<S>): List<S> {
        throw UnsupportedOperationException(MESSAGE)
    }

    override fun <S : SubjectEntity> findAll(
        example: Example<S>,
        sort: Sort
    ): List<S> {
        throw UnsupportedOperationException(MESSAGE)
    }

    override fun <S : SubjectEntity> findOne(example: Example<S>): S {
        throw UnsupportedOperationException(MESSAGE)
    }

    override fun <S : SubjectEntity> findAll(
        example: Example<S>,
        pageable: Pageable
    ): Page<S> {
        throw UnsupportedOperationException(MESSAGE)
    }

    override fun <S : SubjectEntity> count(example: Example<S>): Long {
        throw UnsupportedOperationException(MESSAGE)
    }

    override fun <S : SubjectEntity> exists(example: Example<S>): Boolean {
        throw UnsupportedOperationException(MESSAGE)
    }
}
