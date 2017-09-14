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

import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Status;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class UaaHealthIndicatorTest {

    @Value("${uaaCheckHealthUrl}")
    private String uaaCheckHealthUrl;

    @Test(dataProvider = "statuses")
    public void testHealth(final RestTemplate restTemplate, final Status status,
            final AcsMonitoringUtilities.HealthCode healthCode) throws Exception {
        UaaHealthIndicator uaaHealthIndicator = new UaaHealthIndicator(restTemplate);
        Assert.assertEquals(status, uaaHealthIndicator.health().getStatus());
        Assert.assertEquals(uaaHealthIndicator.getDescription(),
                uaaHealthIndicator.health().getDetails().get(AcsMonitoringUtilities.DESCRIPTION_KEY));
        if (healthCode == AcsMonitoringUtilities.HealthCode.AVAILABLE) {
            Assert.assertFalse(uaaHealthIndicator.health().getDetails().containsKey(AcsMonitoringUtilities.CODE_KEY));
        } else {
            Assert.assertEquals(healthCode,
                    uaaHealthIndicator.health().getDetails().get(AcsMonitoringUtilities.CODE_KEY));
        }
    }

    @DataProvider
    public Object[][] statuses() {
        return new Object[][] {
                new Object[] { mockRestWithUp(), Status.UP, AcsMonitoringUtilities.HealthCode.AVAILABLE },

                { mockRestWithException(new RestClientException("")), Status.DOWN,
                        AcsMonitoringUtilities.HealthCode.UNREACHABLE },

                { mockRestWithException(new RuntimeException()), Status.DOWN,
                        AcsMonitoringUtilities.HealthCode.ERROR }, };
    }

    private RestTemplate mockRestWithUp() {
        RestTemplate restTemplate = Mockito.mock(RestTemplate.class);
        Mockito.when(restTemplate.getForObject(this.uaaCheckHealthUrl, String.class)).thenReturn("OK");
        return restTemplate;
    }

    private RestTemplate mockRestWithException(final Exception e) {
        RestTemplate restTemplate = Mockito.mock(RestTemplate.class);
        Mockito.when(restTemplate.getForObject(this.uaaCheckHealthUrl, String.class)).thenThrow(e);
        return restTemplate;
    }
}
