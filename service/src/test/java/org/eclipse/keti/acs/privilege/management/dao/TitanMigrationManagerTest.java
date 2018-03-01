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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyCollectionOf;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import org.eclipse.keti.acs.zone.management.dao.ZoneEntity;

public class TitanMigrationManagerTest {

    private ResourceRepository resourceRepository;
    private GraphResourceRepository resourceHierarchicalRepository;
    private SubjectRepository subjectRepository;
    private GraphSubjectRepository subjectHierarchicalRepository;

    private List<ResourceEntity> fromListForResource = new ArrayList<ResourceEntity>();
    private List<SubjectEntity> fromListForSubject = new ArrayList<SubjectEntity>();
    private List<ResourceEntity> toListForResource = new ArrayList<ResourceEntity>();
    private List<SubjectEntity> toListForSubject = new ArrayList<SubjectEntity>();

    @BeforeClass
    public void setup() throws Exception {
        this.resourceRepository = Mockito.mock(ResourceRepository.class);
        this.subjectRepository = Mockito.mock(SubjectRepository.class);
        this.resourceHierarchicalRepository = Mockito.mock(GraphResourceRepository.class);
        this.subjectHierarchicalRepository = Mockito.mock(GraphSubjectRepository.class);
    }

    // Save the given Resource entities to a local list to mock graph-save
    private List<ResourceEntity> saveResourcesToGraph(final Iterable<ResourceEntity> entities) {
        List<ResourceEntity> savedEntities = new ArrayList<>();
        entities.forEach(item -> savedEntities.add(item));
        toListForResource.addAll(savedEntities);
        return savedEntities;
    }

    private List<SubjectEntity> saveSubjectsToGraph(final Iterable<SubjectEntity> entities) {
        List<SubjectEntity> savedEntities = new ArrayList<>();
        entities.forEach(item -> savedEntities.add(item));
        toListForSubject.addAll(savedEntities);
        return savedEntities;
    }

    @Test
    public void migrationManagerTest() {
        ZoneEntity zone1 = new ZoneEntity((long) 1, "testzone1");

        ResourceEntity entityResource1 = new ResourceEntity(zone1, "testresource1");
        ResourceEntity entityResource2 = new ResourceEntity(zone1, "testresource2");
        SubjectEntity entitySubject1 = new SubjectEntity(zone1, "testsubject1");
        SubjectEntity entitySubject2 = new SubjectEntity(zone1, "testsubject2");

        Mockito.when(this.resourceRepository.findAll()).thenReturn(fromListForResource);
        Mockito.when(this.resourceHierarchicalRepository.findAll()).thenReturn(this.toListForResource);
        Mockito.when(this.subjectRepository.findAll()).thenReturn(fromListForSubject);
        Mockito.when(this.subjectHierarchicalRepository.findAll()).thenReturn(this.toListForSubject);

        // Verify that both graph and non-graph repos are empty
        assertThat(this.resourceRepository.findAll().size(), equalTo(0));
        assertThat(this.resourceHierarchicalRepository.findAll().size(), equalTo(0));
        assertThat(this.subjectRepository.findAll().size(), equalTo(0));
        assertThat(this.subjectHierarchicalRepository.findAll().size(), equalTo(0));

        fromListForResource.addAll(Arrays.asList(entityResource1, entityResource2));
        fromListForSubject.addAll(Arrays.asList(entitySubject1, entitySubject2));

        // This mocked findAll() takes in a Pageable and returns a Page.
        Mockito.when(this.resourceRepository.findAll(any(Pageable.class)))
                .thenAnswer(new Answer<PageImpl<ResourceEntity>>() {
                    public PageImpl<ResourceEntity> answer(final InvocationOnMock invocation) {
                        return new PageImpl<ResourceEntity>(fromListForResource, null, fromListForResource.size());
                    }
                });
        Mockito.when(this.subjectRepository.findAll(any(Pageable.class)))
                .thenAnswer(new Answer<PageImpl<SubjectEntity>>() {
                    public PageImpl<SubjectEntity> answer(final InvocationOnMock invocation) {
                        return new PageImpl<SubjectEntity>(fromListForSubject, null, fromListForSubject.size());
                    }
                });
        Mockito.when(this.resourceRepository.count()).thenReturn((long) fromListForResource.size());
        Mockito.when(this.subjectRepository.count()).thenReturn((long) fromListForSubject.size());

        // Verify that the non-graph repos are populated
        assertThat(this.resourceRepository.findAll().size(), equalTo(2));
        assertThat(this.resourceHierarchicalRepository.findAll().size(), equalTo(0));

        assertThat(this.subjectRepository.findAll().size(), equalTo(2));
        assertThat(this.subjectHierarchicalRepository.findAll().size(), equalTo(0));

        // Mock graph-save function with a locally defined saveResourcesToGraph()
        Mockito.when(this.resourceHierarchicalRepository.save(anyCollectionOf(ResourceEntity.class)))
                .thenAnswer(new Answer<List<ResourceEntity>>() {
                    @SuppressWarnings("unchecked")
                    public List<ResourceEntity> answer(final InvocationOnMock invocation) {
                        Object[] args = invocation.getArguments();
                        return saveResourcesToGraph((List<ResourceEntity>) args[0]);
                    }
                });
        Mockito.when(this.subjectHierarchicalRepository.save(anyCollectionOf(SubjectEntity.class)))
                .thenAnswer(new Answer<List<SubjectEntity>>() {
                    public List<SubjectEntity> answer(final InvocationOnMock invocation) {
                        Object[] args = invocation.getArguments();
                        @SuppressWarnings("unchecked")
                        List<SubjectEntity> list = (List<SubjectEntity>) args[0];
                        return saveSubjectsToGraph(list);
                    }
                });

        new ResourceMigrationManager().doResourceMigration(resourceRepository,
                resourceHierarchicalRepository, TitanMigrationManager.PAGE_SIZE);
        new SubjectMigrationManager().doSubjectMigration(subjectRepository,
                subjectHierarchicalRepository, TitanMigrationManager.PAGE_SIZE);

        // Verify that the data from non-graph repo has been copied over to graph-repo, post-migration.
        assertThat(this.resourceRepository.findAll().size(), equalTo(2));
        assertThat(this.resourceHierarchicalRepository.findAll().size(), equalTo(2));

        assertThat(this.subjectRepository.findAll().size(), equalTo(2));
        assertThat(this.subjectHierarchicalRepository.findAll().size(), equalTo(2));
    }
}
