/*******************************************************************************
 * Copyright 2017 General Electric Company
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.eclipse.keti.acs.privilege.management.dao

import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort

private val LOGGER = LoggerFactory.getLogger(ResourceMigrationManager::class.java)

class ResourceMigrationManager {

    fun doResourceMigration(
        resourceRepository: ResourceRepository,
        resourceHierarchicalRepository: GraphResourceRepository, pageSize: Int
    ) {
        var numOfResourcesSaved = 0
        var pageRequest: Pageable? = PageRequest(0, pageSize, Sort("id"))
        val numOfResourceEntitiesToMigrate = resourceRepository.count()
        var pageOfResources: Page<ResourceEntity>

        do {
            pageOfResources = resourceRepository.findAll(pageRequest)
            val resourceListToSave = pageOfResources.content
            numOfResourcesSaved += pageOfResources.numberOfElements

            resourceListToSave.forEach { item ->
                // Clear the auto-generated id field prior to migrating to graphDB
                item.id = 0.toLong()
                LOGGER.trace(
                    "doResourceMigration Resource-Id: {} Zone-name: {} Zone-id: {}",
                    item.resourceIdentifier, item.zone!!.name, item.zone!!.id
                )
            }

            resourceHierarchicalRepository.save(resourceListToSave)

            LOGGER.info("Total resources migrated so far: {}/{}", numOfResourcesSaved, numOfResourceEntitiesToMigrate)
            pageRequest = pageOfResources.nextPageable()
        } while (pageOfResources.hasNext())

        LOGGER.info("Number of resource entities migrated: {}", numOfResourcesSaved)
        LOGGER.info("Resource migration to graph completed.")
    }

    fun rollbackMigratedData(resourceHierarchicalRepository: GraphResourceRepository) {
        LOGGER.info("Initiating rollback for resourceHierarchicalRepository")
        resourceHierarchicalRepository.deleteAll()
    }
}
