package com.ge.predix.acs.privilege.management.dao;

import java.util.List;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
//import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

@Component
public final class ResourceMigrationManager {
    @Autowired
    @Qualifier("resourceRepository")
    private ResourceRepository from;

    @Autowired
    @Qualifier("resourceHierarchicalRepository")
    private GraphResourceRepository to;

    public ResourceMigrationManager(final ResourceRepository from, final GraphResourceRepository to) {
        this.from = from;
        this.to = to;
    }

    public ResourceMigrationManager() {

    }

    @PostConstruct
    public void doMigration() {
        // TODO: Return value should reflect status of migration?

        System.out.println("doMigration called");
        List<ResourceEntity> allResources = from.findAll();
        System.out.println("Should be 2: " + allResources.size());
        to.save(allResources);
        System.out.println("To-REPO Should be 2: " + to.findAll().size());
        return;
        // PageRequest testPage = new PageRequest(0, 3);
        // List<ResourceEntity> listOfResources = from.findAll(testPage).getContent();
        // List<ResourceEntity> listOfResources = new ArrayList<ResourceEntity>();
        // System.out.println("Resources obtained - PAGE: " + listOfResources.size());

    }
}