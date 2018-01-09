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

package com.ge.predix.acs.monitoring;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.dao.PermissionDeniedDataAccessException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.dao.TransientDataAccessResourceException;
import org.springframework.jdbc.datasource.lookup.DataSourceLookupFailureException;
import org.springframework.stereotype.Component;

import com.ge.predix.acs.privilege.management.dao.TitanMigrationManager;

@Component
public class AcsDbHealthIndicator implements HealthIndicator {

    private static final Logger LOGGER = LoggerFactory.getLogger(AcsDbHealthIndicator.class);
    private static final String ERROR_MESSAGE_FORMAT = "Unexpected exception while checking ACS database status: {}";
    static final String DESCRIPTION = "Health check performed by attempting to run a SELECT query against policy "
            + "sets stored in the database";

    private final AcsMonitoringRepository acsMonitoringRepository;
    private TitanMigrationManager migrationManager;

    @Autowired
    public AcsDbHealthIndicator(final AcsMonitoringRepository acsMonitoringRepository) {
        this.acsMonitoringRepository = acsMonitoringRepository;
    }

    @Autowired(required = false)
    void setMigrationManager(final TitanMigrationManager migrationManager) {
        this.migrationManager = migrationManager;
    }

    @Override
    public Health health() {
        return AcsMonitoringUtilities.health(this::check, DESCRIPTION);
    }

    private AcsMonitoringUtilities.HealthCode check() {
        AcsMonitoringUtilities.HealthCode healthCode;

        try {
            LOGGER.debug("Checking ACS database status");
            this.acsMonitoringRepository.queryPolicySetTable();

            if (this.migrationManager != null && !this.migrationManager.isMigrationComplete()) {
                healthCode = AcsMonitoringUtilities.HealthCode.MIGRATION_INCOMPLETE;
            } else {
                healthCode = AcsMonitoringUtilities.HealthCode.AVAILABLE;
            }
        } catch (TransientDataAccessResourceException | QueryTimeoutException e) {
            healthCode = AcsMonitoringUtilities.logError(AcsMonitoringUtilities.HealthCode.UNAVAILABLE, LOGGER,
                    ERROR_MESSAGE_FORMAT, e);
        } catch (DataSourceLookupFailureException e) {
            healthCode = AcsMonitoringUtilities.logError(AcsMonitoringUtilities.HealthCode.UNREACHABLE, LOGGER,
                    ERROR_MESSAGE_FORMAT, e);
        } catch (PermissionDeniedDataAccessException e) {
            healthCode = AcsMonitoringUtilities.logError(AcsMonitoringUtilities.HealthCode.MISCONFIGURATION, LOGGER,
                    ERROR_MESSAGE_FORMAT, e);
        } catch (Exception e) {
            healthCode = AcsMonitoringUtilities.logError(AcsMonitoringUtilities.HealthCode.ERROR, LOGGER,
                    ERROR_MESSAGE_FORMAT, e);
        }

        return healthCode;
    }
}
