package com.ge.predix.acs.privilege.management.dao;

import static com.ge.predix.acs.privilege.management.dao.GraphGenericRepository.ZONE_ID_KEY;
import static com.ge.predix.acs.privilege.management.dao.GraphResourceRepository.RESOURCE_ID_KEY;
import static com.ge.predix.acs.privilege.management.dao.GraphSubjectRepository.SUBJECT_ID_KEY;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.apache.commons.lang.time.StopWatch;
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
import com.thinkaurelius.titan.core.SchemaViolationException;
import com.thinkaurelius.titan.core.TitanFactory;

public class GraphResourceRepositoryTest {

    private static final String RESOURCE_1_ID = "/x-files/drive";
    private static final String RESOURCE_2_ID = "/x-files/josechung";
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
            assertThat(resource.getAttributesAsJson(), equalTo("{'test' : 'test'}"));
        }
    }

    @Test
    public void testGetByZoneAndResourceIdentifier() {
        assertThat(IteratorUtils.count(this.graph.vertices()), equalTo(0L));
        persistResource1toZone1();
        assertThat(IteratorUtils.count(this.graph.vertices()), equalTo(1L));
        persistResource1toZone2();
        assertThat(IteratorUtils.count(this.graph.vertices()), equalTo(2L));

        ResourceEntity resource = this.resourceRepository.getByZoneAndResourceIdentifier(TEST_ZONE_1, RESOURCE_1_ID);
        assertThat(resource.getResourceIdentifier(), equalTo(RESOURCE_1_ID));
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
        resourceEntitiesToSave.add(new ResourceEntity(TEST_ZONE_1, RESOURCE_1_ID));
        resourceEntitiesToSave.add(new ResourceEntity(TEST_ZONE_1, RESOURCE_2_ID));
        this.resourceRepository.save(resourceEntitiesToSave);
        assertThat(this.resourceRepository.count(), equalTo(2L));
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

    private ResourceEntity persistResource1toZone1() {
        ResourceEntity resource = new ResourceEntity(TEST_ZONE_1, RESOURCE_1_ID);
        resource.setAttributesAsJson("{'test' : 'test'}");
        return this.resourceRepository.save(resource);
    }

    private ResourceEntity persistResource2toZone1() {
        ResourceEntity resource = new ResourceEntity(TEST_ZONE_1, RESOURCE_2_ID);
        resource.setAttributesAsJson("{'test' : 'test'}");
        return this.resourceRepository.save(resource);
    }

    private ResourceEntity persistResource1toZone2() {
        ResourceEntity resource = new ResourceEntity(TEST_ZONE_2, RESOURCE_1_ID);
        resource.setAttributesAsJson("{'test' : 'test'}");
        return this.resourceRepository.save(resource);
    }
}
