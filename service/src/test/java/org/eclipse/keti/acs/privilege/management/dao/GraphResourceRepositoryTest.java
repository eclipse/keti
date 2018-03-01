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

package org.eclipse.keti.acs.privilege.management.dao;

import static org.eclipse.keti.acs.privilege.management.dao.GraphResourceRepository.RESOURCE_ID_KEY;
import static org.eclipse.keti.acs.testutils.XFiles.ASCENSION_ATTRIBUTES;
import static org.eclipse.keti.acs.testutils.XFiles.ASCENSION_ID;
import static org.eclipse.keti.acs.testutils.XFiles.BASEMENT_ATTRIBUTES;
import static org.eclipse.keti.acs.testutils.XFiles.BASEMENT_SITE_ID;
import static org.eclipse.keti.acs.testutils.XFiles.DRIVE_ATTRIBUTES;
import static org.eclipse.keti.acs.testutils.XFiles.DRIVE_ID;
import static org.eclipse.keti.acs.testutils.XFiles.EVIDENCE_IMPLANT_ATTRIBUTES;
import static org.eclipse.keti.acs.testutils.XFiles.EVIDENCE_IMPLANT_ID;
import static org.eclipse.keti.acs.testutils.XFiles.EVIDENCE_SCULLYS_TESTIMONY_ID;
import static org.eclipse.keti.acs.testutils.XFiles.JOSECHUNG_ID;
import static org.eclipse.keti.acs.testutils.XFiles.SCULLYS_TESTIMONY_ATTRIBUTES;
import static org.eclipse.keti.acs.testutils.XFiles.SITE_BASEMENT;
import static org.eclipse.keti.acs.testutils.XFiles.TOP_SECRET_CLASSIFICATION;
import static org.eclipse.keti.acs.testutils.XFiles.TYPE_MONSTER_OF_THE_WEEK;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import org.apache.commons.lang.time.StopWatch;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import org.eclipse.keti.acs.config.GraphConfig;
import org.eclipse.keti.acs.model.Attribute;
import org.eclipse.keti.acs.rest.Parent;
import org.eclipse.keti.acs.utils.JsonUtils;
import org.eclipse.keti.acs.zone.management.dao.ZoneEntity;
import org.janusgraph.core.SchemaViolationException;
import org.janusgraph.core.JanusGraphException;
import org.janusgraph.core.JanusGraphFactory;

public class GraphResourceRepositoryTest {
    private static final JsonUtils JSON_UTILS = new JsonUtils();
    private static final ZoneEntity TEST_ZONE_1 = new ZoneEntity(1L, "testzone1");
    private static final ZoneEntity TEST_ZONE_2 = new ZoneEntity(2L, "testzone2");
    private static final int CONCURRENT_TEST_THREAD_COUNT = 3;
    private static final int CONCURRENT_TEST_INVOCATIONS = 20;

    private GraphResourceRepository resourceRepository;
    private GraphTraversalSource graphTraversalSource;
    private Random randomGenerator = new Random();

    @BeforeClass
    public void setup() throws Exception {
        this.resourceRepository = new GraphResourceRepository();
        setupGraph();
        this.resourceRepository.setGraphTraversal(this.graphTraversalSource);
    }

    @AfterClass
    public void teardown() {
        this.dropAllResources();
    }

    private void setupGraph() throws InterruptedException, ExecutionException {
        Graph graph = JanusGraphFactory.build().set("storage.backend", "inmemory").open();
        GraphConfig.createSchemaElements(graph);
        this.graphTraversalSource = graph.traversal();
        this.dropAllResources();
    }

    private void dropAllResources() {
        this.graphTraversalSource.V().drop().iterate();
    }

    @Test
    public void testCount() {
        dropAllResources();
        assertThat(this.graphTraversalSource.V().count().next().intValue(), equalTo(0));

        persistRandomResourcetoZone1AndAssert();
        assertThat(this.graphTraversalSource.V().count().next().intValue(), equalTo(1));

        persistResource2toZone1AndAssert();
        assertThat(this.graphTraversalSource.V().count().next().intValue(), equalTo(2));
        assertThat(this.resourceRepository.count(), equalTo(2L));
        dropAllResources();
    }

    @Test(threadPoolSize = CONCURRENT_TEST_THREAD_COUNT,
            invocationCount = CONCURRENT_TEST_INVOCATIONS)
    public void testDelete() {
        ResourceEntity resourceEntity = persistRandomResourcetoZone1AndAssert();
        long id = resourceEntity.getId();

        this.resourceRepository.delete(id);
        assertThat(this.resourceRepository.findOne(id), nullValue());
    }

    @Test(threadPoolSize = CONCURRENT_TEST_THREAD_COUNT,
            invocationCount = CONCURRENT_TEST_INVOCATIONS)
    public void testDeleteMultiple() {
        List<ResourceEntity> resourceEntities = new ArrayList<>();

        ResourceEntity resourceEntity1 = persistRandomResourcetoZone1AndAssert();
        Long resourceId1 = resourceEntity1.getId();
        resourceEntities.add(resourceEntity1);

        ResourceEntity resourceEntity2 = persistRandomResourcetoZone1AndAssert();
        Long resourceId2 = resourceEntity2.getId();
        resourceEntities.add(resourceEntity2);

        this.resourceRepository.delete(resourceEntities);

        assertThat(this.resourceRepository.findOne(resourceId1), nullValue());
        assertThat(this.resourceRepository.findOne(resourceId2), nullValue());
    }

    @Test
    public void testDeleteAll() {
        dropAllResources();
        assertThat(this.graphTraversalSource.V().count().next().intValue(), equalTo(0));
        Long resourceId1 = persistRandomResourcetoZone1AndAssert().getId();
        assertThat(this.graphTraversalSource.V().count().next().intValue(), equalTo(1));
        Long resourceId2 = persistResource2toZone1AndAssert().getId();
        assertThat(this.graphTraversalSource.V().count().next().intValue(), equalTo(2));
        this.resourceRepository.deleteAll();
        assertThat(this.graphTraversalSource.V().count().next().intValue(), equalTo(0));
        assertThat(this.resourceRepository.findOne(resourceId1), nullValue());
        assertThat(this.resourceRepository.findOne(resourceId2), nullValue());
        dropAllResources();
    }

    @Test
    public void testFindAll() {
        dropAllResources();
        assertThat(this.graphTraversalSource.V().count().next().intValue(), equalTo(0));
        ResourceEntity resourceEntity1 = persistRandomResourcetoZone1AndAssert();
        assertThat(this.graphTraversalSource.V().count().next().intValue(), equalTo(1));
        ResourceEntity resourceEntity2 = persistResource2toZone1AndAssert();
        assertThat(this.graphTraversalSource.V().count().next().intValue(), equalTo(2));

        List<ResourceEntity> resources = this.resourceRepository.findAll();
        assertThat(resources.size(), equalTo(2));
        assertThat(resources, hasItems(resourceEntity1, resourceEntity2));
        dropAllResources();
    }

    @Test(threadPoolSize = CONCURRENT_TEST_THREAD_COUNT,
            invocationCount = CONCURRENT_TEST_INVOCATIONS)
    public void testFindByZone() {
        ResourceEntity resourceEntity1 = persistRandomResourcetoZone1AndAssert();
        ResourceEntity resourceEntity2 = persistRandomResourcetoZone1AndAssert();

        List<ResourceEntity> resources = this.resourceRepository.findByZone(TEST_ZONE_1);
        assertThat(resources, hasItems(resourceEntity1, resourceEntity2));
        this.resourceRepository.delete(resourceEntity1);
        this.resourceRepository.delete(resourceEntity2);
    }

    @Test(threadPoolSize = CONCURRENT_TEST_THREAD_COUNT,
            invocationCount = CONCURRENT_TEST_INVOCATIONS)
    public void testGetByZoneAndResourceIdentifier() {
        ResourceEntity resourceEntity1 = persist2LevelRandomResourcetoZone1();

        ResourceEntity resource = this.resourceRepository
                .getByZoneAndResourceIdentifier(TEST_ZONE_1, resourceEntity1.getResourceIdentifier());
        assertThat(resource, equalTo(resourceEntity1));
        assertThat(resource.getAttributes().contains(TYPE_MONSTER_OF_THE_WEEK), equalTo(true));

        //Check that the result does not contain inherited attribute. use getResourceWithInheritedAttributes inherit
        //attributes
        assertThat(resource.getAttributes().contains(SITE_BASEMENT), equalTo(false));
        deleteTwoLevelEntityAndParents(resourceEntity1, TEST_ZONE_1, this.resourceRepository);
    }

    @Test(threadPoolSize = CONCURRENT_TEST_THREAD_COUNT,
            invocationCount = CONCURRENT_TEST_INVOCATIONS)
    public void testGetResourceWithInheritedAttributesWithInheritedAttributes() {
        ResourceEntity resourceEntity1 = persist2LevelRandomResourcetoZone1();

        ResourceEntity resource = this.resourceRepository
                .getResourceWithInheritedAttributes(TEST_ZONE_1, resourceEntity1.getResourceIdentifier());
        assertThat(resource.getZone().getName(), equalTo(resourceEntity1.getZone().getName()));
        assertThat(resource.getResourceIdentifier(), equalTo(resourceEntity1.getResourceIdentifier()));
        assertThat(resource.getAttributes().contains(TYPE_MONSTER_OF_THE_WEEK), equalTo(true));
        // Check that the result contains inherited attribute
        assertThat(resource.getAttributes().contains(SITE_BASEMENT), equalTo(true));
        deleteTwoLevelEntityAndParents(resourceEntity1, TEST_ZONE_1, this.resourceRepository);
    }

    @Test(threadPoolSize = CONCURRENT_TEST_THREAD_COUNT,
            invocationCount = CONCURRENT_TEST_INVOCATIONS)
    public void testGetByZoneAndResourceIdentifierWithEmptyAttributes() {

        ResourceEntity persistedResourceEntity = persistResourceToZoneAndAssert(TEST_ZONE_1,
                DRIVE_ID + getRandomNumber(), Collections.emptySet());

        ResourceEntity resource = this.resourceRepository
                .getByZoneAndResourceIdentifier(TEST_ZONE_1, persistedResourceEntity.getResourceIdentifier());
        assertThat(resource, equalTo(persistedResourceEntity));
        this.resourceRepository.delete(persistedResourceEntity);
    }

    @Test(threadPoolSize = CONCURRENT_TEST_THREAD_COUNT,
            invocationCount = CONCURRENT_TEST_INVOCATIONS)
    public void testGetByZoneAndResourceIdentifierWithNullAttributes() {
        ResourceEntity persistedResourceEntity = persistResourceToZoneAndAssert(TEST_ZONE_1,
                DRIVE_ID + getRandomNumber(), null);
        ResourceEntity resource = this.resourceRepository
                .getByZoneAndResourceIdentifier(TEST_ZONE_1, persistedResourceEntity.getResourceIdentifier());
        assertThat(resource, equalTo(persistedResourceEntity));
        this.resourceRepository.delete(persistedResourceEntity);
    }

    @Test(threadPoolSize = CONCURRENT_TEST_THREAD_COUNT,
            invocationCount = CONCURRENT_TEST_INVOCATIONS)
    public void testGetByZoneAndResourceIdentifierWithInheritedAttributes3LevelHierarchical() {
        ResourceEntity expectedResource = persist3LevelRandomResourcetoZone1();

        ResourceEntity actualResource = this.resourceRepository
                .getResourceWithInheritedAttributes(TEST_ZONE_1, expectedResource.getResourceIdentifier());
        assertThat(actualResource.getAttributes().contains(SITE_BASEMENT), equalTo(true));
        assertThat(actualResource.getAttributes().contains(TYPE_MONSTER_OF_THE_WEEK), equalTo(true));
        assertThat(actualResource.getAttributes().contains(TOP_SECRET_CLASSIFICATION), equalTo(true));
        deleteThreeLevelEntityAndParents(expectedResource, TEST_ZONE_1, this.resourceRepository);
    }

    @Test(expectedExceptions = SchemaViolationException.class)
    public void testPreventEntityParentSelfReference() {
        ResourceEntity resource = new ResourceEntity(TEST_ZONE_1, BASEMENT_SITE_ID);
        resource.setAttributes(BASEMENT_ATTRIBUTES);
        resource.setAttributesAsJson(JSON_UTILS.serialize(resource.getAttributes()));
        resource.setParents(new HashSet<>(Collections.singletonList(new Parent(BASEMENT_SITE_ID))));
        this.resourceRepository.save(resource);
    }

    /**
     * First we setup a 3 level graph where 3 -> 2 -> 1. Next, we try to update vertex 1 so that 1 -> 3. We expect
     * this to result in a SchemaViolationException because it would introduce a cyclic reference.
     */
    @Test
    public void testPreventEntityParentCyclicReference() {
        dropAllResources();
        assertThat(this.graphTraversalSource.V().count().next().intValue(), equalTo(0));
        ResourceEntity resource1 = persistResource0toZone1AndAssert();

        ResourceEntity resource2 = new ResourceEntity(TEST_ZONE_1, DRIVE_ID);
        resource2.setAttributes(DRIVE_ATTRIBUTES);
        resource2.setAttributesAsJson(JSON_UTILS.serialize(resource2.getAttributes()));
        resource2.setParents(new HashSet<>(Collections.singletonList(new Parent(resource1.getResourceIdentifier()))));
        this.resourceRepository.save(resource2);

        ResourceEntity resource3 = new ResourceEntity(TEST_ZONE_1, EVIDENCE_IMPLANT_ID);
        resource3.setAttributes(EVIDENCE_IMPLANT_ATTRIBUTES);
        resource3.setAttributesAsJson(JSON_UTILS.serialize(resource3.getAttributes()));
        resource3.setParents(new HashSet<>(Collections.singletonList(new Parent(resource2.getResourceIdentifier()))));
        this.resourceRepository.save(resource3);

        assertThat(this.graphTraversalSource.V().count().next().intValue(), equalTo(3));

        resource1.setParents(new HashSet<>(Collections.singletonList(new Parent(resource3.getResourceIdentifier()))));
        try {
            this.resourceRepository.save(resource1);
        } catch (SchemaViolationException ex) {
            assertThat(ex.getMessage(),
                    equalTo("Updating entity '/site/basement' with parent '/evidence/implant' introduces a cyclic "
                            + "reference."));
            return;
        }
        Assert.fail("save() did not throw the expected exception.");
        dropAllResources();
    }

    @Test(threadPoolSize = CONCURRENT_TEST_THREAD_COUNT,
            invocationCount = CONCURRENT_TEST_INVOCATIONS)
    public void testSave() {
        ResourceEntity resource = persistRandomResourcetoZone1AndAssert();
        String resourceId = resource.getResourceIdentifier();

        GraphTraversal<Vertex, Vertex> traversal = this.graphTraversalSource.V().has(RESOURCE_ID_KEY, resourceId);

        assertThat(traversal.hasNext(), equalTo(true));
        assertThat(traversal.next().property(RESOURCE_ID_KEY).value(), equalTo(resourceId));
        this.resourceRepository.delete(resource);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testSaveWithNoZoneName() {
        ResourceEntity resourceEntity = new ResourceEntity();
        resourceEntity.setResourceIdentifier("testResource");
        resourceEntity.setZone(new ZoneEntity());
        this.resourceRepository.save(resourceEntity);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testSaveWithNoZoneEntity() {
        ResourceEntity resourceEntity = new ResourceEntity();
        resourceEntity.setResourceIdentifier("testResource");
        this.resourceRepository.save(resourceEntity);
    }

    @Test(threadPoolSize = CONCURRENT_TEST_THREAD_COUNT,
            invocationCount = CONCURRENT_TEST_INVOCATIONS)
    public void testSaveHierarchical() {
        ResourceEntity childResource = persist2LevelRandomResourcetoZone1();
        String childResourceId = childResource.getResourceIdentifier();

        GraphTraversal<Vertex, Vertex> traversal = this.graphTraversalSource.V().has(RESOURCE_ID_KEY, childResourceId);
        assertThat(traversal.hasNext(), equalTo(true));
        Vertex childResourceVertex = traversal.next();
        assertThat(childResourceVertex.property(RESOURCE_ID_KEY).value(), equalTo(childResourceId));

        Parent parent = (Parent) childResource.getParents().toArray()[0];
        traversal = this.graphTraversalSource.V(childResourceVertex.id()).out("parent")
                .has(RESOURCE_ID_KEY, parent.getIdentifier());
        assertThat(traversal.hasNext(), equalTo(true));
        Vertex parentVertex = traversal.next();
        assertThat(parentVertex.property(RESOURCE_ID_KEY).value(), equalTo(parent.getIdentifier()));
        deleteTwoLevelEntityAndParents(childResource, TEST_ZONE_1, this.resourceRepository);
    }

    @Test(threadPoolSize = CONCURRENT_TEST_THREAD_COUNT,
            invocationCount = CONCURRENT_TEST_INVOCATIONS)
    public void testUpdateAttachedEntity() {
        ResourceEntity resourceEntity = persistRandomResourcetoZone1AndAssert();
        String resourceId = resourceEntity.getResourceIdentifier();

        GraphTraversalSource g = this.graphTraversalSource;
        GraphTraversal<Vertex, Vertex> traversal = g.V().has(RESOURCE_ID_KEY, resourceId);
        assertThat(traversal.hasNext(), equalTo(true));
        assertThat(traversal.next().property(RESOURCE_ID_KEY).value(), equalTo(resourceId));

        // Update the resource.
        String updateJSON = "{\'status':'test'}";
        resourceEntity.setAttributesAsJson(updateJSON);
        saveWithRetry(this.resourceRepository, resourceEntity, 3);
        assertThat(this.resourceRepository.getEntity(TEST_ZONE_1, resourceEntity.getResourceIdentifier())
                .getAttributesAsJson(), equalTo(updateJSON));
        this.resourceRepository.delete(resourceEntity);
    }

    @Test(expectedExceptions = SchemaViolationException.class)
    public void testUpdateUnattachedEntity() {
        String resourceId = persistRandomResourcetoZone1AndAssert().getResourceIdentifier();

        GraphTraversalSource g = this.graphTraversalSource;
        GraphTraversal<Vertex, Vertex> traversal = g.V().has(RESOURCE_ID_KEY, resourceId);
        assertThat(traversal.hasNext(), equalTo(true));
        assertThat(traversal.next().property(RESOURCE_ID_KEY).value(), equalTo(resourceId));

        // Save a new resource which violates the uniqueness constraint.
        persistResourceToZoneAndAssert(TEST_ZONE_1, resourceId, DRIVE_ATTRIBUTES);
    }

    @Test(threadPoolSize = CONCURRENT_TEST_THREAD_COUNT,
            invocationCount = CONCURRENT_TEST_INVOCATIONS)
    public void testSaveMultiple() {
        List<ResourceEntity> resourceEntitiesToSave = new ArrayList<>();
        ResourceEntity resourceEntity1 = new ResourceEntity(TEST_ZONE_1, DRIVE_ID + getRandomNumber());
        ResourceEntity resourceEntity2 = new ResourceEntity(TEST_ZONE_1, JOSECHUNG_ID + getRandomNumber());
        resourceEntitiesToSave.add(resourceEntity1);
        resourceEntitiesToSave.add(resourceEntity2);
        this.resourceRepository.save(resourceEntitiesToSave);
        assertThat(this.resourceRepository.getEntity(TEST_ZONE_1, resourceEntity1.getResourceIdentifier()),
                equalTo(resourceEntity1));
        assertThat(this.resourceRepository.getEntity(TEST_ZONE_1, resourceEntity2.getResourceIdentifier()),
                equalTo(resourceEntity2));
        this.resourceRepository.delete(resourceEntity1);
        this.resourceRepository.delete(resourceEntity2);
    }

    @Test
    public void testSaveResourceWithNonexistentParent() {
        this.dropAllResources();
        assertThat(this.graphTraversalSource.V().count().next().intValue(), equalTo(0));
        ResourceEntity resource = new ResourceEntity(TEST_ZONE_1, DRIVE_ID);
        resource.setParents(new HashSet<>(Collections.singletonList(new Parent(BASEMENT_SITE_ID))));

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

    @Test(expectedExceptions = AttributeLimitExceededException.class,
            expectedExceptionsMessageRegExp = "The number of attributes on this resource .* has exceeded the maximum "
                    + "limit of .*")
    public void testSearchAttributesTraversalLimitException() {
        long traversalLimit = 256L;
        try {
            ResourceEntity resource1 = persist3LevelRandomResourcetoZone1();
            traversalLimit = this.resourceRepository.getTraversalLimit();
            assertThat(traversalLimit, equalTo(256L));
            this.resourceRepository.setTraversalLimit(2);
            assertThat(this.resourceRepository.getTraversalLimit(), equalTo(2L));
            this.resourceRepository.getResourceWithInheritedAttributes(TEST_ZONE_1, resource1.getResourceIdentifier());
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
            ResourceEntity resource1 = persist3LevelRandomResourcetoZone1();
            ResourceEntity actualResource = this.resourceRepository
                    .getResourceWithInheritedAttributes(TEST_ZONE_1, resource1.getResourceIdentifier());
            assertThat(actualResource.getAttributes().size(), equalTo(3));
        } finally {
            this.resourceRepository.setTraversalLimit(traversalLimit);
            assertThat(this.resourceRepository.getTraversalLimit(), equalTo(256L));
        }
    }

    @Test(threadPoolSize = CONCURRENT_TEST_THREAD_COUNT,
            invocationCount = CONCURRENT_TEST_INVOCATIONS)
    public void testGetResourceEntityAndDescendantsIds() {

        ResourceEntity basement = persistResourceToZoneAndAssert(TEST_ZONE_1, BASEMENT_SITE_ID + getRandomNumber(),
                BASEMENT_ATTRIBUTES);

        ResourceEntity drive = persistResourceWithParentsToZoneAndAssert(TEST_ZONE_1, DRIVE_ID + getRandomNumber(),
                DRIVE_ATTRIBUTES,
                new HashSet<>(Collections.singletonList(new Parent(basement.getResourceIdentifier()))));
        ResourceEntity ascension = persistResourceWithParentsToZoneAndAssert(TEST_ZONE_1,
                ASCENSION_ID + getRandomNumber(), ASCENSION_ATTRIBUTES,
                new HashSet<>(Collections.singletonList(new Parent(basement.getResourceIdentifier()))));

        ResourceEntity implant = persistResourceWithParentsToZoneAndAssert(TEST_ZONE_1,
                EVIDENCE_IMPLANT_ID + getRandomNumber(), EVIDENCE_IMPLANT_ATTRIBUTES, new HashSet<>(
                        Arrays.asList(new Parent(drive.getResourceIdentifier()),
                                new Parent(ascension.getResourceIdentifier()))));
        ResourceEntity scullysTestimony = persistResourceWithParentsToZoneAndAssert(TEST_ZONE_1,
                EVIDENCE_SCULLYS_TESTIMONY_ID + getRandomNumber(), SCULLYS_TESTIMONY_ATTRIBUTES,
                new HashSet<>(Collections.singletonList(new Parent(ascension.getResourceIdentifier()))));

        Set<String> descendantsIds = this.resourceRepository.getResourceEntityAndDescendantsIds(basement);

        assertThat(descendantsIds, hasSize(5));

        assertThat(descendantsIds, hasItems(basement.getResourceIdentifier(), drive.

                        getResourceIdentifier(), ascension.getResourceIdentifier(), implant.getResourceIdentifier(),
                scullysTestimony.getResourceIdentifier()));

        descendantsIds = this.resourceRepository.getResourceEntityAndDescendantsIds(ascension);

        assertThat(descendantsIds, hasSize(3));

        assertThat(descendantsIds, hasItems(ascension.getResourceIdentifier(), implant.

                getResourceIdentifier(), scullysTestimony.getResourceIdentifier()));

        descendantsIds = this.resourceRepository.getResourceEntityAndDescendantsIds(drive);

        assertThat(descendantsIds, hasSize(2));

        assertThat(descendantsIds, hasItems(drive.getResourceIdentifier(), implant.

                getResourceIdentifier()));

        descendantsIds = this.resourceRepository.getResourceEntityAndDescendantsIds(implant);

        assertThat(descendantsIds, hasSize(1));

        assertThat(descendantsIds, hasItems(implant.getResourceIdentifier()));

        descendantsIds = this.resourceRepository.getResourceEntityAndDescendantsIds(scullysTestimony);

        assertThat(descendantsIds, hasSize(1));

        assertThat(descendantsIds, hasItems(scullysTestimony.getResourceIdentifier()));

        descendantsIds = this.resourceRepository.getResourceEntityAndDescendantsIds(null);

        assertThat(descendantsIds, empty());

        descendantsIds = this.resourceRepository.getResourceEntityAndDescendantsIds(new

                ResourceEntity(TEST_ZONE_1, "/nonexistent-resource"));

        assertThat(descendantsIds, empty());
        deleteThreeLevelEntityAndParents(basement, TEST_ZONE_1, this.resourceRepository);
    }

    @Test(expectedExceptions = IllegalStateException.class)
    public void testChildWithDifferentZone() {
        ResourceEntity basement = persistResourceToZoneAndAssert(TEST_ZONE_1, BASEMENT_SITE_ID + getRandomNumber(),
                BASEMENT_ATTRIBUTES);

        persistResourceWithParentsToZoneAndAssert(TEST_ZONE_2, DRIVE_ID + getRandomNumber(), DRIVE_ATTRIBUTES,
                new HashSet<>(Collections.singletonList(new Parent(basement.getResourceIdentifier()))));

    }

    @Test
    public void testVersion() {
        this.dropAllResources();
        assertThat(this.graphTraversalSource.V().count().next().intValue(), equalTo(0));
        assertThat(this.resourceRepository.checkVersionVertexExists(1), equalTo(false));
        this.resourceRepository.createVersionVertex(1);
        assertThat(this.resourceRepository.checkVersionVertexExists(1), equalTo(true));
        assertThat(this.graphTraversalSource.V().count().next().intValue(), equalTo(1));

        // assert that createVersionVertex creates a new vertex.
        this.resourceRepository.createVersionVertex(2);
        assertThat(this.graphTraversalSource.V().count().next().intValue(), equalTo(2));
        dropAllResources();
    }

    private ResourceEntity persist2LevelRandomResourcetoZone1() {
        ResourceEntity parentResource = persistResourceToZoneAndAssert(TEST_ZONE_1,
                BASEMENT_SITE_ID + getRandomNumber(), BASEMENT_ATTRIBUTES);
        HashSet<Parent> parents = new HashSet<>(
                Collections.singletonList(new Parent(parentResource.getResourceIdentifier())));
        return persistResourceWithParentsToZoneAndAssert(TEST_ZONE_1, DRIVE_ID + getRandomNumber(), DRIVE_ATTRIBUTES,
                parents);
    }

    private ResourceEntity persist3LevelRandomResourcetoZone1() {
        ResourceEntity parentResource = persist2LevelRandomResourcetoZone1();
        HashSet<Parent> parents = new HashSet<>(
                Collections.singletonList(new Parent(parentResource.getResourceIdentifier())));
        return persistResourceWithParentsToZoneAndAssert(TEST_ZONE_1, EVIDENCE_IMPLANT_ID + getRandomNumber(),
                EVIDENCE_IMPLANT_ATTRIBUTES, parents);
    }

    private ResourceEntity persistResourceWithParentsToZoneAndAssert(final ZoneEntity zoneEntity,
            final String resourceIdentifier, final Set<Attribute> attributes, final Set<Parent> parents) {
        ResourceEntity resource = new ResourceEntity(zoneEntity, resourceIdentifier);
        resource.setAttributes(attributes);
        resource.setAttributesAsJson(JSON_UTILS.serialize(resource.getAttributes()));
        resource.setParents(parents);
        ResourceEntity resourceEntity = saveWithRetry(this.resourceRepository, resource, 3);
        assertThat(this.resourceRepository.findOne(resourceEntity.getId()), equalTo(resource));
        return resourceEntity;
    }

    private ResourceEntity persistRandomResourcetoZone1AndAssert() {
        return persistResourceToZoneAndAssert(TEST_ZONE_1, DRIVE_ID + getRandomNumber(), DRIVE_ATTRIBUTES);
    }

    private ResourceEntity persistResource0toZone1AndAssert() {
        return persistResourceToZoneAndAssert(TEST_ZONE_1, BASEMENT_SITE_ID, BASEMENT_ATTRIBUTES);
    }

    private ResourceEntity persistResource2toZone1AndAssert() {
        return persistResourceToZoneAndAssert(TEST_ZONE_1, JOSECHUNG_ID, DRIVE_ATTRIBUTES);
    }

    private int getRandomNumber() {
        return randomGenerator.nextInt(100000000);
    }

    //Because of unique indices, saves on ZonableEntity can throw a lock exception due to contention on the index
    //update. This method allows does a sleep and retry the save.

    /*
     * Sample Exception: [Z: C:] 2016-11-03 15:13:33 ERROR [TestNG] database.StandardTitanGraph
     * [StandardTitanGraph.java:779] Could not commit transaction [10] due to exceptionsg
     * com.thinkaurelius.titan.diskstorage.locking.PermanentLockingException: Local lock contention at
     * com.thinkaurelius.titan.diskstorage.locking.AbstractLocker.writeLock(AbstractLocker.java:313) at
     * com.thinkaurelius.titan.diskstorage.locking.consistentkey.ExpectedValueCheckingStore.acquireLock(
     * ExpectedValueCheckingStore.java:89) at
     * com.thinkaurelius.titan.diskstorage.keycolumnvalue.KCVSProxy.acquireLock(KCVSProxy.java:40) at
     * com.thinkaurelius.titan.diskstorage.BackendTransaction.acquireIndexLock(BackendTransaction.java:240) at
     * com.thinkaurelius.titan.graphdb.database.StandardTitanGraph.prepareCommit(StandardTitanGraph.java:554) at
     * com.thinkaurelius.titan.graphdb.database.StandardTitanGraph.commit(StandardTitanGraph.java:683) at
     * com.thinkaurelius.titan.graphdb.transaction.StandardTitanTx.commit(StandardTitanTx.java:1352) at
     * com.thinkaurelius.titan.graphdb.tinkerpop.TitanBlueprintsGraph$GraphTransaction.doCommit(TitanBlueprintsGraph.
     * java:263) at
     * org.apache.tinkerpop.gremlin.structure.util.AbstractTransaction.commit(AbstractTransaction.java:105) at
     * org.eclipse.keti.acs.privilege.management.dao.GraphGenericRepository.save(GraphGenericRepository.java:221) at
     * org.eclipse.keti.acs.privilege.management.dao.GraphResourceRepositoryTest.saveWithRetry(
     * GraphResourceRepositoryTest.java:554) at
     * org.eclipse.keti.acs.privilege.management.dao.GraphSubjectRepositoryTest.saveWithRetry(
     * GraphSubjectRepositoryTest.java:277) at
     * org.eclipse.keti.acs.privilege.management.dao.GraphSubjectRepositoryTest.persistSubjectToZoneAndAssert(
     * GraphSubjectRepositoryTest.java:246) at
     * org.eclipse.keti.acs.privilege.management.dao.GraphSubjectRepositoryTest.persistRandomSubjectToZone1AndAssert(
     * GraphSubjectRepositoryTest.java:221) at
     * org.eclipse.keti.acs.privilege.management.dao.GraphSubjectRepositoryTest.testGetByZoneAndSubjectIdentifier(
     * GraphSubjectRepositoryTest.java:71)
     */
    static <E extends ZonableEntity> E saveWithRetry(final GraphGenericRepository<E> repository,
            final E zonableEntity, final int retryCount) throws JanusGraphException {
        try {
            repository.save(zonableEntity);
        } catch (JanusGraphException te) {
            if (te.getCause().getCause().getMessage().contains("Local lock") && retryCount > 0) {
                try {
                    Thread.sleep(250);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                zonableEntity.setId(0L); // Repository does not reset the vertex Id, on commit failure. Clear.
                saveWithRetry(repository, zonableEntity, retryCount - 1);
            } else {
                throw te;
            }
        }
        return zonableEntity;
    }

    private ResourceEntity persistResourceToZoneAndAssert(final ZoneEntity zoneEntity, final String resourceIdentifier,
            final Set<Attribute> attributes) {

        ResourceEntity resource = new ResourceEntity(zoneEntity, resourceIdentifier);
        resource.setAttributes(attributes);
        resource.setAttributesAsJson(JSON_UTILS.serialize(resource.getAttributes()));
        ResourceEntity resourceEntity = saveWithRetry(this.resourceRepository, resource, 3);
        assertThat(this.resourceRepository.findOne(resourceEntity.getId()), equalTo(resource));
        return resourceEntity;
    }

    static <E extends ZonableEntity> void deleteTwoLevelEntityAndParents(final E entity, final ZoneEntity zone,
            final GraphGenericRepository<E> repository) {
        Set<Parent> parents = entity.getParents();
        for (Parent parent : parents) {
            repository.delete(repository.getEntity(zone, parent.getIdentifier()));
        }
        repository.delete(entity);
    }

    private static <E extends ZonableEntity> void deleteThreeLevelEntityAndParents(final E entity,
            final ZoneEntity zone, final GraphGenericRepository<E> repository) {
        Set<Parent> parents = entity.getParents();
        for (Parent parent : parents) {
            deleteTwoLevelEntityAndParents(repository.getEntity(zone, parent.getIdentifier()), zone, repository);
        }
    }
}
