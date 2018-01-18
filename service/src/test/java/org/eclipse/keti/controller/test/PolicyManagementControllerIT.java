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
 *
 * SPDX-License-Identifier: Apache-2.0
 *******************************************************************************/

package org.eclipse.keti.controller.test;

import static org.eclipse.keti.acs.commons.web.AcsApiUriTemplates.POLICY_SET_URL;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.net.URI;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.web.context.WebApplicationContext;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import org.eclipse.keti.acs.commons.web.UriTemplateUtils;
import org.eclipse.keti.acs.model.PolicySet;
import org.eclipse.keti.acs.rest.PolicyEvaluationRequestV1;
import org.eclipse.keti.acs.rest.Zone;
import org.eclipse.keti.acs.testutils.MockAcsRequestContext;
import org.eclipse.keti.acs.testutils.MockMvcContext;
import org.eclipse.keti.acs.testutils.MockSecurityContext;
import org.eclipse.keti.acs.testutils.TestActiveProfilesResolver;
import org.eclipse.keti.acs.testutils.TestUtils;
import org.eclipse.keti.acs.utils.JsonUtils;
import org.eclipse.keti.acs.zone.management.ZoneService;

@WebAppConfiguration
@ContextConfiguration("classpath:controller-tests-context.xml")
@ActiveProfiles(resolver = TestActiveProfilesResolver.class)
@Test
public class PolicyManagementControllerIT extends AbstractTestNGSpringContextTests {

    public static final ConfigureEnvironment CONFIGURE_ENVIRONMENT = new ConfigureEnvironment();

    private final ObjectWriter objectWriter = new ObjectMapper().writer().withDefaultPrettyPrinter();

    @Autowired
    private ZoneService zoneService;

    @Autowired
    private WebApplicationContext wac;

    private PolicySet policySet;

    private final JsonUtils jsonUtils = new JsonUtils();
    private final TestUtils testUtils = new TestUtils();
    private Zone testZone;
    private Zone testZone2;

    private static final String VERSION = "v1/";

    @BeforeClass
    public void setup() throws Exception {
        this.testZone = new TestUtils().setupTestZone("PolicyMgmtControllerIT", zoneService);
        this.policySet = this.jsonUtils.deserializeFromFile("controller-test/complete-sample-policy-set.json",
                PolicySet.class);
        Assert.assertNotNull(this.policySet, "complete-sample-policy-set.json file not found or invalid");
    }

    public void testCreatePolicyInvalidMediaTypeResponseStatusCheck() throws Exception {

        String thisUri = VERSION + "/policy-set/" + this.policySet.getName();
        // create policy-set in first zone
        MockMvcContext putContext = this.testUtils
                .createWACWithCustomPUTRequestBuilder(this.wac, this.testZone.getSubdomain(), thisUri);
        putContext.getMockMvc().perform(putContext.getBuilder().contentType(MediaType.APPLICATION_XML_VALUE)
                .content("testString"))
                .andExpect(status().isUnsupportedMediaType());
    }

    public void policyZoneDoesNotExistException() throws Exception {
        // NOTE: To throw a ZoneDoesNotExistException, we must ensure that the AcsRequestContext in the
        //       SpringSecurityZoneResolver class returns a null ZoneEntity
        MockSecurityContext.mockSecurityContext(null);
        MockAcsRequestContext.mockAcsRequestContext();
        String thisUri = VERSION + "/policy-set/" + this.policySet.getName();
        // create policy-set in first zone
        MockMvcContext putContext = this.testUtils.createWACWithCustomPUTRequestBuilder(this.wac,
                this.testZone.getSubdomain(), thisUri);
        ResultActions resultActions = putContext.getMockMvc().perform(
                putContext.getBuilder().contentType(MediaType.APPLICATION_JSON)
                        .content(this.objectWriter.writeValueAsString(this.policySet)));
        resultActions.andExpect(status().isUnprocessableEntity());
        resultActions.andReturn().getResponse().getContentAsString().contains("zone 'null' does not exist");

        MockSecurityContext.mockSecurityContext(this.testZone);
        MockAcsRequestContext.mockAcsRequestContext();
    }

    public void testCreateSamePolicyDifferentZones() throws Exception {
        String thisUri = VERSION + "/policy-set/" + this.policySet.getName();
        // create policy-set in first zone
        MockMvcContext putContext = this.testUtils.createWACWithCustomPUTRequestBuilder(this.wac,
                this.testZone.getSubdomain(), thisUri);
        putContext.getMockMvc().perform(putContext.getBuilder().contentType(MediaType.APPLICATION_JSON)
                .content(this.objectWriter.writeValueAsString(this.policySet))).andExpect(status().isCreated());

        // create policy set in second zone
        this.testZone2 = new TestUtils().createTestZone("PolicyMgmtControllerIT2");
        this.zoneService.upsertZone(this.testZone2);
        MockSecurityContext.mockSecurityContext(this.testZone2);
        putContext = this.testUtils.createWACWithCustomPUTRequestBuilder(this.wac, this.testZone2.getSubdomain(),
                thisUri);
        putContext.getMockMvc().perform(putContext.getBuilder().contentType(MediaType.APPLICATION_JSON)
                .content(this.objectWriter.writeValueAsString(this.policySet))).andExpect(status().isCreated());
        // we expect both policy sets to be create in each zone
        // set security context back to first test zone
        MockSecurityContext.mockSecurityContext(this.testZone);
        MockAcsRequestContext.mockAcsRequestContext();
    }

    public void testCreatePolicy() throws Exception {
        String policySetName = upsertPolicySet(this.policySet);
        MockMvcContext mockMvcContext = this.testUtils.createWACWithCustomGETRequestBuilder(this.wac,
                this.testZone.getSubdomain(), VERSION + "policy-set/" + policySetName);
        mockMvcContext.getMockMvc().perform(mockMvcContext.getBuilder()).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("name").value(policySetName)).andExpect(jsonPath("policies").isArray())
                .andExpect(jsonPath("policies[1].target.resource.attributes[0].name").value("group"));
    }

    @Test
    public void testCreateMultiplePolicySets() throws Exception {
        //create first policy set
        String policySetName = upsertPolicySet(this.policySet);

        MockMvcContext mockMvcContext = this.testUtils.createWACWithCustomGETRequestBuilder(this.wac,
                this.testZone.getSubdomain(), VERSION + "policy-set/" + policySetName);

        //assert first policy set
        mockMvcContext.getMockMvc().perform(mockMvcContext.getBuilder()).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("name").value(policySetName)).andExpect(jsonPath("policies").isArray())
                .andExpect(jsonPath("policies[1].target.resource.attributes[0].name").value("group"));

        String policySet2Name = "";
        try {
            //create second policy set
            PolicySet policySet2 = this.jsonUtils.deserializeFromFile("controller-test/multiple-policy-set-test.json",
                    PolicySet.class);
            Assert.assertNotNull(policySet2, "multiple-policy-set-test.json file not found or invalid");
            policySet2Name = upsertPolicySet(policySet2);

            mockMvcContext = this.testUtils.createWACWithCustomGETRequestBuilder(this.wac,
                    this.testZone.getSubdomain(), VERSION + "policy-set/" + policySet2Name);

            //assert second policy set
            mockMvcContext.getMockMvc().perform(mockMvcContext.getBuilder()).andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
            .andExpect(jsonPath("name").value(policySet2Name));

            //assert that policy evaluation fails
            PolicyEvaluationRequestV1 evalRequest = new PolicyEvaluationRequestV1();
            evalRequest.setAction("GET");
            evalRequest.setSubjectIdentifier("test-user");
            evalRequest.setResourceIdentifier("/app/testuri");
            String evalRequestJson = this.objectWriter.writeValueAsString(evalRequest);
            mockMvcContext = this.testUtils.createWACWithCustomPOSTRequestBuilder(this.wac,
                    this.testZone.getSubdomain(), VERSION + "policy-evaluation");
            mockMvcContext.getMockMvc()
                    .perform(mockMvcContext.getBuilder().content(evalRequestJson)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("ErrorDetails.errorMessage")
                          .value("More than one policy set exists for this zone. Please provide an ordered list "
                                  + "of policy set names to consider for this evaluation and resubmit the request."));
        } finally {
            mockMvcContext = this.testUtils.createWACWithCustomDELETERequestBuilder(this.wac,
                    this.testZone.getSubdomain(), VERSION + "policy-set/" + policySet2Name);
            mockMvcContext.getMockMvc().perform(mockMvcContext.getBuilder()).andExpect(status().is2xxSuccessful());
        }
    }

    public void testGetNonExistentPolicySet() throws Exception {
        MockMvcContext mockMvcContext = this.testUtils.createWACWithCustomGETRequestBuilder(this.wac,
                this.testZone.getSubdomain(), VERSION + "/policy-set/non-existent");
        mockMvcContext.getMockMvc().perform(mockMvcContext.getBuilder().accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    public void testCreatePolicyWithNoPolicySet() throws Exception {
        MockMvcContext ctxt = this.testUtils.createWACWithCustomPUTRequestBuilder(this.wac,
                this.testZone.getSubdomain(), VERSION + "policy-set/policyNoBody");
        ctxt.getMockMvc().perform(ctxt.getBuilder().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(status().isBadRequest());
    }

    public void testDeletePolicySet() throws Exception {
        String policySetName = upsertPolicySet(this.policySet);
        MockMvcContext ctxt = this.testUtils.createWACWithCustomDELETERequestBuilder(this.wac,
                this.testZone.getSubdomain(), VERSION + "/policy-set/" + policySetName);
        ctxt.getMockMvc().perform(ctxt.getBuilder()).andExpect(status().isNoContent());

        // assert policy is gone
        MockMvcContext getContext = this.testUtils.createWACWithCustomGETRequestBuilder(this.wac,
                this.testZone.getSubdomain(), VERSION + "/policy-set/" + policySetName);
        getContext.getMockMvc().perform(getContext.getBuilder()).andExpect(status().isNotFound());
    }

    public void testGetAllPolicySets() throws Exception {
        String firstPolicySetName = upsertPolicySet(this.policySet);
        MockMvcContext mockMvcContext = this.testUtils.createWACWithCustomGETRequestBuilder(this.wac,
                this.testZone.getSubdomain(), VERSION + "policy-set");
        mockMvcContext.getMockMvc().perform(mockMvcContext.getBuilder().accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("$[0].name", is(firstPolicySetName)));

    }

    private String upsertPolicySet(final PolicySet myPolicySet) throws JsonProcessingException, Exception {

        String policySetContent = this.objectWriter.writeValueAsString(myPolicySet);
        String policySetName = myPolicySet.getName();
        MockMvcContext ctxt = this.testUtils.createWACWithCustomPUTRequestBuilder(this.wac,
                this.testZone.getSubdomain(), VERSION + "policy-set/" + myPolicySet.getName());
        URI policySetUri = UriTemplateUtils.expand(POLICY_SET_URL, "policySetId:" + policySetName);
        String policySetPath = policySetUri.getPath();

        ctxt.getMockMvc().perform(ctxt.getBuilder().content(policySetContent).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated()).andExpect(header().string("Location", policySetPath));

        return policySetName;
    }

    public void testCreatePolicyEmptyPolicySetName() throws Exception {
        PolicySet simplePolicyEmptyName = this.jsonUtils
                .deserializeFromFile("controller-test/simple-policy-set-empty-name.json", PolicySet.class);
        Assert.assertNotNull(simplePolicyEmptyName, "simple-policy-set-empty-name.json file not found or invalid");

        MockMvcContext ctxt = this.testUtils.createWACWithCustomPUTRequestBuilder(this.wac,
                this.testZone.getSubdomain(), VERSION + "policy-set/policyWithEmptyName");

        String policySetPayload = this.jsonUtils.serialize(simplePolicyEmptyName);
        ctxt.getMockMvc().perform(ctxt.getBuilder().contentType(MediaType.APPLICATION_JSON).content(policySetPayload))
                .andExpect(status().isUnprocessableEntity());
    }

    public void testCreatePolicyNoPolicySetName() throws Exception {
        PolicySet simplePolicyNoName = this.jsonUtils
                .deserializeFromFile("controller-test/simple-policy-set-no-name.json", PolicySet.class);
        Assert.assertNotNull(simplePolicyNoName, "simple-policy-set-no-name.json file not found or invalid");

        MockMvcContext ctxt = this.testUtils.createWACWithCustomPUTRequestBuilder(this.wac,
                this.testZone.getSubdomain(), VERSION + "policy-set/policyWithNoName");
        String policySetPayload = this.jsonUtils.serialize(simplePolicyNoName);
        ctxt.getMockMvc().perform(ctxt.getBuilder().contentType(MediaType.APPLICATION_JSON).content(policySetPayload))
                .andExpect(status().isUnprocessableEntity());

    }

    public void testCreatePolicyUriPolicySetIdMismatch() throws Exception {

        String policySetPayload = this.jsonUtils.serialize(this.policySet);
        MockMvcContext ctxt = this.testUtils.createWACWithCustomPUTRequestBuilder(this.wac,
                this.testZone.getSubdomain(), VERSION + "policy-set/mismatchWithPolicy");
        ctxt.getMockMvc().perform(ctxt.getBuilder().contentType(MediaType.APPLICATION_JSON).content(policySetPayload))
                .andExpect(status().isUnprocessableEntity());
    }
}
