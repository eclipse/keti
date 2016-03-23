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

import org.mockito.Mockito;
import org.mockito.internal.util.reflection.Whitebox;
import org.springframework.boot.actuate.health.Status;
import org.springframework.dao.PermissionDeniedDataAccessException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.dao.TransientDataAccessResourceException;
import org.springframework.jdbc.datasource.lookup.DataSourceLookupFailureException;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class AcsDBHealthIndicatorTest {

    private final AcsDBHealthIndicator acsDBHealthIndicator = new AcsDBHealthIndicator();

    @Test(dataProvider = "dp")
    public void health(final AcsMonitoringRepository acsMonitoringRepository, final Status status) {

        Whitebox.setInternalState(this.acsDBHealthIndicator, "acsMonitoringRepository", acsMonitoringRepository);
        Assert.assertEquals(status, this.acsDBHealthIndicator.health().getStatus());

    }

    @DataProvider
    public Object[][] dp() {
        return new Object[][] { new Object[] { mockDBWithUp(), Status.UP },
                { mockDBWithException(new TransientDataAccessResourceException("")),
                        new Status(AcsMonitoringConstants.ACS_DB_OUT_OF_SERVICE) },

                { mockDBWithException(new QueryTimeoutException("")),
                        new Status(AcsMonitoringConstants.ACS_DB_OUT_OF_SERVICE) },

                { mockDBWithException(new DataSourceLookupFailureException("")),
                        new Status(AcsMonitoringConstants.ACS_DB_OUT_OF_SERVICE) },

                { mockDBWithException(new PermissionDeniedDataAccessException("", null)),
                        new Status(AcsMonitoringConstants.ACS_DB_OUT_OF_SERVICE) }, };
    }

    private AcsMonitoringRepository mockDBWithUp() {

        AcsMonitoringRepository acsMonitoringRepository = Mockito.mock(AcsMonitoringRepository.class);
        Mockito.doNothing().when(acsMonitoringRepository).checkDBAccess();

        return acsMonitoringRepository;
    }

    private AcsMonitoringRepository mockDBWithException(final Exception e) {

        AcsMonitoringRepository acsMonitoringRepository = Mockito.mock(AcsMonitoringRepository.class);
        Mockito.doThrow(e).when(acsMonitoringRepository).checkDBAccess();

        return acsMonitoringRepository;
    }

}
