/*******************************************************************************
 * Copyright 2018 General Electric Company
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
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.eclipse.keti.acs.monitoring

import org.springframework.boot.actuate.health.Health
import org.springframework.boot.actuate.health.Status
import org.testng.Assert
import org.testng.annotations.BeforeMethod
import org.testng.annotations.DataProvider
import org.testng.annotations.Test
import java.util.Arrays
import java.util.HashMap

// Modified version of org.springframework.boot.actuate.health.OrderedHealthAggregatorTests
class AcsHealthAggregatorTest {

    private var healthAggregator: AcsHealthAggregator? = null

    @BeforeMethod
    fun beforeMethod() {
        this.healthAggregator = AcsHealthAggregator()
    }

    @Test
    fun defaultOrder() {
        val healths = HashMap<String, Health>()
        healths["h1"] = Health.Builder().status(Status.DOWN).build()
        healths["h2"] = Health.Builder().status(Status.UP).build()
        healths["h3"] = Health.Builder().status(Status.UNKNOWN).build()
        healths["h4"] = Health.Builder().status(Status.OUT_OF_SERVICE).build()
        Assert.assertEquals(this.healthAggregator!!.aggregate(healths).status, Status.DOWN)
    }

    @Test
    fun customOrder() {
        this.healthAggregator!!.setStatusOrder(Status.UNKNOWN, Status.UP, Status.OUT_OF_SERVICE, Status.DOWN)
        val healths = HashMap<String, Health>()
        healths["h1"] = Health.Builder().status(Status.DOWN).build()
        healths["h2"] = Health.Builder().status(Status.UP).build()
        healths["h3"] = Health.Builder().status(Status.UNKNOWN).build()
        healths["h4"] = Health.Builder().status(Status.OUT_OF_SERVICE).build()
        Assert.assertEquals(this.healthAggregator!!.aggregate(healths).status, Status.UNKNOWN)
    }

    @Test
    fun defaultOrderWithCustomStatus() {
        val healths = HashMap<String, Health>()
        healths["h1"] = Health.Builder().status(Status.DOWN).build()
        healths["h2"] = Health.Builder().status(Status.UP).build()
        healths["h3"] = Health.Builder().status(Status.UNKNOWN).build()
        healths["h4"] = Health.Builder().status(Status.OUT_OF_SERVICE).build()
        healths["h5"] = Health.Builder().status(Status("CUSTOM")).build()
        Assert.assertEquals(this.healthAggregator!!.aggregate(healths).status, Status.DOWN)
    }

    @Test(dataProvider = "statuses")
    fun defaultOrderWithDegradedStatus(
        expectedStatus: Status,
        actualStatuses: List<Status>
    ) {
        val healths = HashMap<String, Health>()
        healths["h1"] = Health.Builder().status(actualStatuses[0]).build()
        healths["h2"] = Health.Builder().status(actualStatuses[1]).build()
        healths["h3"] = Health.Builder().status(actualStatuses[2]).build()
        Assert.assertEquals(this.healthAggregator!!.aggregate(healths).status, expectedStatus)
    }

    @DataProvider
    fun statuses(): Array<Array<Any?>> {
        return arrayOf(
            arrayOf(DEGRADED_STATUS, Arrays.asList(Status.UP, DEGRADED_STATUS, Status.UP)),

            arrayOf(Status.UP, Arrays.asList(Status.UP, Status.UNKNOWN, Status.UP)),

            arrayOf(Status.DOWN, Arrays.asList(Status.UP, DEGRADED_STATUS, Status.DOWN))
        )
    }

    @Test
    fun customOrderWithCustomStatus() {
        this.healthAggregator!!.setStatusOrder(Arrays.asList("DOWN", "OUT_OF_SERVICE", "UP", "UNKNOWN", "CUSTOM"))
        val healths = HashMap<String, Health>()
        healths["h1"] = Health.Builder().status(Status.DOWN).build()
        healths["h2"] = Health.Builder().status(Status.UP).build()
        healths["h3"] = Health.Builder().status(Status.UNKNOWN).build()
        healths["h4"] = Health.Builder().status(Status.OUT_OF_SERVICE).build()
        healths["h5"] = Health.Builder().status(Status("CUSTOM")).build()
        Assert.assertEquals(this.healthAggregator!!.aggregate(healths).status, Status.DOWN)
    }

    @Test(dataProvider = "singleStatuses")
    fun customOrderWithSingleStatus(
        key: String,
        health: Health,
        expectedStatus: Status
    ) {
        val healths = HashMap<String, Health>()
        healths[key] = health
        Assert.assertEquals(this.healthAggregator!!.aggregate(healths).status, expectedStatus)
    }

    @DataProvider
    fun singleStatuses(): Array<Array<Any?>> {
        return arrayOf(
            arrayOf("h1", Health.Builder().status(Status("CUSTOM")).build(), Status.UNKNOWN),

            arrayOf("cache", Health.Builder().status(Status.DOWN).build(), DEGRADED_STATUS),

            arrayOf("cache", Health.Builder().status(Status.UNKNOWN).build(), Status.UP)
        )
    }

    @Test
    @Throws(Exception::class)
    fun noStatuses() {
        val healths = HashMap<String, Health>()
        Assert.assertEquals(this.healthAggregator!!.aggregate(healths).status, Status.UNKNOWN)
    }
}
