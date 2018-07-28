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

package org.eclipse.keti.acs.zone.management

import org.eclipse.keti.acs.config.InMemoryDataSourceConfig
import org.eclipse.keti.acs.encryption.Encryptor
import org.eclipse.keti.acs.rest.AttributeAdapterConnection
import org.eclipse.keti.acs.rest.AttributeConnector
import org.eclipse.keti.acs.testutils.TestActiveProfilesResolver
import org.eclipse.keti.acs.zone.management.dao.ZoneEntity
import org.eclipse.keti.acs.zone.management.dao.ZoneRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.testng.AbstractTransactionalTestNGSpringContextTests
import org.testng.Assert
import org.testng.annotations.Test

@EnableAutoConfiguration
@ContextConfiguration(classes = [(InMemoryDataSourceConfig::class), (Encryptor::class)])
@ActiveProfiles(resolver = TestActiveProfilesResolver::class)
@TestPropertySource("/application.properties")
class ZoneRepositoryTest : AbstractTransactionalTestNGSpringContextTests() {

    @Autowired
    private lateinit var zoneRepository: ZoneRepository

    @Test
    @Throws(Exception::class)
    fun testAddConnector() {
        createZoneWithConnectorAndAssert()
    }

    @Test
    @Throws(Exception::class)
    fun testUpdateConnector() {
        val zone = createZoneWithConnectorAndAssert()

        val expectedConnector = AttributeConnector()
        expectedConnector.isActive = true
        expectedConnector.adapters = setOf(
            AttributeAdapterConnection(
                "http://some-adapter.com",
                "http://some-uaa.com", "some-client", "some-secret"
            )
        )
        zone.resourceAttributeConnector = expectedConnector

        this.zoneRepository.save(zone)

        // Assert that zone connectors and adapters are updated
        val actualConnector = this.zoneRepository.getByName(zone.name!!)!!
            .resourceAttributeConnector
        Assert.assertEquals(actualConnector, expectedConnector)
        Assert.assertEquals(actualConnector!!.adapters, expectedConnector.adapters)
    }

    @Test
    @Throws(Exception::class)
    fun testDeleteConnector() {
        val zone = createZoneWithConnectorAndAssert()

        zone.resourceAttributeConnector = null
        this.zoneRepository.save(zone)
        Assert.assertNull(this.zoneRepository.getByName(zone.name!!)!!.resourceAttributeConnector)
    }

    @Throws(Exception::class)
    private fun createZoneWithConnectorAndAssert(): ZoneEntity {
        val expectedConnector = AttributeConnector()
        val expectedAdapters =
            setOf(AttributeAdapterConnection("http://my-adapter.com", "http://my-uaa", "my-client", "my-secret"))
        expectedConnector.adapters = expectedAdapters
        expectedConnector.maxCachedIntervalMinutes = 24
        val zone = ZoneEntity()
        zone.name = "azone"
        zone.subdomain = "asubdomain"
        zone.description = "adescription"
        zone.resourceAttributeConnector = expectedConnector
        this.zoneRepository.save(zone)
        val acutalZone = this.zoneRepository.getByName("azone")
        Assert.assertEquals(acutalZone!!.subjectAttributeConnector, null)
        Assert.assertEquals(acutalZone.resourceAttributeConnector, expectedConnector)
        Assert.assertEquals(acutalZone.resourceAttributeConnector!!.adapters, expectedAdapters)
        return zone
    }
}
