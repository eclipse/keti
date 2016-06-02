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

import static com.ge.predix.acs.testutils.Constants.RESOURCE_EVIDENCE_0_ATTRS_0;
import static com.ge.predix.acs.testutils.Constants.RESOURCE_EVIDENCE_0_ID;
import static com.ge.predix.acs.testutils.Constants.RESOURCE_SITE_0_ATTRS_0;
import static com.ge.predix.acs.testutils.Constants.RESOURCE_SITE_0_ID;
import static com.ge.predix.acs.testutils.Constants.RESOURCE_XFILE_0_ATTRS_0;
import static com.ge.predix.acs.testutils.Constants.RESOURCE_XFILE_0_ID;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.context.WebApplicationContext;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ge.predix.acs.rest.BaseResource;
import com.ge.predix.acs.rest.Parent;
import com.ge.predix.acs.rest.Zone;
import com.ge.predix.acs.testutils.MockAcsRequestContext;
import com.ge.predix.acs.testutils.MockMvcContext;
import com.ge.predix.acs.testutils.MockSecurityContext;
import com.ge.predix.acs.testutils.TestUtils;
import com.ge.predix.acs.utils.JsonUtils;
import com.ge.predix.acs.zone.management.ZoneService;

@WebAppConfiguration
@ContextConfiguration("classpath:controller-tests-context.xml")
@ActiveProfiles(profiles = { "h2", "public", "simple-cache", "titan" })
public class ResourcePrivilegeMgmtWithGraphDbControllerIT extends AbstractTestNGSpringContextTests {

    public static final ConfigureEnvironment CONFIGURE_ENVIRONMENT = new ConfigureEnvironment();

    private static final String RESOURCE_BASE_URL = "/v1/resource";

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Autowired
    private WebApplicationContext wac;

    @Autowired
    private ZoneService zoneService;

    private final JsonUtils jsonUtils = new JsonUtils();

    private final TestUtils testUtils = new TestUtils();

    private Zone testZone;

    @BeforeClass
    public void setup() throws Exception {
        this.testZone = new TestUtils().createTestZone("ResourceMgmtControllerIT");
        this.zoneService.upsertZone(this.testZone);
        MockSecurityContext.mockSecurityContext(this.testZone);
        MockAcsRequestContext.mockAcsRequestContext(this.testZone);
    }

    @Test
    public void testCreateResourceWithSelfReference() throws Exception {
        BaseResource resource = this.jsonUtils.deserializeFromFile("controller-test/a-resource.json",
                BaseResource.class);
        Assert.assertNotNull(resource);

        // Add resource itself as a parent
        resource.setParents(new HashSet<>(Arrays.asList(new Parent(resource.getResourceIdentifier()))));

        String aResourceURI = RESOURCE_BASE_URL + "/%2Fservices%2Fsecured-api";
        // Create resource
        MockMvcContext putContext = this.testUtils.createWACWithCustomPUTRequestBuilder(this.wac,
                this.testZone.getSubdomain(), aResourceURI);
        MvcResult result = putContext.getMockMvc()
                .perform(putContext.getBuilder().contentType(MediaType.APPLICATION_JSON)
                        .content(OBJECT_MAPPER.writeValueAsString(resource)))
                .andExpect(status().isUnprocessableEntity())
                .andReturn();
        Assert.assertTrue(result.getResolvedException().getMessage()
                .contains("Unable to persist Resource identified by resourceIdentifier = /services/secured-api"));
    }

    /**
     * First we setup a 3 level graph where 3 -> 2 -> 1. Next, we try to update vertex 1 so that 1 -> 3. We expect
     * this to result in a SchemaViolationException because it would introduce a cyclic reference.
     */
    @Test
    public void testCreateResourceWithCyclicReference() throws Exception {
        BaseResource resource0 = new BaseResource(RESOURCE_SITE_0_ID);
        resource0.setAttributes(RESOURCE_SITE_0_ATTRS_0);

        BaseResource resource1 = new BaseResource(RESOURCE_XFILE_0_ID);
        resource1.setAttributes(RESOURCE_XFILE_0_ATTRS_0);
        resource1.setParents(
                new HashSet<>(Arrays.asList(new Parent[] { new Parent(resource0.getResourceIdentifier()) })));

        BaseResource resource2 = new BaseResource(RESOURCE_EVIDENCE_0_ID);
        resource2.setAttributes(RESOURCE_EVIDENCE_0_ATTRS_0);
        resource2.setParents(
                new HashSet<>(Arrays.asList(new Parent[] { new Parent(resource1.getResourceIdentifier()) })));

        List<BaseResource> resources = Arrays.asList(new BaseResource[] { resource0, resource1, resource2 });
        Assert.assertNotNull(resources);

        // Append a list of resources
        MockMvcContext postContext = this.testUtils.createWACWithCustomPOSTRequestBuilder(this.wac,
                this.testZone.getSubdomain(), RESOURCE_BASE_URL);
        postContext.getMockMvc().perform(postContext.getBuilder().contentType(MediaType.APPLICATION_JSON)
                .content(OBJECT_MAPPER.writeValueAsString(resources))).andExpect(status().isNoContent());

        String aResourceURI = RESOURCE_BASE_URL + "/%2Fsite%2Fbasement";

        // Update a given resource
        resource0.setParents(
                new HashSet<>(Arrays.asList(new Parent[] { new Parent(resource2.getResourceIdentifier()) })));

        MockMvcContext updateContext = this.testUtils.createWACWithCustomPUTRequestBuilder(this.wac,
                this.testZone.getSubdomain(), aResourceURI);
        MvcResult result = updateContext.getMockMvc()
                .perform(updateContext.getBuilder().contentType(MediaType.APPLICATION_JSON)
                        .content(OBJECT_MAPPER.writeValueAsString(resource0)))
                .andExpect(status().isUnprocessableEntity())
                .andReturn();
        Assert.assertTrue(result.getResolvedException().getMessage()
                .contains("Unable to persist Resource identified by resourceIdentifier = /site/basement"));
    }
}