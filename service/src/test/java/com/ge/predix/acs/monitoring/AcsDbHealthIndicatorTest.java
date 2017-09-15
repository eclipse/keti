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
 *******************************************************************************/

package com.ge.predix.acs.monitoring;

import com.ge.predix.acs.privilege.management.dao.TitanMigrationManager;
import org.mockito.Mockito;
import org.mockito.internal.util.reflection.Whitebox;
import org.springframework.boot.actuate.health.Status;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.PermissionDeniedDataAccessException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.dao.TransientDataAccessResourceException;
import org.springframework.jdbc.datasource.lookup.DataSourceLookupFailureException;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class AcsDbHealthIndicatorTest {

    private static final String IS_MIGRATION_COMPLETE_FIELD_NAME = "isMigrationComplete";

    @Test(dataProvider = "statuses")
    public void testHealth(final AcsMonitoringRepository acsMonitoringRepository, final Status status,
            final AcsMonitoringUtilities.HealthCode healthCode, final TitanMigrationManager titanMigrationManager)
            throws Exception {
        AcsDbHealthIndicator acsDbHealthIndicator = new AcsDbHealthIndicator(acsMonitoringRepository);
        acsDbHealthIndicator.setMigrationManager(titanMigrationManager);
        Assert.assertEquals(status, acsDbHealthIndicator.health().getStatus());
        Assert.assertEquals(AcsDbHealthIndicator.DESCRIPTION,
                acsDbHealthIndicator.health().getDetails().get(AcsMonitoringUtilities.DESCRIPTION_KEY));
        if (healthCode == AcsMonitoringUtilities.HealthCode.AVAILABLE) {
            Assert.assertFalse(acsDbHealthIndicator.health().getDetails().containsKey(AcsMonitoringUtilities.CODE_KEY));
        } else {
            Assert.assertEquals(healthCode,
                    acsDbHealthIndicator.health().getDetails().get(AcsMonitoringUtilities.CODE_KEY));
        }
    }

    @DataProvider
    public Object[][] statuses() {
        TitanMigrationManager happyMigrationManager = new TitanMigrationManager();
        TitanMigrationManager sadMigrationManager = new TitanMigrationManager();
        Whitebox.setInternalState(happyMigrationManager, IS_MIGRATION_COMPLETE_FIELD_NAME, true);
        Whitebox.setInternalState(sadMigrationManager, IS_MIGRATION_COMPLETE_FIELD_NAME, false);

        return new Object[][] { new Object[] { mockDbWithUp(), Status.UP, AcsMonitoringUtilities.HealthCode.AVAILABLE,
                happyMigrationManager },

                { mockDbWithException(new TransientDataAccessResourceException("")), Status.DOWN,
                        AcsMonitoringUtilities.HealthCode.UNAVAILABLE, happyMigrationManager },

                { mockDbWithException(new QueryTimeoutException("")), Status.DOWN,
                        AcsMonitoringUtilities.HealthCode.UNAVAILABLE, happyMigrationManager },

                { mockDbWithException(new DataSourceLookupFailureException("")), Status.DOWN,
                        AcsMonitoringUtilities.HealthCode.UNREACHABLE, happyMigrationManager },

                { mockDbWithException(new PermissionDeniedDataAccessException("", null)), Status.DOWN,
                        AcsMonitoringUtilities.HealthCode.MISCONFIGURATION, happyMigrationManager },

                { mockDbWithException(new ConcurrencyFailureException("")), Status.DOWN,
                        AcsMonitoringUtilities.HealthCode.ERROR, happyMigrationManager },

                { mockDbWithUp(), Status.DOWN, AcsMonitoringUtilities.HealthCode.MIGRATION_INCOMPLETE,
                        sadMigrationManager }, };
    }

    private AcsMonitoringRepository mockDbWithUp() {
        AcsMonitoringRepository acsMonitoringRepository = Mockito.mock(AcsMonitoringRepository.class);
        Mockito.doNothing().when(acsMonitoringRepository).queryPolicySetTable();
        return acsMonitoringRepository;
    }

    private AcsMonitoringRepository mockDbWithException(final Exception e) {
        AcsMonitoringRepository acsMonitoringRepository = Mockito.mock(AcsMonitoringRepository.class);
        Mockito.doThrow(e).when(acsMonitoringRepository).queryPolicySetTable();
        return acsMonitoringRepository;
    }
}
