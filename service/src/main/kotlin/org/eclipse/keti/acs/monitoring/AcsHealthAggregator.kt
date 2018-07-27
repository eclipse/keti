/*******************************************************************************
 * Copyright 2017 General Electric Company
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
import org.springframework.boot.actuate.health.HealthAggregator
import org.springframework.boot.actuate.health.Status
import org.springframework.stereotype.Component
import org.springframework.util.Assert
import java.util.ArrayList
import java.util.Comparator
import java.util.LinkedHashMap

val DEGRADED_STATUS = Status(HealthCode.DEGRADED.toString())

private fun aggregateDetails(healths: Map<String, Health>): Map<String, Any> {
    return LinkedHashMap<String, Any>(healths)
}

@Component
// Modified version of org.springframework.boot.actuate.health.OrderedHealthAggregator
class AcsHealthAggregator internal constructor() : HealthAggregator {

    private var statusOrder: List<String>? = null

    init {
        this.setStatusOrder(Status.DOWN, Status.OUT_OF_SERVICE, DEGRADED_STATUS, Status.UP, Status.UNKNOWN)
    }

    fun setStatusOrder(vararg statusOrder: Status) {
        this.setStatusOrder(statusOrder.map { it.code }.toList())
    }

    fun setStatusOrder(statusOrder: List<String>) {
        Assert.notNull(statusOrder, "StatusOrder must not be null")
        this.statusOrder = statusOrder
    }

    override fun aggregate(healths: Map<String, Health>): Health {
        val statusCandidates = ArrayList<Status>()
        for ((key, value) in healths) {
            val status = value.status
            if (key.toLowerCase().contains("cache")) {
                if (status == Status.DOWN) {
                    statusCandidates.add(DEGRADED_STATUS)
                } else if (status == Status.UNKNOWN) {
                    statusCandidates.add(Status.UP)
                }
            } else {
                statusCandidates.add(value.status)
            }
        }
        val status = this.aggregateStatus(statusCandidates)
        val details = aggregateDetails(healths)
        return Health.Builder(status, details).build()
    }

    private fun aggregateStatus(candidates: List<Status>): Status {
        // Only sort those status instances that we know about
        val filteredCandidates = ArrayList<Status>()
        for (candidate in candidates) {
            if (this.statusOrder!!.contains(candidate.code)) {
                filteredCandidates.add(candidate)
            }
        }
        // If no status is given return UNKNOWN
        if (filteredCandidates.isEmpty()) {
            return Status.UNKNOWN
        }
        // Sort given Status instances by configured order
        filteredCandidates.sortWith(StatusComparator(this.statusOrder!!))
        return filteredCandidates[0]
    }

    private inner class StatusComparator internal constructor(private val statusOrder: List<String>) :
        Comparator<Status> {

        override fun compare(
            s1: Status,
            s2: Status
        ): Int {
            val i1 = this.statusOrder.indexOf(s1.code)
            val i2 = this.statusOrder.indexOf(s2.code)
            return when {
                i1 < i2 -> -1
                i1 == i2 -> s1.code.compareTo(s2.code)
                else -> 1
            }
        }
    }
}
