/*******************************************************************************
 * Copyright 2018 General Electric Company
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

package org.eclipse.keti.acs.privilege.management.dao

import com.nhaarman.mockito_kotlin.any
import org.eclipse.keti.acs.zone.management.dao.ZoneEntity
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.mockito.Mockito
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.testng.annotations.BeforeClass
import org.testng.annotations.Test
import java.util.ArrayList

class GraphMigrationManagerTest {

    private var resourceRepository: ResourceRepository? = null
    private var resourceHierarchicalRepository: GraphResourceRepository? = null
    private var subjectRepository: SubjectRepository? = null
    private var subjectHierarchicalRepository: GraphSubjectRepository? = null

    private val fromListForResource = ArrayList<ResourceEntity>()
    private val fromListForSubject = ArrayList<SubjectEntity>()
    private val toListForResource = ArrayList<ResourceEntity>()
    private val toListForSubject = ArrayList<SubjectEntity>()

    @BeforeClass
    @Throws(Exception::class)
    fun setup() {
        this.resourceRepository = Mockito.mock(ResourceRepository::class.java)
        this.subjectRepository = Mockito.mock(SubjectRepository::class.java)
        this.resourceHierarchicalRepository = Mockito.mock(GraphResourceRepository::class.java)
        this.subjectHierarchicalRepository = Mockito.mock(GraphSubjectRepository::class.java)
    }

    // Save the given Resource entities to a local list to mock graph-save
    private fun saveResourcesToGraph(entities: Iterable<ResourceEntity>): List<ResourceEntity> {
        val savedEntities = ArrayList<ResourceEntity>()
        entities.forEach { item -> savedEntities.add(item) }
        toListForResource.addAll(savedEntities)
        return savedEntities
    }

    private fun saveSubjectsToGraph(entities: Iterable<SubjectEntity>): List<SubjectEntity> {
        val savedEntities = ArrayList<SubjectEntity>()
        entities.forEach { item -> savedEntities.add(item) }
        toListForSubject.addAll(savedEntities)
        return savedEntities
    }

    @Test
    fun migrationManagerTest() {
        val zone1 = ZoneEntity(1.toLong(), "testzone1")

        val entityResource1 = ResourceEntity(zone1, "testresource1")
        val entityResource2 = ResourceEntity(zone1, "testresource2")
        val entitySubject1 = SubjectEntity(zone1, "testsubject1")
        val entitySubject2 = SubjectEntity(zone1, "testsubject2")

        Mockito.`when`(this.resourceRepository!!.findAll()).thenReturn(fromListForResource)
        Mockito.`when`(this.resourceHierarchicalRepository!!.findAll()).thenReturn(this.toListForResource)
        Mockito.`when`(this.subjectRepository!!.findAll()).thenReturn(fromListForSubject)
        Mockito.`when`(this.subjectHierarchicalRepository!!.findAll()).thenReturn(this.toListForSubject)

        // Verify that both graph and non-graph repos are empty
        assertThat(this.resourceRepository!!.findAll().size, equalTo(0))
        assertThat(this.resourceHierarchicalRepository!!.findAll().size, equalTo(0))
        assertThat(this.subjectRepository!!.findAll().size, equalTo(0))
        assertThat(this.subjectHierarchicalRepository!!.findAll().size, equalTo(0))

        fromListForResource.addAll(listOf(entityResource1, entityResource2))
        fromListForSubject.addAll(listOf(entitySubject1, entitySubject2))

        // This mocked findAll() takes in a Pageable and returns a Page.
        Mockito
            .`when`<Page<ResourceEntity>>(this.resourceRepository!!.findAll(any<Pageable>()))
            .thenAnswer { PageImpl(fromListForResource, null, fromListForResource.size.toLong()) }
        Mockito
            .`when`<Page<SubjectEntity>>(this.subjectRepository!!.findAll(any<Pageable>()))
            .thenAnswer { PageImpl(fromListForSubject, null, fromListForSubject.size.toLong()) }
        Mockito.`when`(this.resourceRepository!!.count()).thenReturn(fromListForResource.size.toLong())
        Mockito.`when`(this.subjectRepository!!.count()).thenReturn(fromListForSubject.size.toLong())

        // Verify that the non-graph repos are populated
        assertThat(this.resourceRepository!!.findAll().size, equalTo(2))
        assertThat(this.resourceHierarchicalRepository!!.findAll().size, equalTo(0))

        assertThat(this.subjectRepository!!.findAll().size, equalTo(2))
        assertThat(this.subjectHierarchicalRepository!!.findAll().size, equalTo(0))

        // Mock graph-save function with a locally defined saveResourcesToGraph()
        Mockito.`when`<List<ResourceEntity>>(
            this.resourceHierarchicalRepository!!.save(
                any<Collection<ResourceEntity>>()
            )
        )
            .thenAnswer { invocation ->
                val args = invocation.arguments
                saveResourcesToGraph(args[0] as List<ResourceEntity>)
            }
        Mockito.`when`<List<SubjectEntity>>(
            this.subjectHierarchicalRepository!!.save(
                any<Collection<SubjectEntity>>()
            )
        )
            .thenAnswer { invocation ->
                val args = invocation.arguments
                val list = args[0] as List<SubjectEntity>
                saveSubjectsToGraph(list)
            }

        ResourceMigrationManager().doResourceMigration(
            resourceRepository!!,
            resourceHierarchicalRepository!!,
            PAGE_SIZE
        )
        SubjectMigrationManager().doSubjectMigration(
            subjectRepository!!,
            subjectHierarchicalRepository!!,
            PAGE_SIZE
        )

        // Verify that the data from non-graph repo has been copied over to graph-repo, post-migration.
        assertThat(this.resourceRepository!!.findAll().size, equalTo(2))
        assertThat(this.resourceHierarchicalRepository!!.findAll().size, equalTo(2))

        assertThat(this.subjectRepository!!.findAll().size, equalTo(2))
        assertThat(this.subjectHierarchicalRepository!!.findAll().size, equalTo(2))
    }
}
