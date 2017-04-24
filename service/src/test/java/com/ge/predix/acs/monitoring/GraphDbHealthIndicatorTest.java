/*
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
 */
package com.ge.predix.acs.monitoring;

import com.ge.predix.acs.privilege.management.dao.GraphResourceRepository;
import com.thinkaurelius.titan.core.QueryException;
import com.thinkaurelius.titan.core.TitanConfigurationException;
import com.thinkaurelius.titan.diskstorage.ResourceUnavailableException;
import com.thinkaurelius.titan.graphdb.database.idassigner.IDPoolExhaustedException;
import org.mockito.Mockito;
import org.springframework.boot.actuate.health.Status;
import org.springframework.test.util.ReflectionTestUtils;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class GraphDbHealthIndicatorTest {

    @Test(dataProvider = "statuses")
    public void testHealth(final GraphResourceRepository graphResourceRepository, final Status status,
            final AcsMonitoringUtilities.HealthCode healthCode, final boolean cassandraEnabled) throws Exception {
        GraphDbHealthIndicator graphDbHealthIndicator = new GraphDbHealthIndicator(graphResourceRepository);
        ReflectionTestUtils.setField(graphDbHealthIndicator, "cassandraEnabled", cassandraEnabled);
        Assert.assertEquals(status, graphDbHealthIndicator.health().getStatus());
        Assert.assertEquals(GraphDbHealthIndicator.DESCRIPTION,
                graphDbHealthIndicator.health().getDetails().get(AcsMonitoringUtilities.DESCRIPTION_KEY));
        if (healthCode == AcsMonitoringUtilities.HealthCode.AVAILABLE) {
            Assert.assertFalse(
                    graphDbHealthIndicator.health().getDetails().containsKey(AcsMonitoringUtilities.CODE_KEY));
        } else {
            Assert.assertEquals(healthCode,
                    graphDbHealthIndicator.health().getDetails().get(AcsMonitoringUtilities.CODE_KEY));
        }
    }

    @DataProvider
    public Object[][] statuses() {
        return new Object[][] {
                new Object[] { mockGraphDbWithUp(), Status.UP, AcsMonitoringUtilities.HealthCode.IN_MEMORY, false },

                { mockGraphDbWithUp(), Status.UP, AcsMonitoringUtilities.HealthCode.AVAILABLE, true },

                { mockGraphDbWithExceptionWhileCheckingVersion(new QueryException("")), Status.DOWN,
                        AcsMonitoringUtilities.HealthCode.INVALID_QUERY, true },

                { mockGraphDbWithExceptionWhileCheckingVersion(new ResourceUnavailableException("")), Status.DOWN,
                        AcsMonitoringUtilities.HealthCode.UNAVAILABLE, true },

                { mockGraphDbWithExceptionWhileCheckingVersion(new TitanConfigurationException("")), Status.DOWN,
                        AcsMonitoringUtilities.HealthCode.MISCONFIGURATION, true },

                { mockGraphDbWithExceptionWhileCheckingVersion(new IDPoolExhaustedException("")), Status.DOWN,
                        AcsMonitoringUtilities.HealthCode.ERROR, true }, };

    }

    private GraphResourceRepository mockGraphDbWithUp() {
        GraphResourceRepository graphResourceRepository = Mockito.mock(GraphResourceRepository.class);
        Mockito.doReturn(true).when(graphResourceRepository).checkVersionVertexExists(Mockito.anyInt());
        return graphResourceRepository;
    }

    private GraphResourceRepository mockGraphDbWithExceptionWhileCheckingVersion(final Exception e) {
        GraphResourceRepository graphResourceRepository = Mockito.mock(GraphResourceRepository.class);
        Mockito.doThrow(e).when(graphResourceRepository).checkVersionVertexExists(Mockito.anyInt());
        return graphResourceRepository;
    }
}
