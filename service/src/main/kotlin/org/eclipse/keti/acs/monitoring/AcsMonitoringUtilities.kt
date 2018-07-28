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

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.actuate.health.Health
import org.springframework.boot.actuate.health.Status

private val LOGGER = LoggerFactory.getLogger(AcsMonitoringUtilities::class.java)
internal const val ERROR_MESSAGE_FORMAT = "Unexpected exception while checking health: {}"
const val DESCRIPTION_KEY = "description"
const val CODE_KEY = "code"

internal fun health(
    status: Status,
    healthCode: HealthCode,
    description: String
): Health {
    val healthBuilder = Health.status(status)
    if (healthCode != HealthCode.AVAILABLE) {
        healthBuilder.withDetail(CODE_KEY, healthCode)
    }
    healthBuilder.withDetail(DESCRIPTION_KEY, description)
    return healthBuilder.build()
}

internal fun health(
    check: () -> HealthCode,
    description: String
): Health {
    return try {
        val healthCode = check()
        if (healthCode == HealthCode.AVAILABLE) {
            health(Status.UP, healthCode, description)
        } else health(Status.DOWN, healthCode, description)
    } catch (e: Exception) {
        health(
            Status.DOWN,
            logError(HealthCode.ERROR, LOGGER, ERROR_MESSAGE_FORMAT, e), description
        )
    }
}

internal fun logError(
    healthCode: HealthCode,
    logger: Logger,
    format: String,
    throwable: Throwable
): HealthCode {
    logger.error(format, healthCode, throwable)
    return healthCode
}

enum class HealthCode {
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

class AcsMonitoringUtilities private constructor() {

    init {
        throw UnsupportedOperationException()
    }
}
