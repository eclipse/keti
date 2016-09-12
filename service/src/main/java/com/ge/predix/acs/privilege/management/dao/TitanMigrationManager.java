package com.ge.predix.acs.privilege.management.dao;

import java.util.concurrent.Executors;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("titan")
public final class TitanMigrationManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(TitanMigrationManager.class);

    @Autowired
    @Qualifier("resourceRepository")
    private ResourceRepository resourceRepository;

    @Autowired
    @Qualifier("resourceHierarchicalRepository")
    private GraphResourceRepository resourceHierarchicalRepository;

    @Autowired
    @Qualifier("subjectRepository")
    private SubjectRepository subjectRepository;

    @Autowired
    @Qualifier("subjectHierarchicalRepository")
    private GraphSubjectRepository subjectHierarchicalRepository;

    private Boolean isMigrationComplete = false;

    static final int PAGE_SIZE = 1024;

    @PostConstruct
    public void doMigration() {
        try {
            // This version vertex is common to both subject and resource repositories. So this check is sufficient to
            // trigger migrations in both repos.
            if (this.resourceHierarchicalRepository.getVersion() == 0) {

                //Migration needs to be performed in a separate thread to prevent cloud-foundry health check timeout,
                //which restarts the service. (Max timeout is 180 seconds which is not enough)
                Executors.newSingleThreadExecutor().execute(new Runnable() {
                    @Override
                    public void run() {
                        LOGGER.info("Starting attribute migration process to Titan.");

                        //Resource and Subject must be performed sequentially for now to prevent a lock exception being 
                        //thrown from titan.
                        new ResourceMigrationManager().doResourceMigration(resourceRepository,
                                resourceHierarchicalRepository, PAGE_SIZE);
                        new SubjectMigrationManager().doSubjectMigration(subjectRepository,
                                subjectHierarchicalRepository, PAGE_SIZE);

                        resourceHierarchicalRepository.setVersion(1);
                        isMigrationComplete = true;

                        LOGGER.info("Exiting from Titan attribute migration process.");
                    }
                });
            }  else {
                isMigrationComplete = true;
                LOGGER.info("Attribute Graph migration not required.");
            }
        } catch (Throwable e) {
            LOGGER.error("Exception during attribute migration: ", e);
            throw e;
        }
    }

    public boolean isMigrationComplete() {
        return this.isMigrationComplete;
    }
    
}
