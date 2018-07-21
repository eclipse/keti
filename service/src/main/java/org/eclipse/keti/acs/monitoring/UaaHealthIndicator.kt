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

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.actuate.health.Health
import org.springframework.boot.actuate.health.HealthIndicator
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestTemplate

private val LOGGER = LoggerFactory.getLogger(UaaHealthIndicator::class.java)
private const val UAA_ERROR_MESSAGE_FORMAT = "Unexpected exception while checking UAA status: {}"

@Component
class UaaHealthIndicator @Autowired
constructor(private val uaaTemplate: RestTemplate) : HealthIndicator {

    @Value("\${uaaCheckHealthUrl}")
    private var uaaCheckHealthUrl: String? = null

    val description: String
        get() = String.format("Health check performed by attempting to hit '%s'", this.uaaCheckHealthUrl)

    override fun health(): Health {
        return health({ this.check() }, this.description)
    }

    private fun check(): HealthCode {
        return try {
            LOGGER.debug("Checking UAA status using URL: {}", this.uaaCheckHealthUrl)
            this.uaaTemplate.getForObject(this.uaaCheckHealthUrl, String::class.java)
            HealthCode.AVAILABLE
        } catch (e: RestClientException) {
            logError(
                HealthCode.UNREACHABLE, LOGGER,
                UAA_ERROR_MESSAGE_FORMAT, e
            )
        } catch (e: Exception) {
            logError(
                HealthCode.ERROR, LOGGER,
                UAA_ERROR_MESSAGE_FORMAT, e
            )
        }
    }
}
