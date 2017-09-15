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
 *******************************************************************************/

package com.ge.predix.controller.test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.ge.predix.acs.rest.Zone;
import com.ge.predix.acs.testutils.TestActiveProfilesResolver;
import com.ge.predix.acs.utils.JsonUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebAppConfiguration
@ContextConfiguration("classpath:controller-tests-context.xml")
@ActiveProfiles(resolver = TestActiveProfilesResolver.class)
@Test
public class URLPathMatchingIT extends AbstractTestNGSpringContextTests {
    @Autowired
    private WebApplicationContext wac;

    private MockMvc mockMvc;
    private final JsonUtils jsonUtils = new JsonUtils();
    private final ObjectWriter objectWriter = new ObjectMapper().writer().withDefaultPrettyPrinter();
    private Zone zone;

    @BeforeClass
    public void setup() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).build();
    }

    @Test(dataProvider = "nonMatchedUrlPatternDp")
    public void testReturnNotFoundForNotMatchedURLs(final RequestBuilder request) throws Exception {
        this.mockMvc.perform(request).andExpect(status().isNotFound());
    }

    @DataProvider
    public Object[][] nonMatchedUrlPatternDp() throws JsonProcessingException {
        this.zone = this.jsonUtils.deserializeFromFile("controller-test/createZone.json", Zone.class);
        Assert.assertNotNull(this.zone, "createZone.json file not found or invalid");
        String zoneContent = this.objectWriter.writeValueAsString(this.zone);

        return new Object[][] {
                { put("/ /v1/zone/zone-1", "zone-1").contentType(MediaType.APPLICATION_JSON).content(zoneContent) },
                { put("/v1/ /zone/zone-1", "zone-1").contentType(MediaType.APPLICATION_JSON).content(zoneContent) },
                { get("/ /v1/zone/zone-2").contentType(MediaType.APPLICATION_JSON) },
                { get("/v1/ /zone/zone-2").contentType(MediaType.APPLICATION_JSON) }
        };
    }
}
