package com.ge.predix.acs.privilege.management.dao;

import static com.ge.predix.acs.privilege.management.dao.GraphGenericRepository.PARENT_EDGE_LABEL;
import static com.ge.predix.acs.privilege.management.dao.GraphGenericRepository.SCOPE_PROPERTY_KEY;
import static com.ge.predix.acs.privilege.management.dao.GraphGenericRepository.ZONE_ID_KEY;
import static com.ge.predix.acs.privilege.management.dao.GraphResourceRepository.RESOURCE_ID_KEY;
import static com.ge.predix.acs.privilege.management.dao.GraphSubjectRepository.SUBJECT_ID_KEY;
import static com.ge.predix.acs.testutils.Constants.ATTR_CLASSIFICATION_0;
import static com.ge.predix.acs.testutils.Constants.ATTR_SITE_0;
import static com.ge.predix.acs.testutils.Constants.ATTR_TYPE_1;
import static com.ge.predix.acs.testutils.Constants.RESOURCE_EVIDENCE_1_ATTRS_0;
import static com.ge.predix.acs.testutils.Constants.RESOURCE_EVIDENCE_1_ID;
import static com.ge.predix.acs.testutils.Constants.RESOURCE_SITE_0_ATTRS_0;
import static com.ge.predix.acs.testutils.Constants.RESOURCE_SITE_0_ID;
import static com.ge.predix.acs.testutils.Constants.RESOURCE_XFILE_1_ATTRS_0;
import static com.ge.predix.acs.testutils.Constants.RESOURCE_XFILE_1_ID;
import static com.ge.predix.acs.testutils.Constants.RESOURCE_XFILE_2_ID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.apache.commons.lang.time.StopWatch;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.util.iterator.IteratorUtils;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.ge.predix.acs.config.GraphConfig;
import com.ge.predix.acs.rest.Parent;
import com.ge.predix.acs.utils.JsonUtils;
import com.ge.predix.acs.zone.management.dao.ZoneEntity;
import com.thinkaurelius.titan.core.SchemaViolationException;
import com.thinkaurelius.titan.core.TitanFactory;

public class GraphResourceRepositoryTest {
    private static final JsonUtils JSON_UTILS = new JsonUtils();
    private static final ZoneEntity TEST_ZONE_1 = new ZoneEntity(1L, "testzone1");
    private static final ZoneEntity TEST_ZONE_2 = new ZoneEntity(2L, "testzone2");

    GraphResourceRepository resourceRepository;
    Graph graph;

    @BeforeClass
    public void setup() throws Exception {
        this.resourceRepository = new GraphResourceRepository();
        setupTitanGraph();
        this.resourceRepository.setGraph(this.graph);
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
    public void testCount() {
        assertThat(IteratorUtils.count(this.graph.vertices()), equalTo(0L));
        persistResource1toZone1();
        assertThat(IteratorUtils.count(this.graph.vertices()), equalTo(1L));
        persistResource2toZone1();
        assertThat(IteratorUtils.count(this.graph.vertices()), equalTo(2L));
        assertThat(this.resourceRepository.count(), equalTo(2L));
    }

    @Test
    public void testDelete() {
        assertThat(IteratorUtils.count(this.graph.vertices()), equalTo(0L));
        long id = persistResource1toZone1().getId();
        assertThat(IteratorUtils.count(this.graph.vertices()), equalTo(1L));
        this.resourceRepository.delete(id);
        assertThat(IteratorUtils.count(this.graph.vertices()), equalTo(0L));
    }

    @Test
    public void testDeleteMultiple() {
        List<ResourceEntity> resourceEntities = new ArrayList<>();
        assertThat(IteratorUtils.count(this.graph.vertices()), equalTo(0L));
        ResourceEntity resourceEntity1 = new ResourceEntity();
        resourceEntity1.setId(persistResource1toZone1().getId());
        resourceEntities.add(resourceEntity1);
        assertThat(IteratorUtils.count(this.graph.vertices()), equalTo(1L));
        ResourceEntity resourceEntity2 = new ResourceEntity();
        resourceEntity2.setId(persistResource2toZone1().getId());
        resourceEntities.add(resourceEntity2);
        assertThat(IteratorUtils.count(this.graph.vertices()), equalTo(2L));
        this.resourceRepository.delete(resourceEntities);
        assertThat(IteratorUtils.count(this.graph.vertices()), equalTo(0L));
    }

    @Test
    public void testDeleteAll() {
        assertThat(IteratorUtils.count(this.graph.vertices()), equalTo(0L));
        persistResource1toZone1();
        assertThat(IteratorUtils.count(this.graph.vertices()), equalTo(1L));
        persistResource2toZone1();
        assertThat(IteratorUtils.count(this.graph.vertices()), equalTo(2L));
        this.resourceRepository.deleteAll();
        assertThat(IteratorUtils.count(this.graph.vertices()), equalTo(0L));
    }

    @Test
    public void testFindAll() {
        assertThat(IteratorUtils.count(this.graph.vertices()), equalTo(0L));
        persistResource1toZone1();
        assertThat(IteratorUtils.count(this.graph.vertices()), equalTo(1L));
        persistResource2toZone1();
        assertThat(IteratorUtils.count(this.graph.vertices()), equalTo(2L));

        List<ResourceEntity> resources = this.resourceRepository.findAll();
        assertThat(resources.size(), equalTo(2));
    }

    @Test
    public void testFindByZone() {
        assertThat(IteratorUtils.count(this.graph.vertices()), equalTo(0L));
        persistResource1toZone1();
        assertThat(IteratorUtils.count(this.graph.vertices()), equalTo(1L));
        persistResource2toZone1();
        assertThat(IteratorUtils.count(this.graph.vertices()), equalTo(2L));

        List<ResourceEntity> resources = this.resourceRepository.findByZone(TEST_ZONE_1);
        assertThat(resources.size(), equalTo(2));
        for (ResourceEntity resource : resources) {
            assertThat(resource.getZone().getName(), equalTo(TEST_ZONE_1.getName()));
            assertThat(resource.getResourceIdentifier(), notNullValue());
            assertThat(resource.getAttributesAsJson(), equalTo(JSON_UTILS.serialize(RESOURCE_XFILE_1_ATTRS_0)));
        }
    }

    @Test
    public void testGetByZoneAndResourceIdentifier() {
        assertThat(IteratorUtils.count(this.graph.vertices()), equalTo(0L));
        persistResource1toZone1();
        assertThat(IteratorUtils.count(this.graph.vertices()), equalTo(1L));
        persistResource1toZone2();
        assertThat(IteratorUtils.count(this.graph.vertices()), equalTo(2L));

        ResourceEntity resource = this.resourceRepository.getByZoneAndResourceIdentifier(TEST_ZONE_1,
                RESOURCE_XFILE_1_ID);
        assertThat(resource.getResourceIdentifier(), equalTo(RESOURCE_XFILE_1_ID));
    }

    @Test
    public void testGetByZoneAndResourceIdentifier2LevelHierarchical() {
        assertThat(IteratorUtils.count(this.graph.vertices()), equalTo(0L));
        String resourceIdentifier = persist2LevelHierarchicalResource1toZone1().getResourceIdentifier();
        assertThat(IteratorUtils.count(this.graph.vertices()), equalTo(2L));

        ResourceEntity resource = this.resourceRepository.getByZoneAndResourceIdentifier(TEST_ZONE_1,
                resourceIdentifier);
        assertThat(resource.getAttributes().contains(ATTR_SITE_0), equalTo(true));
        assertThat(resource.getAttributes().contains(ATTR_TYPE_1), equalTo(true));
    }

    @Test
    public void testGetByZoneAndResourceIdentifier3LevelHierarchical() {
        assertThat(IteratorUtils.count(this.graph.vertices()), equalTo(0L));
        String resourceIdentifier = persist3LevelHierarchicalResource1toZone1().getResourceIdentifier();
        assertThat(IteratorUtils.count(this.graph.vertices()), equalTo(3L));

        ResourceEntity resource = this.resourceRepository.getByZoneAndResourceIdentifier(TEST_ZONE_1,
                resourceIdentifier);
        assertThat(resource.getAttributes().contains(ATTR_SITE_0), equalTo(true));
        assertThat(resource.getAttributes().contains(ATTR_TYPE_1), equalTo(true));
        assertThat(resource.getAttributes().contains(ATTR_CLASSIFICATION_0), equalTo(true));
    }

    @Test(expectedExceptions = SchemaViolationException.class)
    public void testPreventEntityParentSelfReference() {
        assertThat(IteratorUtils.count(this.graph.vertices()), equalTo(0L));
        ResourceEntity resource = new ResourceEntity(TEST_ZONE_1, RESOURCE_SITE_0_ID);
        resource.setAttributes(RESOURCE_SITE_0_ATTRS_0);
        resource.setAttributesAsJson(JSON_UTILS.serialize(resource.getAttributes()));
        resource.setParents(new HashSet<>(Arrays.asList(new Parent(RESOURCE_SITE_0_ID))));
        this.resourceRepository.save(resource);
    }

    /**
     * First we setup a 3 level graph where 3 -> 2 -> 1. Next, we try to update vertex 1 so that 1 -> 3. We expect
     * this to result in a SchemaViolationException because it would introduce a cyclic reference.
     */
    @Test
    public void testPreventEntityParentCyclicReference() {
        assertThat(IteratorUtils.count(this.graph.vertices()), equalTo(0L));
        ResourceEntity resource1 = persistResource0toZone1();

        ResourceEntity resource2 = new ResourceEntity(TEST_ZONE_1, RESOURCE_XFILE_1_ID);
        resource2.setAttributes(RESOURCE_XFILE_1_ATTRS_0);
        resource2.setAttributesAsJson(JSON_UTILS.serialize(resource2.getAttributes()));
        resource2.setParents(
                new HashSet<Parent>(Arrays.asList(new Parent[] { new Parent(resource1.getResourceIdentifier()) })));
        this.resourceRepository.save(resource2);

        ResourceEntity resource3 = new ResourceEntity(TEST_ZONE_1, RESOURCE_EVIDENCE_1_ID);
        resource3.setAttributes(RESOURCE_EVIDENCE_1_ATTRS_0);
        resource3.setAttributesAsJson(JSON_UTILS.serialize(resource3.getAttributes()));
        resource3.setParents(
                new HashSet<Parent>(Arrays.asList(new Parent[] { new Parent(resource2.getResourceIdentifier()) })));
        this.resourceRepository.save(resource3);

        assertThat(IteratorUtils.count(this.graph.vertices()), equalTo(3L));

        resource1.setParents(
                new HashSet<>(Arrays.asList(new Parent[] { new Parent(resource3.getResourceIdentifier()) })));
        try {
            this.resourceRepository.save(resource1);
        } catch (SchemaViolationException ex) {
            assertThat(ex.getMessage(), equalTo(
                    "Updating entity '/site/basement' with parent '/evidence/implant' introduces a cyclic reference."));
            return;
        }
        Assert.fail("save() did not throw the expected exception.");
    }

    @Test
    public void testSave() {
        assertThat(IteratorUtils.count(this.graph.vertices()), equalTo(0L));
        String resourceIdentifier = persistResource1toZone1().getResourceIdentifier();
        assertThat(IteratorUtils.count(this.graph.vertices()), equalTo(1L));

        GraphTraversalSource g = this.graph.traversal();
        String resourceId = resourceIdentifier;
        GraphTraversal<Vertex, Vertex> traversal = g.V().has(RESOURCE_ID_KEY, resourceId);
        assertThat(traversal.hasNext(), equalTo(true));
        assertThat(traversal.next().property(RESOURCE_ID_KEY).value(), equalTo(resourceId));
    }

    @Test
    public void testSaveHierarchical() {
        assertThat(IteratorUtils.count(this.graph.vertices()), equalTo(0L));
        String resourceIdentifier = persist2LevelHierarchicalResource1toZone1().getResourceIdentifier();
        assertThat(IteratorUtils.count(this.graph.vertices()), equalTo(2L));

        GraphTraversalSource g = this.graph.traversal();
        String resourceId = resourceIdentifier;
        GraphTraversal<Vertex, Vertex> traversal = g.V().has(RESOURCE_ID_KEY, resourceId);
        assertThat(traversal.hasNext(), equalTo(true));
        Vertex resourceVertex = traversal.next();
        assertThat(resourceVertex.property(RESOURCE_ID_KEY).value(), equalTo(resourceId));

        traversal = this.graph.traversal().V(resourceVertex.id()).out("parent").has(RESOURCE_ID_KEY,
                RESOURCE_SITE_0_ID);
        assertThat(traversal.hasNext(), equalTo(true));
        resourceVertex = traversal.next();
        assertThat(resourceVertex.property(RESOURCE_ID_KEY).value(), equalTo(RESOURCE_SITE_0_ID));
    }

    @Test
    public void testUpdateAttachedEntity() {
        assertThat(IteratorUtils.count(this.graph.vertices()), equalTo(0L));
        ResourceEntity resourceEntity = persistResource1toZone1();
        String resourceIdentifier = resourceEntity.getResourceIdentifier();
        assertThat(IteratorUtils.count(this.graph.vertices()), equalTo(1L));

        GraphTraversalSource g = this.graph.traversal();
        String resourceId = resourceIdentifier;
        GraphTraversal<Vertex, Vertex> traversal = g.V().has(RESOURCE_ID_KEY, resourceId);
        assertThat(traversal.hasNext(), equalTo(true));
        assertThat(traversal.next().property(RESOURCE_ID_KEY).value(), equalTo(resourceId));

        // Update the resource.
        resourceEntity.setAttributesAsJson("{\'status':'test'}");
        this.resourceRepository.save(resourceEntity);
        assertThat(IteratorUtils.count(this.graph.vertices()), equalTo(1L));
    }

    @Test(expectedExceptions = SchemaViolationException.class)
    public void testUpdateUnattachedEntity() {
        assertThat(IteratorUtils.count(this.graph.vertices()), equalTo(0L));
        String resourceIdentifier = persistResource1toZone1().getResourceIdentifier();
        assertThat(IteratorUtils.count(this.graph.vertices()), equalTo(1L));

        GraphTraversalSource g = this.graph.traversal();
        String resourceId = resourceIdentifier;
        GraphTraversal<Vertex, Vertex> traversal = g.V().has(RESOURCE_ID_KEY, resourceId);
        assertThat(traversal.hasNext(), equalTo(true));
        assertThat(traversal.next().property(RESOURCE_ID_KEY).value(), equalTo(resourceId));

        // Save a new resource which violates the uniqueness constraint.
        persistResource1toZone1();
    }

    @Test
    public void testSaveMultiple() {
        List<ResourceEntity> resourceEntitiesToSave = new ArrayList<>();
        resourceEntitiesToSave.add(new ResourceEntity(TEST_ZONE_1, RESOURCE_XFILE_1_ID));
        resourceEntitiesToSave.add(new ResourceEntity(TEST_ZONE_1, RESOURCE_XFILE_2_ID));
        this.resourceRepository.save(resourceEntitiesToSave);
        assertThat(this.resourceRepository.count(), equalTo(2L));
    }

    @Test
    public void testSaveResourceWithNonexistentParent() {
        assertThat(IteratorUtils.count(this.graph.vertices()), equalTo(0L));
        ResourceEntity resource = new ResourceEntity(TEST_ZONE_1, RESOURCE_XFILE_1_ID);
        resource.setParents(new HashSet<>(Arrays.asList(new Parent(RESOURCE_SITE_0_ID))));

        // Save a resource with nonexistent parent which should throw IllegalStateException exception 
        // while saving parent relationships.
        try {
            this.resourceRepository.save(resource);
        } catch (IllegalStateException ex) {
            assertThat(ex.getMessage(), equalTo(
                    "No parent exists in zone 'testzone1' with 'resourceId' value of '/site/basement'."));
            return;
        }
        Assert.fail("save() did not throw the expected IllegalStateException exception.");
    }

    @Test(enabled = false)
    public void testPerformance() throws Exception {
        for (int i = 0; i < 100000; i++) {
            StopWatch stopWatch = new StopWatch();
            stopWatch.start();
            this.resourceRepository.save(new ResourceEntity(TEST_ZONE_1, String.format("/x-files/%d", i)));
            this.resourceRepository.getByZoneAndResourceIdentifier(TEST_ZONE_1, String.format("/x-files/%d", i));
            stopWatch.stop();
            System.out.println(String.format("Test %d took: %d ms", i, stopWatch.getTime()));
            Thread.sleep(1L);
        }
    }

    public ResourceEntity persist2LevelHierarchicalResource1toZone1() {
        ResourceEntity parentResource = persistResource0toZone1();

        ResourceEntity resource = new ResourceEntity(TEST_ZONE_1, RESOURCE_XFILE_1_ID);
        resource.setAttributes(RESOURCE_XFILE_1_ATTRS_0);
        resource.setAttributesAsJson(JSON_UTILS.serialize(resource.getAttributes()));
        resource.setParents(new HashSet<Parent>(
                Arrays.asList(new Parent[] { new Parent(parentResource.getResourceIdentifier()) })));
        return this.resourceRepository.save(resource);
    }

    public ResourceEntity persist3LevelHierarchicalResource1toZone1() {
        ResourceEntity parentResource = persist2LevelHierarchicalResource1toZone1();

        ResourceEntity resource = new ResourceEntity(TEST_ZONE_1, RESOURCE_EVIDENCE_1_ID);
        resource.setAttributes(RESOURCE_EVIDENCE_1_ATTRS_0);
        resource.setAttributesAsJson(JSON_UTILS.serialize(resource.getAttributes()));
        resource.setParents(new HashSet<Parent>(
                Arrays.asList(new Parent[] { new Parent(parentResource.getResourceIdentifier()) })));
        return this.resourceRepository.save(resource);
    }

    private ResourceEntity persistResource0toZone1() {
        ResourceEntity resource = new ResourceEntity(TEST_ZONE_1, RESOURCE_SITE_0_ID);
        resource.setAttributes(RESOURCE_SITE_0_ATTRS_0);
        resource.setAttributesAsJson(JSON_UTILS.serialize(resource.getAttributes()));
        return this.resourceRepository.save(resource);
    }

    private ResourceEntity persistResource1toZone1() {
        ResourceEntity resource = new ResourceEntity(TEST_ZONE_1, RESOURCE_XFILE_1_ID);
        resource.setAttributes(RESOURCE_XFILE_1_ATTRS_0);
        resource.setAttributesAsJson(JSON_UTILS.serialize(resource.getAttributes()));
        return this.resourceRepository.save(resource);
    }

    private ResourceEntity persistResource2toZone1() {
        ResourceEntity resource = new ResourceEntity(TEST_ZONE_1, RESOURCE_XFILE_2_ID);
        resource.setAttributes(RESOURCE_XFILE_1_ATTRS_0);
        resource.setAttributesAsJson(JSON_UTILS.serialize(resource.getAttributes()));
        return this.resourceRepository.save(resource);
    }

    private ResourceEntity persistResource1toZone2() {
        ResourceEntity resource = new ResourceEntity(TEST_ZONE_2, RESOURCE_XFILE_1_ID);
        resource.setAttributes(RESOURCE_XFILE_1_ATTRS_0);
        resource.setAttributesAsJson(JSON_UTILS.serialize(resource.getAttributes()));
        return this.resourceRepository.save(resource);
    }
}
