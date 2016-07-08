package com.ge.predix.acs.privilege.management.dao;

import static com.ge.predix.acs.privilege.management.dao.GraphGenericRepository.PARENT_EDGE_LABEL;
import static com.ge.predix.acs.privilege.management.dao.GraphGenericRepository.SCOPE_PROPERTY_KEY;
import static com.ge.predix.acs.privilege.management.dao.GraphGenericRepository.ZONE_ID_KEY;
import static com.ge.predix.acs.privilege.management.dao.GraphResourceRepository.RESOURCE_ID_KEY;
import static com.ge.predix.acs.privilege.management.dao.GraphSubjectRepository.SUBJECT_ID_KEY;
import static com.ge.predix.acs.testutils.XFiles.BASEMENT_ATTRIBUTES;
import static com.ge.predix.acs.testutils.XFiles.BASEMENT_SITE_ID;
import static com.ge.predix.acs.testutils.XFiles.DRIVE_ATTRIBUTES;
import static com.ge.predix.acs.testutils.XFiles.DRIVE_ID;
import static com.ge.predix.acs.testutils.XFiles.EVIDENCE_IMPLANT_ATTRIBUTES;
import static com.ge.predix.acs.testutils.XFiles.EVIDENCE_IMPLANT_ID;
import static com.ge.predix.acs.testutils.XFiles.JOSECHUNG_ID;
import static com.ge.predix.acs.testutils.XFiles.SITE_BASEMENT;
import static com.ge.predix.acs.testutils.XFiles.TOP_SECRET_CLASSIFICATION;
import static com.ge.predix.acs.testutils.XFiles.TYPE_MONSTER_OF_THE_WEEK;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.nullValue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
import com.ge.predix.acs.model.Attribute;
import com.ge.predix.acs.rest.Parent;
import com.ge.predix.acs.utils.JsonUtils;
import com.ge.predix.acs.zone.management.dao.ZoneEntity;
import com.thinkaurelius.titan.core.QueryException;
import com.thinkaurelius.titan.core.SchemaViolationException;
import com.thinkaurelius.titan.core.TitanFactory;

public class GraphResourceRepositoryTest {
    private static final JsonUtils JSON_UTILS = new JsonUtils();
    private static final ZoneEntity TEST_ZONE_1 = new ZoneEntity(1L, "testzone1");
    private static final ZoneEntity TEST_ZONE_2 = new ZoneEntity(2L, "testzone2");

    private GraphResourceRepository resourceRepository;
    private Graph graph;

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
        persistResource1toZone1AndAssert();
        assertThat(IteratorUtils.count(this.graph.vertices()), equalTo(1L));
        persistResource2toZone1AndAssert();
        assertThat(IteratorUtils.count(this.graph.vertices()), equalTo(2L));
        assertThat(this.resourceRepository.count(), equalTo(2L));
    }

    @Test
    public void testDelete() {
        assertThat(IteratorUtils.count(this.graph.vertices()), equalTo(0L));
        ResourceEntity resourceEntity = persistResource1toZone1AndAssert();
        long id = resourceEntity.getId();
        assertThat(IteratorUtils.count(this.graph.vertices()), equalTo(1L));
        this.resourceRepository.delete(id);
        assertThat(IteratorUtils.count(this.graph.vertices()), equalTo(0L));
        assertThat(this.resourceRepository.findOne(id), nullValue());
    }

    @Test
    public void testDeleteMultiple() {
        List<ResourceEntity> resourceEntities = new ArrayList<>();
        assertThat(IteratorUtils.count(this.graph.vertices()), equalTo(0L));
        ResourceEntity resourceEntity1 = persistResource1toZone1AndAssert();
        Long resourceId1 = resourceEntity1.getId();
        resourceEntities.add(resourceEntity1);
        assertThat(IteratorUtils.count(this.graph.vertices()), equalTo(1L));
        ResourceEntity resourceEntity2 = persistResource2toZone1AndAssert();
        Long resourceId2 = resourceEntity2.getId();
        resourceEntities.add(resourceEntity2);
        assertThat(IteratorUtils.count(this.graph.vertices()), equalTo(2L));
        this.resourceRepository.delete(resourceEntities);
        assertThat(IteratorUtils.count(this.graph.vertices()), equalTo(0L));
        assertThat(this.resourceRepository.findOne(resourceId1), nullValue());
        assertThat(this.resourceRepository.findOne(resourceId2), nullValue());
    }

    @Test
    public void testDeleteAll() {
        assertThat(IteratorUtils.count(this.graph.vertices()), equalTo(0L));
        Long resourceId1 = persistResource1toZone1AndAssert().getId();
        assertThat(IteratorUtils.count(this.graph.vertices()), equalTo(1L));
        Long resourceId2 = persistResource2toZone1AndAssert().getId();
        assertThat(IteratorUtils.count(this.graph.vertices()), equalTo(2L));
        this.resourceRepository.deleteAll();
        assertThat(IteratorUtils.count(this.graph.vertices()), equalTo(0L));
        assertThat(this.resourceRepository.findOne(resourceId1), nullValue());
        assertThat(this.resourceRepository.findOne(resourceId2), nullValue());
    }

    @Test
    public void testFindAll() {
        assertThat(IteratorUtils.count(this.graph.vertices()), equalTo(0L));
        ResourceEntity resourceEntity1 = persistResource1toZone1AndAssert();
        assertThat(IteratorUtils.count(this.graph.vertices()), equalTo(1L));
        ResourceEntity resourceEntity2 = persistResource2toZone1AndAssert();
        assertThat(IteratorUtils.count(this.graph.vertices()), equalTo(2L));

        List<ResourceEntity> resources = this.resourceRepository.findAll();
        assertThat(resources.size(), equalTo(2));
        assertThat(resources, hasItems(resourceEntity1, resourceEntity2));
    }

    @Test
    public void testFindByZone() {
        assertThat(IteratorUtils.count(this.graph.vertices()), equalTo(0L));
        ResourceEntity resourceEntity1 = persistResource1toZone1AndAssert();
        assertThat(IteratorUtils.count(this.graph.vertices()), equalTo(1L));
        ResourceEntity resourceEntity2 = persistResource2toZone1AndAssert();
        assertThat(IteratorUtils.count(this.graph.vertices()), equalTo(2L));

        List<ResourceEntity> resources = this.resourceRepository.findByZone(TEST_ZONE_1);
        assertThat(resources.size(), equalTo(2));
        assertThat(resources, hasItems(resourceEntity1, resourceEntity2));
    }

    @Test
    public void testGetByZoneAndResourceIdentifier() {
        assertThat(IteratorUtils.count(this.graph.vertices()), equalTo(0L));
        ResourceEntity resourceEntity1 = persist2LevelHierarchicalResource1toZone1();
        assertThat(IteratorUtils.count(this.graph.vertices()), equalTo(2L));
        persistResource1toZone2AndAssert();
        assertThat(IteratorUtils.count(this.graph.vertices()), equalTo(3L));

        ResourceEntity resource = this.resourceRepository.getByZoneAndResourceIdentifier(TEST_ZONE_1,
                resourceEntity1.getResourceIdentifier());
        assertThat(resource, equalTo(resourceEntity1));
        // Check that the result does not contain inherited attribute
        assertThat(resource.getAttributes().contains(SITE_BASEMENT), equalTo(false));
        assertThat(resource.getAttributes().contains(TYPE_MONSTER_OF_THE_WEEK), equalTo(true));
    }

    @Test
    public void testGetByZoneAndResourceIdentifierWithInheritedAttributes() {
        assertThat(IteratorUtils.count(this.graph.vertices()), equalTo(0L));
        ResourceEntity resourceEntity1 = persist2LevelHierarchicalResource1toZone1();
        assertThat(IteratorUtils.count(this.graph.vertices()), equalTo(2L));
        persistResource1toZone2AndAssert();
        assertThat(IteratorUtils.count(this.graph.vertices()), equalTo(3L));

        ResourceEntity resource = this.resourceRepository.getByZoneAndResourceIdentifierWithInheritedAttributes(
                TEST_ZONE_1, resourceEntity1.getResourceIdentifier());
        assertThat(resource.getZone().getName(), equalTo(resourceEntity1.getZone().getName()));
        assertThat(resource.getResourceIdentifier(), equalTo(resourceEntity1.getResourceIdentifier()));
        // Check that the result contains inherited attribute
        assertThat(resource.getAttributes().contains(SITE_BASEMENT), equalTo(true));
        assertThat(resource.getAttributes().contains(TYPE_MONSTER_OF_THE_WEEK), equalTo(true));
    }

    @Test
    public void testGetByZoneAndResourceIdentifierWithEmptyAttributes() {
        assertThat(IteratorUtils.count(this.graph.vertices()), equalTo(0L));
        ResourceEntity persistedResourceEntity = persistResourceToZoneAndAssert(TEST_ZONE_1, DRIVE_ID,
                Collections.emptySet());
        assertThat(IteratorUtils.count(this.graph.vertices()), equalTo(1L));

        ResourceEntity resource = this.resourceRepository.getByZoneAndResourceIdentifier(TEST_ZONE_1, DRIVE_ID);
        assertThat(resource, equalTo(persistedResourceEntity));
    }

    @Test
    public void testGetByZoneAndResourceIdentifierWithNullAttributes() {
        assertThat(IteratorUtils.count(this.graph.vertices()), equalTo(0L));
        ResourceEntity persistedResourceEntity = persistResourceToZoneAndAssert(TEST_ZONE_1, DRIVE_ID, null);
        assertThat(IteratorUtils.count(this.graph.vertices()), equalTo(1L));

        ResourceEntity resource = this.resourceRepository.getByZoneAndResourceIdentifier(TEST_ZONE_1, DRIVE_ID);
        assertThat(resource, equalTo(persistedResourceEntity));
    }

    @Test
    public void testGetByZoneAndResourceIdentifierWithInheritedAttributes2LevelHierarchical() {
        assertThat(IteratorUtils.count(this.graph.vertices()), equalTo(0L));
        String resourceIdentifier = persist2LevelHierarchicalResource1toZone1().getResourceIdentifier();
        assertThat(IteratorUtils.count(this.graph.vertices()), equalTo(2L));

        ResourceEntity resource = this.resourceRepository
                .getByZoneAndResourceIdentifierWithInheritedAttributes(TEST_ZONE_1, resourceIdentifier);
        assertThat(resource.getAttributes().contains(SITE_BASEMENT), equalTo(true));
        assertThat(resource.getAttributes().contains(TYPE_MONSTER_OF_THE_WEEK), equalTo(true));
    }

    @Test
    public void testGetByZoneAndResourceIdentifierWithInheritedAttributes3LevelHierarchical() {
        assertThat(IteratorUtils.count(this.graph.vertices()), equalTo(0L));
        String resourceIdentifier = persist3LevelHierarchicalResource1toZone1().getResourceIdentifier();
        assertThat(IteratorUtils.count(this.graph.vertices()), equalTo(3L));

        ResourceEntity resource = this.resourceRepository
                .getByZoneAndResourceIdentifierWithInheritedAttributes(TEST_ZONE_1, resourceIdentifier);
        assertThat(resource.getAttributes().contains(SITE_BASEMENT), equalTo(true));
        assertThat(resource.getAttributes().contains(TYPE_MONSTER_OF_THE_WEEK), equalTo(true));
        assertThat(resource.getAttributes().contains(TOP_SECRET_CLASSIFICATION), equalTo(true));
    }

    @Test(expectedExceptions = SchemaViolationException.class)
    public void testPreventEntityParentSelfReference() {
        assertThat(IteratorUtils.count(this.graph.vertices()), equalTo(0L));
        ResourceEntity resource = new ResourceEntity(TEST_ZONE_1, BASEMENT_SITE_ID);
        resource.setAttributes(BASEMENT_ATTRIBUTES);
        resource.setAttributesAsJson(JSON_UTILS.serialize(resource.getAttributes()));
        resource.setParents(new HashSet<>(Arrays.asList(new Parent(BASEMENT_SITE_ID))));
        this.resourceRepository.save(resource);
    }

    /**
     * First we setup a 3 level graph where 3 -> 2 -> 1. Next, we try to update vertex 1 so that 1 -> 3. We expect
     * this to result in a SchemaViolationException because it would introduce a cyclic reference.
     */
    @Test
    public void testPreventEntityParentCyclicReference() {
        assertThat(IteratorUtils.count(this.graph.vertices()), equalTo(0L));
        ResourceEntity resource1 = persistResource0toZone1AndAssert();

        ResourceEntity resource2 = new ResourceEntity(TEST_ZONE_1, DRIVE_ID);
        resource2.setAttributes(DRIVE_ATTRIBUTES);
        resource2.setAttributesAsJson(JSON_UTILS.serialize(resource2.getAttributes()));
        resource2.setParents(
                new HashSet<>(Arrays.asList(new Parent[] { new Parent(resource1.getResourceIdentifier()) })));
        this.resourceRepository.save(resource2);

        ResourceEntity resource3 = new ResourceEntity(TEST_ZONE_1, EVIDENCE_IMPLANT_ID);
        resource3.setAttributes(EVIDENCE_IMPLANT_ATTRIBUTES);
        resource3.setAttributesAsJson(JSON_UTILS.serialize(resource3.getAttributes()));
        resource3.setParents(
                new HashSet<>(Arrays.asList(new Parent[] { new Parent(resource2.getResourceIdentifier()) })));
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
        String resourceIdentifier = persistResource1toZone1AndAssert().getResourceIdentifier();
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

        traversal = this.graph.traversal().V(resourceVertex.id()).out("parent").has(RESOURCE_ID_KEY, BASEMENT_SITE_ID);
        assertThat(traversal.hasNext(), equalTo(true));
        resourceVertex = traversal.next();
        assertThat(resourceVertex.property(RESOURCE_ID_KEY).value(), equalTo(BASEMENT_SITE_ID));
    }

    @Test
    public void testUpdateAttachedEntity() {
        assertThat(IteratorUtils.count(this.graph.vertices()), equalTo(0L));
        ResourceEntity resourceEntity = persistResource1toZone1AndAssert();
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
        String resourceIdentifier = persistResource1toZone1AndAssert().getResourceIdentifier();
        assertThat(IteratorUtils.count(this.graph.vertices()), equalTo(1L));

        GraphTraversalSource g = this.graph.traversal();
        String resourceId = resourceIdentifier;
        GraphTraversal<Vertex, Vertex> traversal = g.V().has(RESOURCE_ID_KEY, resourceId);
        assertThat(traversal.hasNext(), equalTo(true));
        assertThat(traversal.next().property(RESOURCE_ID_KEY).value(), equalTo(resourceId));

        // Save a new resource which violates the uniqueness constraint.
        persistResource1toZone1AndAssert();
    }

    @Test
    public void testSaveMultiple() {
        List<ResourceEntity> resourceEntitiesToSave = new ArrayList<>();
        resourceEntitiesToSave.add(new ResourceEntity(TEST_ZONE_1, DRIVE_ID));
        resourceEntitiesToSave.add(new ResourceEntity(TEST_ZONE_1, JOSECHUNG_ID));
        this.resourceRepository.save(resourceEntitiesToSave);
        assertThat(this.resourceRepository.count(), equalTo(2L));
    }

    @Test
    public void testSaveResourceWithNonexistentParent() {
        assertThat(IteratorUtils.count(this.graph.vertices()), equalTo(0L));
        ResourceEntity resource = new ResourceEntity(TEST_ZONE_1, DRIVE_ID);
        resource.setParents(new HashSet<>(Arrays.asList(new Parent(BASEMENT_SITE_ID))));

        // Save a resource with nonexistent parent which should throw IllegalStateException exception
        // while saving parent relationships.
        try {
            this.resourceRepository.save(resource);
        } catch (IllegalStateException ex) {
            assertThat(ex.getMessage(),
                    equalTo("No parent exists in zone 'testzone1' with 'resourceId' value of '/site/basement'."));
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

    @Test(
            expectedExceptions = QueryException.class,
            expectedExceptionsMessageRegExp = "Graph search failed: traversal limit exceeded.")
    public void testSearchAttributesTraversalLimitException() {
        long traversalLimit = 256L;
        try {
            ResourceEntity resource1 = persist3LevelHierarchicalResource1toZone1();
            traversalLimit = this.resourceRepository.getTraversalLimit();
            assertThat(traversalLimit, equalTo(256L));
            this.resourceRepository.setTraversalLimit(2);
            assertThat(this.resourceRepository.getTraversalLimit(), equalTo(2L));
            this.resourceRepository.getByZoneAndResourceIdentifierWithInheritedAttributes(TEST_ZONE_1,
                    resource1.getResourceIdentifier());
        } finally {
            this.resourceRepository.setTraversalLimit(traversalLimit);
            assertThat(this.resourceRepository.getTraversalLimit(), equalTo(256L));
        }
    }

    @Test
    public void testSearchAttributesEqualsTraversalLimit() {
        long traversalLimit = 256L;
        try {
            traversalLimit = this.resourceRepository.getTraversalLimit();
            assertThat(traversalLimit, equalTo(256L));
            this.resourceRepository.setTraversalLimit(3);
            assertThat(this.resourceRepository.getTraversalLimit(), equalTo(3L));
            ResourceEntity resource1 = persist3LevelHierarchicalResource1toZone1();
            ResourceEntity actualResource = this.resourceRepository
                    .getByZoneAndResourceIdentifierWithInheritedAttributes(TEST_ZONE_1,
                            resource1.getResourceIdentifier());
            assertThat(actualResource.getAttributes().size(), equalTo(3));
        } finally {
            this.resourceRepository.setTraversalLimit(traversalLimit);
            assertThat(this.resourceRepository.getTraversalLimit(), equalTo(256L));
        }
    }

    public ResourceEntity persist2LevelHierarchicalResource1toZone1() {
        ResourceEntity parentResource = persistResource0toZone1AndAssert();
        HashSet<Parent> parents = new HashSet<>(
                Arrays.asList(new Parent[] { new Parent(parentResource.getResourceIdentifier()) }));
        return persistResourceWithParentsToZoneAndAssert(TEST_ZONE_1, DRIVE_ID, DRIVE_ATTRIBUTES, parents);
    }

    public ResourceEntity persist3LevelHierarchicalResource1toZone1() {
        ResourceEntity parentResource = persist2LevelHierarchicalResource1toZone1();
        HashSet<Parent> parents = new HashSet<>(
                Arrays.asList(new Parent[] { new Parent(parentResource.getResourceIdentifier()) }));
        return persistResourceWithParentsToZoneAndAssert(TEST_ZONE_1, EVIDENCE_IMPLANT_ID, EVIDENCE_IMPLANT_ATTRIBUTES,
                parents);
    }

    private ResourceEntity persistResourceWithParentsToZoneAndAssert(final ZoneEntity zoneEntity,
            final String resourceIdentifier, final Set<Attribute> attributes, final Set<Parent> parents) {
        ResourceEntity resource = new ResourceEntity(zoneEntity, resourceIdentifier);
        resource.setAttributes(attributes);
        resource.setAttributesAsJson(JSON_UTILS.serialize(resource.getAttributes()));
        resource.setParents(parents);
        ResourceEntity resourceEntity = this.resourceRepository.save(resource);
        assertThat(this.resourceRepository.findOne(resourceEntity.getId()), equalTo(resource));
        return resourceEntity;
    }

    private ResourceEntity persistResource0toZone1AndAssert() {
        return persistResourceToZoneAndAssert(TEST_ZONE_1, BASEMENT_SITE_ID, BASEMENT_ATTRIBUTES);
    }

    private ResourceEntity persistResource1toZone1AndAssert() {
        return persistResourceToZoneAndAssert(TEST_ZONE_1, DRIVE_ID, DRIVE_ATTRIBUTES);
    }

    private ResourceEntity persistResource2toZone1AndAssert() {
        return persistResourceToZoneAndAssert(TEST_ZONE_1, JOSECHUNG_ID, DRIVE_ATTRIBUTES);
    }

    private ResourceEntity persistResource1toZone2AndAssert() {
        return persistResourceToZoneAndAssert(TEST_ZONE_2, DRIVE_ID, DRIVE_ATTRIBUTES);
    }

    private ResourceEntity persistResourceToZoneAndAssert(final ZoneEntity zoneEntity, final String resourceIdentifier,
            final Set<Attribute> attributes) {
        ResourceEntity resource = new ResourceEntity(zoneEntity, resourceIdentifier);
        resource.setAttributes(attributes);
        resource.setAttributesAsJson(JSON_UTILS.serialize(resource.getAttributes()));
        ResourceEntity resourceEntity = this.resourceRepository.save(resource);
        assertThat(this.resourceRepository.findOne(resourceEntity.getId()), equalTo(resource));
        return resourceEntity;
    }
}
