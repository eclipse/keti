package com.ge.predix.acs.privilege.management.dao;

import static com.ge.predix.acs.privilege.management.dao.GraphGenericRepository.ZONE_ID_KEY;
import static com.ge.predix.acs.privilege.management.dao.GraphResourceRepository.RESOURCE_ID_KEY;
import static com.ge.predix.acs.privilege.management.dao.GraphSubjectRepository.SUBJECT_ID_KEY;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.util.concurrent.ExecutionException;

import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.util.iterator.IteratorUtils;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.ge.predix.acs.config.GraphConfig;
import com.ge.predix.acs.zone.management.dao.ZoneEntity;
import com.thinkaurelius.titan.core.TitanFactory;

public class GraphSubjectRepositoryTest {

    private static final String SUBJECT_1_ID = "scully";
    private static final ZoneEntity TEST_ZONE_1 = new ZoneEntity(1L, "testzone1");
    private static final ZoneEntity TEST_ZONE_2 = new ZoneEntity(2L, "testzone2");

    GraphSubjectRepository repository;
    Graph graph;

    @BeforeClass
    public void setup() throws Exception {
        this.repository = new GraphSubjectRepository();
        setupTitanGraph();
        this.repository.setGraph(this.graph);
    }

    private void setupTitanGraph() throws InterruptedException, ExecutionException {
        this.graph = TitanFactory.build().set("storage.backend", "inmemory").open();
        GraphConfig.createIndex(this.graph, GraphConfig.BY_ZONE_INDEX_NAME, ZONE_ID_KEY);
        GraphConfig.createTwoKeyUniqueCompositeIndex(this.graph, GraphConfig.BY_ZONE_AND_RESOURCE_UNIQUE_INDEX_NAME,
                ZONE_ID_KEY, RESOURCE_ID_KEY);
        GraphConfig.createTwoKeyUniqueCompositeIndex(this.graph, GraphConfig.BY_ZONE_AND_SUBJECT_UNIQUE_INDEX_NAME,
                ZONE_ID_KEY, SUBJECT_ID_KEY);
    }

    @BeforeMethod
    public void setupTest() {
        // Drop all vertices.
        this.graph.traversal().V().drop().iterate();
    }

    @Test
    public void testGetByZoneAndSubjectIdentifier() {
        assertThat(IteratorUtils.count(this.graph.vertices()), equalTo(0L));
        persistSubject1toZone1();
        assertThat(IteratorUtils.count(this.graph.vertices()), equalTo(1L));
        persistSubject1toZone2();
        assertThat(IteratorUtils.count(this.graph.vertices()), equalTo(2L));

        SubjectEntity subject = this.repository.getByZoneAndSubjectIdentifier(TEST_ZONE_1, SUBJECT_1_ID);
        assertThat(subject.getSubjectIdentifier(), equalTo(SUBJECT_1_ID));
    }

    @Test
    public void testSave() {
        assertThat(IteratorUtils.count(this.graph.vertices()), equalTo(0L));
        String subjectIdentifier = persistSubject1toZone1().getSubjectIdentifier();
        assertThat(IteratorUtils.count(this.graph.vertices()), equalTo(1L));

        GraphTraversalSource g = this.graph.traversal();
        String subjectId = subjectIdentifier;
        GraphTraversal<Vertex, Vertex> traversal = g.V().has(SUBJECT_ID_KEY, subjectId);
        assertThat(traversal.hasNext(), equalTo(true));
        assertThat(traversal.next().property(SUBJECT_ID_KEY).value(), equalTo(subjectId));
    }

    private SubjectEntity persistSubject1toZone1() {
        SubjectEntity subject = new SubjectEntity(TEST_ZONE_1, SUBJECT_1_ID);
        return this.repository.save(subject);
    }

    private SubjectEntity persistSubject1toZone2() {
        SubjectEntity subject = new SubjectEntity(TEST_ZONE_2, SUBJECT_1_ID);
        return this.repository.save(subject);
    }
}
