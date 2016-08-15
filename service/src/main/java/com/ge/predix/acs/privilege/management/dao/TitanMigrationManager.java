package com.ge.predix.acs.privilege.management.dao;

import java.util.List;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

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

    private static final int PAGE_SIZE = 100;

    public static int getPageSize() {
        return PAGE_SIZE;
    }

    public TitanMigrationManager(final ResourceRepository resourceRepository,
            final GraphResourceRepository resourceHierarchicalRepository, final SubjectRepository subjectRepository,
            final GraphSubjectRepository subjectHierarchicalRepository) {
        this.resourceRepository = resourceRepository;
        this.resourceHierarchicalRepository = resourceHierarchicalRepository;
        this.subjectRepository = subjectRepository;
        this.subjectHierarchicalRepository = subjectHierarchicalRepository;
    }

    public TitanMigrationManager() {

    }

    private void doResourceMigration() {
        int numOfResourcesSaved = 0;
        Pageable pageRequest = new PageRequest(0, PAGE_SIZE);
        long numOfResourceEntitiesToMigrate = resourceRepository.count();
        Page<ResourceEntity> pageOfResources;

        do {
            pageOfResources = resourceRepository.findAll(pageRequest);
            List<ResourceEntity> resourceListToSave = pageOfResources.getContent();
            numOfResourcesSaved += pageOfResources.getNumberOfElements();
            // Clear the auto-generated id field prior to migrating to graphDB
            resourceListToSave.forEach(item -> item.setId((long) 0));
            resourceHierarchicalRepository.save(resourceListToSave);
            LOGGER.info(
                    "Total resources migrated so far: " + numOfResourcesSaved + "/" + numOfResourceEntitiesToMigrate);
            pageRequest = pageOfResources.nextPageable();
        } while (pageOfResources.hasNext());

        LOGGER.info("Number of resource entities migrated: " + numOfResourcesSaved);
        LOGGER.info("Resource migration to Titan completed.");
    }

    private void doSubjectMigration() {
        int numOfSubjectsSaved = 0;
        Pageable pageRequest = new PageRequest(0, PAGE_SIZE);
        long numOfSubjectEntitiesToMigrate = subjectRepository.count();
        Page<SubjectEntity> pageOfSubjects;

        do {
            pageOfSubjects = subjectRepository.findAll(pageRequest);
            List<SubjectEntity> subjectListToSave = pageOfSubjects.getContent();
            numOfSubjectsSaved += pageOfSubjects.getNumberOfElements();
            subjectListToSave.forEach(item -> item.setId((long) 0));
            subjectHierarchicalRepository.save(subjectListToSave);
            LOGGER.info("Total subjects migrated so far: " + numOfSubjectsSaved + "/" + numOfSubjectEntitiesToMigrate);
            pageRequest = pageOfSubjects.nextPageable();
        } while (pageOfSubjects.hasNext());

        LOGGER.info("Number of subject entities migrated: " + numOfSubjectsSaved);
        LOGGER.info("Subject migration to Titan completed.");

    }

    @PostConstruct
    public void doMigration() {
        try {

            // This version vertex is common to both subject and resource repositories. So this check is sufficient to
            // trigger migrations in both repos.
            if (this.resourceHierarchicalRepository.getVersion() == 0) {

                LOGGER.info("Starting attribute migration process to Titan.");
                doResourceMigration();
                doSubjectMigration();
                this.resourceHierarchicalRepository.setVersion(1);
                LOGGER.info("Exiting from Titan attribute migration process.");

            }
            return;
        } catch (Throwable e) {
            LOGGER.error("Exception doing migration: ", e);
        }
    }
}