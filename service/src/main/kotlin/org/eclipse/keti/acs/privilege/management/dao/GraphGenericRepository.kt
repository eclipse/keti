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

import com.google.common.collect.Sets
import org.apache.commons.lang.NotImplementedException
import org.apache.tinkerpop.gremlin.process.traversal.P.eq
import org.apache.tinkerpop.gremlin.process.traversal.P.test
import org.apache.tinkerpop.gremlin.process.traversal.Traverser
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.`__`.`in`
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.`__`.outE
import org.apache.tinkerpop.gremlin.structure.Direction
import org.apache.tinkerpop.gremlin.structure.T
import org.apache.tinkerpop.gremlin.structure.Vertex
import org.eclipse.keti.acs.model.Attribute
import org.eclipse.keti.acs.privilege.management.dao.AttributePredicate.elementOf
import org.eclipse.keti.acs.rest.Parent
import org.eclipse.keti.acs.utils.JsonUtils
import org.eclipse.keti.acs.zone.management.dao.ZoneEntity
import org.janusgraph.core.SchemaViolationException
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.util.Assert
import java.util.ArrayList
import java.util.HashSet

private val LOGGER = LoggerFactory.getLogger(GraphGenericRepository::class.java)
private val JSON_UTILS = JsonUtils()

const val ATTRIBUTES_PROPERTY_KEY = "attributes"
const val PARENT_EDGE_LABEL = "parent"
const val SCOPE_PROPERTY_KEY = "scope"
const val ZONE_ID_KEY = "zoneId"
const val VERSION_PROPERTY_KEY = "schemaVersion"
const val VERSION_VERTEX_LABEL = "version"

private fun iterableToArray(iterable: Iterable<Any>): Array<Any> {
    val idList = ArrayList<Any>()
    iterable.forEach { idList.add(it) }
    return idList.toTypedArray()
}

fun getPropertyOrEmptyString(
    vertex: Vertex,
    propertyKey: String
): String {
    val property = vertex.property<String>(propertyKey)
    return if (property.isPresent) {
        property.value()
    } else ""
}

fun getPropertyOrFail(
    vertex: Vertex,
    propertyKey: String
): String {
    val property = vertex.property<String>(propertyKey)
    if (property.isPresent) {
        return property.value()
    }
    throw IllegalStateException(
        String.format(
            "The vertex with id '%s' does not conatin the expected property '%s'.", vertex.id(),
            propertyKey
        )
    )
}

abstract class GraphGenericRepository<E : ZonableEntity> : JpaRepository<E, Long> {

    @Autowired
    lateinit var graphTraversal: GraphTraversalSource

    @Value("\${GRAPH_TRAVERSAL_LIMIT:256}")
    var traversalLimit: Long = 256

    abstract val entityIdKey: String

    abstract val entityLabel: String

    abstract val relationshipKey: String

    override fun deleteAllInBatch() {
        deleteAll()
    }

    override fun deleteInBatch(entities: Iterable<E>) {
        delete(entities)
    }

    override fun findAll(): List<E> {
        try {
            return this.graphTraversal.V().has(ZONE_ID_KEY).has(entityIdKey).toList().map { this.vertexToEntity(it) }
        } finally {
            this.graphTraversal.tx().commit()
        }
    }

    override fun findAll(arg0: Sort): List<E> {
        throw NotImplementedException("This repository does not support sortable find all queries.")
    }

    override fun findAll(ids: Iterable<Long>): List<E> {
        try {
            return this.graphTraversal.V(*iterableToArray(ids)).toList().map { this.vertexToEntity(it) }
        } finally {
            this.graphTraversal.tx().commit()
        }
    }

    override fun flush() {
        // Nothing to do.
    }

    override fun getOne(id: Long?): E? {
        return findOne(id)
    }

    override fun <S : E> save(entities: Iterable<S>): List<S> {
        val savedEntities = ArrayList<S>()
        this.commitTransaction(Runnable { entities.forEach { item -> savedEntities.add(saveCommon(item)) } })
        return savedEntities
    }

    override fun <S : E> saveAndFlush(entity: S): S {
        return save(entity)
    }

    override fun findAll(pageable: Pageable): Page<E> {
        throw NotImplementedException("This repository does not support pageable find all queries.")
    }

    override fun count(): Long {
        try {
            return this.graphTraversal.V().has(entityIdKey).count().next()
        } finally {
            this.graphTraversal.tx().commit()
        }
    }

    override fun delete(id: Long?) {
        this.commitTransaction(Runnable { this.graphTraversal.V(id!!).drop().iterate() })
    }

    override fun delete(entity: E) {
        this.commitTransaction(Runnable { this.graphTraversal.V(entity.id!!).drop().iterate() })
    }

    override fun delete(entities: Iterable<E>) {
        val ids = ArrayList<Long>()
        entities.forEach { item -> ids.add(item.id!!) }
        this.commitTransaction(Runnable { this.graphTraversal.V(*ids.toTypedArray()).drop().iterate() })
    }

    override fun deleteAll() {
        this.commitTransaction(Runnable { this.graphTraversal.V().has(entityIdKey).drop().iterate() })
    }

    override fun exists(id: Long?): Boolean {
        try {
            val traversal = this.graphTraversal.V(id!!)
            return traversal.hasNext()
        } finally {
            this.graphTraversal.tx().commit()
        }
    }

    override fun findOne(id: Long?): E? {
        try {
            val traversal = this.graphTraversal.V(id!!)
            if (traversal.hasNext()) {
                val vertex = traversal.next()
                return vertexToEntity(vertex)
            }
            return null
        } finally {
            this.graphTraversal.tx().commit()
        }
    }

    override fun <S : E> save(entity: S): S {
        val saveCommon: S
        try {
            saveCommon = saveCommon(entity)
            this.graphTraversal.tx().commit()
        } catch (e: Exception) {
            this.graphTraversal.tx().rollback()
            throw e
        }

        return saveCommon
    }

    private fun <S : E> saveCommon(entity: S): S {
        // Create the entity if the id is null otherwise update an existing entity.
        if (null == entity.id || 0L == entity.id) {
            verifyEntityNotSelfReferencing(entity)
            val entityId = getEntityId(entity)
            Assert.notNull(entity.zone, "ZonableEntity must have a non-null zone.")
            val zoneId = entity.zone!!.name
            Assert.hasText(zoneId, "zoneName is required.")
            val entityVertex = this.graphTraversal.addV().property(T.label, entityLabel)
                .property(ZONE_ID_KEY, zoneId).property(entityIdKey, entityId).next()

            updateVertexProperties(entity, entityVertex)
            saveParentRelationships(entity, entityVertex, false)
            entity.id = entityVertex.id() as Long
        } else {
            verifyEntityReferencesNotCyclic(entity)
            val traversal = this.graphTraversal.V(entity.id!!)
            val entityVertex = traversal.next()
            updateVertexProperties(entity, entityVertex)
            saveParentRelationships(entity, entityVertex, true)
        }
        return entity
    }

    private fun verifyEntityNotSelfReferencing(entity: E) {
        if (entity.parents.contains(Parent(getEntityId(entity)!!))) {
            throw SchemaViolationException(
                String.format("The entity '%s' references itself as a parent.", getEntityId(entity))
            )
        }
    }

    private fun verifyEntityReferencesNotCyclic(entity: E) {
        // First verify the entity does not reference itself as a parent.
        verifyEntityNotSelfReferencing(entity)

        // Now check for potential cyclic references.
        this.graphTraversal.V(entity.id!!).has(entityIdKey).emit().repeat(`in`<Any>().has(entityIdKey))
            .until(eq<Traverser<Vertex>>(null)).values<Any>(entityIdKey).toStream().forEach { id ->
                if (entity.parents.contains(Parent(id as String))) {
                    throw SchemaViolationException(
                        String.format(
                            "Updating entity '%s' with parent '%s' introduces a cyclic reference.",
                            getEntityId(entity), id
                        )
                    )
                }
            }
    }

    internal fun saveParentRelationships(
        entity: E,
        vertex: Vertex,
        update: Boolean
    ) {
        if (update) { // If this is an update remove all existing edges.
            vertex.edges(Direction.OUT, PARENT_EDGE_LABEL).forEachRemaining { it.remove() }
        }
        entity.parents.forEach { parent -> saveParentRelationship(entity, vertex, parent) }
    }

    private fun saveParentRelationship(
        entity: E,
        vertex: Vertex,
        parent: Parent
    ) {
        val traversal = this.graphTraversal.V().has(ZONE_ID_KEY, entity.zone!!.name)
            .has(entityIdKey, parent.identifier)
        if (!traversal.hasNext()) {
            throw IllegalStateException(
                String.format(
                    "No parent exists in zone '%s' with '%s' value of '%s'.", entity.zone!!.name,
                    entityIdKey, parent.identifier
                )
            )
        }
        val parentEdge = vertex.addEdge(PARENT_EDGE_LABEL, traversal.next())
        parent.scopes.forEach { scope -> parentEdge.property(SCOPE_PROPERTY_KEY, JSON_UTILS.serialize(scope)) }
    }

    fun checkVersionVertexExists(versionNumber: Int): Boolean {
        try {
            var versionVertex: Vertex? = null
            // Value has to be provided to the has() method for index to be used. Composite indexes only work on
            // equality comparisons.
            val traversal = this.graphTraversal.V()
                .has(VERSION_VERTEX_LABEL, VERSION_PROPERTY_KEY, versionNumber)
            if (traversal.hasNext()) {
                versionVertex = traversal.next()
                // There should be only one version entity with a given version
                Assert.isTrue(!traversal.hasNext(), "There are two schema version vertices in the graph")
            }
            return versionVertex != null
        } finally {
            this.graphTraversal.tx().commit()
        }
    }

    fun createVersionVertex(version: Int) {
        this.commitTransaction(Runnable {
            this.graphTraversal.addV().property(T.label, VERSION_VERTEX_LABEL)
                .property(VERSION_PROPERTY_KEY, version).next()
        })
    }

    fun findByZone(zoneEntity: ZoneEntity): List<E> {
        try {
            val zoneName = zoneEntity.name
            val traversal = this.graphTraversal.V()
                .has(entityLabel, ZONE_ID_KEY, zoneName).has(entityIdKey)
            val entities = ArrayList<E>()
            while (traversal.hasNext()) {
                entities.add(vertexToEntity(traversal.next()))
            }
            return entities
        } finally {
            this.graphTraversal.tx().commit()
        }
    }

    /**
     * Returns the entity with attributes only on the requested vertex. No parent attributes are included.
     */
    fun getEntity(
        zone: ZoneEntity,
        identifier: String
    ): E? {
        try {
            val traversal = this.graphTraversal.V().has(ZONE_ID_KEY, zone.name)
                .has(entityIdKey, identifier)
            if (!traversal.hasNext()) {
                return null
            }
            val vertex = traversal.next()
            val entity = vertexToEntity(vertex)

            // There should be only one entity with a given entity id.
            Assert.isTrue(
                !traversal.hasNext(),
                String.format("There are two entities with the same %s.", entityIdKey)
            )
            return entity
        } finally {
            this.graphTraversal.tx().commit()
        }
    }

    fun getEntityWithInheritedAttributes(
        zone: ZoneEntity,
        identifier: String,
        scopes: Set<Attribute>?
    ): E? {
        try {
            val traversal = this.graphTraversal.V().has(ZONE_ID_KEY, zone.name)
                .has(entityIdKey, identifier)
            if (!traversal.hasNext()) {
                return null
            }
            val vertex = traversal.next()
            val entity = vertexToEntity(vertex)
            searchAttributesWithScopes(entity, vertex, scopes)

            // There should be only one entity with a given entity id.
            Assert.isTrue(
                !traversal.hasNext(),
                String.format("There are two entities with the same %s.", entityIdKey)
            )
            return entity
        } finally {
            this.graphTraversal.tx().commit()
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun searchAttributesWithScopes(
        entity: E,
        vertex: Vertex,
        scopes: Set<Attribute>?
    ) {
        val attributes = HashSet<Attribute>()

        // First add all attributes inherited from non-scoped relationships.
        this.graphTraversal.V(vertex.id()).has(ATTRIBUTES_PROPERTY_KEY).emit()
            .repeat(outE<Any>().hasNot(SCOPE_PROPERTY_KEY).otherV().simplePath().has(ATTRIBUTES_PROPERTY_KEY))
            .until(eq<Traverser<Vertex>>(null)).limit(this.traversalLimit + 1).values<Any>(ATTRIBUTES_PROPERTY_KEY)
            .toStream()
            .forEach { it ->
                val deserializedAttributes = JSON_UTILS
                    .deserialize(it as String, Set::class.java as Class<Set<Attribute>>, Attribute::class.java)
                if (deserializedAttributes != null) {
                    attributes.addAll(deserializedAttributes)
                    // This enforces the limit on the count of attributes returned from the traversal, instead of
                    // number of vertices traversed. To do the latter will require traversing the graph twice.
                    checkTraversalLimitOrFail(entity, attributes)
                }
            }

        this.graphTraversal.V(vertex.id()).has(ATTRIBUTES_PROPERTY_KEY).emit()
            .repeat(
                outE<Any>().has(SCOPE_PROPERTY_KEY, test(elementOf(), scopes)).otherV().simplePath()
                    .has(ATTRIBUTES_PROPERTY_KEY)
            ).until(eq<Traverser<Vertex>>(null)).limit(this.traversalLimit + 1)
            .values<Any>(ATTRIBUTES_PROPERTY_KEY).toStream().forEach { it ->
                val deserializedAttributes =
                    JSON_UTILS
                        .deserialize(it as String, Set::class.java as Class<Set<Attribute>>, Attribute::class.java)
                if (deserializedAttributes != null) {
                    attributes.addAll(deserializedAttributes)
                    checkTraversalLimitOrFail(entity, attributes)
                }
            }
        entity.attributes = attributes
        entity.attributesAsJson = JSON_UTILS.serialize<Set<Attribute>>(attributes)
    }

    private fun checkTraversalLimitOrFail(
        e: E,
        attributes: Set<Attribute>
    ) {
        if (attributes.size > this.traversalLimit) {
            val exceptionMessage = String
                .format(
                    "The number of attributes on this " + e.entityType + " '" + e.entityId
                    + "' has exceeded the maximum limit of %d", this.traversalLimit
                )
            throw AttributeLimitExceededException(exceptionMessage)
        }
    }

    fun getParents(
        vertex: Vertex,
        identifierKey: String
    ): Set<Parent> {
        val parentSet = HashSet<Parent>()
        vertex.edges(Direction.OUT, PARENT_EDGE_LABEL).forEachRemaining { edge ->
            val parentIdentifier = getPropertyOrFail(edge.inVertex(), identifierKey)
            val scope: Attribute?
            var parent: Parent
            try {
                scope =
                    JSON_UTILS.deserialize(
                        edge.property<Any>(SCOPE_PROPERTY_KEY).value() as String,
                        Attribute::class.java
                    )
                // use ParentEntity ?
                parent = Parent(parentIdentifier, Sets.newHashSet(scope!!))
            } catch (e: Exception) {
                LOGGER.debug("Error deserializing attribute", e)
                parent = Parent(parentIdentifier)
            }

            parentSet.add(parent)

        }
        return parentSet
    }

    fun getEntityAndDescendantsIds(entity: E?): Set<String> {
        return if (entity == null) {
            emptySet()
        } else this.graphTraversal.V(entity.id).has(entityIdKey).emit().repeat(`in`<Any>().has(entityIdKey))
            .until(eq<Traverser<Vertex>>(null)).values<Any>(entityIdKey).map { it.toString() }.toSet()
    }

    abstract fun getEntityId(entity: E): String?

    abstract fun updateVertexProperties(
        entity: E,
        vertex: Vertex
    )

    abstract fun vertexToEntity(vertex: Vertex): E
    private fun commitTransaction(graphQuery: Runnable) {
        try {
            graphQuery.run()
            this.graphTraversal.tx().commit()
        } catch (e: Exception) {
            this.graphTraversal.tx().rollback()
            throw e
        }
    }
}
