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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Status;
import org.springframework.web.client.RestTemplate;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class UaaHealthIndicatorTest {

    @Value("${uaaCheckHealthUrl}")
    private String uaaCheckHealthUrl;

    private final UaaHealthIndicator uaaHealthIndicator = new UaaHealthIndicator();

    @Test(dataProvider = "dp")
    public void health(final RestTemplate restTemplate, final Status status) {

        Whitebox.setInternalState(this.uaaHealthIndicator, "uaaTemplate", restTemplate);
        Assert.assertEquals(status, this.uaaHealthIndicator.health().getStatus());
    }

    @DataProvider
    public Object[][] dp() {
        return new Object[][] { new Object[] { mockRestWithUp(), Status.UP },
                new Object[] { mockRestWithException(), new Status(AcsMonitoringConstants.UAA_OUT_OF_SERVICE) }, };
    }

    private RestTemplate mockRestWithUp() {
        RestTemplate restTemplate = Mockito.mock(RestTemplate.class);

        Mockito.when(restTemplate.getForObject(this.uaaCheckHealthUrl, String.class)).thenReturn("OK");

        return restTemplate;
    }

    private RestTemplate mockRestWithException() {
        RestTemplate restTemplate = Mockito.mock(RestTemplate.class);

        Mockito.when(restTemplate.getForObject(this.uaaCheckHealthUrl, String.class)).thenThrow(new RuntimeException());

        return restTemplate;
    }
}
