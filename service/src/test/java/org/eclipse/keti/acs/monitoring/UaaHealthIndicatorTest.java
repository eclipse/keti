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
            final HealthCode healthCode) throws Exception {
        UaaHealthIndicator uaaHealthIndicator = new UaaHealthIndicator(restTemplate);
        Assert.assertEquals(status, uaaHealthIndicator.health().getStatus());
        Assert.assertEquals(uaaHealthIndicator.getDescription(),
                uaaHealthIndicator.health().getDetails().get(DESCRIPTION_KEY));
        if (healthCode == HealthCode.AVAILABLE) {
            Assert.assertFalse(uaaHealthIndicator.health().getDetails().containsKey(
                    CODE_KEY));
        } else {
            Assert.assertEquals(healthCode,
                    uaaHealthIndicator.health().getDetails().get(CODE_KEY));
        }
    }

    @DataProvider
    public Object[][] statuses() {
        return new Object[][] {
                new Object[] { mockRestWithUp(), Status.UP, HealthCode.AVAILABLE },

                { mockRestWithException(new RestClientException("")), Status.DOWN,
                        HealthCode.UNREACHABLE },

                { mockRestWithException(new RuntimeException()), Status.DOWN,
                        HealthCode.ERROR }, };
    }

    private RestTemplate mockRestWithUp() {
        RestTemplate restTemplate = Mockito.mock(RestTemplate.class);
        Mockito.when(restTemplate.getForObject(this.uaaCheckHealthUrl, String.class)).thenReturn("OK");
        return restTemplate;
    }

    private RestTemplate mockRestWithException(final Exception e) {
        RestTemplate restTemplate = Mockito.mock(RestTemplate.class);
        Mockito.when(restTemplate.getForObject(this.uaaCheckHealthUrl, String.class))
               .thenAnswer(invocation -> {
                   throw e;
               });
        return restTemplate;
    }
}
