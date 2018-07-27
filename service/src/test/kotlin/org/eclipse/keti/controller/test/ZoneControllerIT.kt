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

package org.eclipse.keti.controller.test

import com.fasterxml.jackson.databind.ObjectMapper
import org.eclipse.keti.acs.commons.web.V1
import org.eclipse.keti.acs.commons.web.ZONE_URL
import org.eclipse.keti.acs.rest.Zone
import org.eclipse.keti.acs.testutils.TestActiveProfilesResolver
import org.eclipse.keti.acs.utils.JsonUtils
import org.hamcrest.Matchers.`is`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests
import org.springframework.test.context.web.WebAppConfiguration
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import org.testng.Assert
import org.testng.annotations.BeforeClass
import org.testng.annotations.Test

private const val V1_ZONE_URL = V1 + ZONE_URL

@WebAppConfiguration
@ContextConfiguration("classpath:controller-tests-context.xml")
@ActiveProfiles(resolver = TestActiveProfilesResolver::class)
@Test
class ZoneControllerIT : AbstractTestNGSpringContextTests() {

    private val objectWriter = ObjectMapper().writer().withDefaultPrettyPrinter()

    @Autowired
    private lateinit var wac: WebApplicationContext

    private var mockMvc: MockMvc? = null

    private val jsonUtils = JsonUtils()

    private var zone: Zone? = null

    @BeforeClass
    fun setup() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).build()
    }

    @Throws(Exception::class)
    fun testCreateAndGetAndDeleteZone() {
        this.zone = this.jsonUtils.deserializeFromFile("controller-test/createZone.json", Zone::class.java)
        Assert.assertNotNull(this.zone, "createZone.json file not found or invalid")
        val zoneContent = this.objectWriter.writeValueAsString(this.zone)
        this.mockMvc!!.perform(put(V1_ZONE_URL, "zone-1").contentType(MediaType.APPLICATION_JSON).content(zoneContent))
            .andExpect(status().isCreated)

        this.mockMvc!!.perform(get(V1_ZONE_URL, "zone-1"))
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8)).andExpect(status().isOk)
            .andExpect(jsonPath("name", `is`("zone-1"))).andExpect(jsonPath("subdomain", `is`("subdomain-1")))

        this.mockMvc!!.perform(delete(V1_ZONE_URL, "zone-1")).andExpect(status().isNoContent)
    }

    @Throws(Exception::class)
    fun testZoneInvalidMediaTypeResponseStatusCheck() {

        this.mockMvc!!.perform(put(V1_ZONE_URL, "zone-1").contentType(MediaType.TEXT_XML_VALUE).content("testString"))
            .andExpect(status().isUnsupportedMediaType)
    }

    @Throws(Exception::class)
    fun testUpdateZone() {
        this.zone = this.jsonUtils.deserializeFromFile("controller-test/createZone.json", Zone::class.java)
        Assert.assertNotNull(this.zone, "createZone.json file not found or invalid")
        val zoneContent = this.objectWriter.writeValueAsString(this.zone)
        this.mockMvc!!.perform(put(V1_ZONE_URL, "zone-1").contentType(MediaType.APPLICATION_JSON).content(zoneContent))
            .andExpect(status().isCreated)

        this.zone = this.jsonUtils.deserializeFromFile("controller-test/updateZone.json", Zone::class.java)
        Assert.assertNotNull(this.zone, "updateZone.json file not found or invalid")
        val updatedZoneContent = this.objectWriter.writeValueAsString(this.zone)
        this.mockMvc!!.perform(
            put(V1_ZONE_URL, "zone-1").contentType(MediaType.APPLICATION_JSON).content(updatedZoneContent)
        ).andExpect(status().isCreated)
        this.mockMvc!!.perform(get(V1_ZONE_URL, "zone-1"))
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8)).andExpect(status().isOk)
            .andExpect(jsonPath("name", `is`("zone-1"))).andExpect(jsonPath("subdomain", `is`("subdomain-2")))

        this.mockMvc!!.perform(delete(V1_ZONE_URL, "zone-1")).andExpect(status().isNoContent)
    }

    @Throws(Exception::class)
    fun testCreateZoneWithExistingSubdomain() {

        val zone1 = this.jsonUtils.deserializeFromFile("controller-test/createZone.json", Zone::class.java)
        Assert.assertNotNull(zone1, "createZone.json file not found or invalid")
        val zoneContent1 = this.objectWriter.writeValueAsString(zone1)

        this.mockMvc!!.perform(put(V1_ZONE_URL, "zone-1").contentType(MediaType.APPLICATION_JSON).content(zoneContent1))
            .andExpect(status().isCreated)

        val zone2 = this.jsonUtils.deserializeFromFile("controller-test/createZoneTwo.json", Zone::class.java)
        Assert.assertNotNull(zone2, "createZoneTwo.json file not found or invalid")
        val zoneContent2 = this.objectWriter.writeValueAsString(zone2)
        this.mockMvc!!.perform(put(V1_ZONE_URL, "zone-2").contentType(MediaType.APPLICATION_JSON).content(zoneContent2))
            .andExpect(status().isUnprocessableEntity)

        this.mockMvc!!.perform(delete(V1_ZONE_URL, "zone-1")).andExpect(status().isNoContent)
    }

    @Throws(Exception::class)
    fun testCreateZonewithNoSubdomain() {
        this.zone = this.jsonUtils.deserializeFromFile("controller-test/zone-with-no-subdomain.json", Zone::class.java)
        Assert.assertNotNull(this.zone, "zone-with-no-subdomain.json file not found or invalid")
        val zoneContent = this.objectWriter.writeValueAsString(this.zone)
        this.mockMvc!!.perform(put(V1_ZONE_URL, "zone-3").contentType(MediaType.APPLICATION_JSON).content(zoneContent))
            .andExpect(status().isUnprocessableEntity)
    }

    @Throws(Exception::class)
    fun testGetZoneWhichDoesNotExists() {
        this.mockMvc!!.perform(get(V1_ZONE_URL, "zone-2")).andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isNotFound)
    }

    @Throws(Exception::class)
    fun testDeleteZoneWhichDoesNotExist() {
        this.mockMvc!!.perform(delete(V1_ZONE_URL, "zone-2")).andExpect(status().isNotFound)
    }
}
