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
 *******************************************************************************/

package com.ge.predix.acs.privilege.management.dao;

import static com.ge.predix.acs.privilege.management.dao.AttributePredicate.elementOf;
import static org.apache.tinkerpop.gremlin.process.traversal.P.eq;
import static org.apache.tinkerpop.gremlin.process.traversal.P.test;
import static org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__.in;
import static org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__.outE;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang.NotImplementedException;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.Direction;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.T;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.structure.VertexProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.util.Assert;

import com.ge.predix.acs.model.Attribute;
import com.ge.predix.acs.rest.Parent;
import com.ge.predix.acs.utils.JsonUtils;
import com.ge.predix.acs.zone.management.dao.ZoneEntity;
import com.google.common.collect.Sets;
import com.thinkaurelius.titan.core.SchemaViolationException;

public abstract class GraphGenericRepository<E extends ZonableEntity> implements JpaRepository<E, Long> {
    private static final Logger LOGGER = LoggerFactory.getLogger(GraphGenericRepository.class);
    private static final JsonUtils JSON_UTILS = new JsonUtils();

    public static final String ATTRIBUTES_PROPERTY_KEY = "attributes";
    public static final String PARENT_EDGE_LABEL = "parent";
    public static final String SCOPE_PROPERTY_KEY = "scope";
    public static final String ZONE_NAME_PROPERTY_KEY = "zoneName";
    public static final String ZONE_ID_KEY = "zoneId";
    public static final String VERSION_PROPERTY_KEY = "schemaVersion";
    public static final String VERSION_VERTEX_LABEL = "version";

    @Autowired
    private GraphTraversalSource graphTraversal;

    @Value("${TITAN_TRAVERSAL_LIMIT:256}")
    private long traversalLimit = 256;

    @Override
    public void deleteAllInBatch() {
        deleteAll();
    }

    @Override
    public void deleteInBatch(final Iterable<E> entities) {
        delete(entities);
    }

    @Override
    public List<E> findAll() {
        try {
            return this.graphTraversal.V().has(ZONE_ID_KEY).has(getEntityIdKey()).toList().stream()
                    .map(this::vertexToEntity).collect(Collectors.toList());
        } finally {
            this.graphTraversal.tx().commit();
        }
    }

    @Override
    public List<E> findAll(final Sort arg0) {
        throw new NotImplementedException("This repository does not support sortable find all queries.");
    }

    @Override
    public List<E> findAll(final Iterable<Long> ids) {
        try {
            return this.graphTraversal.V(iterableToArray(ids)).toList().stream().map(this::vertexToEntity)
                    .collect(Collectors.toList());
        } finally {
            this.graphTraversal.tx().commit();
        }
    }

    private static Object[] iterableToArray(final Iterable<?> iterable) {
        List<Object> idList = new ArrayList<>();
        iterable.forEach(idList::add);
        return idList.toArray();
    }

    @Override
    public void flush() {
        // Nothing to do.
    }

    @Override
    public E getOne(final Long id) {
        return findOne(id);
    }

    @Override
    public <S extends E> List<S> save(final Iterable<S> entities) {
        List<S> savedEntities = new ArrayList<>();
        this.commitTransaction(() -> entities.forEach(item -> savedEntities.add(saveCommon(item))));
        return savedEntities;
    }

    @Override
    public <S extends E> S saveAndFlush(final S entity) {
        return save(entity);
    }

    @Override
    public Page<E> findAll(final Pageable pageable) {
        throw new NotImplementedException("This repository does not support pageable find all queries.");
    }

    @Override
    public long count() {
        try {
            return this.graphTraversal.V().has(getEntityIdKey()).count().next();
        } finally {
            this.graphTraversal.tx().commit();
        }
    }

    @Override
    public void delete(final Long id) {
        this.commitTransaction(() -> this.graphTraversal.V(id).drop().iterate());
    }

    @Override
    public void delete(final E entity) {
        this.commitTransaction(() -> this.graphTraversal.V(entity.getId()).drop().iterate());
    }

    @Override
    public void delete(final Iterable<? extends E> entities) {
        List<Long> ids = new ArrayList<>();
        entities.forEach(item -> ids.add(item.getId()));
        this.commitTransaction(() -> this.graphTraversal.V(ids.toArray()).drop().iterate());
    }

    @Override
    public void deleteAll() {
        this.commitTransaction(() -> this.graphTraversal.V().has(getEntityIdKey()).drop().iterate());
    }

    @Override
    public boolean exists(final Long id) {
        try {
            GraphTraversal<Vertex, Vertex> traversal = this.graphTraversal.V(id);
            return traversal.hasNext();
        } finally {
            this.graphTraversal.tx().commit();
        }
    }

    @Override
    public E findOne(final Long id) {
        try {
            GraphTraversal<Vertex, Vertex> traversal = this.graphTraversal.V(id);
            if (traversal.hasNext()) {
                Vertex vertex = traversal.next();
                return vertexToEntity(vertex);
            }
            return null;
        } finally {
            this.graphTraversal.tx().commit();
        }
    }

    @Override
    public <S extends E> S save(final S entity) {
        S saveCommon;
        try {
            saveCommon = saveCommon(entity);
            this.graphTraversal.tx().commit();
        } catch (Exception e) {
            this.graphTraversal.tx().rollback();
            throw e;
        }
        return saveCommon;
    }

    private <S extends E> S saveCommon(final S entity) {
        // Create the entity if the id is null otherwise update an existing entity.
        if ((null == entity.getId()) || (0 == entity.getId())) {
            verifyEntityNotSelfReferencing(entity);
            String entityId = getEntityId(entity);
            Assert.notNull(entity.getZone(), "ZonableEntity must have a non-null zone.");
            String zoneId = entity.getZone().getName();
            Assert.hasText(zoneId, "zoneName is required.");
            Vertex entityVertex = this.graphTraversal.addV().property(T.label, getEntityLabel())
                    .property(ZONE_ID_KEY, zoneId).property(getEntityIdKey(), entityId).next();

            updateVertexProperties(entity, entityVertex);
            saveParentRelationships(entity, entityVertex, false);
            entity.setId((Long) entityVertex.id());
        } else {
            verifyEntityReferencesNotCyclic(entity);
            GraphTraversal<Vertex, Vertex> traversal = this.graphTraversal.V(entity.getId());
            Vertex entityVertex = traversal.next();
            updateVertexProperties(entity, entityVertex);
            saveParentRelationships(entity, entityVertex, true);
        }
        return entity;
    }

    private void verifyEntityNotSelfReferencing(final E entity) {
        if (entity.getParents().contains(new Parent(getEntityId(entity)))) {
            throw new SchemaViolationException(
                    String.format("The entity '%s' references itself as a parent.", getEntityId(entity)));
        }
    }

    private void verifyEntityReferencesNotCyclic(final E entity) {
        // First verify the entity does not reference itself as a parent.
        verifyEntityNotSelfReferencing(entity);

        // Now check for potential cyclic references.
        this.graphTraversal.V(entity.getId()).has(getEntityIdKey()).emit().repeat(in().has(getEntityIdKey()))
                .until(eq(null)).values(getEntityIdKey()).toStream().forEach(id -> {
            if (entity.getParents().contains(new Parent((String) id))) {
                throw new SchemaViolationException(
                        String.format("Updating entity '%s' with parent '%s' introduces a cyclic reference.",
                                getEntityId(entity), id));
            }
        });
    }

    void saveParentRelationships(final E entity, final Vertex vertex, final boolean update) {
        if (update) { // If this is an update remove all existing edges.
            vertex.edges(Direction.OUT, PARENT_EDGE_LABEL).forEachRemaining(Edge::remove);
        }
        entity.getParents().forEach(parent -> saveParentRelationship(entity, vertex, parent));
    }

    private void saveParentRelationship(final E entity, final Vertex vertex, final Parent parent) {
        GraphTraversal<Vertex, Vertex> traversal = this.graphTraversal.V().has(ZONE_ID_KEY, entity.getZone().getName())
                .has(getEntityIdKey(), parent.getIdentifier());
        if (!traversal.hasNext()) {
            throw new IllegalStateException(
                    String.format("No parent exists in zone '%s' with '%s' value of '%s'.", entity.getZone().getName(),
                            getEntityIdKey(), parent.getIdentifier()));
        }
        Edge parentEdge = vertex.addEdge(PARENT_EDGE_LABEL, traversal.next());
        parent.getScopes().forEach(scope -> parentEdge.property(SCOPE_PROPERTY_KEY, JSON_UTILS.serialize(scope)));
    }

    Set<ParentEntity> getParentEntities(final E entity) {
        Set<ParentEntity> parents = new HashSet<>();
        entity.getParents().forEach(parent -> {
            GraphTraversal<Vertex, Vertex> traversal = this.graphTraversal.V()
                    .has(ZONE_ID_KEY, entity.getZone().getName()).has(getEntityIdKey(), parent.getIdentifier());
            if (!traversal.hasNext()) {
                throw new IllegalStateException(String.format("No parent exists in zone '%s' with '%s' value of '%s'.",
                        entity.getZone().getName(), getEntityIdKey(), parent.getIdentifier()));
            }
            parents.add(new ParentEntity(vertexToEntity(traversal.next()), parent.getScopes()));
        });
        return parents;
    }

    public boolean checkVersionVertexExists(final int versionNumber) {
        try {
            Vertex versionVertex = null;
            // Value has to be provided to the has() method for index to be used. Composite indexes only work on
            // equality comparisons.
            GraphTraversal<Vertex, Vertex> traversal = this.graphTraversal.V()
                    .has(VERSION_VERTEX_LABEL, VERSION_PROPERTY_KEY, versionNumber);
            if (traversal.hasNext()) {
                versionVertex = traversal.next();
                // There should be only one version entity with a given version
                Assert.isTrue(!traversal.hasNext(), "There are two schema version vertices in the graph");
            }
            return versionVertex != null;
        } finally {
            this.graphTraversal.tx().commit();
        }
    }

    public void createVersionVertex(final int version) {
       this.commitTransaction(() -> this.graphTraversal.addV().property(T.label, VERSION_VERTEX_LABEL)
           .property(VERSION_PROPERTY_KEY, version).next());
    }

    public List<E> findByZone(final ZoneEntity zoneEntity) {
        try {
            String zoneName = zoneEntity.getName();
            GraphTraversal<Vertex, Vertex> traversal = this.graphTraversal.V()
                    .has(getEntityLabel(), ZONE_ID_KEY, zoneName).has(getEntityIdKey());
            List<E> entities = new ArrayList<>();
            while (traversal.hasNext()) {
                entities.add(vertexToEntity(traversal.next()));
            }
            return entities;
        } finally {
            this.graphTraversal.tx().commit();
        }
    }

    public static String getPropertyOrEmptyString(final Vertex vertex, final String propertyKey) {
        VertexProperty<String> property = vertex.property(propertyKey);
        if (property.isPresent()) {
            return property.value();
        }
        return "";
    }

    public static String getPropertyOrFail(final Vertex vertex, final String propertyKey) {
        VertexProperty<String> property = vertex.property(propertyKey);
        if (property.isPresent()) {
            return property.value();
        }
        throw new IllegalStateException(
                String.format("The vertex with id '%s' does not conatin the expected property '%s'.", vertex.id(),
                        propertyKey));
    }

    public static String getPropertyOrNull(final Vertex vertex, final String propertyKey) {
        VertexProperty<String> property = vertex.property(propertyKey);
        if (property.isPresent()) {
            return property.value();
        }
        return null;
    }

    /**
     * Returns the entity with attributes only on the requested vertex. No parent attributes are included.
     */
    public E getEntity(final ZoneEntity zone, final String identifier) {
        try {
            GraphTraversal<Vertex, Vertex> traversal = this.graphTraversal.V().has(ZONE_ID_KEY, zone.getName())
                    .has(getEntityIdKey(), identifier);
            if (!traversal.hasNext()) {
                return null;
            }
            Vertex vertex = traversal.next();
            E entity = vertexToEntity(vertex);

            // There should be only one entity with a given entity id.
            Assert.isTrue(!traversal.hasNext(),
                    String.format("There are two entities with the same %s.", getEntityIdKey()));
            return entity;
        } finally {
            this.graphTraversal.tx().commit();
        }
    }

    public E getEntityWithInheritedAttributes(final ZoneEntity zone, final String identifier,
            final Set<Attribute> scopes) {
        try {
            GraphTraversal<Vertex, Vertex> traversal = this.graphTraversal.V().has(ZONE_ID_KEY, zone.getName())
                    .has(getEntityIdKey(), identifier);
            if (!traversal.hasNext()) {
                return null;
            }
            Vertex vertex = traversal.next();
            E entity = vertexToEntity(vertex);
            searchAttributesWithScopes(entity, vertex, scopes);

            // There should be only one entity with a given entity id.
            Assert.isTrue(!traversal.hasNext(),
                    String.format("There are two entities with the same %s.", getEntityIdKey()));
            return entity;
        } finally {
            this.graphTraversal.tx().commit();
        }
    }

    @SuppressWarnings("unchecked")
    private void searchAttributesWithScopes(final E entity, final Vertex vertex, final Set<Attribute> scopes) {
        Set<Attribute> attributes = new HashSet<>();

        // First add all attributes inherited from non-scoped relationships.
        this.graphTraversal.V(vertex.id()).has(ATTRIBUTES_PROPERTY_KEY).emit()
                .repeat(outE().hasNot(SCOPE_PROPERTY_KEY).otherV().simplePath().has(ATTRIBUTES_PROPERTY_KEY))
                .until(eq(null)).limit(this.traversalLimit + 1).values(ATTRIBUTES_PROPERTY_KEY).toStream()
                .forEach(it -> {
                    Set<Attribute> deserializedAttributes = JSON_UTILS
                            .deserialize((String) it, Set.class, Attribute.class);
                    if (deserializedAttributes != null) {
                        attributes.addAll(deserializedAttributes);
                        // This enforces the limit on the count of attributes returned from the traversal, instead of
                        // number of vertices traversed. To do the latter will require traversing the graph twice.
                        checkTraversalLimitOrFail(entity, attributes);
                    }
                });

        this.graphTraversal.V(vertex.id()).has(ATTRIBUTES_PROPERTY_KEY).emit()
                .repeat(outE().has(SCOPE_PROPERTY_KEY, test(elementOf(), scopes)).otherV().simplePath()
                        .has(ATTRIBUTES_PROPERTY_KEY)).until(eq(null)).limit(this.traversalLimit + 1)
                .values(ATTRIBUTES_PROPERTY_KEY).toStream().forEach(it -> {
            Set<Attribute> deserializedAttributes = JSON_UTILS.deserialize((String) it, Set.class, Attribute.class);
            if (deserializedAttributes != null) {
                attributes.addAll(deserializedAttributes);
                checkTraversalLimitOrFail(entity, attributes);
            }
        });
        entity.setAttributes(attributes);
        entity.setAttributesAsJson(JSON_UTILS.serialize(attributes));
    }

    private void checkTraversalLimitOrFail(final E e, final Set<Attribute> attributes) {
        if (attributes.size() > this.traversalLimit) {
            String exceptionMessage = String
                    .format("The number of attributes on this " + e.getEntityType() + " '" + e.getEntityId()
                            + "' has exceeded the maximum limit of %d", this.traversalLimit);
            throw new AttributeLimitExceededException(exceptionMessage);
        }
    }

    public Set<Parent> getParents(final Vertex vertex, final String identifierKey) {
        Set<Parent> parentSet = new HashSet<>();
        vertex.edges(Direction.OUT, PARENT_EDGE_LABEL).forEachRemaining(edge -> {
            String parentIdentifier = getPropertyOrFail(edge.inVertex(), identifierKey);
            Attribute scope;
            Parent parent;
            try {
                scope = JSON_UTILS.deserialize((String) edge.property(SCOPE_PROPERTY_KEY).value(), Attribute.class);
                // use ParentEntity ?
                parent = new Parent(parentIdentifier, Sets.newHashSet(scope));
            } catch (Exception e) {
                LOGGER.debug("Error deserializing attribute", e);
                parent = new Parent(parentIdentifier);
            }
            parentSet.add(parent);

        });
        return parentSet;
    }

    public Set<String> getEntityAndDescendantsIds(final E entity) {
        if (entity == null) {
            return Collections.emptySet();
        }

        return this.graphTraversal.V(entity.getId()).has(getEntityIdKey()).emit().repeat(in().has(getEntityIdKey()))
                .until(eq(null)).values(getEntityIdKey()).toStream().map(Object::toString).collect(Collectors.toSet());
    }

    abstract String getEntityId(E entity);

    abstract String getEntityIdKey();

    abstract String getEntityLabel();

    abstract String getRelationshipKey();

    abstract void updateVertexProperties(E entity, Vertex vertex);

    abstract E vertexToEntity(Vertex vertex);

    public GraphTraversalSource getGraphTraversal() {
        return graphTraversal;
    }

    public void setGraphTraversal(final GraphTraversalSource graphTraversal) {
        this.graphTraversal = graphTraversal;
    }

    public long getTraversalLimit() {
        return this.traversalLimit;
    }

    public void setTraversalLimit(final long traversalLimit) {
        this.traversalLimit = traversalLimit;
    }
    private void commitTransaction(final Runnable graphQuery) {
        try {
            graphQuery.run();
            this.graphTraversal.tx().commit();
        } catch (final Exception e) {
            this.graphTraversal.tx().rollback();
            throw e;
        }
    }
}
