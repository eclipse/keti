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
import org.mockito.Mockito
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.springframework.test.util.ReflectionTestUtils
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Test

class GraphMigrationRollbackTest {

    private val resourceHierarchicalRepository = mock(
        GraphResourceRepository::class.java
    )
    private val subjectHierarchicalRepository = mock(GraphSubjectRepository::class.java)
    private var resourceMigrationManager: ResourceMigrationManager? = null
    private val graphMigrationManager = GraphMigrationManager()
    private var subjectMigrationManager: SubjectMigrationManager? = null

    @BeforeMethod
    fun beforeMethod() {
        this.resourceMigrationManager = Mockito.mock(ResourceMigrationManager::class.java)
        this.subjectMigrationManager = Mockito.mock(SubjectMigrationManager::class.java)

        ReflectionTestUtils.setField(
            this.graphMigrationManager, "resourceHierarchicalRepository",
            this.resourceHierarchicalRepository
        )
        ReflectionTestUtils.setField(
            this.graphMigrationManager, "subjectHierarchicalRepository",
            this.subjectHierarchicalRepository
        )

        ReflectionTestUtils.setField(
            this.graphMigrationManager, "resourceMigrationManager",
            this.resourceMigrationManager
        )
        ReflectionTestUtils.setField(
            this.graphMigrationManager, "subjectMigrationManager",
            this.subjectMigrationManager
        )
    }

    @Test
    @Throws(InterruptedException::class)
    fun testMigrationRollbackCalled() {
        Mockito.`when`(this.resourceHierarchicalRepository.checkVersionVertexExists(1)).thenReturn(false)
        this.graphMigrationManager.doMigration()

        // This sleep is because we invoke resourceMigrationManager asynchronously
        Thread.sleep(100)

        Mockito.verify<SubjectMigrationManager>(this.subjectMigrationManager)
            .rollbackMigratedData(any())
        Mockito.verify<ResourceMigrationManager>(this.resourceMigrationManager)
            .rollbackMigratedData(any())
    }

    @Test
    @Throws(InterruptedException::class)
    fun testMigrationRollbackNotCalled() {
        Mockito.`when`(this.resourceHierarchicalRepository.checkVersionVertexExists(1)).thenReturn(true)
        this.graphMigrationManager.doMigration()

        // This sleep is because we invoke resourceMigrationManager asynchronously
        Thread.sleep(100)

        Mockito.verify<SubjectMigrationManager>(this.subjectMigrationManager, times(0))
            .rollbackMigratedData(any())
        Mockito.verify<ResourceMigrationManager>(this.resourceMigrationManager, times(0))
            .rollbackMigratedData(any())
    }
}
