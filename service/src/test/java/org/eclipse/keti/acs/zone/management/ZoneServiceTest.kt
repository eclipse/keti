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

package org.eclipse.keti.acs.zone.management

import com.ge.predix.uaa.token.lib.ZoneOAuth2Authentication
import org.eclipse.keti.acs.SpringSecurityPolicyContextResolver
import org.eclipse.keti.acs.config.GraphConfig
import org.eclipse.keti.acs.config.InMemoryDataSourceConfig
import org.eclipse.keti.acs.privilege.management.dao.ResourceRepository
import org.eclipse.keti.acs.privilege.management.dao.SubjectRepository
import org.eclipse.keti.acs.rest.Zone
import org.eclipse.keti.acs.testutils.TestActiveProfilesResolver
import org.eclipse.keti.acs.testutils.TestUtils
import org.eclipse.keti.acs.zone.management.dao.ZoneEntity
import org.eclipse.keti.acs.zone.management.dao.ZoneRepository
import org.eclipse.keti.acs.zone.resolver.SpringSecurityZoneResolver
import org.mockito.ArgumentMatchers.anyList
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.provider.OAuth2Request
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.testng.AbstractTransactionalTestNGSpringContextTests
import org.testng.Assert
import org.testng.annotations.BeforeMethod
import org.testng.annotations.DataProvider
import org.testng.annotations.Test

/**
 *
 * @author acs-engineers@ge.com
 */

@ContextConfiguration(
    classes = [
        GraphConfig::class,
        InMemoryDataSourceConfig::class,
        SpringSecurityPolicyContextResolver::class,
        SpringSecurityZoneResolver::class,
        ZoneServiceImpl::class
    ]
)
@TestPropertySource("/application.properties")
@ActiveProfiles(resolver = TestActiveProfilesResolver::class)
@Test
class ZoneServiceTest : AbstractTransactionalTestNGSpringContextTests() {

    @Autowired
    private val zoneService: ZoneService? = null

    private val testUtils = TestUtils()

    @BeforeMethod
    fun createSampleData() {
        this.zoneService!!.upsertZone(this.testUtils.createZone("zone1", "subdomain1"))
        val acsAuth = ZoneOAuth2Authentication(
            Mockito.mock(OAuth2Request::class.java), null,
            "subdomain1"
        )
        SecurityContextHolder.getContext().authentication = acsAuth
    }

    @Test(dataProvider = "badSubDomainDataProvider")
    fun testZoneCreationWithIllegalZoneNames(zoneSubdomain: String) {
        try {
            this.zoneService!!.upsertZone(Zone("illegal_zone", zoneSubdomain, "desc"))
            Assert.fail("Expected an exception for invalid zone name")
        } catch (e: ZoneManagementException) {
            Assert.assertTrue(e.message?.contains("Invalid Zone Subdomain")!!)
        }

    }

    @Test(
        expectedExceptions = [(RuntimeException::class)],
        expectedExceptionsMessageRegExp = "Subject Deletion Failed!"
    )
    fun deleteZoneFailsWhenDeleteSubjectFails() {
        val zoneRepository = Mockito.mock(ZoneRepository::class.java)
        Mockito.`when`<ZoneEntity>(zoneRepository.getByName("test-zone")).thenReturn(ZoneEntity(1L, "test-zone"))
        val resourceRepository = Mockito.mock(ResourceRepository::class.java)
        Mockito.doNothing().`when`(resourceRepository).delete(anyList())
        val subjectRepository = Mockito.mock(SubjectRepository::class.java)
        Mockito.doAnswer { _ -> throw RuntimeException("Subject Deletion Failed!") }.`when`(subjectRepository)
            .delete(anyList())
        this.testUtils.setField(this.zoneService, "subjectRepository", subjectRepository)
        this.testUtils.setField(this.zoneService, "resourceRepository", resourceRepository)
        this.testUtils.setField(this.zoneService, "zoneRepository", zoneRepository)
        this.zoneService!!.deleteZone("test-zone")
    }

    @DataProvider(name = "badSubDomainDataProvider")
    private fun badSubDomainDataProvider(): Array<Array<Any?>> {
        return arrayOf<Array<Any?>>(
            arrayOf("-baddomain"),
            arrayOf("baddomain-"),
            arrayOf("bad.domain"),
            arrayOf(".baddomain"),
            arrayOf("baddomain."),
            arrayOf("bad$#%#$"),
            arrayOf("_baddomain")
        )
    }
}
