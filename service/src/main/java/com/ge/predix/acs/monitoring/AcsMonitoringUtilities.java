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

import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

public final class AcsMonitoringUtilities {

    enum HealthCode {
        ERROR,
        AVAILABLE,
        UNAVAILABLE,
        UNREACHABLE,
        MISCONFIGURATION,
        MIGRATION_INCOMPLETE,
        INVALID_QUERY,
        INVALID_JSON,
        DEGRADED,
        DISABLED,
        HEALTH_CHECK_DISABLED,
        IN_MEMORY
    }

    public static final String STATUS = "status";
    private static final Logger LOGGER = LoggerFactory.getLogger(AcsMonitoringUtilities.class);
    static final String ERROR_MESSAGE_FORMAT = "Unexpected exception while checking health: {}";
    static final String DESCRIPTION_KEY = "description";
    static final String CODE_KEY = "code";

    private AcsMonitoringUtilities() {
        throw new UnsupportedOperationException();
    }

    static Health health(final Status status, final HealthCode healthCode, final String description) {
        Health.Builder healthBuilder = Health.status(status);
        if (healthCode != HealthCode.AVAILABLE) {
            healthBuilder.withDetail(CODE_KEY, healthCode);
        }
        healthBuilder.withDetail(DESCRIPTION_KEY, description);
        return healthBuilder.build();
    }

    static Health health(final Supplier<HealthCode> check, final String description) {
        try {
            HealthCode healthCode = check.get();
            if (healthCode == HealthCode.AVAILABLE) {
                return health(Status.UP, healthCode, description);
            }
            return health(Status.DOWN, healthCode, description);
        } catch (Exception e) {
            return health(Status.DOWN,
                    AcsMonitoringUtilities.logError(HealthCode.ERROR, LOGGER, ERROR_MESSAGE_FORMAT, e), description);
        }
    }

    static HealthCode logError(final HealthCode healthCode, final Logger logger, final String format,
            final Throwable throwable) {
        logger.error(format, healthCode, throwable);
        return healthCode;
    }
}
