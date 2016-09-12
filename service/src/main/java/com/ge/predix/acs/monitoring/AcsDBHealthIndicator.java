/*******************************************************************************
 * Copyright 2016 General Electric Company.
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
 *******************************************************************************/

package com.ge.predix.acs.monitoring;

import static com.ge.predix.acs.monitoring.AcsMonitoringConstants.ACS_DB_OUT_OF_SERVICE;
import static com.ge.predix.acs.monitoring.AcsMonitoringConstants.ACS_DB_MIGRATION_INCOMPLETE;
import static com.ge.predix.acs.monitoring.AcsMonitoringConstants.DB_FAILED_STATUS;
import static com.ge.predix.acs.monitoring.AcsMonitoringConstants.DB_MISCONFIGURATION_STATUS;
import static com.ge.predix.acs.monitoring.AcsMonitoringConstants.DB_UNAVAILABLE_STATUS;
import static com.ge.predix.acs.monitoring.AcsMonitoringConstants.DB_UNREACHABLE_STATUS;
import static com.ge.predix.acs.monitoring.AcsMonitoringConstants.FAILED_CHECK;
import static com.ge.predix.acs.monitoring.AcsMonitoringConstants.SUCCESS_CHECK;

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

/**
 *
 * @author 212360328
 */
@Component
public class AcsDBHealthIndicator implements HealthIndicator {

    private static final Logger LOGGER = LoggerFactory.getLogger(AcsDBHealthIndicator.class);

    @Autowired(required = false)
    private TitanMigrationManager migrationManager;

    @Autowired
    private AcsMonitoringRepository acsMonitoringRepository;

    private static final String ERROR_MSG = "Unexpected exception while checking DB Status: %s";

    @Override
    public Health health() {
        int errorCode = check(); // perform some specific health check
        if (errorCode != 0) {
            return Health.status(ACS_DB_OUT_OF_SERVICE).withDetail("Error Code for ACS DB", errorCode).build();
        }

        if (null != this.migrationManager && !this.migrationManager.isMigrationComplete()) {
            return Health.status(ACS_DB_MIGRATION_INCOMPLETE).withDetail("Migration not complete.", FAILED_CHECK)
                    .build();
        }

        return Health.up().build();
    }

    private int check() {

        int dbStatus = FAILED_CHECK;
        try {
            LOGGER.debug("Checking ACS DB Status");
            this.acsMonitoringRepository.checkDBAccess();
            dbStatus = SUCCESS_CHECK;
        } catch (TransientDataAccessResourceException | QueryTimeoutException e) {
            LOGGER.error(String.format(ERROR_MSG, DB_UNAVAILABLE_STATUS), e);
        } catch (DataSourceLookupFailureException e) {
            LOGGER.error(String.format(ERROR_MSG, DB_UNREACHABLE_STATUS), e);
        } catch (PermissionDeniedDataAccessException e) {
            LOGGER.error(String.format(ERROR_MSG, DB_MISCONFIGURATION_STATUS), e);
        } catch (Exception e) {
            LOGGER.error(String.format(ERROR_MSG, DB_FAILED_STATUS), e);
        }
        return dbStatus;
    }
}
