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

package org.eclipse.keti.acs.monitoring;

import static org.eclipse.keti.acs.monitoring.AcsMonitoringUtilitiesKt.CODE_KEY;
import static org.eclipse.keti.acs.monitoring.AcsMonitoringUtilitiesKt.DESCRIPTION_KEY;
import static org.eclipse.keti.acs.monitoring.GraphDbHealthIndicatorKt.GRAPH_DESCRIPTION;

import org.eclipse.keti.acs.privilege.management.dao.GraphResourceRepository;
import org.janusgraph.core.JanusGraphConfigurationException;
import org.janusgraph.core.QueryException;
import org.janusgraph.diskstorage.ResourceUnavailableException;
import org.janusgraph.graphdb.database.idassigner.IDPoolExhaustedException;
import org.mockito.Mockito;
import org.springframework.boot.actuate.health.Status;
import org.springframework.test.util.ReflectionTestUtils;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class GraphDbHealthIndicatorTest {

    @Test(dataProvider = "statuses")
    public void testHealth(final GraphResourceRepository graphResourceRepository, final Status status,
            final HealthCode healthCode, final boolean cassandraEnabled) throws Exception {
        GraphDbHealthIndicator graphDbHealthIndicator = new GraphDbHealthIndicator(graphResourceRepository);
        ReflectionTestUtils.setField(graphDbHealthIndicator, "cassandraEnabled", cassandraEnabled);
        Assert.assertEquals(status, graphDbHealthIndicator.health().getStatus());
        Assert.assertEquals(GRAPH_DESCRIPTION,
                            graphDbHealthIndicator.health().getDetails().get(
                                    DESCRIPTION_KEY));
        if (healthCode == HealthCode.AVAILABLE) {
            Assert.assertFalse(
                    graphDbHealthIndicator.health().getDetails().containsKey(
                            CODE_KEY));
        } else {
            Assert.assertEquals(healthCode,
                    graphDbHealthIndicator.health().getDetails().get(CODE_KEY));
        }
    }

    @DataProvider
    public Object[][] statuses() {
        return new Object[][] {
                new Object[] { mockGraphDbWithUp(), Status.UP, HealthCode.IN_MEMORY, false },

                { mockGraphDbWithUp(), Status.UP, HealthCode.AVAILABLE, true },

                { mockGraphDbWithExceptionWhileCheckingVersion(new QueryException("")), Status.DOWN,
                        HealthCode.INVALID_QUERY, true },

                { mockGraphDbWithExceptionWhileCheckingVersion(new ResourceUnavailableException("")), Status.DOWN,
                        HealthCode.UNAVAILABLE, true },

                { mockGraphDbWithExceptionWhileCheckingVersion(new JanusGraphConfigurationException("")), Status.DOWN,
                        HealthCode.MISCONFIGURATION, true },

                { mockGraphDbWithExceptionWhileCheckingVersion(new IDPoolExhaustedException("")), Status.DOWN,
                        HealthCode.ERROR, true }, };

    }

    private GraphResourceRepository mockGraphDbWithUp() {
        GraphResourceRepository graphResourceRepository = Mockito.mock(GraphResourceRepository.class);
        Mockito.doReturn(true).when(graphResourceRepository).checkVersionVertexExists(Mockito.anyInt());
        return graphResourceRepository;
    }

    private GraphResourceRepository mockGraphDbWithExceptionWhileCheckingVersion(final Exception e) {
        GraphResourceRepository graphResourceRepository = Mockito.mock(GraphResourceRepository.class);
        Mockito.doAnswer(invocation -> {
            throw e;
        }).when(graphResourceRepository).checkVersionVertexExists(Mockito.anyInt());
        return graphResourceRepository;
    }
}
