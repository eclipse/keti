package com.ge.predix.acs.privilege.management.dao;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("titan")
public final class MigrationManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(MigrationManager.class);

    @Autowired
    @Qualifier("resourceRepositoryProxy")
    private ResourceRepositoryProxy resourceProxy;

    @Autowired
    @Qualifier("subjectRepositoryProxy")
    private SubjectRepositoryProxy subjectProxy;

    public MigrationManager(final ResourceRepositoryProxy resourceProxy, final SubjectRepositoryProxy subjectProxy) {
        this.resourceProxy = resourceProxy;
        this.subjectProxy = subjectProxy;
    }

    public MigrationManager() {

    }

    @PostConstruct
    public void doMigration() {
        // TODO: Return value should reflect status of migration?
        // Success, failure, interrupted etc ?

        try {
            // This is basically bringing all of Postgres data into memory. Should we
            // implement a safe paging/caching solution instead of blind migration ?
            // PageRequest page = new PageRequest(0, In_memory_page_size);
            // call iteratively :
            // List<ResourceEntity> listOfResources = from.findAll(page).getContent();
            resourceProxy.getGraphRepository().save(resourceProxy.getNonGraphRepository().findAll());
            subjectProxy.getGraphRepository().save(subjectProxy.getNonGraphRepository().findAll());
            return;
        } catch (Exception e) {
            LOGGER.error("Exception doing migration: ", e);
        }

    }
}