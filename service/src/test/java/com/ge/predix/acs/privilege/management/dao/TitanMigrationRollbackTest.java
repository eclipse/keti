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

package com.ge.predix.acs.privilege.management.dao;

import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

import org.mockito.Mockito;
import org.mockito.internal.util.reflection.Whitebox;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class TitanMigrationRollbackTest {

    private final GraphResourceRepository resourceHierarchicalRepository = mock(
            GraphResourceRepository.class);
    private final SubjectHierarchicalRepository subjectHierarchicalRepository = mock(GraphSubjectRepository.class);
    private ResourceMigrationManager resourceMigrationManager;
    private TitanMigrationManager titanMigrationManager = new TitanMigrationManager();
    private SubjectMigrationManager subjectMigrationManager;

    @BeforeMethod
    public void beforeMethod() {
        this.resourceMigrationManager = Mockito.mock(ResourceMigrationManager.class);
        this.subjectMigrationManager = Mockito.mock(SubjectMigrationManager.class);

        Whitebox.setInternalState(this.titanMigrationManager, "resourceHierarchicalRepository",
                this.resourceHierarchicalRepository);
        Whitebox.setInternalState(this.titanMigrationManager, "subjectHierarchicalRepository",
                this.subjectHierarchicalRepository);

        Whitebox.setInternalState(this.titanMigrationManager, "resourceMigrationManager",
                this.resourceMigrationManager);
        Whitebox.setInternalState(this.titanMigrationManager, "subjectMigrationManager",
                this.subjectMigrationManager);
    }

    @Test
    public void testMigrationRollbackCalled() throws InterruptedException {
        Mockito.when(this.resourceHierarchicalRepository.checkVersionVertexExists(1)).thenReturn(false);
        this.titanMigrationManager.doMigration();

        // This sleep is because TitanMigrationManager invokes resourceMigrationManager asynchronously
        Thread.sleep(100);

        Mockito.verify(this.subjectMigrationManager).rollbackMigratedData(anyObject());
        Mockito.verify(this.resourceMigrationManager).rollbackMigratedData(anyObject());
    }

    @Test
    public void testMigrationRollbackNotCalled() throws InterruptedException {
        Mockito.when(this.resourceHierarchicalRepository.checkVersionVertexExists(1)).thenReturn(true);
        this.titanMigrationManager.doMigration();

        // This sleep is because TitanMigrationManager invokes resourceMigrationManager asynchronously
        Thread.sleep(100);

        Mockito.verify(this.subjectMigrationManager, times(0)).rollbackMigratedData(anyObject());
        Mockito.verify(this.resourceMigrationManager, times(0)).rollbackMigratedData(anyObject());
    }

}
