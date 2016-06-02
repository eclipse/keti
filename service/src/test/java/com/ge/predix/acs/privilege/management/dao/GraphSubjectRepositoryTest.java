package com.ge.predix.acs.privilege.management.dao;

import static com.ge.predix.acs.privilege.management.dao.GraphGenericRepository.PARENT_EDGE_LABEL;
import static com.ge.predix.acs.privilege.management.dao.GraphGenericRepository.SCOPE_PROPERTY_KEY;
import static com.ge.predix.acs.privilege.management.dao.GraphGenericRepository.ZONE_ID_KEY;
import static com.ge.predix.acs.privilege.management.dao.GraphResourceRepository.RESOURCE_ID_KEY;
import static com.ge.predix.acs.privilege.management.dao.GraphSubjectRepository.SUBJECT_ID_KEY;
import static com.ge.predix.acs.testutils.Constants.ATTR_CLASSIFICATION_0;
import static com.ge.predix.acs.testutils.Constants.ATTR_CLASSIFICATION_1;
import static com.ge.predix.acs.testutils.Constants.ATTR_SITE_0;
import static com.ge.predix.acs.testutils.Constants.ATTR_SITE_1;
import static com.ge.predix.acs.testutils.Constants.RESOURCE_XFILE_1_ID;
import static com.ge.predix.acs.testutils.Constants.RESOURCE_XFILE_2_ID;
import static com.ge.predix.acs.testutils.Constants.SUBJECT_GROUP_2_ATTRS_0;
import static com.ge.predix.acs.testutils.Constants.SUBJECT_GROUP_2_ID;
import static com.ge.predix.acs.testutils.Constants.SUBJECT_GROUP_3_ATTRS_0;
import static com.ge.predix.acs.testutils.Constants.SUBJECT_GROUP_3_ID;
import static com.ge.predix.acs.testutils.Constants.SUBJECT_USER_1_ATTRS_0;
import static com.ge.predix.acs.testutils.Constants.SUBJECT_USER_1_ID;
import static com.ge.predix.acs.testutils.Constants.SUBJECT_USER_2_ID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
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
import com.ge.predix.acs.model.Attribute;
import com.ge.predix.acs.rest.Parent;
import com.ge.predix.acs.utils.JsonUtils;
import com.ge.predix.acs.zone.management.dao.ZoneEntity;
import com.thinkaurelius.titan.core.TitanFactory;

public class GraphSubjectRepositoryTest {
    private static final JsonUtils JSON_UTILS = new JsonUtils();
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
        GraphConfig.createEdgeIndex(this.graph, GraphConfig.BY_SCOPE_INDEX_NAME, PARENT_EDGE_LABEL, SCOPE_PROPERTY_KEY);
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

        SubjectEntity subject = this.repository.getByZoneAndSubjectIdentifier(TEST_ZONE_1, SUBJECT_USER_2_ID);
        assertThat(subject.getSubjectIdentifier(), equalTo(SUBJECT_USER_2_ID));
    }

    @Test
    public void testGetByZoneAndSubjectIdentifierAndScopes() {
        assertThat(IteratorUtils.count(this.graph.vertices()), equalTo(0L));
        String subjectIdentifier = persistScopedHierarchy().getSubjectIdentifier();
        assertThat(IteratorUtils.count(this.graph.vertices()), equalTo(3L));

        SubjectEntity subject = this.repository.getByZoneAndIdentifierAndScopes(TEST_ZONE_1, subjectIdentifier,
                new HashSet<>(Arrays.asList(new Attribute[] { ATTR_SITE_0 })));
        assertThat(subject.getSubjectIdentifier(), equalTo(subjectIdentifier));
        assertThat(subject.getAttributes().contains(ATTR_CLASSIFICATION_0), equalTo(true));
        assertThat(subject.getAttributes().contains(ATTR_CLASSIFICATION_1), equalTo(true));

        subject = this.repository.getByZoneAndIdentifierAndScopes(TEST_ZONE_1, subjectIdentifier,
                new HashSet<>(Arrays.asList(new Attribute[] { ATTR_SITE_1 })));
        assertThat(subject.getSubjectIdentifier(), equalTo(subjectIdentifier));
        assertThat(subject.getAttributes().contains(ATTR_CLASSIFICATION_0), equalTo(false));
        assertThat(subject.getAttributes().contains(ATTR_CLASSIFICATION_1), equalTo(true));
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

    @Test
    public void testSaveWithNoAttributes() {
        assertThat(IteratorUtils.count(this.graph.vertices()), equalTo(0L));
        this.repository.save(new SubjectEntity(TEST_ZONE_1, SUBJECT_USER_2_ID));
        assertThat(IteratorUtils.count(this.graph.vertices()), equalTo(1L));
    }

    @Test
    public void testSaveScopes() {
        assertThat(IteratorUtils.count(this.graph.vertices()), equalTo(0L));
        SubjectEntity subject = persistScopedHierarchy();
        assertThat(IteratorUtils.count(this.graph.vertices()), equalTo(3L));
        assertThat(IteratorUtils.count(this.graph.traversal().V(subject.getId()).outE(PARENT_EDGE_LABEL)), equalTo(2L));

        // Persist again (i.e. update) and make sure vertex and edge count are stable.
        this.repository.save(subject);
        assertThat(IteratorUtils.count(this.graph.vertices()), equalTo(3L));
        assertThat(IteratorUtils.count(this.graph.traversal().V(subject.getId()).outE(PARENT_EDGE_LABEL)), equalTo(2L));
    }

    private SubjectEntity persistSubject1toZone1() {
        SubjectEntity subject = new SubjectEntity(TEST_ZONE_1, SUBJECT_USER_2_ID);
        subject.setAttributes(Collections.emptySet());
        subject.setAttributesAsJson(JSON_UTILS.serialize(subject.getAttributes()));
        return this.repository.save(subject);
    }

    private SubjectEntity persistSubject1toZone2() {
        SubjectEntity subject = new SubjectEntity(TEST_ZONE_2, SUBJECT_USER_2_ID);
        subject.setAttributes(Collections.emptySet());
        subject.setAttributesAsJson(JSON_UTILS.serialize(subject.getAttributes()));
        return this.repository.save(subject);
    }

    private SubjectEntity persistSubject3toZone1() {
        SubjectEntity subject = new SubjectEntity(TEST_ZONE_1, SUBJECT_GROUP_2_ID);
        subject.setAttributes(SUBJECT_GROUP_2_ATTRS_0);
        subject.setAttributesAsJson(JSON_UTILS.serialize(subject.getAttributes()));
        return this.repository.save(subject);
    }

    private SubjectEntity persistSubject4toZone1() {
        SubjectEntity subject = new SubjectEntity(TEST_ZONE_1, SUBJECT_GROUP_3_ID);
        subject.setAttributes(SUBJECT_GROUP_3_ATTRS_0);
        subject.setAttributesAsJson(JSON_UTILS.serialize(subject.getAttributes()));
        return this.repository.save(subject);
    }

    private SubjectEntity persistScopedHierarchy() {
        SubjectEntity parent = persistSubject4toZone1();
        SubjectEntity scopedParent = persistSubject3toZone1();

        SubjectEntity subject = new SubjectEntity(TEST_ZONE_1, SUBJECT_USER_1_ID);
        subject.setAttributes(SUBJECT_USER_1_ATTRS_0);
        subject.setAttributesAsJson(JSON_UTILS.serialize(subject.getAttributes()));
        subject.setParents(new HashSet<>(Arrays.asList(new Parent[] {
                new Parent(scopedParent.getSubjectIdentifier(),
                        new HashSet<>(Arrays.asList(new Attribute[] { ATTR_SITE_0 }))),
                new Parent(parent.getSubjectIdentifier()) })));
        return this.repository.save(subject);
    }
}
