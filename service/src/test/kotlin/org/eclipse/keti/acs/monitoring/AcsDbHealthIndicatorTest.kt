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

import org.eclipse.keti.acs.privilege.management.dao.GraphStartupManager
import org.mockito.Mockito
import org.springframework.boot.actuate.health.Status
import org.springframework.dao.ConcurrencyFailureException
import org.springframework.dao.PermissionDeniedDataAccessException
import org.springframework.dao.QueryTimeoutException
import org.springframework.dao.TransientDataAccessResourceException
import org.springframework.jdbc.datasource.lookup.DataSourceLookupFailureException
import org.springframework.test.util.ReflectionTestUtils
import org.testng.Assert
import org.testng.annotations.DataProvider
import org.testng.annotations.Test

private const val IS_STARTUP_COMPLETE_FIELD_NAME = "isStartupComplete"

class AcsDbHealthIndicatorTest {

    @Test(dataProvider = "statuses")
    @Throws(Exception::class)
    fun testHealth(
        acsMonitoringRepository: AcsMonitoringRepository,
        status: Status,
        healthCode: HealthCode,
        graphStartupManager: GraphStartupManager
    ) {
        val acsDbHealthIndicator = AcsDbHealthIndicator(acsMonitoringRepository)
        acsDbHealthIndicator.setStartupManager(graphStartupManager)
        Assert.assertEquals(status, acsDbHealthIndicator.health().status)
        Assert.assertEquals(
            DB_DESCRIPTION,
            acsDbHealthIndicator.health().details[DESCRIPTION_KEY]
        )
        if (healthCode === HealthCode.AVAILABLE) {
            Assert.assertFalse(
                acsDbHealthIndicator.health().details.containsKey(
                    CODE_KEY
                )
            )
        } else {
            Assert.assertEquals(
                healthCode,
                acsDbHealthIndicator.health().details[CODE_KEY]
            )
        }
    }

    @DataProvider
    fun statuses(): Array<Array<out Any?>> {
        val happyStartupManager = GraphStartupManager()
        val sadStartupManager = GraphStartupManager()
        ReflectionTestUtils.setField(happyStartupManager, IS_STARTUP_COMPLETE_FIELD_NAME, true)
        ReflectionTestUtils.setField(sadStartupManager, IS_STARTUP_COMPLETE_FIELD_NAME, false)

        return arrayOf(
            arrayOf(mockDbWithUp(), Status.UP, HealthCode.AVAILABLE, happyStartupManager),

            arrayOf(
                mockDbWithException(TransientDataAccessResourceException("")),
                Status.DOWN,
                HealthCode.UNAVAILABLE,
                happyStartupManager
            ),

            arrayOf(
                mockDbWithException(QueryTimeoutException("")),
                Status.DOWN,
                HealthCode.UNAVAILABLE,
                happyStartupManager
            ),

            arrayOf(
                mockDbWithException(DataSourceLookupFailureException("")),
                Status.DOWN,
                HealthCode.UNREACHABLE,
                happyStartupManager
            ),

            arrayOf(
                mockDbWithException(PermissionDeniedDataAccessException("", null)),
                Status.DOWN,
                HealthCode.MISCONFIGURATION,
                happyStartupManager
            ),

            arrayOf(
                mockDbWithException(ConcurrencyFailureException("")),
                Status.DOWN,
                HealthCode.ERROR,
                happyStartupManager
            ),

            arrayOf(mockDbWithUp(), Status.DOWN, HealthCode.GRAPH_STARTUP_INCOMPLETE, sadStartupManager)
        )
    }

    private fun mockDbWithUp(): AcsMonitoringRepository {
        val acsMonitoringRepository = Mockito.mock(AcsMonitoringRepository::class.java)
        Mockito.doNothing().`when`(acsMonitoringRepository).queryPolicySetTable()
        return acsMonitoringRepository
    }

    private fun mockDbWithException(e: Exception): AcsMonitoringRepository {
        val acsMonitoringRepository = Mockito.mock(AcsMonitoringRepository::class.java)
        Mockito.doAnswer { _ -> throw e }.`when`(acsMonitoringRepository).queryPolicySetTable()
        return acsMonitoringRepository
    }
}
