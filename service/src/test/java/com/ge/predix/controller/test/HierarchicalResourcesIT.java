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
import org.springframework.web.context.WebApplicationContext;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.net.URLEncoder;
import java.util.Arrays;
import java.util.List;

import static com.ge.predix.acs.testutils.XFiles.ASCENSION_ID;
import static com.ge.predix.acs.testutils.XFiles.BASEMENT_SITE_ID;
import static com.ge.predix.acs.testutils.XFiles.EVIDENCE_SCULLYS_TESTIMONY_ID;
import static com.ge.predix.acs.testutils.XFiles.SITE_BASEMENT;
import static com.ge.predix.acs.testutils.XFiles.SPECIAL_AGENTS_GROUP_ATTRIBUTE;
import static com.ge.predix.acs.testutils.XFiles.TOP_SECRET_CLASSIFICATION;
import static com.ge.predix.acs.testutils.XFiles.TYPE_MYTHARC;
import static com.ge.predix.acs.testutils.XFiles.createThreeLevelResourceHierarchy;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebAppConfiguration
@ContextConfiguration("classpath:controller-tests-context.xml")
@ActiveProfiles(resolver = TestActiveProfilesResolver.class)
public class HierarchicalResourcesIT extends AbstractTestNGSpringContextTests {
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
        if (!Arrays.asList(this.configurableEnvironment.getActiveProfiles()).contains("titan")) {
            throw new SkipException("This test only applies when using the \"titan\" profile");
        }

        this.zoneService.upsertZone(TEST_ZONE);
        MockSecurityContext.mockSecurityContext(TEST_ZONE);
        MockAcsRequestContext.mockAcsRequestContext(TEST_ZONE);
    }

    @Test
    public void testPostAndGetHierarchicalResources() throws Exception {
        List<BaseResource> resources = createThreeLevelResourceHierarchy();

        // Append a list of resources
        MockMvcContext postContext =
            ResourcePrivilegeManagementControllerIT.TEST_UTILS.createWACWithCustomPOSTRequestBuilder(
                this.wac,
                TEST_ZONE.getSubdomain(),
                ResourcePrivilegeManagementControllerIT.RESOURCE_BASE_URL);

        postContext.getMockMvc().perform(postContext.getBuilder().contentType(MediaType.APPLICATION_JSON)
                                                    .content(ResourcePrivilegeManagementControllerIT.OBJECT_MAPPER
                                                                 .writeValueAsString(resources)))
                   .andExpect(status().isNoContent());

        // Get the child resource without query string specifier.
        MockMvcContext getContext0 =
            ResourcePrivilegeManagementControllerIT.TEST_UTILS.createWACWithCustomGETRequestBuilder(
                this.wac,
                TEST_ZONE.getSubdomain(),
                ResourcePrivilegeManagementControllerIT.RESOURCE_BASE_URL + "/"
                + URLEncoder.encode(EVIDENCE_SCULLYS_TESTIMONY_ID, "UTF-8"));

        getContext0.getMockMvc().perform(getContext0.getBuilder()).andExpect(status().isOk())
                   .andExpect(jsonPath("resourceIdentifier", is(EVIDENCE_SCULLYS_TESTIMONY_ID)))
                   .andExpect(jsonPath("attributes[0].name", is(TOP_SECRET_CLASSIFICATION.getName())))
                   .andExpect(jsonPath("attributes[0].value", is(TOP_SECRET_CLASSIFICATION.getValue())))
                   .andExpect(jsonPath("attributes[0].issuer", is(TOP_SECRET_CLASSIFICATION.getIssuer())))
                   .andExpect(jsonPath("attributes[1]").doesNotExist());

        // Get the child resource without inherited attributes.
        MockMvcContext getContext1 =
            ResourcePrivilegeManagementControllerIT.TEST_UTILS.createWACWithCustomGETRequestBuilder(
                this.wac,
                TEST_ZONE.getSubdomain(),
                ResourcePrivilegeManagementControllerIT.RESOURCE_BASE_URL + "/"
                + URLEncoder.encode(EVIDENCE_SCULLYS_TESTIMONY_ID, "UTF-8") + "?includeInheritedAttributes=false");

        getContext1.getMockMvc().perform(getContext1.getBuilder()).andExpect(status().isOk())
                   .andExpect(jsonPath("resourceIdentifier", is(EVIDENCE_SCULLYS_TESTIMONY_ID)))
                   .andExpect(jsonPath("attributes[0].name", is(TOP_SECRET_CLASSIFICATION.getName())))
                   .andExpect(jsonPath("attributes[0].value", is(TOP_SECRET_CLASSIFICATION.getValue())))
                   .andExpect(jsonPath("attributes[0].issuer", is(TOP_SECRET_CLASSIFICATION.getIssuer())))
                   .andExpect(jsonPath("attributes[1]").doesNotExist());

        // Get the child resource with inherited attributes.
        MockMvcContext getContext2 =
            ResourcePrivilegeManagementControllerIT.TEST_UTILS.createWACWithCustomGETRequestBuilder(
                this.wac,
                TEST_ZONE.getSubdomain(),
                ResourcePrivilegeManagementControllerIT.RESOURCE_BASE_URL + "/"
                + URLEncoder.encode(EVIDENCE_SCULLYS_TESTIMONY_ID, "UTF-8") + "?includeInheritedAttributes=true");

        getContext2.getMockMvc().perform(getContext2.getBuilder()).andExpect(status().isOk())
                       .andExpect(jsonPath("resourceIdentifier", is(EVIDENCE_SCULLYS_TESTIMONY_ID)))
                       .andExpect(jsonPath("attributes[0].name", is(TOP_SECRET_CLASSIFICATION.getName())))
                       .andExpect(jsonPath("attributes[0].value", is(TOP_SECRET_CLASSIFICATION.getValue())))
                       .andExpect(jsonPath("attributes[0].issuer", is(TOP_SECRET_CLASSIFICATION.getIssuer())))
                       .andExpect(jsonPath("attributes[1].name", is(SPECIAL_AGENTS_GROUP_ATTRIBUTE.getName())))
                       .andExpect(jsonPath("attributes[1].value", is(SPECIAL_AGENTS_GROUP_ATTRIBUTE.getValue())))
                       .andExpect(jsonPath("attributes[1].issuer", is(SPECIAL_AGENTS_GROUP_ATTRIBUTE.getIssuer())))
                       .andExpect(jsonPath("attributes[2].name", is(TYPE_MYTHARC.getName())))
                       .andExpect(jsonPath("attributes[2].value", is(TYPE_MYTHARC.getValue())))
                       .andExpect(jsonPath("attributes[2].issuer", is(TYPE_MYTHARC.getIssuer())))
                       .andExpect(jsonPath("attributes[3].name", is(SITE_BASEMENT.getName())))
                       .andExpect(jsonPath("attributes[3].value", is(SITE_BASEMENT.getValue())))
                       .andExpect(jsonPath("attributes[3].issuer", is(SITE_BASEMENT.getIssuer())));

        // Clean up after ourselves.
        deleteResource(EVIDENCE_SCULLYS_TESTIMONY_ID);
        deleteResource(ASCENSION_ID);
        deleteResource(BASEMENT_SITE_ID);
    }

    private void deleteResource(final String resourceIdentifier) throws Exception {
        String resourceToDeleteURI = ResourcePrivilegeManagementControllerIT.RESOURCE_BASE_URL + "/"
                                     + URLEncoder.encode(resourceIdentifier, "UTF-8");

        MockMvcContext deleteContext =
            ResourcePrivilegeManagementControllerIT.TEST_UTILS.createWACWithCustomDELETERequestBuilder(
                this.wac,
                TEST_ZONE.getSubdomain(), resourceToDeleteURI);

        deleteContext.getMockMvc().perform(deleteContext.getBuilder()).andExpect(status().isNoContent());
    }
}
