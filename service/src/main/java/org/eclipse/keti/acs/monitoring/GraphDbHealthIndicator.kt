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

package org.eclipse.keti.acs.monitoring

import org.eclipse.keti.acs.privilege.management.dao.GraphResourceRepository
import org.eclipse.keti.acs.privilege.management.dao.INITIAL_ATTRIBUTE_GRAPH_VERSION
import org.janusgraph.core.JanusGraphConfigurationException
import org.janusgraph.core.QueryException
import org.janusgraph.diskstorage.ResourceUnavailableException
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.actuate.health.Health
import org.springframework.boot.actuate.health.HealthIndicator
import org.springframework.boot.actuate.health.Status
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

private val LOGGER = LoggerFactory.getLogger(GraphDbHealthIndicator::class.java)
private const val GRAPH_ERROR_MESSAGE_FORMAT = "Unexpected exception while checking graph database status: {}"
const val GRAPH_DESCRIPTION =
    "Health check performed by attempting to create a version vertex and retrieve it " + "from the underlying graph store"

@Component
@Profile("graph")
class GraphDbHealthIndicator @Autowired
constructor(private val resourceHierarchicalRepository: GraphResourceRepository) : HealthIndicator {

    @Value("\${GRAPH_ENABLE_CASSANDRA:false}")
    private var cassandraEnabled: Boolean = false

    override fun health(): Health {
        var health = health({ this.check() }, GRAPH_DESCRIPTION)
        val status = health.status
        if (status == Status.UP && !this.cassandraEnabled) {
            health = health(status, HealthCode.IN_MEMORY, GRAPH_DESCRIPTION)
        }
        return health
    }

    private fun check(): HealthCode {
        var healthCode: HealthCode = HealthCode.ERROR

        try {
            LOGGER.debug("Checking graph database status")
            if (this.resourceHierarchicalRepository
                    .checkVersionVertexExists(INITIAL_ATTRIBUTE_GRAPH_VERSION)
            ) {
                healthCode = HealthCode.AVAILABLE
            }
        } catch (e: QueryException) {
            healthCode = logError(
                HealthCode.INVALID_QUERY, LOGGER,
                GRAPH_ERROR_MESSAGE_FORMAT, e
            )
        } catch (e: ResourceUnavailableException) {
            healthCode = logError(
                HealthCode.UNAVAILABLE, LOGGER,
                GRAPH_ERROR_MESSAGE_FORMAT, e
            )
        } catch (e: JanusGraphConfigurationException) {
            healthCode = logError(
                HealthCode.MISCONFIGURATION, LOGGER,
                GRAPH_ERROR_MESSAGE_FORMAT, e
            )
        } catch (e: Exception) {
            healthCode = logError(
                HealthCode.ERROR, LOGGER,
                GRAPH_ERROR_MESSAGE_FORMAT, e
            )
        }

        return healthCode
    }
}
