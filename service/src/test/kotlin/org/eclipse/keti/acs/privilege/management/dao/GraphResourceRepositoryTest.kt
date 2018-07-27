/*******************************************************************************
 * Copyright 2018 General Electric Company
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

package org.eclipse.keti.acs.privilege.management.dao

import org.apache.commons.lang.time.StopWatch
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource
import org.eclipse.keti.acs.config.createSchemaElements
import org.eclipse.keti.acs.model.Attribute
import org.eclipse.keti.acs.rest.Parent
import org.eclipse.keti.acs.testutils.ASCENSION_ATTRIBUTES
import org.eclipse.keti.acs.testutils.ASCENSION_ID
import org.eclipse.keti.acs.testutils.BASEMENT_ATTRIBUTES
import org.eclipse.keti.acs.testutils.BASEMENT_SITE_ID
import org.eclipse.keti.acs.testutils.DRIVE_ATTRIBUTES
import org.eclipse.keti.acs.testutils.DRIVE_ID
import org.eclipse.keti.acs.testutils.EVIDENCE_IMPLANT_ATTRIBUTES
import org.eclipse.keti.acs.testutils.EVIDENCE_IMPLANT_ID
import org.eclipse.keti.acs.testutils.EVIDENCE_SCULLYS_TESTIMONY_ID
import org.eclipse.keti.acs.testutils.JOSECHUNG_ID
import org.eclipse.keti.acs.testutils.SCULLYS_TESTIMONY_ATTRIBUTES
import org.eclipse.keti.acs.testutils.SITE_BASEMENT
import org.eclipse.keti.acs.testutils.TOP_SECRET_CLASSIFICATION
import org.eclipse.keti.acs.testutils.TYPE_MONSTER_OF_THE_WEEK
import org.eclipse.keti.acs.utils.JsonUtils
import org.eclipse.keti.acs.zone.management.dao.ZoneEntity
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.empty
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.hasItems
import org.hamcrest.Matchers.hasSize
import org.hamcrest.Matchers.nullValue
import org.janusgraph.core.JanusGraphException
import org.janusgraph.core.JanusGraphFactory
import org.janusgraph.core.SchemaViolationException
import org.testng.Assert
import org.testng.annotations.AfterClass
import org.testng.annotations.BeforeClass
import org.testng.annotations.Test
import java.util.ArrayList
import java.util.Arrays
import java.util.HashSet
import java.util.Random
import java.util.concurrent.ExecutionException

private val JSON_UTILS = JsonUtils()
private val TEST_ZONE_1 = ZoneEntity(1L, "testzone1")
private val TEST_ZONE_2 = ZoneEntity(2L, "testzone2")
private const val CONCURRENT_TEST_THREAD_COUNT = 3
private const val CONCURRENT_TEST_INVOCATIONS = 20

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
@Throws(JanusGraphException::class)
internal fun <E : ZonableEntity> saveWithRetry(
    repository: GraphGenericRepository<E>,
    zonableEntity: E,
    retryCount: Int
): E {
    try {
        repository.save(zonableEntity)
    } catch (te: JanusGraphException) {
        if (te.cause?.cause?.message?.contains("Local lock")!! && retryCount > 0) {
            try {
                Thread.sleep(250)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }

            zonableEntity.id = 0L // Repository does not reset the vertex Id, on commit failure. Clear.
            saveWithRetry(repository, zonableEntity, retryCount - 1)
        } else {
            throw te
        }
    }

    return zonableEntity
}

internal fun <E : ZonableEntity> deleteTwoLevelEntityAndParents(
    entity: E,
    zone: ZoneEntity,
    repository: GraphGenericRepository<E>?
) {
    val parents = entity.parents
    for (parent in parents) {
        repository!!.delete(repository.getEntity(zone, parent.identifier!!)!!)
    }
    repository!!.delete(entity)
}

private fun <E : ZonableEntity> deleteThreeLevelEntityAndParents(
    entity: E,
    zone: ZoneEntity,
    repository: GraphGenericRepository<E>
) {
    val parents = entity.parents
    for (parent in parents) {
        deleteTwoLevelEntityAndParents(repository.getEntity(zone, parent.identifier!!)!!, zone, repository)
    }
}

class GraphResourceRepositoryTest {

    private var resourceRepository: GraphResourceRepository? = null
    private var graphTraversalSource: GraphTraversalSource? = null
    private val randomGenerator = Random()

    private val randomNumber: Int
        get() = randomGenerator.nextInt(100000000)

    @BeforeClass
    @Throws(Exception::class)
    fun setup() {
        this.resourceRepository = GraphResourceRepository()
        setupGraph()
        this.resourceRepository!!.graphTraversal = this.graphTraversalSource!!
    }

    @AfterClass
    fun teardown() {
        this.dropAllResources()
    }

    @Throws(InterruptedException::class, ExecutionException::class)
    private fun setupGraph() {
        val graph = JanusGraphFactory.build().set("storage.backend", "inmemory").open()
        createSchemaElements(graph)
        this.graphTraversalSource = graph.traversal()
        this.dropAllResources()
    }

    private fun dropAllResources() {
        this.graphTraversalSource!!.V().drop().iterate()
    }

    @Test
    fun testCount() {
        dropAllResources()
        assertThat(this.graphTraversalSource!!.V().count().next().toInt(), equalTo(0))

        persistRandomResourcetoZone1AndAssert()
        assertThat(this.graphTraversalSource!!.V().count().next().toInt(), equalTo(1))

        persistResource2toZone1AndAssert()
        assertThat(this.graphTraversalSource!!.V().count().next().toInt(), equalTo(2))
        assertThat(this.resourceRepository!!.count(), equalTo(2L))
        dropAllResources()
    }

    @Test(threadPoolSize = CONCURRENT_TEST_THREAD_COUNT, invocationCount = CONCURRENT_TEST_INVOCATIONS)
    fun testDelete() {
        val resourceEntity = persistRandomResourcetoZone1AndAssert()
        val id = resourceEntity.id!!

        this.resourceRepository!!.delete(id)
        assertThat<ResourceEntity>(this.resourceRepository!!.findOne(id), nullValue())
    }

    @Test(threadPoolSize = CONCURRENT_TEST_THREAD_COUNT, invocationCount = CONCURRENT_TEST_INVOCATIONS)
    fun testDeleteMultiple() {
        val resourceEntities = ArrayList<ResourceEntity>()

        val resourceEntity1 = persistRandomResourcetoZone1AndAssert()
        val resourceId1 = resourceEntity1.id
        resourceEntities.add(resourceEntity1)

        val resourceEntity2 = persistRandomResourcetoZone1AndAssert()
        val resourceId2 = resourceEntity2.id
        resourceEntities.add(resourceEntity2)

        this.resourceRepository!!.delete(resourceEntities)

        assertThat<ResourceEntity>(this.resourceRepository!!.findOne(resourceId1), nullValue())
        assertThat<ResourceEntity>(this.resourceRepository!!.findOne(resourceId2), nullValue())
    }

    @Test
    fun testDeleteAll() {
        dropAllResources()
        assertThat(this.graphTraversalSource!!.V().count().next().toInt(), equalTo(0))
        val resourceId1 = persistRandomResourcetoZone1AndAssert().id
        assertThat(this.graphTraversalSource!!.V().count().next().toInt(), equalTo(1))
        val resourceId2 = persistResource2toZone1AndAssert().id
        assertThat(this.graphTraversalSource!!.V().count().next().toInt(), equalTo(2))
        this.resourceRepository!!.deleteAll()
        assertThat(this.graphTraversalSource!!.V().count().next().toInt(), equalTo(0))
        assertThat<ResourceEntity>(this.resourceRepository!!.findOne(resourceId1), nullValue())
        assertThat<ResourceEntity>(this.resourceRepository!!.findOne(resourceId2), nullValue())
        dropAllResources()
    }

    @Test
    fun testFindAll() {
        dropAllResources()
        assertThat(this.graphTraversalSource!!.V().count().next().toInt(), equalTo(0))
        val resourceEntity1 = persistRandomResourcetoZone1AndAssert()
        assertThat(this.graphTraversalSource!!.V().count().next().toInt(), equalTo(1))
        val resourceEntity2 = persistResource2toZone1AndAssert()
        assertThat(this.graphTraversalSource!!.V().count().next().toInt(), equalTo(2))

        val resources = this.resourceRepository!!.findAll()
        assertThat(resources.size, equalTo(2))
        assertThat(resources, hasItems(resourceEntity1, resourceEntity2))
        dropAllResources()
    }

    @Test(threadPoolSize = CONCURRENT_TEST_THREAD_COUNT, invocationCount = CONCURRENT_TEST_INVOCATIONS)
    fun testFindByZone() {
        val resourceEntity1 = persistRandomResourcetoZone1AndAssert()
        val resourceEntity2 = persistRandomResourcetoZone1AndAssert()

        val resources = this.resourceRepository!!.findByZone(TEST_ZONE_1)
        assertThat(resources, hasItems(resourceEntity1, resourceEntity2))
        this.resourceRepository!!.delete(resourceEntity1)
        this.resourceRepository!!.delete(resourceEntity2)
    }

    @Test(threadPoolSize = CONCURRENT_TEST_THREAD_COUNT, invocationCount = CONCURRENT_TEST_INVOCATIONS)
    fun testGetByZoneAndResourceIdentifier() {
        val resourceEntity1 = persist2LevelRandomResourcetoZone1()

        val resource = this.resourceRepository!!
            .getByZoneAndResourceIdentifier(TEST_ZONE_1, resourceEntity1.resourceIdentifier!!)
        assertThat<ResourceEntity>(resource, equalTo(resourceEntity1))
        assertThat(resource!!.attributes!!.contains(TYPE_MONSTER_OF_THE_WEEK), equalTo(true))

        //Check that the result does not contain inherited attribute. use getResourceWithInheritedAttributes inherit
        //attributes
        assertThat(resource.attributes!!.contains(SITE_BASEMENT), equalTo(false))
        deleteTwoLevelEntityAndParents(resourceEntity1, TEST_ZONE_1, this.resourceRepository)
    }

    @Test(threadPoolSize = CONCURRENT_TEST_THREAD_COUNT, invocationCount = CONCURRENT_TEST_INVOCATIONS)
    fun testGetResourceWithInheritedAttributesWithInheritedAttributes() {
        val resourceEntity1 = persist2LevelRandomResourcetoZone1()

        val resource = this.resourceRepository!!
            .getResourceWithInheritedAttributes(TEST_ZONE_1, resourceEntity1.resourceIdentifier!!)
        assertThat<String>(resource!!.zone!!.name, equalTo<String>(resourceEntity1.zone!!.name))
        assertThat<String>(resource.resourceIdentifier, equalTo<String>(resourceEntity1.resourceIdentifier))
        assertThat(resource.attributes!!.contains(TYPE_MONSTER_OF_THE_WEEK), equalTo(true))
        // Check that the result contains inherited attribute
        assertThat(resource.attributes!!.contains(SITE_BASEMENT), equalTo(true))
        deleteTwoLevelEntityAndParents(resourceEntity1, TEST_ZONE_1, this.resourceRepository)
    }

    @Test(threadPoolSize = CONCURRENT_TEST_THREAD_COUNT, invocationCount = CONCURRENT_TEST_INVOCATIONS)
    fun testGetByZoneAndResourceIdentifierWithEmptyAttributes() {

        val persistedResourceEntity = persistResourceToZoneAndAssert(
            TEST_ZONE_1,
            DRIVE_ID + randomNumber, emptySet()
        )

        val resource = this.resourceRepository!!
            .getByZoneAndResourceIdentifier(TEST_ZONE_1, persistedResourceEntity.resourceIdentifier!!)
        assertThat<ResourceEntity>(resource, equalTo(persistedResourceEntity))
        this.resourceRepository!!.delete(persistedResourceEntity)
    }

    @Test(threadPoolSize = CONCURRENT_TEST_THREAD_COUNT, invocationCount = CONCURRENT_TEST_INVOCATIONS)
    fun testGetByZoneAndResourceIdentifierWithNullAttributes() {
        val persistedResourceEntity = persistResourceToZoneAndAssert(
            TEST_ZONE_1,
            DRIVE_ID + randomNumber, null
        )
        val resource = this.resourceRepository!!
            .getByZoneAndResourceIdentifier(TEST_ZONE_1, persistedResourceEntity.resourceIdentifier!!)
        assertThat<ResourceEntity>(resource, equalTo(persistedResourceEntity))
        this.resourceRepository!!.delete(persistedResourceEntity)
    }

    @Test(threadPoolSize = CONCURRENT_TEST_THREAD_COUNT, invocationCount = CONCURRENT_TEST_INVOCATIONS)
    fun testGetByZoneAndResourceIdentifierWithInheritedAttributes3LevelHierarchical() {
        val expectedResource = persist3LevelRandomResourcetoZone1()

        val actualResource = this.resourceRepository!!
            .getResourceWithInheritedAttributes(TEST_ZONE_1, expectedResource.resourceIdentifier!!)
        assertThat(actualResource!!.attributes!!.contains(SITE_BASEMENT), equalTo(true))
        assertThat(actualResource.attributes!!.contains(TYPE_MONSTER_OF_THE_WEEK), equalTo(true))
        assertThat(actualResource.attributes!!.contains(TOP_SECRET_CLASSIFICATION), equalTo(true))
        deleteThreeLevelEntityAndParents(expectedResource, TEST_ZONE_1, this.resourceRepository!!)
    }

    @Test(expectedExceptions = [(SchemaViolationException::class)])
    fun testPreventEntityParentSelfReference() {
        val resource = ResourceEntity(TEST_ZONE_1, BASEMENT_SITE_ID)
        resource.attributes = BASEMENT_ATTRIBUTES
        resource.attributesAsJson = JSON_UTILS.serialize(resource.attributes!!)
        resource.parents = HashSet(listOf(Parent(BASEMENT_SITE_ID)))
        this.resourceRepository!!.save(resource)
    }

    /**
     * First we setup a 3 level graph where 3 -> 2 -> 1. Next, we try to update vertex 1 so that 1 -> 3. We expect
     * this to result in a SchemaViolationException because it would introduce a cyclic reference.
     */
    @Test
    fun testPreventEntityParentCyclicReference() {
        dropAllResources()
        assertThat(this.graphTraversalSource!!.V().count().next().toInt(), equalTo(0))
        val resource1 = persistResource0toZone1AndAssert()

        val resource2 = ResourceEntity(TEST_ZONE_1, DRIVE_ID)
        resource2.attributes = DRIVE_ATTRIBUTES
        resource2.attributesAsJson = JSON_UTILS.serialize(resource2.attributes!!)
        resource2.parents = HashSet(listOf(Parent(resource1.resourceIdentifier!!)))
        this.resourceRepository!!.save(resource2)

        val resource3 = ResourceEntity(TEST_ZONE_1, EVIDENCE_IMPLANT_ID)
        resource3.attributes = EVIDENCE_IMPLANT_ATTRIBUTES
        resource3.attributesAsJson = JSON_UTILS.serialize(resource3.attributes!!)
        resource3.parents = HashSet(listOf(Parent(resource2.resourceIdentifier!!)))
        this.resourceRepository!!.save(resource3)

        assertThat(this.graphTraversalSource!!.V().count().next().toInt(), equalTo(3))

        resource1.parents = HashSet(listOf(Parent(resource3.resourceIdentifier!!)))
        try {
            this.resourceRepository!!.save(resource1)
        } catch (ex: SchemaViolationException) {
            assertThat<String>(
                ex.message,
                equalTo("Updating entity '/site/basement' with parent '/evidence/implant' introduces a cyclic " + "reference.")
            )
            return
        }

        Assert.fail("save() did not throw the expected exception.")
        dropAllResources()
    }

    @Test(threadPoolSize = CONCURRENT_TEST_THREAD_COUNT, invocationCount = CONCURRENT_TEST_INVOCATIONS)
    fun testSave() {
        val resource = persistRandomResourcetoZone1AndAssert()
        val resourceId = resource.resourceIdentifier

        val traversal = this.graphTraversalSource!!.V().has(RESOURCE_ID_KEY, resourceId)

        assertThat(traversal.hasNext(), equalTo(true))
        assertThat(traversal.next().property<Any>(RESOURCE_ID_KEY).value(), equalTo<Any>(resourceId))
        this.resourceRepository!!.delete(resource)
    }

    @Test(expectedExceptions = [(IllegalArgumentException::class)])
    fun testSaveWithNoZoneName() {
        val resourceEntity = ResourceEntity()
        resourceEntity.resourceIdentifier = "testResource"
        resourceEntity.zone = ZoneEntity()
        this.resourceRepository!!.save(resourceEntity)
    }

    @Test(expectedExceptions = [(IllegalArgumentException::class)])
    fun testSaveWithNoZoneEntity() {
        val resourceEntity = ResourceEntity()
        resourceEntity.resourceIdentifier = "testResource"
        this.resourceRepository!!.save(resourceEntity)
    }

    @Test(threadPoolSize = CONCURRENT_TEST_THREAD_COUNT, invocationCount = CONCURRENT_TEST_INVOCATIONS)
    fun testSaveHierarchical() {
        val childResource = persist2LevelRandomResourcetoZone1()
        val childResourceId = childResource.resourceIdentifier

        var traversal = this.graphTraversalSource!!.V().has(RESOURCE_ID_KEY, childResourceId)
        assertThat(traversal.hasNext(), equalTo(true))
        val childResourceVertex = traversal.next()
        assertThat(childResourceVertex.property<Any>(RESOURCE_ID_KEY).value(), equalTo<Any>(childResourceId))

        val parent = childResource.parents.toTypedArray()[0]
        traversal = this.graphTraversalSource!!.V(childResourceVertex.id()).out("parent")
            .has(RESOURCE_ID_KEY, parent.identifier)
        assertThat(traversal.hasNext(), equalTo(true))
        val parentVertex = traversal.next()
        assertThat(parentVertex.property<Any>(RESOURCE_ID_KEY).value(), equalTo<Any>(parent.identifier))
        deleteTwoLevelEntityAndParents(childResource, TEST_ZONE_1, this.resourceRepository)
    }

    @Test(threadPoolSize = CONCURRENT_TEST_THREAD_COUNT, invocationCount = CONCURRENT_TEST_INVOCATIONS)
    fun testUpdateAttachedEntity() {
        val resourceEntity = persistRandomResourcetoZone1AndAssert()
        val resourceId = resourceEntity.resourceIdentifier

        val g = this.graphTraversalSource
        val traversal = g!!.V().has(RESOURCE_ID_KEY, resourceId)
        assertThat(traversal.hasNext(), equalTo(true))
        assertThat(traversal.next().property<Any>(RESOURCE_ID_KEY).value(), equalTo<Any>(resourceId))

        // Update the resource.
        val updateJSON = "{\'status':'test'}"
        resourceEntity.attributesAsJson = updateJSON
        saveWithRetry(this.resourceRepository!!, resourceEntity, 3)
        assertThat<String>(
            this.resourceRepository!!.getEntity(TEST_ZONE_1, resourceEntity.resourceIdentifier!!)!!
                .attributesAsJson, equalTo(updateJSON)
        )
        this.resourceRepository!!.delete(resourceEntity)
    }

    @Test(expectedExceptions = [(SchemaViolationException::class)])
    fun testUpdateUnattachedEntity() {
        val resourceId = persistRandomResourcetoZone1AndAssert().resourceIdentifier

        val g = this.graphTraversalSource
        val traversal = g!!.V().has(RESOURCE_ID_KEY, resourceId)
        assertThat(traversal.hasNext(), equalTo(true))
        assertThat(traversal.next().property<Any>(RESOURCE_ID_KEY).value(), equalTo<Any>(resourceId))

        // Save a new resource which violates the uniqueness constraint.
        persistResourceToZoneAndAssert(TEST_ZONE_1, resourceId, DRIVE_ATTRIBUTES)
    }

    @Test(threadPoolSize = CONCURRENT_TEST_THREAD_COUNT, invocationCount = CONCURRENT_TEST_INVOCATIONS)
    fun testSaveMultiple() {
        val resourceEntitiesToSave = ArrayList<ResourceEntity>()
        val resourceEntity1 = ResourceEntity(TEST_ZONE_1, DRIVE_ID + randomNumber)
        val resourceEntity2 = ResourceEntity(TEST_ZONE_1, JOSECHUNG_ID + randomNumber)
        resourceEntitiesToSave.add(resourceEntity1)
        resourceEntitiesToSave.add(resourceEntity2)
        this.resourceRepository!!.save(resourceEntitiesToSave)
        assertThat<ResourceEntity>(
            this.resourceRepository!!.getEntity(TEST_ZONE_1, resourceEntity1.resourceIdentifier!!),
            equalTo(resourceEntity1)
        )
        assertThat<ResourceEntity>(
            this.resourceRepository!!.getEntity(TEST_ZONE_1, resourceEntity2.resourceIdentifier!!),
            equalTo(resourceEntity2)
        )
        this.resourceRepository!!.delete(resourceEntity1)
        this.resourceRepository!!.delete(resourceEntity2)
    }

    @Test
    fun testSaveResourceWithNonexistentParent() {
        this.dropAllResources()
        assertThat(this.graphTraversalSource!!.V().count().next().toInt(), equalTo(0))
        val resource = ResourceEntity(TEST_ZONE_1, DRIVE_ID)
        resource.parents = HashSet(listOf(Parent(BASEMENT_SITE_ID)))

        // Save a resource with nonexistent parent which should throw IllegalStateException exception
        // while saving parent relationships.
        try {
            this.resourceRepository!!.save(resource)
        } catch (ex: IllegalStateException) {
            assertThat<String>(
                ex.message,
                equalTo("No parent exists in zone 'testzone1' with 'resourceId' value of '/site/basement'.")
            )
            return
        }

        Assert.fail("save() did not throw the expected IllegalStateException exception.")

    }

    @Test(enabled = false)
    @Throws(Exception::class)
    fun testPerformance() {
        for (i in 0..99999) {
            val stopWatch = StopWatch()
            stopWatch.start()
            this.resourceRepository!!.save(ResourceEntity(TEST_ZONE_1, String.format("/x-files/%d", i)))
            this.resourceRepository!!.getByZoneAndResourceIdentifier(TEST_ZONE_1, String.format("/x-files/%d", i))
            stopWatch.stop()
            println(String.format("Test %d took: %d ms", i, stopWatch.time))
            Thread.sleep(1L)
        }
    }

    @Test(
        expectedExceptions = [(AttributeLimitExceededException::class)],
        expectedExceptionsMessageRegExp = "The number of attributes on this resource .* has exceeded the maximum " + "limit of .*"
    )
    fun testSearchAttributesTraversalLimitException() {
        var traversalLimit = 256L
        try {
            val resource1 = persist3LevelRandomResourcetoZone1()
            traversalLimit = this.resourceRepository!!.traversalLimit
            assertThat(traversalLimit, equalTo(256L))
            this.resourceRepository!!.traversalLimit = 2
            assertThat(this.resourceRepository!!.traversalLimit, equalTo(2L))
            this.resourceRepository!!.getResourceWithInheritedAttributes(TEST_ZONE_1, resource1.resourceIdentifier!!)
        } finally {
            this.resourceRepository!!.traversalLimit = traversalLimit
            assertThat(this.resourceRepository!!.traversalLimit, equalTo(256L))
        }
    }

    @Test
    fun testSearchAttributesEqualsTraversalLimit() {
        var traversalLimit = 256L
        try {
            traversalLimit = this.resourceRepository!!.traversalLimit
            assertThat(traversalLimit, equalTo(256L))
            this.resourceRepository!!.traversalLimit = 3
            assertThat(this.resourceRepository!!.traversalLimit, equalTo(3L))
            val resource1 = persist3LevelRandomResourcetoZone1()
            val actualResource = this.resourceRepository!!
                .getResourceWithInheritedAttributes(TEST_ZONE_1, resource1.resourceIdentifier!!)
            assertThat(actualResource!!.attributes!!.size, equalTo(3))
        } finally {
            this.resourceRepository!!.traversalLimit = traversalLimit
            assertThat(this.resourceRepository!!.traversalLimit, equalTo(256L))
        }
    }

    @Test(threadPoolSize = CONCURRENT_TEST_THREAD_COUNT, invocationCount = CONCURRENT_TEST_INVOCATIONS)
    fun testGetResourceEntityAndDescendantsIds() {

        val basement = persistResourceToZoneAndAssert(
            TEST_ZONE_1, BASEMENT_SITE_ID + randomNumber,
            BASEMENT_ATTRIBUTES
        )

        val drive = persistResourceWithParentsToZoneAndAssert(
            TEST_ZONE_1, DRIVE_ID + randomNumber,
            DRIVE_ATTRIBUTES,
            HashSet(listOf(Parent(basement.resourceIdentifier!!)))
        )
        val ascension = persistResourceWithParentsToZoneAndAssert(
            TEST_ZONE_1,
            ASCENSION_ID + randomNumber, ASCENSION_ATTRIBUTES,
            HashSet(listOf(Parent(basement.resourceIdentifier!!)))
        )

        val implant = persistResourceWithParentsToZoneAndAssert(
            TEST_ZONE_1,
            EVIDENCE_IMPLANT_ID + randomNumber, EVIDENCE_IMPLANT_ATTRIBUTES, HashSet(
                Arrays.asList(
                    Parent(drive.resourceIdentifier!!),
                    Parent(ascension.resourceIdentifier!!)
                )
            )
        )
        val scullysTestimony = persistResourceWithParentsToZoneAndAssert(
            TEST_ZONE_1,
            EVIDENCE_SCULLYS_TESTIMONY_ID + randomNumber, SCULLYS_TESTIMONY_ATTRIBUTES,
            HashSet(listOf(Parent(ascension.resourceIdentifier!!)))
        )

        var descendantsIds = this.resourceRepository!!.getResourceEntityAndDescendantsIds(basement)

        assertThat(descendantsIds, hasSize(5))

        assertThat(
            descendantsIds, hasItems<String>(
                basement.resourceIdentifier,
                drive.resourceIdentifier,
                ascension.resourceIdentifier,
                implant.resourceIdentifier,
                scullysTestimony.resourceIdentifier
            )
        )

        descendantsIds = this.resourceRepository!!.getResourceEntityAndDescendantsIds(ascension)

        assertThat(descendantsIds, hasSize(3))

        assertThat(
            descendantsIds,
            hasItems<String>(
                ascension.resourceIdentifier,
                implant.resourceIdentifier,
                scullysTestimony.resourceIdentifier
            )
        )

        descendantsIds = this.resourceRepository!!.getResourceEntityAndDescendantsIds(drive)

        assertThat(descendantsIds, hasSize(2))

        assertThat(descendantsIds, hasItems<String>(drive.resourceIdentifier, implant.resourceIdentifier))

        descendantsIds = this.resourceRepository!!.getResourceEntityAndDescendantsIds(implant)

        assertThat(descendantsIds, hasSize(1))

        assertThat(descendantsIds, hasItems(implant.resourceIdentifier!!))

        descendantsIds = this.resourceRepository!!.getResourceEntityAndDescendantsIds(scullysTestimony)

        assertThat(descendantsIds, hasSize(1))

        assertThat(descendantsIds, hasItems(scullysTestimony.resourceIdentifier!!))

        descendantsIds = this.resourceRepository!!.getResourceEntityAndDescendantsIds(null)

        assertThat(descendantsIds, empty())

        descendantsIds = this.resourceRepository!!
            .getResourceEntityAndDescendantsIds(ResourceEntity(TEST_ZONE_1, "/nonexistent-resource"))

        assertThat(descendantsIds, empty())
        deleteThreeLevelEntityAndParents(basement, TEST_ZONE_1, this.resourceRepository!!)
    }

    @Test(expectedExceptions = [(IllegalStateException::class)])
    fun testChildWithDifferentZone() {
        val basement = persistResourceToZoneAndAssert(
            TEST_ZONE_1, BASEMENT_SITE_ID + randomNumber,
            BASEMENT_ATTRIBUTES
        )

        persistResourceWithParentsToZoneAndAssert(
            TEST_ZONE_2, DRIVE_ID + randomNumber, DRIVE_ATTRIBUTES,
            HashSet(listOf(Parent(basement.resourceIdentifier!!)))
        )

    }

    @Test
    fun testVersion() {
        this.dropAllResources()
        assertThat(this.graphTraversalSource!!.V().count().next().toInt(), equalTo(0))
        assertThat(this.resourceRepository!!.checkVersionVertexExists(1), equalTo(false))
        this.resourceRepository!!.createVersionVertex(1)
        assertThat(this.resourceRepository!!.checkVersionVertexExists(1), equalTo(true))
        assertThat(this.graphTraversalSource!!.V().count().next().toInt(), equalTo(1))

        // assert that createVersionVertex creates a new vertex.
        this.resourceRepository!!.createVersionVertex(2)
        assertThat(this.graphTraversalSource!!.V().count().next().toInt(), equalTo(2))
        dropAllResources()
    }

    private fun persist2LevelRandomResourcetoZone1(): ResourceEntity {
        val parentResource = persistResourceToZoneAndAssert(
            TEST_ZONE_1,
            BASEMENT_SITE_ID + randomNumber, BASEMENT_ATTRIBUTES
        )
        val parents = HashSet(
            listOf(Parent(parentResource.resourceIdentifier!!))
        )
        return persistResourceWithParentsToZoneAndAssert(
            TEST_ZONE_1, DRIVE_ID + randomNumber, DRIVE_ATTRIBUTES,
            parents
        )
    }

    private fun persist3LevelRandomResourcetoZone1(): ResourceEntity {
        val parentResource = persist2LevelRandomResourcetoZone1()
        val parents = HashSet(
            listOf(Parent(parentResource.resourceIdentifier!!))
        )
        return persistResourceWithParentsToZoneAndAssert(
            TEST_ZONE_1, EVIDENCE_IMPLANT_ID + randomNumber,
            EVIDENCE_IMPLANT_ATTRIBUTES, parents
        )
    }

    private fun persistResourceWithParentsToZoneAndAssert(
        zoneEntity: ZoneEntity,
        resourceIdentifier: String,
        attributes: Set<Attribute>,
        parents: Set<Parent>
    ): ResourceEntity {
        val resource = ResourceEntity(zoneEntity, resourceIdentifier)
        resource.attributes = attributes
        resource.attributesAsJson = JSON_UTILS.serialize(resource.attributes!!)
        resource.parents = parents
        val resourceEntity = saveWithRetry(this.resourceRepository!!, resource, 3)
        assertThat<ResourceEntity>(this.resourceRepository!!.findOne(resourceEntity.id), equalTo(resource))
        return resourceEntity
    }

    private fun persistRandomResourcetoZone1AndAssert(): ResourceEntity {
        return persistResourceToZoneAndAssert(TEST_ZONE_1, DRIVE_ID + randomNumber, DRIVE_ATTRIBUTES)
    }

    private fun persistResource0toZone1AndAssert(): ResourceEntity {
        return persistResourceToZoneAndAssert(TEST_ZONE_1, BASEMENT_SITE_ID, BASEMENT_ATTRIBUTES)
    }

    private fun persistResource2toZone1AndAssert(): ResourceEntity {
        return persistResourceToZoneAndAssert(TEST_ZONE_1, JOSECHUNG_ID, DRIVE_ATTRIBUTES)
    }

    private fun persistResourceToZoneAndAssert(
        zoneEntity: ZoneEntity,
        resourceIdentifier: String?,
        attributes: Set<Attribute>?
    ): ResourceEntity {

        val resource = ResourceEntity(zoneEntity, resourceIdentifier!!)
        resource.attributes = attributes
        resource.attributesAsJson = JSON_UTILS.serialize(resource.attributes!!)
        val resourceEntity = saveWithRetry(this.resourceRepository!!, resource, 3)
        assertThat<ResourceEntity>(this.resourceRepository!!.findOne(resourceEntity.id), equalTo(resource))
        return resourceEntity
    }
}
