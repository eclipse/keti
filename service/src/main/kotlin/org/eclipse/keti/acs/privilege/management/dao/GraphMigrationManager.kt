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
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import java.util.concurrent.Executors
import javax.annotation.PostConstruct

const val INITIAL_ATTRIBUTE_GRAPH_VERSION = 1

private val LOGGER = LoggerFactory.getLogger(GraphMigrationManager::class.java)

internal const val PAGE_SIZE = 1024

@Component
@Profile("graph")
class GraphMigrationManager {

    @Autowired
    @Qualifier("resourceRepository")
    private lateinit var resourceRepository: ResourceRepository

    @Autowired
    @Qualifier("resourceHierarchicalRepository")
    private lateinit var resourceHierarchicalRepository: GraphResourceRepository

    @Autowired
    @Qualifier("subjectRepository")
    private lateinit var subjectRepository: SubjectRepository

    @Autowired
    @Qualifier("subjectHierarchicalRepository")
    private lateinit var subjectHierarchicalRepository: GraphSubjectRepository

    private var isMigrationComplete: Boolean = false

    private val resourceMigrationManager = ResourceMigrationManager()
    private val subjectMigrationManager = SubjectMigrationManager()

    @PostConstruct
    fun doMigration() {
        // This version vertex is common to both subject and resource repositories. So this check is sufficient to
        // trigger migrations in both repos.
        if (!this.resourceHierarchicalRepository.checkVersionVertexExists(INITIAL_ATTRIBUTE_GRAPH_VERSION)) {

            // Migration needs to be performed in a separate thread to prevent cloud-foundry health check timeout,
            // which restarts the service. (Max timeout is 180 seconds which is not enough)
            Executors.newSingleThreadExecutor().execute {
                try {
                    LOGGER.info("Starting attribute migration process to graph.")

                    // Rollback in the beginning to start with a clean state
                    resourceMigrationManager.rollbackMigratedData(resourceHierarchicalRepository)
                    subjectMigrationManager.rollbackMigratedData(subjectHierarchicalRepository)

                    // Run migration
                    resourceMigrationManager.doResourceMigration(
                        resourceRepository,
                        resourceHierarchicalRepository, PAGE_SIZE
                    )
                    subjectMigrationManager.doSubjectMigration(
                        subjectRepository,
                        subjectHierarchicalRepository, PAGE_SIZE
                    )

                    // Create version vertex, to record completion.
                    resourceHierarchicalRepository.createVersionVertex(INITIAL_ATTRIBUTE_GRAPH_VERSION)
                    isMigrationComplete = true

                    LOGGER.info("Graph attribute migration complete. Created version: $INITIAL_ATTRIBUTE_GRAPH_VERSION")
                } catch (e: Exception) {
                    LOGGER.error("Exception during attribute migration: ", e)
                }
            }
        } else {
            isMigrationComplete = true
            LOGGER.info("Attribute Graph migration not required.")
        }
    }

    fun isMigrationComplete(): Boolean {
        return this.isMigrationComplete
    }
}
