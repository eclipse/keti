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

import com.ge.predix.acs.commons.web.BaseRestApi;
import com.ge.predix.acs.rest.BaseResource;
import com.ge.predix.acs.rest.Zone;
import com.ge.predix.acs.testutils.MockAcsRequestContext;
import com.ge.predix.acs.testutils.MockMvcContext;
import com.ge.predix.acs.testutils.MockSecurityContext;
import com.ge.predix.acs.testutils.TestActiveProfilesResolver;
import com.ge.predix.acs.zone.management.ZoneService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.context.WebApplicationContext;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.Arrays;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebAppConfiguration
@ContextConfiguration("classpath:controller-tests-context.xml")
@ActiveProfiles(resolver = TestActiveProfilesResolver.class)
public final class NonHierarchicalResourcesIT extends AbstractTestNGSpringContextTests {
    private static final Zone TEST_ZONE =
        ResourcePrivilegeManagementControllerIT.TEST_UTILS.createTestZone("ResourceMgmtControllerIT");

    @Autowired
    private ZoneService zoneService;

    @Autowired
    private WebApplicationContext wac;

    @Autowired
    private ConfigurableEnvironment configurableEnvironment;

    @BeforeClass
    public void beforeClass() throws Exception {
        if (Arrays.asList(this.configurableEnvironment.getActiveProfiles()).contains("titan")) {
            throw new SkipException("This test only applies when NOT using the \"titan\" profile");
        }

        this.zoneService.upsertZone(TEST_ZONE);
        MockSecurityContext.mockSecurityContext(TEST_ZONE);
        MockAcsRequestContext.mockAcsRequestContext(TEST_ZONE);
    }

    @Test
    public void testResourceWithParentsFailWhenNotUsingTitan() throws Exception {
        BaseResource resource = ResourcePrivilegeManagementControllerIT.JSON_UTILS.deserializeFromFile(
            "controller-test/a-resource-with-parents.json", BaseResource.class);
        Assert.assertNotNull(resource);

        MockMvcContext putContext =
            ResourcePrivilegeManagementControllerIT.TEST_UTILS.createWACWithCustomPUTRequestBuilder(
                this.wac, TEST_ZONE.getSubdomain(),
                ResourcePrivilegeManagementControllerIT.RESOURCE_BASE_URL
                + "/%2Fservices%2Fsecured-api-with-parents");
        final MvcResult result = putContext.getMockMvc()
                                           .perform(putContext.getBuilder().contentType(MediaType.APPLICATION_JSON)
                                                              .content(ResourcePrivilegeManagementControllerIT
                                                                           .OBJECT_MAPPER.writeValueAsString(resource)))
                                           .andExpect(status().isNotImplemented())
                                           .andReturn();

        Assert.assertEquals(result.getResponse().getContentAsString(),
                            "{\"ErrorDetails\":{\"errorCode\":\"FAILURE\","
                            + "\"errorMessage\":\"" + BaseRestApi.PARENTS_ATTR_NOT_SUPPORTED_MSG + "\"}}");
    }
}
