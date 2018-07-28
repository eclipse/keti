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

package org.eclipse.keti.acs.service.policy.admin.dao

import org.eclipse.keti.acs.config.InMemoryDataSourceConfig
import org.eclipse.keti.acs.testutils.TestActiveProfilesResolver
import org.eclipse.keti.acs.zone.management.dao.ZoneEntity
import org.eclipse.keti.acs.zone.management.dao.ZoneRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.testng.AbstractTransactionalTestNGSpringContextTests
import org.testng.Assert
import org.testng.annotations.Test

private const val SUBDOMAIN = "PolicySetRepositoryTest-acs"

@ContextConfiguration(classes = [(InMemoryDataSourceConfig::class)])
@EnableAutoConfiguration
@ActiveProfiles(resolver = TestActiveProfilesResolver::class)
class PolicySetRepositoryTest : AbstractTransactionalTestNGSpringContextTests() {

    @Autowired
    private lateinit var policySetRepository: PolicySetRepository

    @Autowired
    private lateinit var zoneRepository: ZoneRepository

    @Test
    fun testPersistPolicy() {

        val zone = createZone()
        this.zoneRepository.save(zone)

        val policySetEntity = PolicySetEntity(zone, "policy-set-2", "{}")
        val savedPolicySet = this.policySetRepository.save(policySetEntity)
        Assert.assertEquals(this.policySetRepository.count(), 1)
        Assert.assertTrue(savedPolicySet.id!! > 0)
    }

    private fun createZone(): ZoneEntity {
        val zone = ZoneEntity()
        zone.name = "PolicySetRepositoryTest-ACS"
        zone.subdomain = SUBDOMAIN
        zone.description = "PolicySetRepositoryTest zone description"
        return zone
    }
}
