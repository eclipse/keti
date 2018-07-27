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

import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource
import org.apache.tinkerpop.gremlin.util.iterator.IteratorUtils
import org.eclipse.keti.acs.config.createSchemaElements
import org.eclipse.keti.acs.model.Attribute
import org.eclipse.keti.acs.rest.Parent
import org.eclipse.keti.acs.testutils.AGENT_MULDER
import org.eclipse.keti.acs.testutils.AGENT_SCULLY
import org.eclipse.keti.acs.testutils.FBI
import org.eclipse.keti.acs.testutils.FBI_ATTRIBUTES
import org.eclipse.keti.acs.testutils.MULDERS_ATTRIBUTES
import org.eclipse.keti.acs.testutils.PENTAGON_ATTRIBUTES
import org.eclipse.keti.acs.testutils.SECRET_CLASSIFICATION
import org.eclipse.keti.acs.testutils.SECRET_GROUP
import org.eclipse.keti.acs.testutils.SECRET_GROUP_ATTRIBUTES
import org.eclipse.keti.acs.testutils.SITE_BASEMENT
import org.eclipse.keti.acs.testutils.SITE_PENTAGON
import org.eclipse.keti.acs.testutils.SPECIAL_AGENTS_GROUP
import org.eclipse.keti.acs.testutils.SPECIAL_AGENTS_GROUP_ATTRIBUTES
import org.eclipse.keti.acs.testutils.TOP_SECRET_CLASSIFICATION
import org.eclipse.keti.acs.testutils.TOP_SECRET_GROUP
import org.eclipse.keti.acs.testutils.TOP_SECRET_GROUP_ATTRIBUTES
import org.eclipse.keti.acs.utils.JsonUtils
import org.eclipse.keti.acs.zone.management.dao.ZoneEntity
import org.hamcrest.CoreMatchers.notNullValue
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.empty
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.hasItems
import org.hamcrest.Matchers.hasSize
import org.janusgraph.core.JanusGraphException
import org.janusgraph.core.JanusGraphFactory
import org.testng.annotations.BeforeClass
import org.testng.annotations.Test
import java.util.Arrays
import java.util.HashSet
import java.util.Random

private val JSON_UTILS = JsonUtils()
private val TEST_ZONE_1 = ZoneEntity(1L, "testzone1")
private val TEST_ZONE_2 = ZoneEntity(2L, "testzone2")
private const val CONCURRENT_TEST_THREAD_COUNT = 3
private const val CONCURRENT_TEST_INVOCATIONS = 20

class GraphSubjectRepositoryTest {

    private var subjectRepository: GraphSubjectRepository? = null
    private var graphTraversalSource: GraphTraversalSource? = null
    private val randomGenerator = Random()

    private val randomNumber: Int
        get() = randomGenerator.nextInt(10000000)

    @BeforeClass
    @Throws(Exception::class)
    fun setup() {
        this.subjectRepository = GraphSubjectRepository()
        val graph = JanusGraphFactory.build().set("storage.backend", "inmemory").open()
        createSchemaElements(graph)
        this.graphTraversalSource = graph.traversal()
        this.subjectRepository!!.graphTraversal = this.graphTraversalSource!!
    }

    @Test(threadPoolSize = CONCURRENT_TEST_THREAD_COUNT, invocationCount = CONCURRENT_TEST_INVOCATIONS)
    fun testGetByZoneAndSubjectIdentifier() {
        val subjectEntityForZone1 = persistRandomSubjectToZone1AndAssert()
        val subjectEntityForZone2 = persistRandomSubjectToZone2AndAssert()

        val actualSubjectForZone1 = this.subjectRepository!!
            .getByZoneAndSubjectIdentifier(TEST_ZONE_1, subjectEntityForZone1.subjectIdentifier!!)
        val actualSubjectForZone2 = this.subjectRepository!!
            .getByZoneAndSubjectIdentifier(TEST_ZONE_2, subjectEntityForZone2.subjectIdentifier!!)
        assertThat<SubjectEntity>(actualSubjectForZone1, equalTo(subjectEntityForZone1))
        assertThat<SubjectEntity>(actualSubjectForZone2, equalTo(subjectEntityForZone2))
        this.subjectRepository!!.delete(subjectEntityForZone1)
        this.subjectRepository!!.delete(subjectEntityForZone2)
    }

    @Test(threadPoolSize = CONCURRENT_TEST_THREAD_COUNT, invocationCount = CONCURRENT_TEST_INVOCATIONS)
    fun testGetByZoneAndSubjectIdentifierAndScopes() {
        val expectedSubject = persistScopedHierarchy(AGENT_MULDER + randomNumber, SITE_BASEMENT)
        val subjectIdentifier = expectedSubject.subjectIdentifier

        var expectedAttributes = HashSet(
            Arrays.asList(SECRET_CLASSIFICATION, TOP_SECRET_CLASSIFICATION, SITE_BASEMENT)
        )
        expectedSubject.attributes = expectedAttributes
        expectedSubject.attributesAsJson = JSON_UTILS.serialize(expectedAttributes)

        var actualSubject = this.subjectRepository!!
            .getSubjectWithInheritedAttributesForScopes(
                TEST_ZONE_1, subjectIdentifier!!,
                HashSet(listOf(SITE_BASEMENT))
            )
        assertThat<SubjectEntity>(actualSubject, equalTo(expectedSubject))

        expectedAttributes = HashSet(Arrays.asList(SECRET_CLASSIFICATION, SITE_BASEMENT))
        expectedSubject.attributes = expectedAttributes
        expectedSubject.attributesAsJson = JSON_UTILS.serialize(expectedAttributes)
        actualSubject = this.subjectRepository!!
            .getSubjectWithInheritedAttributesForScopes(
                TEST_ZONE_1, subjectIdentifier,
                HashSet(listOf(SITE_PENTAGON))
            )
        assertThat<SubjectEntity>(actualSubject, equalTo(expectedSubject))
        deleteTwoLevelEntityAndParents(expectedSubject, TEST_ZONE_1, this.subjectRepository)
    }

    @Test(threadPoolSize = CONCURRENT_TEST_THREAD_COUNT, invocationCount = CONCURRENT_TEST_INVOCATIONS)
    fun testParentAndChildSameAttribute() {
        val agentScully = SubjectEntity(TEST_ZONE_1, AGENT_SCULLY + randomNumber)
        agentScully.attributes = MULDERS_ATTRIBUTES
        agentScully.attributesAsJson = JSON_UTILS.serialize(agentScully.attributes!!)
        saveWithRetry(agentScully, 3)
        val agentMulder = SubjectEntity(TEST_ZONE_1, AGENT_MULDER + randomNumber)
        agentMulder.attributes = MULDERS_ATTRIBUTES
        agentMulder.attributesAsJson = JSON_UTILS.serialize(agentMulder.attributes!!)
        agentMulder
            .parents = HashSet(listOf(Parent(agentScully.subjectIdentifier!!)))
        saveWithRetry(agentMulder, 3)
        val actualAgentMulder = this.subjectRepository!!
            .getSubjectWithInheritedAttributesForScopes(TEST_ZONE_1, agentMulder.subjectIdentifier!!, null)
        assertThat<String>(
            actualAgentMulder!!.attributesAsJson,
            equalTo("[{\"issuer\":\"acs.example.org\",\"name\":\"site\",\"value\":\"basement\"}]")
        )
        deleteTwoLevelEntityAndParents(agentScully, TEST_ZONE_1, this.subjectRepository)
    }

    @Test(threadPoolSize = CONCURRENT_TEST_THREAD_COUNT, invocationCount = CONCURRENT_TEST_INVOCATIONS)
    fun testParentAndChildAttributeSameNameDifferentValues() {
        val agentScully = SubjectEntity(TEST_ZONE_1, AGENT_SCULLY + randomNumber)
        agentScully.attributes = PENTAGON_ATTRIBUTES
        agentScully.attributesAsJson = JSON_UTILS.serialize(agentScully.attributes!!)
        saveWithRetry(agentScully, 3)
        val agentMulder = SubjectEntity(TEST_ZONE_1, AGENT_MULDER + randomNumber)
        agentMulder.attributes = MULDERS_ATTRIBUTES
        agentMulder.attributesAsJson = JSON_UTILS.serialize(agentMulder.attributes!!)
        agentMulder
            .parents = HashSet(listOf(Parent(agentScully.subjectIdentifier!!)))
        saveWithRetry(agentMulder, 3)
        val actualAgentMulder = this.subjectRepository!!
            .getSubjectWithInheritedAttributesForScopes(TEST_ZONE_1, agentMulder.subjectIdentifier!!, null)
        assertThat<String>(
            actualAgentMulder!!.attributesAsJson,
            equalTo("[{\"issuer\":\"acs.example.org\",\"name\":\"site\",\"value\":\"basement\"}," + "{\"issuer\":\"acs.example.org\",\"name\":\"site\",\"value\":\"pentagon\"}]")
        )
        deleteTwoLevelEntityAndParents(agentScully, TEST_ZONE_1, this.subjectRepository)
    }

    @Test(threadPoolSize = CONCURRENT_TEST_THREAD_COUNT, invocationCount = CONCURRENT_TEST_INVOCATIONS)
    fun testSave() {
        val subjectEntity = persistRandomSubjectToZone1AndAssert()
        val subjectId = subjectEntity.subjectIdentifier
        val traversal = this.graphTraversalSource!!.V().has(SUBJECT_ID_KEY, subjectId)
        assertThat(traversal.hasNext(), equalTo(true))
        assertThat(traversal.next().property<Any>(SUBJECT_ID_KEY).value(), equalTo<Any>(subjectId))
        this.subjectRepository!!.delete(subjectEntity)
    }

    @Test(threadPoolSize = CONCURRENT_TEST_THREAD_COUNT, invocationCount = CONCURRENT_TEST_INVOCATIONS)
    fun testSaveWithNoAttributes() {
        val subject = SubjectEntity(TEST_ZONE_1, AGENT_SCULLY + randomNumber)
        saveWithRetry(subject, 3)
        assertThat<SubjectEntity>(
            this.subjectRepository!!.getByZoneAndSubjectIdentifier(TEST_ZONE_1, subject.subjectIdentifier!!),
            equalTo(subject)
        )
        this.subjectRepository!!.delete(subject)
    }

    @Test(threadPoolSize = CONCURRENT_TEST_THREAD_COUNT, invocationCount = CONCURRENT_TEST_INVOCATIONS)
    fun testSaveScopes() {
        val subject = persistScopedHierarchy(AGENT_MULDER + randomNumber, SITE_BASEMENT)
        assertThat(
            IteratorUtils.count(
                this.graphTraversalSource!!.V(subject.id!!).outE(
                    PARENT_EDGE_LABEL
                )
            ),
            equalTo(2L)
        )

        // Persist again (i.e. update) and make sure vertex and edge count are stable.
        this.subjectRepository!!.save(subject)
        assertThat(
            IteratorUtils.count(
                this.graphTraversalSource!!.V(subject.id!!).outE(
                    PARENT_EDGE_LABEL
                )
            ),
            equalTo(2L)
        )

        var parent: Parent? = null
        for (tempParent in this.subjectRepository!!.findOne(subject.id)!!.parents) {
            if (tempParent.identifier!!.contains(TOP_SECRET_GROUP)) {
                parent = tempParent
            }
        }
        assertThat<Parent>(parent, notNullValue())
        assertThat("Expected scope not found on subject.", parent!!.scopes.contains(SITE_BASEMENT))
        deleteTwoLevelEntityAndParents(subject, TEST_ZONE_1, this.subjectRepository)
    }

    @Test(threadPoolSize = CONCURRENT_TEST_THREAD_COUNT, invocationCount = CONCURRENT_TEST_INVOCATIONS)
    fun testGetSubjectEntityAndDescendantsIds() {

        val fbi = persistSubjectToZoneAndAssert(TEST_ZONE_1, FBI + randomNumber, FBI_ATTRIBUTES)

        val specialAgentsGroup = persistSubjectWithParentsToZoneAndAssert(
            TEST_ZONE_1,
            SPECIAL_AGENTS_GROUP + randomNumber, SPECIAL_AGENTS_GROUP_ATTRIBUTES,
            HashSet(listOf(Parent(fbi.subjectIdentifier!!)))
        )

        val topSecretGroup = persistSubjectToZoneAndAssert(
            TEST_ZONE_1, TOP_SECRET_GROUP + randomNumber,
            TOP_SECRET_GROUP_ATTRIBUTES
        )

        val agentMulder = persistSubjectWithParentsToZoneAndAssert(
            TEST_ZONE_1,
            AGENT_MULDER + randomNumber, MULDERS_ATTRIBUTES, HashSet(
                Arrays.asList(
                    Parent(specialAgentsGroup.subjectIdentifier!!),
                    Parent(topSecretGroup.subjectIdentifier!!)
                )
            )
        )

        var descendantsIds = this.subjectRepository!!.getSubjectEntityAndDescendantsIds(fbi)
        assertThat(descendantsIds, hasSize(3))
        assertThat(
            descendantsIds, hasItems<String>(
                fbi.subjectIdentifier, specialAgentsGroup.subjectIdentifier,
                agentMulder.subjectIdentifier
            )
        )

        descendantsIds = this.subjectRepository!!.getSubjectEntityAndDescendantsIds(specialAgentsGroup)
        assertThat(descendantsIds, hasSize(2))
        assertThat(
            descendantsIds,
            hasItems<String>(specialAgentsGroup.subjectIdentifier, agentMulder.subjectIdentifier)
        )

        descendantsIds = this.subjectRepository!!.getSubjectEntityAndDescendantsIds(topSecretGroup)
        assertThat(descendantsIds, hasSize(2))
        assertThat(descendantsIds, hasItems<String>(topSecretGroup.subjectIdentifier, agentMulder.subjectIdentifier))

        descendantsIds = this.subjectRepository!!.getSubjectEntityAndDescendantsIds(agentMulder)
        assertThat(descendantsIds, hasSize(1))
        assertThat(descendantsIds, hasItems(agentMulder.subjectIdentifier!!))

        descendantsIds = this.subjectRepository!!.getSubjectEntityAndDescendantsIds(null)
        assertThat(descendantsIds, empty())

        descendantsIds = this.subjectRepository!!
            .getSubjectEntityAndDescendantsIds(SubjectEntity(TEST_ZONE_1, "/nonexistent-subject"))
        assertThat(descendantsIds, empty())
        deleteTwoLevelEntityAndParents(agentMulder, TEST_ZONE_1, this.subjectRepository)
    }

    private fun persistRandomSubjectToZone1AndAssert(): SubjectEntity {
        return persistSubjectToZoneAndAssert(TEST_ZONE_1, AGENT_SCULLY + randomNumber, emptySet())
    }

    private fun persistRandomSubjectToZone2AndAssert(): SubjectEntity {
        return persistSubjectToZoneAndAssert(TEST_ZONE_2, AGENT_SCULLY + randomNumber, emptySet())
    }

    private fun persistRandomTopSecretGroupAndAssert(): SubjectEntity {
        return persistSubjectToZoneAndAssert(
            TEST_ZONE_1, TOP_SECRET_GROUP + randomNumber,
            TOP_SECRET_GROUP_ATTRIBUTES
        )
    }

    private fun persistRandomSecretGroupAndAssert(): SubjectEntity {
        return persistSubjectToZoneAndAssert(TEST_ZONE_1, SECRET_GROUP + randomNumber, SECRET_GROUP_ATTRIBUTES)
    }

    private fun persistSubjectToZoneAndAssert(
        zone: ZoneEntity,
        subjectIdentifier: String,
        attributes: Set<Attribute>
    ): SubjectEntity {
        val subject = SubjectEntity(zone, subjectIdentifier)
        subject.attributes = attributes
        subject.attributesAsJson = JSON_UTILS.serialize(subject.attributes!!)
        val subjectEntity = saveWithRetry(subject, 3)
        assertThat<SubjectEntity>(this.subjectRepository!!.findOne(subjectEntity.id), equalTo(subject))
        return subjectEntity
    }

    private fun persistSubjectWithParentsToZoneAndAssert(
        zoneEntity: ZoneEntity,
        subjectIdentifier: String,
        attributes: Set<Attribute>,
        parents: Set<Parent>
    ): SubjectEntity {
        val subject = SubjectEntity(zoneEntity, subjectIdentifier)
        subject.attributes = attributes
        subject.attributesAsJson = JSON_UTILS.serialize(subject.attributes!!)
        subject.parents = parents
        val subjectEntity = saveWithRetry(subject, 3)
        assertThat<SubjectEntity>(this.subjectRepository!!.findOne(subjectEntity.id), equalTo(subject))
        return subjectEntity
    }

    private fun persistScopedHierarchy(
        subjectIdentifier: String,
        scope: Attribute
    ): SubjectEntity {
        val secretGroup = persistRandomSecretGroupAndAssert()
        val topSecretGroup = persistRandomTopSecretGroupAndAssert()

        val agentMulder = SubjectEntity(TEST_ZONE_1, subjectIdentifier)
        agentMulder.attributes = MULDERS_ATTRIBUTES
        agentMulder.attributesAsJson = JSON_UTILS.serialize(agentMulder.attributes!!)
        agentMulder.parents = HashSet(
            Arrays.asList(
                Parent(topSecretGroup.subjectIdentifier!!, HashSet(listOf(scope))),
                Parent(secretGroup.subjectIdentifier!!)
            )
        )
        return this.subjectRepository!!.save(agentMulder)
    }

    @Throws(JanusGraphException::class)
    private fun saveWithRetry(
        subject: SubjectEntity,
        retryCount: Int
    ): SubjectEntity {
        return saveWithRetry(this.subjectRepository!!, subject, retryCount)
    }
}
