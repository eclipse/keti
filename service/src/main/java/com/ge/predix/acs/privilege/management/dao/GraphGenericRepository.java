package com.ge.predix.acs.privilege.management.dao;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang.NotImplementedException;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.T;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.structure.VertexProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

import com.ge.predix.acs.zone.management.dao.ZoneEntity;

public abstract class GraphGenericRepository<E extends ZonableEntity> implements JpaRepository<E, Long> {

    public static final String ZONE_NAME_PROPERTY_KEY = "zoneName";
    public static final String ZONE_ID_KEY = "zoneId";

    @Autowired
    private Graph graph;

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
            return this.graph.traversal().V().has(ZONE_ID_KEY).has(getEntityIdKey()).toList().stream()
                    .map(item -> vertexToEntity(item)).collect(Collectors.toList());
        } finally {
            this.graph.tx().commit();
        }
    }

    @Override
    public List<E> findAll(final Sort arg0) {
        throw new NotImplementedException("This repository does not support sortable find all queries.");
    }

    @Override
    public List<E> findAll(final Iterable<Long> ids) {
        try {
            return this.graph.traversal().V(iterableToArray(ids)).toList().stream().map(item -> vertexToEntity(item))
                    .collect(Collectors.toList());
        } finally {
            this.graph.tx().commit();
        }
    }

    private static Object[] iterableToArray(final Iterable<?> iterable) {
        List<Object> idList = new ArrayList<>();
        iterable.forEach(item -> idList.add(item));
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
    @SuppressWarnings("unchecked")
    public <S extends E> List<S> save(final Iterable<S> entities) {
        try {
            List<E> savedEntities = new ArrayList<>();
            entities.forEach(item -> savedEntities.add(saveCommon(item)));
            return (List<S>) savedEntities;
        } finally {
            this.graph.tx().commit();
        }
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
            return this.graph.traversal().V().has(getEntityIdKey()).count().next();
        } finally {
            this.graph.tx().commit();
        }
    }

    @Override
    public void delete(final Long id) {
        try {
            this.graph.traversal().V(id).drop().iterate();
        } finally {
            this.graph.tx().commit();
        }
    }

    @Override
    public void delete(final E entity) {
        try {
            this.graph.traversal().V(entity.getId()).drop().iterate();
        } finally {
            this.graph.tx().commit();
        }
    }

    @Override
    public void delete(final Iterable<? extends E> entities) {
        try {
            // Assemble list of entity ids.
            List<Long> ids = new ArrayList<>();
            entities.forEach(item -> ids.add(item.getId()));
            // Delete all entities in one fell swoop.
            this.graph.traversal().V(ids.toArray()).drop().iterate();
        } finally {
            this.graph.tx().commit();
        }
    }

    @Override
    public void deleteAll() {
        try {
            this.graph.traversal().V().has(getEntityIdKey()).drop().iterate();
        } finally {
            this.graph.tx().commit();
        }
    }

    @Override
    public boolean exists(final Long id) {
        try {
            GraphTraversal<Vertex, Vertex> traversal = this.graph.traversal().V(id);
            if (traversal.hasNext()) {
                return true;
            }
            return false;
        } finally {
            this.graph.tx().commit();
        }
    }

    @Override
    public E findOne(final Long id) {
        try {
            GraphTraversal<Vertex, Vertex> traversal = this.graph.traversal().V(id);
            if (traversal.hasNext()) {
                Vertex vertex = traversal.next();
                E entity = vertexToEntity(vertex);
                return entity;
            }
            return null;
        } finally {
            this.graph.tx().commit();
        }
    }

    @Override
    public <S extends E> S save(final S entity) {
        try {
            return saveCommon(entity);
        } finally {
            this.graph.tx().commit();
        }
    }

    private <S extends E> S saveCommon(final S entity) {
        // Create the entity if the id is null otherwise update an existing entity.
        if ((null == entity.getId()) || (0 == entity.getId())) {
            String entityId = computeId(entity);
            String zoneId = entity.getZone().getName();
            Vertex entityVertex = this.graph.addVertex(T.label, getEntityLabel(), ZONE_ID_KEY, zoneId, getEntityIdKey(),
                    entityId);
            upsertEntityVertex(entity, entityVertex);
            entity.setId((long) entityVertex.id());
        } else {
            GraphTraversal<Vertex, Vertex> traversal = this.graph.traversal().V(entity.getId());
            Vertex entityVertex = traversal.next();
            upsertEntityVertex(entity, entityVertex);
        }
        return entity;
    }

    public List<E> findByZone(final ZoneEntity zoneEntity) {
        try {
            String zoneName = zoneEntity.getName();
            GraphTraversal<Vertex, Vertex> traversal = this.graph.traversal().V()
                    .has(getEntityLabel(), ZONE_ID_KEY, zoneName).has(getEntityIdKey());
            List<E> entities = new ArrayList<>();
            while (traversal.hasNext()) {
                entities.add(vertexToEntity(traversal.next()));
            }
            return entities;
        } finally {
            this.graph.tx().commit();
        }
    }

    public static String getVertexStringPropertyOrEmpty(final Vertex vertex, final String propertyKey) {
        VertexProperty<String> property = vertex.property(propertyKey);
        if (property.isPresent()) {
            return property.value();
        }
        return "";
    }

    public static String getVertexStringPropertyOrFail(final Vertex vertex, final String propertyKey) {
        VertexProperty<String> property = vertex.property(propertyKey);
        if (property.isPresent()) {
            return property.value();
        }
        throw new IllegalStateException(String.format(
                "The vertex with id '%d' does not conatin the expected property '%s'.", vertex.id(), propertyKey));
    }

    public static String getVertexStringPropertyOrNull(final Vertex vertex, final String propertyKey) {
        VertexProperty<String> property = vertex.property(propertyKey);
        if (property.isPresent()) {
            return property.value();
        }
        return null;
    }

    abstract String computeId(E entity);

    abstract String getEntityIdKey();

    abstract String getEntityLabel();

    abstract String getRelationshipKey();

    abstract void upsertEntityVertex(final E entity, final Vertex vertex);

    abstract E vertexToEntity(final Vertex vertex);

    public Graph getGraph() {
        return this.graph;
    }

    public void setGraph(final Graph graph) {
        this.graph = graph;
    }
}
