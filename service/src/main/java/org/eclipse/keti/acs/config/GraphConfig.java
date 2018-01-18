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

package org.eclipse.keti.acs.config;

import static org.eclipse.keti.acs.privilege.management.dao.GraphGenericRepository.PARENT_EDGE_LABEL;
import static org.eclipse.keti.acs.privilege.management.dao.GraphGenericRepository.SCOPE_PROPERTY_KEY;
import static org.eclipse.keti.acs.privilege.management.dao.GraphGenericRepository.ZONE_ID_KEY;
import static org.eclipse.keti.acs.privilege.management.dao.GraphResourceRepository.RESOURCE_ID_KEY;
import static org.eclipse.keti.acs.privilege.management.dao.GraphSubjectRepository.SUBJECT_ID_KEY;

import java.util.Arrays;
import java.util.concurrent.ExecutionException;

import javax.annotation.PostConstruct;

import org.apache.commons.lang.StringUtils;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.Direction;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.util.Assert;

import org.eclipse.keti.acs.privilege.management.dao.GraphGenericRepository;
import org.eclipse.keti.acs.privilege.management.dao.GraphResourceRepository;
import org.eclipse.keti.acs.privilege.management.dao.GraphSubjectRepository;
import com.thinkaurelius.titan.core.EdgeLabel;
import com.thinkaurelius.titan.core.PropertyKey;
import com.thinkaurelius.titan.core.TitanFactory;
import com.thinkaurelius.titan.core.TitanFactory.Builder;
import com.thinkaurelius.titan.core.TitanGraph;
import com.thinkaurelius.titan.core.VertexLabel;
import com.thinkaurelius.titan.core.schema.SchemaAction;
import com.thinkaurelius.titan.core.schema.SchemaStatus;
import com.thinkaurelius.titan.core.schema.TitanManagement;
import com.thinkaurelius.titan.core.schema.TitanManagement.IndexBuilder;
import com.thinkaurelius.titan.graphdb.database.management.ManagementSystem;

@Configuration
@Profile({ "titan" })
public class GraphConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(GraphConfig.class);

    public static final String BY_SCOPE_INDEX_NAME = "byScopeIndex";
    public static final String BY_SUBJECT_UNIQUE_INDEX_NAME = "bySubjectUnique";
    public static final String BY_RESOURCE_UNIQUE_INDEX_NAME = "byResourceUnique";
    public static final String BY_ZONE_INDEX_NAME = "byZone";
    public static final String BY_VERSION_UNIQUE_INDEX_NAME = "byVersion";
    public static final String BY_ZONE_AND_RESOURCE_UNIQUE_INDEX_NAME = "byZoneAndResourceUnique";
    public static final String BY_ZONE_AND_SUBJECT_UNIQUE_INDEX_NAME = "byZoneAndSubjectUnique";

    @Value("${TITAN_ENABLE_CASSANDRA:false}")
    private boolean cassandraEnabled;
    @Value("${TITAN_STORAGE_CASSANDRA_KEYSPACE:titan}")
    private String cassandraKeyspace;
    @Value("${TITAN_STORAGE_HOSTNAME:localhost}")
    private String hostname;
    @Value("${TITAN_STORAGE_USERNAME:}")
    private String username;
    @Value("${TITAN_STORAGE_PASSWORD:}")
    private String password;
    @Value("${TITAN_STORAGE_PORT:9160}")
    private int port;
    @Value("${TITAN_ENABLE_CACHE:true}")
    private boolean cacheEnabled;
    @Value("${TITAN_CACHE_CLEAN_WAIT:50}")
    private int titanCacheCleanWait;
    @Value("${TITAN_CACHE_TIME:1000}")
    private int titanCacheTime;
    @Value("${TITAN_CACHE_SIZE:0.25}")
    private double titanCacheSize;

    private Graph graph;

    @PostConstruct
    public void init() throws InterruptedException {
        this.graph = createGraph();
    }

    public Graph createGraph() throws InterruptedException {
        Graph newGraph;
        if (this.cassandraEnabled) {
            Builder titanBuilder = TitanFactory.build().set("storage.backend", "cassandra")
                    .set("storage.cassandra.keyspace", this.cassandraKeyspace)
                    .set("storage.hostname", Arrays.asList(hostname.split(","))).set("storage.port", this.port)
                    .set("cache.db-cache", this.cacheEnabled).set("cache.db-cache-clean-wait", this.titanCacheCleanWait)
                    .set("cache.db-cache-time", this.titanCacheTime).set("cache.db-cache-size", this.titanCacheSize);
            if (StringUtils.isNotEmpty(this.username)) {
                titanBuilder = titanBuilder.set("storage.username", this.username);
            }
            if (StringUtils.isNotEmpty(this.password)) {
                titanBuilder = titanBuilder.set("storage.password", this.password);
            }
            newGraph = titanBuilder.open();
        } else {
            newGraph = TitanFactory.build().set("storage.backend", "inmemory").open();
        }
        createSchemaElements(newGraph);
        LOGGER.info("Initialized titan db.");
        return newGraph;
    }

    public static void createSchemaElements(final Graph newGraph) throws InterruptedException {
        createVertexLabel(newGraph, GraphResourceRepository.RESOURCE_LABEL);
        createVertexLabel(newGraph, GraphSubjectRepository.SUBJECT_LABEL);
        createVertexLabel(newGraph, GraphGenericRepository.VERSION_VERTEX_LABEL);

        createIndex(newGraph, BY_ZONE_INDEX_NAME, ZONE_ID_KEY);
        createUniqueCompositeIndex(newGraph, BY_ZONE_AND_RESOURCE_UNIQUE_INDEX_NAME,
                new String[] { ZONE_ID_KEY, RESOURCE_ID_KEY });

        createUniqueCompositeIndex(newGraph, BY_ZONE_AND_SUBJECT_UNIQUE_INDEX_NAME,
                new String[] { ZONE_ID_KEY, SUBJECT_ID_KEY });

        createUniqueCompositeIndex(newGraph, BY_VERSION_UNIQUE_INDEX_NAME,
                new String[] { GraphGenericRepository.VERSION_PROPERTY_KEY }, Integer.class);

        createEdgeIndex(newGraph, BY_SCOPE_INDEX_NAME, PARENT_EDGE_LABEL, SCOPE_PROPERTY_KEY);
    }

    public static void createIndex(final Graph newGraph, final String indexName, final String indexKey)
            throws InterruptedException {
        newGraph.tx().rollback(); // Never create new indexes while a transaction is active
        TitanManagement mgmt = ((TitanGraph) newGraph).openManagement();
        if (!mgmt.containsGraphIndex(indexName)) {
            PropertyKey indexPropertyKey = mgmt.makePropertyKey(indexKey).dataType(String.class).make();
            mgmt.buildIndex(indexName, Vertex.class).addKey(indexPropertyKey).buildCompositeIndex();
        }
        mgmt.commit();
        // Wait for the index to become available
        ManagementSystem.awaitGraphIndexStatus((TitanGraph) newGraph, indexName).status(SchemaStatus.ENABLED).call();
    }

    public static void createUniqueIndexForLabel(final Graph newGraph, final String indexName, final String indexKey,
            final String label) throws InterruptedException {
        newGraph.tx().rollback(); // Never create new indexes while a transaction is active
        TitanManagement mgmt = ((TitanGraph) newGraph).openManagement();
        if (!mgmt.containsGraphIndex(indexName)) {
            PropertyKey indexPropertyKey = mgmt.makePropertyKey(indexKey).dataType(Integer.class).make();
            VertexLabel versionLabel = mgmt.makeVertexLabel(label).make();
            // Create a unique composite index for the property key that indexes only vertices with a given label
            mgmt.buildIndex(indexName, Vertex.class).addKey(indexPropertyKey).indexOnly(versionLabel).unique()
                    .buildCompositeIndex();
        }
        mgmt.commit();
        // Wait for the index to become available
        ManagementSystem.awaitGraphIndexStatus((TitanGraph) newGraph, indexName).status(SchemaStatus.ENABLED).call();
    }

    private static void createUniqueCompositeIndex(final Graph newGraph, final String indexName,
            final String[] propertyKeyNames) throws InterruptedException {
        createUniqueCompositeIndex(newGraph, indexName, propertyKeyNames, String.class);
    }

    public static void createUniqueCompositeIndex(final Graph newGraph, final String indexName,
            final String[] propertyKeyNames, final Class<?> propertyKeyType) throws InterruptedException {

        Assert.notEmpty(propertyKeyNames);
        newGraph.tx().rollback(); // Never create new indexes while a transaction is active
        TitanManagement mgmt = ((TitanGraph) newGraph).openManagement();
        IndexBuilder indexBuilder = mgmt.buildIndex(indexName, Vertex.class);
        if (!mgmt.containsGraphIndex(indexName)) {
            for (String propertyKeyName : propertyKeyNames) {
                PropertyKey indexPropertyKey = getOrCreatePropertyKey(propertyKeyName, propertyKeyType, mgmt);
                indexBuilder.addKey(indexPropertyKey);
            }
            indexBuilder.unique().buildCompositeIndex();
        }
        mgmt.commit();
        // Wait for the index to become available
        ManagementSystem.awaitGraphIndexStatus((TitanGraph) newGraph, indexName).status(SchemaStatus.ENABLED).call();
    }

    private static PropertyKey getOrCreatePropertyKey(final String keyName, final Class<?> keyType,
            final TitanManagement mgmt) {
        PropertyKey propertyKey = mgmt.getPropertyKey(keyName);
        if (null == propertyKey) {
            propertyKey = mgmt.makePropertyKey(keyName).dataType(keyType).make();
        }
        return propertyKey;
    }

    public static void createEdgeIndex(final Graph newGraph, final String indexName, final String label,
            final String indexKey) throws InterruptedException {
        newGraph.tx().rollback(); // Never create new indexes while a transaction is active
        TitanManagement mgmt = ((TitanGraph) newGraph).openManagement();
        EdgeLabel edgeLabel = mgmt.getOrCreateEdgeLabel(label);
        if (!mgmt.containsRelationIndex(edgeLabel, indexName)) {
            PropertyKey indexPropertyKey = getOrCreatePropertyKey(indexKey, String.class, mgmt);
            mgmt.buildEdgeIndex(edgeLabel, indexName, Direction.OUT, indexPropertyKey);
        }
        mgmt.commit();
        // Wait for the index to become available
        ManagementSystem.awaitRelationIndexStatus((TitanGraph) newGraph, indexName, label).status(SchemaStatus.ENABLED)
                .call();
    }

    public static void createVertexLabel(final Graph newGraph, final String vertexLabel) throws InterruptedException {
        newGraph.tx().rollback();
        TitanManagement mgmt = ((TitanGraph) newGraph).openManagement();
        mgmt.getOrCreateVertexLabel(vertexLabel);
        mgmt.commit();
    }

    public static void reIndex(final Graph newGraph, final String indexName)
            throws InterruptedException, ExecutionException {
        TitanManagement mgmt = ((TitanGraph) newGraph).openManagement();
        mgmt.updateIndex(mgmt.getGraphIndex(indexName), SchemaAction.REINDEX).get();
        newGraph.tx().commit();
    }

    @Bean
    GraphTraversalSource graphTraversal() {
        return this.graph.traversal();
    }
}
