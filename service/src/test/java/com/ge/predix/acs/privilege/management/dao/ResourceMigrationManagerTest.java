package com.ge.predix.acs.privilege.management.dao;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.ge.predix.acs.zone.management.dao.ZoneEntity;

public class ResourceMigrationManagerTest {
    
    private ResourceRepository nonGraphRepo;
    private GraphResourceRepository graphRepo;
    
    @Autowired
    private ResourceMigrationManager resourceMigrationManager;
    
    @BeforeClass
    public void setup() throws Exception {
        this.nonGraphRepo = Mockito.mock(ResourceRepository.class);
        this.graphRepo = Mockito.mock(GraphResourceRepository.class);
        resourceMigrationManager = new ResourceMigrationManager(this.nonGraphRepo, this.graphRepo);
        
    }
    
    @Test
    public void migrationManagerTest(){     
        
          
        ZoneEntity zone1 = new ZoneEntity((long) 1, "testzone1");
        ResourceEntity entity1 = new ResourceEntity(zone1, "testresource1");
        ResourceEntity entity2 = new ResourceEntity(zone1, "testresource2");
        Mockito.when(this.nonGraphRepo.save(entity1)).thenReturn(entity1);
        Mockito.when(this.nonGraphRepo.save(entity2)).thenReturn(entity2);
        List<ResourceEntity> fromList = new ArrayList<ResourceEntity>(Arrays.asList(entity1, entity2));
        List<ResourceEntity> toList = new ArrayList<ResourceEntity>();
        Mockito.when(this.graphRepo.findAll()).thenReturn(toList);
        
        System.out.println(this.nonGraphRepo.findAll().size());
        Assert.assertEquals(this.nonGraphRepo.findAll().size(), 0);
        Assert.assertEquals(0, this.graphRepo.findAll().size());
        
        this.nonGraphRepo.save(entity1);
        this.nonGraphRepo.save(entity2);
        Mockito.when(this.nonGraphRepo.findAll()).thenReturn(fromList);
        Assert.assertEquals(this.nonGraphRepo.findAll().size(), 2);
        
       // System.out.println("RM:"+resourceMigrationManager);
        
        
        resourceMigrationManager.doMigration();
        //Assert.assertEquals(this.graphRepo.findAll().size(), 2);
        Assert.assertEquals(this.nonGraphRepo.findAll().size(), 2);

    }
}
