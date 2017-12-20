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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public class ResourceMigrationManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResourceMigrationManager.class);

    public void doResourceMigration(final ResourceRepository resourceRepository,
            final GraphResourceRepository resourceHierarchicalRepository, final int pageSize) {
        int numOfResourcesSaved = 0;
        Pageable pageRequest = new PageRequest(0, pageSize, new Sort("id"));
        long numOfResourceEntitiesToMigrate = resourceRepository.count();
        Page<ResourceEntity> pageOfResources;

        do {
            pageOfResources = resourceRepository.findAll(pageRequest);
            List<ResourceEntity> resourceListToSave = pageOfResources.getContent();
            numOfResourcesSaved += pageOfResources.getNumberOfElements();

            resourceListToSave.forEach(item -> {
                // Clear the auto-generated id field prior to migrating to graphDB
                item.setId((long) 0);
                LOGGER.trace("doResourceMigration Resource-Id: {} Zone-name: {} Zone-id: {}",
                        item.getResourceIdentifier(), item.getZone().getName(), item.getZone().getId());
            });

            resourceHierarchicalRepository.save(resourceListToSave);

            LOGGER.info("Total resources migrated so far: {}/{}", numOfResourcesSaved, numOfResourceEntitiesToMigrate);
            pageRequest = pageOfResources.nextPageable();
        } while (pageOfResources.hasNext());

        LOGGER.info("Number of resource entities migrated: {}", numOfResourcesSaved);
        LOGGER.info("Resource migration to Titan completed.");
    }

    public void rollbackMigratedData(final GraphResourceRepository resourceHierarchicalRepository) {
        LOGGER.info("Initiating rollback for resourceHierarchicalRepository");
        resourceHierarchicalRepository.deleteAll();
    }

}
