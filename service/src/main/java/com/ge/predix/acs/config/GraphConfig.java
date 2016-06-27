package com.ge.predix.acs.config;

import static com.ge.predix.acs.privilege.management.dao.GraphGenericRepository.PARENT_EDGE_LABEL;
import static com.ge.predix.acs.privilege.management.dao.GraphGenericRepository.SCOPE_PROPERTY_KEY;
import static com.ge.predix.acs.privilege.management.dao.GraphGenericRepository.ZONE_ID_KEY;
import static com.ge.predix.acs.privilege.management.dao.GraphResourceRepository.RESOURCE_ID_KEY;
import static com.ge.predix.acs.privilege.management.dao.GraphSubjectRepository.SUBJECT_ID_KEY;

import java.util.concurrent.ExecutionException;

import javax.annotation.PostConstruct;

import org.apache.commons.lang.StringUtils;
import org.apache.tinkerpop.gremlin.structure.Direction;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.thinkaurelius.titan.core.EdgeLabel;
import com.thinkaurelius.titan.core.PropertyKey;
import com.thinkaurelius.titan.core.TitanFactory;
import com.thinkaurelius.titan.core.TitanFactory.Builder;
import com.thinkaurelius.titan.core.TitanGraph;
import com.thinkaurelius.titan.core.schema.SchemaAction;
import com.thinkaurelius.titan.core.schema.SchemaStatus;
import com.thinkaurelius.titan.core.schema.TitanManagement;
import com.thinkaurelius.titan.graphdb.database.management.ManagementSystem;

@Configuration
@EnableAutoConfiguration
public class GraphConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(GraphConfig.class);

    public static final String BY_SCOPE_INDEX_NAME = "byScopeIndex";
    public static final String BY_SUBJECT_UNIQUE_INDEX_NAME = "bySubjectUnique";
    public static final String BY_RESOURCE_UNIQUE_INDEX_NAME = "byResourceUnique";
    public static final String BY_ZONE_INDEX_NAME = "byZone";
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
                    .set("storage.cassandra.keyspace", this.cassandraKeyspace).set("storage.hostname", this.hostname)
                    .set("storage.port", this.port).set("cache.db-cache", this.cacheEnabled)
                    .set("cache.db-cache-clean-wait", this.titanCacheCleanWait)
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

        createIndex(newGraph, BY_ZONE_INDEX_NAME, ZONE_ID_KEY);
        createTwoKeyUniqueCompositeIndex(newGraph, BY_ZONE_AND_RESOURCE_UNIQUE_INDEX_NAME, ZONE_ID_KEY,
                RESOURCE_ID_KEY);
        createTwoKeyUniqueCompositeIndex(newGraph, BY_ZONE_AND_SUBJECT_UNIQUE_INDEX_NAME, ZONE_ID_KEY, SUBJECT_ID_KEY);
        createEdgeIndex(newGraph, BY_SCOPE_INDEX_NAME, PARENT_EDGE_LABEL, SCOPE_PROPERTY_KEY);

        LOGGER.info("Initialized titan db.");
        return newGraph;
    }

    public static void createIndex(final Graph newGraph, final String indexName, final String indexKey)
            throws InterruptedException {
        newGraph.tx().rollback(); // Never create new indexes while a transaction is active
        TitanManagement mgmt = ((TitanGraph) newGraph).openManagement();
        // Create index for zones.
        if (!mgmt.containsGraphIndex(indexName)) {
            PropertyKey indexPropertyKey = mgmt.makePropertyKey(indexKey).dataType(String.class).make();
            mgmt.buildIndex(indexName, Vertex.class).addKey(indexPropertyKey).buildCompositeIndex();
        }
        mgmt.commit();
        // Wait for the index to become available
        ManagementSystem.awaitGraphIndexStatus((TitanGraph) newGraph, indexName).status(SchemaStatus.ENABLED).call();
    }

    public static void createUniqueIndex(final Graph newGraph, final String indexName, final String indexKey)
            throws InterruptedException {
        newGraph.tx().rollback(); // Never create new indexes while a transaction is active
        TitanManagement mgmt = ((TitanGraph) newGraph).openManagement();
        // Create index for zones.
        if (!mgmt.containsGraphIndex(indexName)) {
            PropertyKey indexPropertyKey = mgmt.makePropertyKey(indexKey).dataType(String.class).make();
            mgmt.buildIndex(indexName, Vertex.class).addKey(indexPropertyKey).unique().buildCompositeIndex();
        }
        mgmt.commit();
        // Wait for the index to become available
        ManagementSystem.awaitGraphIndexStatus((TitanGraph) newGraph, indexName).status(SchemaStatus.ENABLED).call();
    }

    public static void createTwoKeyUniqueCompositeIndex(final Graph newGraph, final String indexName,
            final String indexKey1, final String indexKey2) throws InterruptedException {
        newGraph.tx().rollback(); // Never create new indexes while a transaction is active
        TitanManagement mgmt = ((TitanGraph) newGraph).openManagement();
        // Create index for zones.
        if (!mgmt.containsGraphIndex(indexName)) {
            PropertyKey indexPropertyKey1 = mgmt.getPropertyKey(indexKey1);
            if (null == indexPropertyKey1) {
                indexPropertyKey1 = mgmt.makePropertyKey(indexKey1).dataType(String.class).make();
            }
            PropertyKey indexPropertyKey2 = mgmt.getPropertyKey(indexKey2);
            if (null == indexPropertyKey2) {
                indexPropertyKey2 = mgmt.makePropertyKey(indexKey2).dataType(String.class).make();
            }
            mgmt.buildIndex(indexName, Vertex.class).addKey(indexPropertyKey1).addKey(indexPropertyKey2).unique()
                    .buildCompositeIndex();
        }
        mgmt.commit();
        // Wait for the index to become available
        ManagementSystem.awaitGraphIndexStatus((TitanGraph) newGraph, indexName).status(SchemaStatus.ENABLED).call();
    }

    public static void createEdgeIndex(final Graph newGraph, final String indexName, final String label,
            final String indexKey) throws InterruptedException {
        newGraph.tx().rollback(); // Never create new indexes while a transaction is active
        TitanManagement mgmt = ((TitanGraph) newGraph).openManagement();
        if (!mgmt.containsGraphIndex(indexName)) {
            EdgeLabel edgeLabel = mgmt.getOrCreateEdgeLabel(label);
            PropertyKey indexPropertyKey = mgmt.getPropertyKey(indexKey);
            if (null == indexPropertyKey) {
                indexPropertyKey = mgmt.makePropertyKey(indexKey).dataType(String.class).make();
            }
            mgmt.buildEdgeIndex(edgeLabel, indexName, Direction.OUT, indexPropertyKey);
        }
        mgmt.commit();
        // Wait for the index to become available
        ManagementSystem.awaitRelationIndexStatus((TitanGraph) newGraph, indexName, label).status(SchemaStatus.ENABLED)
                .call();
    }

    public static void reIndex(final Graph newGraph, final String indexName)
            throws InterruptedException, ExecutionException {
        TitanManagement mgmt = ((TitanGraph) newGraph).openManagement();
        mgmt.updateIndex(mgmt.getGraphIndex(indexName), SchemaAction.REINDEX).get();
        newGraph.tx().commit();
    }

    @Bean
    Graph graph() {
        return this.graph;
    }
}
