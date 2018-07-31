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

import org.eclipse.keti.acs.privilege.management.dao.GraphResourceRepository
import org.janusgraph.core.JanusGraphConfigurationException
import org.janusgraph.core.QueryException
import org.janusgraph.diskstorage.ResourceUnavailableException
import org.janusgraph.graphdb.database.idassigner.IDPoolExhaustedException
import org.mockito.Mockito
import org.springframework.boot.actuate.health.Status
import org.springframework.test.util.ReflectionTestUtils
import org.testng.Assert
import org.testng.annotations.DataProvider
import org.testng.annotations.Test

class GraphDbHealthIndicatorTest {

    @Test(dataProvider = "statuses")
    @Throws(Exception::class)
    fun testHealth(
        graphResourceRepository: GraphResourceRepository,
        status: Status,
        healthCode: HealthCode,
        cassandraEnabled: Boolean
    ) {
        val graphDbHealthIndicator = GraphDbHealthIndicator(graphResourceRepository)
        ReflectionTestUtils.setField(graphDbHealthIndicator, "cassandraEnabled", cassandraEnabled)
        Assert.assertEquals(status, graphDbHealthIndicator.health().status)
        Assert.assertEquals(
            GRAPH_DESCRIPTION,
            graphDbHealthIndicator.health().details[DESCRIPTION_KEY]
        )
        if (healthCode === HealthCode.AVAILABLE) {
            Assert.assertFalse(
                graphDbHealthIndicator.health().details.containsKey(
                    CODE_KEY
                )
            )
        } else {
            Assert.assertEquals(
                healthCode,
                graphDbHealthIndicator.health().details[CODE_KEY]
            )
        }
    }

    @DataProvider
    fun statuses(): Array<Array<out Any?>> {
        return arrayOf(
            arrayOf(mockGraphDbWithUp(), Status.UP, HealthCode.IN_MEMORY, false),

            arrayOf(mockGraphDbWithUp(), Status.UP, HealthCode.AVAILABLE, true),

            arrayOf(
                mockGraphDbWithExceptionWhileCheckingVersion(QueryException("")),
                Status.DOWN,
                HealthCode.INVALID_QUERY,
                true
            ),

            arrayOf(
                mockGraphDbWithExceptionWhileCheckingVersion(ResourceUnavailableException("")),
                Status.DOWN,
                HealthCode.UNAVAILABLE,
                true
            ),

            arrayOf(
                mockGraphDbWithExceptionWhileCheckingVersion(JanusGraphConfigurationException("")),
                Status.DOWN,
                HealthCode.MISCONFIGURATION,
                true
            ),

            arrayOf(
                mockGraphDbWithExceptionWhileCheckingVersion(IDPoolExhaustedException("")),
                Status.DOWN,
                HealthCode.ERROR,
                true
            )
        )
    }

    private fun mockGraphDbWithUp(): GraphResourceRepository {
        val graphResourceRepository = Mockito.mock(GraphResourceRepository::class.java)
        Mockito.doReturn(true).`when`(graphResourceRepository).checkVersionVertexExists(Mockito.anyInt())
        return graphResourceRepository
    }

    private fun mockGraphDbWithExceptionWhileCheckingVersion(e: Exception): GraphResourceRepository {
        val graphResourceRepository = Mockito.mock(GraphResourceRepository::class.java)
        Mockito.doAnswer { _ -> throw e }.`when`(graphResourceRepository)
            .checkVersionVertexExists(Mockito.anyInt())
        return graphResourceRepository
    }
}
