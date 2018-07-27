/*******************************************************************************
 * Copyright 2018 General Electric Company
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

import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.actuate.health.Status
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestTemplate
import org.testng.Assert
import org.testng.annotations.DataProvider
import org.testng.annotations.Test

class UaaHealthIndicatorTest {

    @Value("\${uaaCheckHealthUrl}")
    private lateinit var uaaCheckHealthUrl: String

    @Test(dataProvider = "statuses")
    @Throws(Exception::class)
    fun testHealth(
        restTemplate: RestTemplate,
        status: Status,
        healthCode: HealthCode
    ) {
        val uaaHealthIndicator = UaaHealthIndicator(restTemplate)
        Assert.assertEquals(status, uaaHealthIndicator.health().status)
        Assert.assertEquals(
            uaaHealthIndicator.description,
            uaaHealthIndicator.health().details[DESCRIPTION_KEY]
        )
        if (healthCode === HealthCode.AVAILABLE) {
            Assert.assertFalse(
                uaaHealthIndicator.health().details.containsKey(
                    CODE_KEY
                )
            )
        } else {
            Assert.assertEquals(
                healthCode,
                uaaHealthIndicator.health().details[CODE_KEY]
            )
        }
    }

    @DataProvider
    fun statuses(): Array<Array<Any?>> {
        return arrayOf(
            arrayOf(mockRestWithUp(), Status.UP, HealthCode.AVAILABLE),

            arrayOf(mockRestWithException(RestClientException("")), Status.DOWN, HealthCode.UNREACHABLE),

            arrayOf(mockRestWithException(RuntimeException()), Status.DOWN, HealthCode.ERROR)
        )
    }

    private fun mockRestWithUp(): RestTemplate {
        val restTemplate = Mockito.mock(RestTemplate::class.java)
        Mockito.`when`(restTemplate.getForObject(this.uaaCheckHealthUrl, String::class.java)).thenReturn("OK")
        return restTemplate
    }

    private fun mockRestWithException(e: Exception): RestTemplate {
        val restTemplate = Mockito.mock(RestTemplate::class.java)
        Mockito.`when`(restTemplate.getForObject(this.uaaCheckHealthUrl, String::class.java))
            .thenAnswer { _ -> throw e }
        return restTemplate
    }
}
