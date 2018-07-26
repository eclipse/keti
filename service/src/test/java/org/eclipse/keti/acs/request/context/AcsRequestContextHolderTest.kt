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

package org.eclipse.keti.acs.request.context

import org.eclipse.keti.acs.config.GraphConfig
import org.eclipse.keti.acs.config.InMemoryDataSourceConfig
import org.eclipse.keti.acs.request.context.AcsRequestContext.ACSRequestContextAttribute
import org.eclipse.keti.acs.rest.Zone
import org.eclipse.keti.acs.testutils.TestActiveProfilesResolver
import org.eclipse.keti.acs.testutils.TestUtils
import org.eclipse.keti.acs.zone.management.ZoneService
import org.eclipse.keti.acs.zone.management.ZoneServiceImpl
import org.eclipse.keti.acs.zone.management.dao.ZoneEntity
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests
import org.testng.Assert
import org.testng.annotations.AfterClass
import org.testng.annotations.BeforeClass
import org.testng.annotations.Test

private const val ZONE_NAME = "AcsRequestContextHolderTest"
private const val ZONE_NAME_SUFFIX = ".zone"
private const val ZONE_SUBDOMAIN_SUFFIX = "-subdomain"

@ContextConfiguration(
    classes = [
        InMemoryDataSourceConfig::class,
        ZoneServiceImpl::class,
        AcsRequestContextHolder::class,
        GraphConfig::class
    ]
)
@ActiveProfiles(resolver = TestActiveProfilesResolver::class)
@Test
class AcsRequestContextHolderTest : AbstractTestNGSpringContextTests() {

    @Autowired
    private lateinit var zoneService: ZoneService

    private val testUtils = TestUtils()
    private var testZone: Zone? = null

    @BeforeClass
    fun setup() {
        this.testZone = this.testUtils.setupTestZone("AcsRequestContextHolderTest", this.zoneService)
    }

    @AfterClass
    fun cleanup() {
        this.zoneService.deleteZone(this.testZone!!.name!!)
    }

    @Test
    fun testAcsRequestContextSet() {
        val acsRequestContext = acsRequestContext
        val zoneEntity = acsRequestContext!![ACSRequestContextAttribute.ZONE_ENTITY] as ZoneEntity?
        Assert.assertEquals(zoneEntity!!.name, ZONE_NAME + ZONE_NAME_SUFFIX)
        Assert.assertEquals(zoneEntity.subdomain, ZONE_NAME + ZONE_SUBDOMAIN_SUFFIX)
    }

    @Test
    fun testClearAcsRequestContext() {
        clear()
        Assert.assertNull(acsRequestContext)
    }
}
