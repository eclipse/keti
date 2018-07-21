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

package org.eclipse.keti.acs.config

import org.apache.commons.lang.StringUtils
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource
import org.apache.tinkerpop.gremlin.structure.Direction
import org.apache.tinkerpop.gremlin.structure.Graph
import org.apache.tinkerpop.gremlin.structure.Vertex
import org.eclipse.keti.acs.privilege.management.dao.PARENT_EDGE_LABEL
import org.eclipse.keti.acs.privilege.management.dao.RESOURCE_ID_KEY
import org.eclipse.keti.acs.privilege.management.dao.RESOURCE_LABEL
import org.eclipse.keti.acs.privilege.management.dao.SCOPE_PROPERTY_KEY
import org.eclipse.keti.acs.privilege.management.dao.SUBJECT_ID_KEY
import org.eclipse.keti.acs.privilege.management.dao.SUBJECT_LABEL
import org.eclipse.keti.acs.privilege.management.dao.VERSION_PROPERTY_KEY
import org.eclipse.keti.acs.privilege.management.dao.VERSION_VERTEX_LABEL
import org.eclipse.keti.acs.privilege.management.dao.ZONE_ID_KEY
import org.janusgraph.core.JanusGraph
import org.janusgraph.core.JanusGraphFactory
import org.janusgraph.core.PropertyKey
import org.janusgraph.core.schema.JanusGraphManagement
import org.janusgraph.core.schema.SchemaAction
import org.janusgraph.core.schema.SchemaStatus
import org.janusgraph.graphdb.database.management.ManagementSystem
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.util.Assert
import java.util.Arrays
import java.util.concurrent.ExecutionException
import javax.annotation.PostConstruct

private val LOGGER = LoggerFactory.getLogger(GraphConfig::class.java)

const val BY_SCOPE_INDEX_NAME = "byScopeIndex"
const val BY_SUBJECT_UNIQUE_INDEX_NAME = "bySubjectUnique"
const val BY_RESOURCE_UNIQUE_INDEX_NAME = "byResourceUnique"
const val BY_ZONE_INDEX_NAME = "byZone"
const val BY_VERSION_UNIQUE_INDEX_NAME = "byVersion"
const val BY_ZONE_AND_RESOURCE_UNIQUE_INDEX_NAME = "byZoneAndResourceUnique"
const val BY_ZONE_AND_SUBJECT_UNIQUE_INDEX_NAME = "byZoneAndSubjectUnique"

@Throws(InterruptedException::class)
fun createSchemaElements(newGraph: Graph) {
    createVertexLabel(newGraph, RESOURCE_LABEL)
    createVertexLabel(newGraph, SUBJECT_LABEL)
    createVertexLabel(newGraph, VERSION_VERTEX_LABEL)

    createIndex(newGraph, BY_ZONE_INDEX_NAME, ZONE_ID_KEY)
    createUniqueCompositeIndex(
        newGraph, BY_ZONE_AND_RESOURCE_UNIQUE_INDEX_NAME, arrayOf(ZONE_ID_KEY, RESOURCE_ID_KEY)
    )

    createUniqueCompositeIndex(
        newGraph, BY_ZONE_AND_SUBJECT_UNIQUE_INDEX_NAME, arrayOf(ZONE_ID_KEY, SUBJECT_ID_KEY)
    )

    createUniqueCompositeIndex(
        newGraph, BY_VERSION_UNIQUE_INDEX_NAME, arrayOf(VERSION_PROPERTY_KEY), Integer::class.java
    )

    createEdgeIndex(
        newGraph, BY_SCOPE_INDEX_NAME, PARENT_EDGE_LABEL, SCOPE_PROPERTY_KEY
    )
}

@Throws(InterruptedException::class)
fun createIndex(
    newGraph: Graph,
    indexName: String,
    indexKey: String
) {
    newGraph.tx().rollback() // Never create new indexes while a transaction is active
    val mgmt = (newGraph as JanusGraph).openManagement()
    if (!mgmt.containsGraphIndex(indexName)) {
        val indexPropertyKey = mgmt.makePropertyKey(indexKey).dataType(String::class.java).make()
        mgmt.buildIndex(indexName, Vertex::class.java).addKey(indexPropertyKey).buildCompositeIndex()
    }
    mgmt.commit()
    // Wait for the index to become available
    ManagementSystem.awaitGraphIndexStatus(newGraph, indexName).status(SchemaStatus.ENABLED).call()
}

@Throws(InterruptedException::class)
fun createUniqueIndexForLabel(
    newGraph: Graph,
    indexName: String,
    indexKey: String,
    label: String
) {
    newGraph.tx().rollback() // Never create new indexes while a transaction is active
    val mgmt = (newGraph as JanusGraph).openManagement()
    if (!mgmt.containsGraphIndex(indexName)) {
        val indexPropertyKey = mgmt.makePropertyKey(indexKey).dataType(Integer::class.java).make()
        val versionLabel = mgmt.makeVertexLabel(label).make()
        // Create a unique composite index for the property key that indexes only vertices with a given label
        mgmt.buildIndex(indexName, Vertex::class.java).addKey(indexPropertyKey).indexOnly(versionLabel).unique()
            .buildCompositeIndex()
    }
    mgmt.commit()
    // Wait for the index to become available
    ManagementSystem.awaitGraphIndexStatus(newGraph, indexName).status(SchemaStatus.ENABLED).call()
}

@Throws(InterruptedException::class)
private fun createUniqueCompositeIndex(
    newGraph: Graph,
    indexName: String,
    propertyKeyNames: Array<String>
) {
    createUniqueCompositeIndex(newGraph, indexName, propertyKeyNames, String::class.java)
}

@Throws(InterruptedException::class)
fun createUniqueCompositeIndex(
    newGraph: Graph,
    indexName: String,
    propertyKeyNames: Array<String>,
    propertyKeyType: Class<*>
) {

    Assert.notEmpty(propertyKeyNames)
    newGraph.tx().rollback() // Never create new indexes while a transaction is active
    val mgmt = (newGraph as JanusGraph).openManagement()
    val indexBuilder = mgmt.buildIndex(indexName, Vertex::class.java)
    if (!mgmt.containsGraphIndex(indexName)) {
        for (propertyKeyName in propertyKeyNames) {
            val indexPropertyKey = getOrCreatePropertyKey(propertyKeyName, propertyKeyType, mgmt)
            indexBuilder.addKey(indexPropertyKey)
        }
        indexBuilder.unique().buildCompositeIndex()
    }
    mgmt.commit()
    // Wait for the index to become available
    ManagementSystem.awaitGraphIndexStatus(newGraph, indexName).status(SchemaStatus.ENABLED).call()
}

private fun getOrCreatePropertyKey(
    keyName: String,
    keyType: Class<*>,
    mgmt: JanusGraphManagement
): PropertyKey? {
    var propertyKey: PropertyKey? = mgmt.getPropertyKey(keyName)
    if (null == propertyKey) {
        propertyKey = mgmt.makePropertyKey(keyName).dataType(keyType).make()
    }
    return propertyKey
}

@Throws(InterruptedException::class)
fun createEdgeIndex(
    newGraph: Graph,
    indexName: String,
    label: String,
    indexKey: String
) {
    newGraph.tx().rollback() // Never create new indexes while a transaction is active
    val mgmt = (newGraph as JanusGraph).openManagement()
    val edgeLabel = mgmt.getOrCreateEdgeLabel(label)
    if (!mgmt.containsRelationIndex(edgeLabel, indexName)) {
        val indexPropertyKey = getOrCreatePropertyKey(indexKey, String::class.java, mgmt)
        mgmt.buildEdgeIndex(edgeLabel, indexName, Direction.OUT, indexPropertyKey)
    }
    mgmt.commit()
    // Wait for the index to become available
    ManagementSystem.awaitRelationIndexStatus(newGraph, indexName, label).status(SchemaStatus.ENABLED).call()
}

@Throws(InterruptedException::class)
fun createVertexLabel(
    newGraph: Graph,
    vertexLabel: String
) {
    newGraph.tx().rollback()
    val mgmt = (newGraph as JanusGraph).openManagement()
    mgmt.getOrCreateVertexLabel(vertexLabel)
    mgmt.commit()
}

@Throws(InterruptedException::class, ExecutionException::class)
fun reIndex(
    newGraph: Graph,
    indexName: String
) {
    val mgmt = (newGraph as JanusGraph).openManagement()
    mgmt.updateIndex(mgmt.getGraphIndex(indexName), SchemaAction.REINDEX).get()
    newGraph.tx().commit()
}

@Configuration
@Profile("graph")
class GraphConfig {

    @Value("\${GRAPH_ENABLE_CASSANDRA:false}")
    private var cassandraEnabled: Boolean = false
    @Value("\${GRAPH_STORAGE_CASSANDRA_KEYSPACE:graph}")
    private var cassandraKeyspace: String = "graph"
    @Value("\${GRAPH_STORAGE_HOSTNAME:localhost}")
    private var hostname: String = "localhost"
    @Value("\${GRAPH_STORAGE_USERNAME:}")
    private var username: String? = null
    @Value("\${GRAPH_STORAGE_PASSWORD:}")
    private var password: String? = null
    @Value("\${GRAPH_STORAGE_PORT:9160}")
    private var port: Int = 9160
    @Value("\${GRAPH_ENABLE_CACHE:true}")
    private var cacheEnabled: Boolean = true
    @Value("\${GRAPH_CACHE_CLEAN_WAIT:50}")
    private var graphCacheCleanWait: Int = 50
    @Value("\${GRAPH_CACHE_TIME:1000}")
    private var graphCacheTime: Int = 1000
    @Value("\${GRAPH_CACHE_SIZE:0.25}")
    private var graphCacheSize: Double = 0.25

    private lateinit var graph: Graph

    @PostConstruct
    @Throws(InterruptedException::class)
    fun init() {
        this.graph = createGraph()
    }

    @Throws(InterruptedException::class)
    fun createGraph(): Graph {
        val newGraph: Graph
        if (this.cassandraEnabled) {
            var graphBuilder = JanusGraphFactory.build().set("storage.backend", "cassandra")
                .set("storage.cassandra.keyspace", this.cassandraKeyspace).set(
                    "storage.hostname",
                    Arrays.asList(*hostname.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray())
                ).set("storage.port", this.port).set("cache.db-cache", this.cacheEnabled)
                .set("cache.db-cache-clean-wait", this.graphCacheCleanWait)
                .set("cache.db-cache-time", this.graphCacheTime).set("cache.db-cache-size", this.graphCacheSize)
            if (StringUtils.isNotEmpty(this.username)) {
                graphBuilder = graphBuilder.set("storage.username", this.username)
            }
            if (StringUtils.isNotEmpty(this.password)) {
                graphBuilder = graphBuilder.set("storage.password", this.password)
            }
            newGraph = graphBuilder.open()
        } else {
            newGraph = JanusGraphFactory.build().set("storage.backend", "inmemory").open()
        }
        createSchemaElements(newGraph)
        LOGGER.info("Initialized graph db.")
        return newGraph
    }

    @Bean
    internal fun graphTraversal(): GraphTraversalSource {
        return this.graph.traversal()
    }
}
