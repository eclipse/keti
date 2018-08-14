/*******************************************************************************
 * Copyright 2018 General Electric Company
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

package org.eclipse.keti.acs.privilege.management.dao

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import java.util.concurrent.Executors
import javax.annotation.PostConstruct

const val INITIAL_ATTRIBUTE_GRAPH_VERSION = 1

private val LOGGER = LoggerFactory.getLogger(GraphStartupManager::class.java)

@Component
@Profile("graph")
class GraphStartupManager {

    @Autowired
    @Qualifier("resourceHierarchicalRepository")
    private lateinit var resourceHierarchicalRepository: GraphResourceRepository

    private var isStartupComplete: Boolean = false

    @PostConstruct
    fun doStartup() {
        // This version vertex is common to both subject and resource repositories. So this check is sufficient to
        // trigger startups in both repos.
        if (!this.resourceHierarchicalRepository.checkVersionVertexExists(INITIAL_ATTRIBUTE_GRAPH_VERSION)) {

            // Startup needs to be performed in a separate thread to prevent cloud-foundry health check timeout,
            // which restarts the service. (Max timeout is 180 seconds which is not enough)
            Executors.newSingleThreadExecutor().execute {
                try {
                    LOGGER.info("Starting attribute startup process to graph.")
                    // Create version vertex, to record completion.
                    resourceHierarchicalRepository.createVersionVertex(INITIAL_ATTRIBUTE_GRAPH_VERSION)
                    isStartupComplete = true
                    LOGGER.info("Graph attribute startup complete. Created version: $INITIAL_ATTRIBUTE_GRAPH_VERSION")
                } catch (e: Exception) {
                    LOGGER.error("Exception during attribute startup: ", e)
                }
            }
        } else {
            isStartupComplete = true
            LOGGER.info("Attribute Graph startup not required.")
        }
    }

    fun isStartupComplete(): Boolean {
        return this.isStartupComplete
    }
}
