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

package org.eclipse.keti.acs.monitoring

import org.eclipse.keti.acs.privilege.management.dao.GraphMigrationManager
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.actuate.health.Health
import org.springframework.boot.actuate.health.HealthIndicator
import org.springframework.dao.PermissionDeniedDataAccessException
import org.springframework.dao.QueryTimeoutException
import org.springframework.dao.TransientDataAccessResourceException
import org.springframework.jdbc.datasource.lookup.DataSourceLookupFailureException
import org.springframework.stereotype.Component

private val LOGGER = LoggerFactory.getLogger(AcsDbHealthIndicator::class.java)
private const val DB_ERROR_MESSAGE_FORMAT = "Unexpected exception while checking ACS database status: {}"
const val DB_DESCRIPTION =
    "Health check performed by attempting to run a SELECT query against policy " + "sets stored in the database"

@Component
class AcsDbHealthIndicator @Autowired
constructor(private val acsMonitoringRepository: AcsMonitoringRepository) : HealthIndicator {

    private var migrationManager: GraphMigrationManager? = null

    @Autowired(required = false)
    fun setMigrationManager(migrationManager: GraphMigrationManager) {
        this.migrationManager = migrationManager
    }

    override fun health(): Health {
        return health({ this.check() }, DB_DESCRIPTION)
    }

    private fun check(): HealthCode {
        return try {
            LOGGER.debug("Checking ACS database status")
            this.acsMonitoringRepository.queryPolicySetTable()

            if (this.migrationManager != null && !this.migrationManager!!.isMigrationComplete()) {
                HealthCode.MIGRATION_INCOMPLETE
            } else {
                HealthCode.AVAILABLE
            }
        } catch (e: TransientDataAccessResourceException) {
            logError(
                HealthCode.UNAVAILABLE, LOGGER,
                DB_ERROR_MESSAGE_FORMAT, e
            )
        } catch (e: QueryTimeoutException) {
            logError(
                HealthCode.UNAVAILABLE,
                LOGGER,
                DB_ERROR_MESSAGE_FORMAT,
                e
            )
        } catch (e: DataSourceLookupFailureException) {
            logError(
                HealthCode.UNREACHABLE, LOGGER,
                DB_ERROR_MESSAGE_FORMAT, e
            )
        } catch (e: PermissionDeniedDataAccessException) {
            logError(
                HealthCode.MISCONFIGURATION, LOGGER,
                DB_ERROR_MESSAGE_FORMAT, e
            )
        } catch (e: Exception) {
            logError(
                HealthCode.ERROR, LOGGER,
                DB_ERROR_MESSAGE_FORMAT, e
            )
        }
    }
}
