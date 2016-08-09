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

    @PostConstruct
    public int doMigration() {

        System.out.println("doMigration called");
        List<ResourceEntity> allResources = from.findAll();
        System.out.println("Resources obtained - All: " + allResources.size());
        return allResources.size();
        // PageRequest testPage = new PageRequest(0, 3);
        // List<ResourceEntity> listOfResources = from.findAll(testPage).getContent();
        // List<ResourceEntity> listOfResources = new ArrayList<ResourceEntity>();
        // System.out.println("Resources obtained - PAGE: " + listOfResources.size());

    }
}