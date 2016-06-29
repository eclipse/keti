package com.ge.predix.acs.privilege.management.dao;

import static com.ge.predix.acs.privilege.management.dao.GraphGenericRepository.PARENT_EDGE_LABEL;
import static com.ge.predix.acs.privilege.management.dao.GraphGenericRepository.SCOPE_PROPERTY_KEY;
import static com.ge.predix.acs.privilege.management.dao.GraphGenericRepository.ZONE_ID_KEY;
import static com.ge.predix.acs.privilege.management.dao.GraphResourceRepository.RESOURCE_ID_KEY;
import static com.ge.predix.acs.privilege.management.dao.GraphSubjectRepository.SUBJECT_ID_KEY;
import static com.ge.predix.acs.testutils.XFiles.AGENT_MULDER;
import static com.ge.predix.acs.testutils.XFiles.AGENT_SCULLY;
import static com.ge.predix.acs.testutils.XFiles.MULDERS_ATTRIBUTES;
import static com.ge.predix.acs.testutils.XFiles.PENTAGON_ATTRIBUTES;
import static com.ge.predix.acs.testutils.XFiles.SECRET_CLASSIFICATION;
import static com.ge.predix.acs.testutils.XFiles.SECRET_GROUP;
import static com.ge.predix.acs.testutils.XFiles.SECRET_GROUP_ATTRIBUTES;
import static com.ge.predix.acs.testutils.XFiles.SITE_BASEMENT;
import static com.ge.predix.acs.testutils.XFiles.SITE_PENTAGON;
import static com.ge.predix.acs.testutils.XFiles.TOP_SECRET_CLASSIFICATION;
import static com.ge.predix.acs.testutils.XFiles.TOP_SECRET_GROUP;
import static com.ge.predix.acs.testutils.XFiles.TOP_SECRET_GROUP_ATTRIBUTES;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
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

    private GraphSubjectRepository subjectRepository;
    private Graph graph;

    @BeforeClass
    public void setup() throws Exception {
        this.subjectRepository = new GraphSubjectRepository();
        setupTitanGraph();
        this.subjectRepository.setGraph(this.graph);
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
        SubjectEntity subjectEntityForZone1 = persistSubject1toZone1AndAssert();
        assertThat(IteratorUtils.count(this.graph.vertices()), equalTo(1L));
        SubjectEntity subjectEntityForZone2 = persistSubject1toZone2AndAssert();
        assertThat(IteratorUtils.count(this.graph.vertices()), equalTo(2L));

        SubjectEntity actualSubjectForZone1 = this.subjectRepository.getByZoneAndSubjectIdentifier(TEST_ZONE_1,
                AGENT_SCULLY);
        SubjectEntity actualSubjectForZone2 = this.subjectRepository.getByZoneAndSubjectIdentifier(TEST_ZONE_2,
                AGENT_SCULLY);
        assertThat(actualSubjectForZone1, equalTo(subjectEntityForZone1));
        assertThat(actualSubjectForZone2, equalTo(subjectEntityForZone2));
    }

    @Test
    public void testGetByZoneAndSubjectIdentifierAndScopes() {
        assertThat(IteratorUtils.count(this.graph.vertices()), equalTo(0L));
        SubjectEntity expectedSubject = persistScopedHierarchy(AGENT_MULDER, SITE_BASEMENT);
        String subjectIdentifier = expectedSubject.getSubjectIdentifier();
        assertThat(IteratorUtils.count(this.graph.vertices()), equalTo(3L));
        HashSet<Attribute> expectedAttributes = new HashSet<>(
                Arrays.asList(new Attribute[] { SECRET_CLASSIFICATION, TOP_SECRET_CLASSIFICATION, SITE_BASEMENT }));
        expectedSubject.setAttributes(expectedAttributes);
        expectedSubject.setAttributesAsJson(JSON_UTILS.serialize(expectedAttributes));
        SubjectEntity actualSubject = this.subjectRepository.getByZoneAndSubjectIdentifierAndScopes(TEST_ZONE_1,
                subjectIdentifier, new HashSet<>(Arrays.asList(new Attribute[] { SITE_BASEMENT })));
        assertThat(actualSubject, equalTo(expectedSubject));

        expectedAttributes = new HashSet<>(Arrays.asList(new Attribute[] { SECRET_CLASSIFICATION, SITE_BASEMENT }));
        expectedSubject.setAttributes(expectedAttributes);
        expectedSubject.setAttributesAsJson(JSON_UTILS.serialize(expectedAttributes));
        actualSubject = this.subjectRepository.getByZoneAndSubjectIdentifierAndScopes(TEST_ZONE_1, subjectIdentifier,
                new HashSet<>(Arrays.asList(new Attribute[] { SITE_PENTAGON })));
        assertThat(actualSubject, equalTo(expectedSubject));
    }

    @Test
    public void testParentAndChildSameAttribute() {
        assertThat(IteratorUtils.count(this.graph.vertices()), equalTo(0L));
        SubjectEntity agentScully = new SubjectEntity(TEST_ZONE_1, AGENT_SCULLY);
        agentScully.setAttributes(MULDERS_ATTRIBUTES);
        agentScully.setAttributesAsJson(JSON_UTILS.serialize(agentScully.getAttributes()));
        this.subjectRepository.save(agentScully);
        SubjectEntity agentMulder = new SubjectEntity(TEST_ZONE_1, AGENT_MULDER);
        agentMulder.setAttributes(MULDERS_ATTRIBUTES);
        agentMulder.setAttributesAsJson(JSON_UTILS.serialize(agentMulder.getAttributes()));
        agentMulder.setParents(
                new HashSet<>(Arrays.asList(new Parent[] { new Parent(agentScully.getSubjectIdentifier()) })));
        this.subjectRepository.save(agentMulder);
        SubjectEntity actualAgentMulder = this.subjectRepository.getByZoneAndSubjectIdentifierAndScopes(TEST_ZONE_1,
                AGENT_MULDER, null);
        assertThat(actualAgentMulder.getAttributes().size(), equalTo(1));
        assertThat(actualAgentMulder.getAttributesAsJson(),
                equalTo("[{\"issuer\":\"acs.example.org\",\"name\":\"site\",\"value\":\"basement\"}]"));
    }

    @Test
    public void testParentAndChildAttributeSameNameDifferentValues() {
        assertThat(IteratorUtils.count(this.graph.vertices()), equalTo(0L));
        SubjectEntity agentScully = new SubjectEntity(TEST_ZONE_1, AGENT_SCULLY);
        agentScully.setAttributes(PENTAGON_ATTRIBUTES);
        agentScully.setAttributesAsJson(JSON_UTILS.serialize(agentScully.getAttributes()));
        this.subjectRepository.save(agentScully);
        SubjectEntity agentMulder = new SubjectEntity(TEST_ZONE_1, AGENT_MULDER);
        agentMulder.setAttributes(MULDERS_ATTRIBUTES);
        agentMulder.setAttributesAsJson(JSON_UTILS.serialize(agentMulder.getAttributes()));
        agentMulder.setParents(
                new HashSet<>(Arrays.asList(new Parent[] { new Parent(agentScully.getSubjectIdentifier()) })));
        this.subjectRepository.save(agentMulder);
        SubjectEntity actualAgentMulder = this.subjectRepository.getByZoneAndSubjectIdentifierAndScopes(TEST_ZONE_1,
                AGENT_MULDER, null);
        assertThat(actualAgentMulder.getAttributes().size(), equalTo(2));
        System.out.println(actualAgentMulder.getAttributesAsJson());
        assertThat(actualAgentMulder.getAttributesAsJson(),
                equalTo("[{\"issuer\":\"acs.example.org\",\"name\":\"site\",\"value\":\"basement\"},"
                        + "{\"issuer\":\"acs.example.org\",\"name\":\"site\",\"value\":\"pentagon\"}]"));
    }

    @Test
    public void testSave() {
        assertThat(IteratorUtils.count(this.graph.vertices()), equalTo(0L));
        String subjectIdentifier = persistSubject1toZone1AndAssert().getSubjectIdentifier();
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
        SubjectEntity subject = new SubjectEntity(TEST_ZONE_1, AGENT_SCULLY);
        this.subjectRepository.save(subject);
        assertThat(IteratorUtils.count(this.graph.vertices()), equalTo(1L));
        assertThat(this.subjectRepository.getByZoneAndSubjectIdentifier(TEST_ZONE_1, AGENT_SCULLY), equalTo(subject));
    }

    @Test
    public void testSaveScopes() {
        assertThat(IteratorUtils.count(this.graph.vertices()), equalTo(0L));
        SubjectEntity subject = persistScopedHierarchy(AGENT_MULDER, SITE_BASEMENT);
        assertThat(IteratorUtils.count(this.graph.vertices()), equalTo(3L));
        assertThat(IteratorUtils.count(this.graph.traversal().V(subject.getId()).outE(PARENT_EDGE_LABEL)), equalTo(2L));

        // Persist again (i.e. update) and make sure vertex and edge count are stable.
        this.subjectRepository.save(subject);
        assertThat(IteratorUtils.count(this.graph.vertices()), equalTo(3L));
        assertThat(IteratorUtils.count(this.graph.traversal().V(subject.getId()).outE(PARENT_EDGE_LABEL)), equalTo(2L));
        assertThat("Expected scope not found on subject.", this.subjectRepository.findOne(subject.getId()).getParents()
                .iterator().next().getScopes().contains(SITE_BASEMENT));
    }

    private SubjectEntity persistSubject1toZone1AndAssert() {
        return persistSubjectToZoneAndAssert(TEST_ZONE_1, AGENT_SCULLY, Collections.emptySet());
    }

    private SubjectEntity persistSubject1toZone2AndAssert() {
        return persistSubjectToZoneAndAssert(TEST_ZONE_2, AGENT_SCULLY, Collections.emptySet());
    }

    private SubjectEntity persistTopSecretGroupAndAssert() {
        return persistSubjectToZoneAndAssert(TEST_ZONE_1, TOP_SECRET_GROUP, TOP_SECRET_GROUP_ATTRIBUTES);
    }

    private SubjectEntity persistSecretGroupAndAssert() {
        return persistSubjectToZoneAndAssert(TEST_ZONE_1, SECRET_GROUP, SECRET_GROUP_ATTRIBUTES);
    }

    private SubjectEntity persistSubjectToZoneAndAssert(final ZoneEntity zone, final String subjectIdentifier,
            final Set<Attribute> attributes) {
        SubjectEntity subject = new SubjectEntity(zone, subjectIdentifier);
        subject.setAttributes(attributes);
        subject.setAttributesAsJson(JSON_UTILS.serialize(subject.getAttributes()));
        SubjectEntity subjectEntity = this.subjectRepository.save(subject);
        assertThat(this.subjectRepository.findOne(subjectEntity.getId()), equalTo(subject));
        return subjectEntity;
    }

    private SubjectEntity persistScopedHierarchy(final String subjectIdentifier, final Attribute scope) {
        SubjectEntity secretGroup = persistSecretGroupAndAssert();
        SubjectEntity topSecretGroup = persistTopSecretGroupAndAssert();

        SubjectEntity agentMulder = new SubjectEntity(TEST_ZONE_1, subjectIdentifier);
        agentMulder.setAttributes(MULDERS_ATTRIBUTES);
        agentMulder.setAttributesAsJson(JSON_UTILS.serialize(agentMulder.getAttributes()));
        agentMulder.setParents(new HashSet<>(Arrays.asList(new Parent[] {
                new Parent(topSecretGroup.getSubjectIdentifier(),
                        new HashSet<>(Arrays.asList(new Attribute[] { scope }))),
                new Parent(secretGroup.getSubjectIdentifier()) })));
        return this.subjectRepository.save(agentMulder);
    }
}
