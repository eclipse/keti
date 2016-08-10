package com.ge.predix.acs.privilege.management.dao;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

import com.ge.predix.acs.zone.management.dao.ZoneEntity;

public class MigrationManagerTest {

    @Autowired
    @Qualifier("resourceRepositoryProxy")
    private ResourceRepositoryProxy resourceProxy;

    @Autowired
    @Qualifier("subjectRepositoryProxy")
    private SubjectRepositoryProxy subjectProxy;

    @Autowired
    private MigrationManager migrationManager;

    private List<ResourceEntity> toListForResource = new ArrayList<ResourceEntity>();
    private List<SubjectEntity> toListForSubject = new ArrayList<SubjectEntity>();

    @BeforeClass
    public void setup() throws Exception {
        this.resourceProxy = Mockito.mock(ResourceRepositoryProxy.class);
        this.subjectProxy = Mockito.mock(SubjectRepositoryProxy.class);

        Mockito.when(this.resourceProxy.getNonGraphRepository()).thenReturn(Mockito.mock(ResourceRepository.class));
        Mockito.when(this.resourceProxy.getGraphRepository()).thenReturn(Mockito.mock(GraphResourceRepository.class));
        Mockito.when(this.subjectProxy.getNonGraphRepository()).thenReturn(Mockito.mock(SubjectRepository.class));
        Mockito.when(this.subjectProxy.getGraphRepository()).thenReturn(Mockito.mock(GraphSubjectRepository.class));

        migrationManager = new MigrationManager(this.resourceProxy, this.subjectProxy);

    }

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

        List<ResourceEntity> fromListForResource = new ArrayList<ResourceEntity>();
        List<SubjectEntity> fromListForSubject = new ArrayList<SubjectEntity>();

        Mockito.when(this.resourceProxy.getNonGraphRepository().findAll()).thenReturn(fromListForResource);
        Mockito.when(this.resourceProxy.getGraphRepository().findAll()).thenReturn(this.toListForResource);

        Mockito.when(this.subjectProxy.getNonGraphRepository().findAll()).thenReturn(fromListForSubject);
        Mockito.when(this.subjectProxy.getGraphRepository().findAll()).thenReturn(this.toListForSubject);

        assertThat(this.resourceProxy.getNonGraphRepository().findAll().size(), equalTo(0));
        assertThat(this.resourceProxy.getGraphRepository().findAll().size(), equalTo(0));

        assertThat(this.subjectProxy.getNonGraphRepository().findAll().size(), equalTo(0));
        assertThat(this.subjectProxy.getGraphRepository().findAll().size(), equalTo(0));

        fromListForResource.addAll(Arrays.asList(entityResource1, entityResource2));
        fromListForSubject.addAll(Arrays.asList(entitySubject1, entitySubject2));

        assertThat(this.resourceProxy.getNonGraphRepository().findAll().size(), equalTo(2));
        assertThat(this.resourceProxy.getGraphRepository().findAll().size(), equalTo(0));

        assertThat(this.subjectProxy.getNonGraphRepository().findAll().size(), equalTo(2));
        assertThat(this.subjectProxy.getGraphRepository().findAll().size(), equalTo(0));

        Mockito.when(this.resourceProxy.getGraphRepository().save(fromListForResource))
                .thenAnswer(new Answer<List<ResourceEntity>>() {
                    public List<ResourceEntity> answer(InvocationOnMock invocation) {
                        Object[] args = invocation.getArguments();
                        @SuppressWarnings("unchecked")
                        List<ResourceEntity> list = (List<ResourceEntity>) args[0];
                        return saveResourcesToGraph(list);
                    }
                });
        Mockito.when(this.subjectProxy.getGraphRepository().save(fromListForSubject))
                .thenAnswer(new Answer<List<SubjectEntity>>() {
                    public List<SubjectEntity> answer(InvocationOnMock invocation) {
                        Object[] args = invocation.getArguments();
                        @SuppressWarnings("unchecked")
                        List<SubjectEntity> list = (List<SubjectEntity>) args[0];
                        return saveSubjectsToGraph(list);
                    }
                });

        migrationManager.doMigration();

        assertThat(this.resourceProxy.getNonGraphRepository().findAll().size(), equalTo(2));
        assertThat(this.resourceProxy.getGraphRepository().findAll().size(), equalTo(2));

        assertThat(this.subjectProxy.getNonGraphRepository().findAll().size(), equalTo(2));
        assertThat(this.subjectProxy.getGraphRepository().findAll().size(), equalTo(2));
    }
}
