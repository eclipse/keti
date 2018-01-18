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
 *
 * SPDX-License-Identifier: Apache-2.0
 *******************************************************************************/

package org.eclipse.keti.acs.monitoring;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Modified version of org.springframework.boot.actuate.health.OrderedHealthAggregatorTests
public class AcsHealthAggregatorTest {

    private AcsHealthAggregator healthAggregator;

    @BeforeMethod
    public void beforeMethod() {
        this.healthAggregator = new AcsHealthAggregator();
    }

    @Test
    public void defaultOrder() {
        Map<String, Health> healths = new HashMap<>();
        healths.put("h1", new Health.Builder().status(Status.DOWN).build());
        healths.put("h2", new Health.Builder().status(Status.UP).build());
        healths.put("h3", new Health.Builder().status(Status.UNKNOWN).build());
        healths.put("h4", new Health.Builder().status(Status.OUT_OF_SERVICE).build());
        Assert.assertEquals(this.healthAggregator.aggregate(healths).getStatus(), Status.DOWN);
    }

    @Test
    public void customOrder() {
        this.healthAggregator.setStatusOrder(Status.UNKNOWN, Status.UP, Status.OUT_OF_SERVICE, Status.DOWN);
        Map<String, Health> healths = new HashMap<>();
        healths.put("h1", new Health.Builder().status(Status.DOWN).build());
        healths.put("h2", new Health.Builder().status(Status.UP).build());
        healths.put("h3", new Health.Builder().status(Status.UNKNOWN).build());
        healths.put("h4", new Health.Builder().status(Status.OUT_OF_SERVICE).build());
        Assert.assertEquals(this.healthAggregator.aggregate(healths).getStatus(), Status.UNKNOWN);
    }

    @Test
    public void defaultOrderWithCustomStatus() {
        Map<String, Health> healths = new HashMap<>();
        healths.put("h1", new Health.Builder().status(Status.DOWN).build());
        healths.put("h2", new Health.Builder().status(Status.UP).build());
        healths.put("h3", new Health.Builder().status(Status.UNKNOWN).build());
        healths.put("h4", new Health.Builder().status(Status.OUT_OF_SERVICE).build());
        healths.put("h5", new Health.Builder().status(new Status("CUSTOM")).build());
        Assert.assertEquals(this.healthAggregator.aggregate(healths).getStatus(), Status.DOWN);
    }

    @Test(dataProvider = "statuses")
    public void defaultOrderWithDegradedStatus(final Status expectedStatus, final List<Status> actualStatuses) {
        Map<String, Health> healths = new HashMap<>();
        healths.put("h1", new Health.Builder().status(actualStatuses.get(0)).build());
        healths.put("h2", new Health.Builder().status(actualStatuses.get(1)).build());
        healths.put("h3", new Health.Builder().status(actualStatuses.get(2)).build());
        Assert.assertEquals(this.healthAggregator.aggregate(healths).getStatus(), expectedStatus);
    }

    @DataProvider
    public Object[][] statuses() {
        return new Object[][] { new Object[] { AcsHealthAggregator.DEGRADED_STATUS,
                Arrays.asList(Status.UP, AcsHealthAggregator.DEGRADED_STATUS, Status.UP) },

                { Status.UP, Arrays.asList(Status.UP, Status.UNKNOWN, Status.UP) },

                { Status.DOWN, Arrays.asList(Status.UP, AcsHealthAggregator.DEGRADED_STATUS, Status.DOWN) }, };
    }

    @Test
    public void customOrderWithCustomStatus() {
        this.healthAggregator.setStatusOrder(Arrays.asList("DOWN", "OUT_OF_SERVICE", "UP", "UNKNOWN", "CUSTOM"));
        Map<String, Health> healths = new HashMap<>();
        healths.put("h1", new Health.Builder().status(Status.DOWN).build());
        healths.put("h2", new Health.Builder().status(Status.UP).build());
        healths.put("h3", new Health.Builder().status(Status.UNKNOWN).build());
        healths.put("h4", new Health.Builder().status(Status.OUT_OF_SERVICE).build());
        healths.put("h5", new Health.Builder().status(new Status("CUSTOM")).build());
        Assert.assertEquals(this.healthAggregator.aggregate(healths).getStatus(), Status.DOWN);
    }

    @Test(dataProvider = "singleStatuses")
    public void customOrderWithSingleStatus(final String key, final Health health, final Status expectedStatus) {
        Map<String, Health> healths = new HashMap<>();
        healths.put(key, health);
        Assert.assertEquals(this.healthAggregator.aggregate(healths).getStatus(), expectedStatus);
    }

    @DataProvider
    public Object[][] singleStatuses() {
        return new Object[][] {
                new Object[] { "h1", new Health.Builder().status(new Status("CUSTOM")).build(), Status.UNKNOWN },

                { "cache", new Health.Builder().status(Status.DOWN).build(), AcsHealthAggregator.DEGRADED_STATUS },

                { "cache", new Health.Builder().status(Status.UNKNOWN).build(), Status.UP }, };
    }

    @Test
    public void noStatuses() throws Exception {
        Map<String, Health> healths = new HashMap<>();
        Assert.assertEquals(this.healthAggregator.aggregate(healths).getStatus(), Status.UNKNOWN);
    }
}
