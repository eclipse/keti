package com.ge.predix.acs.privilege.management.dao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.concurrent.Executors;

@Component
@Profile("titan")
public final class TitanMigrationManager {
    public static final int INITIAL_ATTRIBUTE_GRAPH_VERSION = 1;

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

    private final ResourceMigrationManager resourceMigrationManager = new ResourceMigrationManager();
    private final SubjectMigrationManager subjectMigrationManager = new SubjectMigrationManager();

    @PostConstruct
    public void doMigration() {
        // This version vertex is common to both subject and resource repositories. So this check is sufficient to
        // trigger migrations in both repos.
        if (!this.resourceHierarchicalRepository.checkVersionVertexExists(INITIAL_ATTRIBUTE_GRAPH_VERSION)) {

            // Migration needs to be performed in a separate thread to prevent cloud-foundry health check timeout,
            // which restarts the service. (Max timeout is 180 seconds which is not enough)
            Executors.newSingleThreadExecutor().execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        LOGGER.info("Starting attribute migration process to Titan.");

                        //Rollback in the beginning to start with a clean state
                        resourceMigrationManager.rollbackMigratedData(resourceHierarchicalRepository);
                        subjectMigrationManager.rollbackMigratedData(subjectHierarchicalRepository);

                        //Run migration
                        resourceMigrationManager.doResourceMigration(resourceRepository,
                                resourceHierarchicalRepository, PAGE_SIZE);
                        subjectMigrationManager.doSubjectMigration(subjectRepository,
                                subjectHierarchicalRepository, PAGE_SIZE);

                        //Create version vertex, to record completion.
                        resourceHierarchicalRepository.createVersionVertex(INITIAL_ATTRIBUTE_GRAPH_VERSION);
                        isMigrationComplete = true;

                        LOGGER.info("Titan attribute migration complete. Created version: "
                                + INITIAL_ATTRIBUTE_GRAPH_VERSION);
                    } catch (Exception e) {
                        LOGGER.error("Exception during attribute migration: ", e);
                    }
                }
            });
        } else {
            isMigrationComplete = true;
            LOGGER.info("Attribute Graph migration not required.");
        }
    }

    public boolean isMigrationComplete() {
        return this.isMigrationComplete;
    }

}
