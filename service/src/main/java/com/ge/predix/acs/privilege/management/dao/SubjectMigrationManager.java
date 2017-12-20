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

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

@Component
@Profile("titan")
public class SubjectMigrationManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(SubjectMigrationManager.class);

    public void doSubjectMigration(final SubjectRepository subjectRepository,
            final GraphSubjectRepository subjectHierarchicalRepository, final int pageSize) {
        int numOfSubjectsSaved = 0;
        Pageable pageRequest = new PageRequest(0, pageSize, new Sort("id"));
        long numOfSubjectEntitiesToMigrate = subjectRepository.count();
        Page<SubjectEntity> pageOfSubjects;

        do {
            pageOfSubjects = subjectRepository.findAll(pageRequest);
            List<SubjectEntity> subjectListToSave = pageOfSubjects.getContent();
            numOfSubjectsSaved += pageOfSubjects.getNumberOfElements();
            subjectListToSave.forEach(item -> {
                item.setId(0);
                LOGGER.trace("doSubjectMigration Subject-Id: {} Zone-name: {} Zone-id: {}", item.getSubjectIdentifier(),
                        item.getZone().getName(), item.getZone().getId());
            });

            subjectHierarchicalRepository.save(subjectListToSave);
            LOGGER.info("Total subjects migrated so far: {}/{}", numOfSubjectsSaved, numOfSubjectEntitiesToMigrate);
            pageRequest = pageOfSubjects.nextPageable();
        } while (pageOfSubjects.hasNext());

        LOGGER.info("Number of subject entities migrated: {}", numOfSubjectsSaved);
        LOGGER.info("Subject migration to Titan completed.");
    }

    public void rollbackMigratedData(final GraphSubjectRepository subjectHierarchicalRepository) {
        LOGGER.info("Initiating rollback for subjectHierarchicalRepository");
        subjectHierarchicalRepository.deleteAll();
    }

}
