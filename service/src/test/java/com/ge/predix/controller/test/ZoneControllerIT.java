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
package com.ge.predix.controller.test;

import static com.ge.predix.acs.commons.web.AcsApiUriTemplates.V1;
import static com.ge.predix.acs.commons.web.AcsApiUriTemplates.ZONE_URL;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.ge.predix.acs.rest.Zone;
import com.ge.predix.acs.testutils.TestActiveProfilesResolver;
import com.ge.predix.acs.utils.JsonUtils;

@WebAppConfiguration
@ContextConfiguration("classpath:controller-tests-context.xml")
@ActiveProfiles(resolver = TestActiveProfilesResolver.class)
@Test
public class ZoneControllerIT extends AbstractTestNGSpringContextTests {

    private final String zoneUrl = V1 + ZONE_URL;

    private final ObjectWriter objectWriter = new ObjectMapper().writer().withDefaultPrettyPrinter();

    @Autowired
    private WebApplicationContext wac;

    private MockMvc mockMvc;

    private final JsonUtils jsonUtils = new JsonUtils();

    private Zone zone;

    @BeforeClass
    public void setup() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).build();
    }

    public void testCreateAndGetAndDeleteZone() throws Exception {
        this.zone = this.jsonUtils.deserializeFromFile("controller-test/createZone.json", Zone.class);
        Assert.assertNotNull(this.zone, "createZone.json file not found or invalid");
        String zoneContent = this.objectWriter.writeValueAsString(this.zone);
        this.mockMvc.perform(put(this.zoneUrl, "zone-1").contentType(MediaType.APPLICATION_JSON).content(zoneContent))
                .andExpect(status().isCreated());

        this.mockMvc.perform(get(this.zoneUrl, "zone-1"))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8)).andExpect(status().isOk())
                .andExpect(jsonPath("name", is("zone-1"))).andExpect(jsonPath("subdomain", is("subdomain-1")));

        this.mockMvc.perform(delete(this.zoneUrl, "zone-1")).andExpect(status().isNoContent());
    }

    public void testUpdateZone() throws Exception {
        this.zone = this.jsonUtils.deserializeFromFile("controller-test/createZone.json", Zone.class);
        Assert.assertNotNull(this.zone, "createZone.json file not found or invalid");
        String zoneContent = this.objectWriter.writeValueAsString(this.zone);
        this.mockMvc.perform(put(this.zoneUrl, "zone-1").contentType(MediaType.APPLICATION_JSON).content(zoneContent))
                .andExpect(status().isCreated());

        this.zone = this.jsonUtils.deserializeFromFile("controller-test/updateZone.json", Zone.class);
        Assert.assertNotNull(this.zone, "updateZone.json file not found or invalid");
        String updatedZoneContent = this.objectWriter.writeValueAsString(this.zone);
        this.mockMvc
                .perform(
                        put(this.zoneUrl, "zone-1").contentType(MediaType.APPLICATION_JSON).content(updatedZoneContent))
                .andExpect(status().isCreated());
        this.mockMvc.perform(get(this.zoneUrl, "zone-1"))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8)).andExpect(status().isOk())
                .andExpect(jsonPath("name", is("zone-1"))).andExpect(jsonPath("subdomain", is("subdomain-2")));

        this.mockMvc.perform(delete(this.zoneUrl, "zone-1")).andExpect(status().isNoContent());
    }

    public void testCreateZoneWithExistingSubdomain() throws Exception {

        Zone zone1 = this.jsonUtils.deserializeFromFile("controller-test/createZone.json", Zone.class);
        Assert.assertNotNull(zone1, "createZone.json file not found or invalid");
        String zoneContent1 = this.objectWriter.writeValueAsString(zone1);

        this.mockMvc.perform(put(this.zoneUrl, "zone-1").contentType(MediaType.APPLICATION_JSON).content(zoneContent1))
                .andExpect(status().isCreated());

        Zone zone2 = this.jsonUtils.deserializeFromFile("controller-test/createZoneTwo.json", Zone.class);
        Assert.assertNotNull(zone2, "createZoneTwo.json file not found or invalid");
        String zoneContent2 = this.objectWriter.writeValueAsString(zone2);
        this.mockMvc.perform(put(this.zoneUrl, "zone-2").contentType(MediaType.APPLICATION_JSON).content(zoneContent2))
                .andExpect(status().isUnprocessableEntity());

        this.mockMvc.perform(delete(this.zoneUrl, "zone-1")).andExpect(status().isNoContent());
    }

    public void testCreateZonewithNoSubdomain() throws Exception {
        this.zone = this.jsonUtils.deserializeFromFile("controller-test/zone-with-no-subdomain.json", Zone.class);
        Assert.assertNotNull(this.zone, "zone-with-no-subdomain.json file not found or invalid");
        String zoneContent = this.objectWriter.writeValueAsString(this.zone);
        this.mockMvc.perform(put(this.zoneUrl, "zone-3").contentType(MediaType.APPLICATION_JSON).content(zoneContent))
                .andExpect(status().isUnprocessableEntity());
    }

    public void testGetZoneWhichDoesNotExists() throws Exception {
        this.mockMvc.perform(get(this.zoneUrl, "zone-2"))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON)).andExpect(status().isNotFound());
    }

    public void testDeleteZoneWhichDoesNotExist() throws Exception {
        this.mockMvc.perform(delete(this.zoneUrl, "zone-2")).andExpect(status().isNotFound());
    }

}
