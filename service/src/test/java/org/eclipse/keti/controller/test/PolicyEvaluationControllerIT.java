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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.web.context.WebApplicationContext;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.keti.acs.model.Effect;
import org.eclipse.keti.acs.model.PolicySet;
import org.eclipse.keti.acs.privilege.management.PrivilegeManagementService;
import org.eclipse.keti.acs.rest.BaseResource;
import org.eclipse.keti.acs.rest.BaseSubject;
import org.eclipse.keti.acs.rest.PolicyEvaluationRequestV1;
import org.eclipse.keti.acs.rest.PolicyEvaluationResult;
import org.eclipse.keti.acs.rest.Zone;
import org.eclipse.keti.acs.service.policy.admin.PolicyManagementService;
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
public class PolicyEvaluationControllerIT extends AbstractTestNGSpringContextTests {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String POLICY_EVAL_URL = "v1/policy-evaluation";
    private static final LinkedHashSet<String> EMPTY_POLICY_EVALUATION_ORDER = new LinkedHashSet<>();

    private final JsonUtils jsonUtils = new JsonUtils();
    private final TestUtils testUtils = new TestUtils();
    private Zone testZone;
    private BaseSubject testSubject;
    private BaseResource testResource;
    private List<PolicySet> denyPolicySet;
    private List<PolicySet> notApplicableAndDenyPolicySets;

    @Autowired
    private WebApplicationContext wac;

    @Autowired
    private ZoneService zoneService;

    @Autowired
    private PrivilegeManagementService privilegeManagementService;

    @Autowired
    private PolicyManagementService policyManagementService;

    @BeforeClass
    public void setup() {

        this.testZone =  new TestUtils().setupTestZone("PolicyEvaluationControllerITZone", zoneService);
        this.testSubject = new BaseSubject("testSubject");
        this.testResource = new BaseResource("testResource");
        Assert.assertTrue(this.privilegeManagementService.upsertResource(this.testResource));
        Assert.assertTrue(this.privilegeManagementService.upsertSubject(this.testSubject));

        this.denyPolicySet = createDenyPolicySet();
        this.notApplicableAndDenyPolicySets = createNotApplicableAndDenyPolicySets();
    }

    @AfterMethod
    public void testCleanup() {
        List<PolicySet> policySets = this.policyManagementService.getAllPolicySets();
        policySets.forEach(policySet -> this.policyManagementService.deletePolicySet(policySet.getName()));
    }

    @Test
    public void testPolicyZoneDoesNotExistException() throws Exception {
        MockSecurityContext.mockSecurityContext(null);
        MockAcsRequestContext.mockAcsRequestContext();
        PolicyEvaluationRequestV1 policyEvalRequest = createPolicyEvalRequest(this.testResource.getResourceIdentifier(),
                this.testSubject.getSubjectIdentifier(), EMPTY_POLICY_EVALUATION_ORDER);
        MockMvcContext postPolicyEvalContext = this.testUtils
                .createWACWithCustomPOSTRequestBuilder(this.wac, this.testZone.getSubdomain(), POLICY_EVAL_URL);
        ResultActions resultActions = postPolicyEvalContext.getMockMvc().perform(
                postPolicyEvalContext.getBuilder().contentType(MediaType.APPLICATION_JSON)
                        .content(OBJECT_MAPPER.writeValueAsString(policyEvalRequest)));
        resultActions.andReturn().getResponse()
                .getContentAsString().contentEquals("Zone not found");
        resultActions.andExpect(status().isBadRequest());

        MockSecurityContext.mockSecurityContext(this.testZone);
        MockAcsRequestContext.mockAcsRequestContext();
    }

    @Test
    public void testPolicyInvalidMediaTypeResponseStatusCheck()
            throws Exception {

        MockMvcContext postPolicyEvalContext = this.testUtils
                .createWACWithCustomPOSTRequestBuilder(this.wac, this.testZone.getSubdomain(), POLICY_EVAL_URL);
        postPolicyEvalContext.getMockMvc().perform(
                postPolicyEvalContext.getBuilder().contentType(MediaType.IMAGE_GIF_VALUE)
                        .content("testString"))
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test(dataProvider = "policyEvalDataProvider")
    public void testPolicyEvaluation(final PolicyEvaluationRequestV1 policyEvalRequest,
            final List<PolicySet> policySets, final Effect expectedEffect) throws Exception {

        if (policySets != null) {
            upsertMultiplePolicySets(policySets);
        }

        MockMvcContext postPolicyEvalContext = this.testUtils
                .createWACWithCustomPOSTRequestBuilder(this.wac, this.testZone.getSubdomain(), POLICY_EVAL_URL);
        MvcResult mvcResult = postPolicyEvalContext.getMockMvc().perform(
                postPolicyEvalContext.getBuilder().contentType(MediaType.APPLICATION_JSON)
                        .content(OBJECT_MAPPER.writeValueAsString(policyEvalRequest))).andExpect(status().isOk())
                .andReturn();
        PolicyEvaluationResult policyEvalResult = OBJECT_MAPPER
                .readValue(mvcResult.getResponse().getContentAsByteArray(), PolicyEvaluationResult.class);

        assertThat(policyEvalResult.getEffect(), equalTo(expectedEffect));
    }

    @Test(dataProvider = "policyEvalBadRequestDataProvider")
    public void testPolicyEvaluationBadRequest(final PolicyEvaluationRequestV1 policyEvalRequest,
            final List<PolicySet> policySets) throws Exception {

        upsertMultiplePolicySets(policySets);

        MockMvcContext postPolicyEvalContext = this.testUtils
                .createWACWithCustomPOSTRequestBuilder(this.wac, this.testZone.getSubdomain(), POLICY_EVAL_URL);
        postPolicyEvalContext.getMockMvc().perform(
                postPolicyEvalContext.getBuilder().contentType(MediaType.APPLICATION_JSON)
                        .content(OBJECT_MAPPER.writeValueAsString(policyEvalRequest)))
                .andExpect(status().isBadRequest());
    }

    @DataProvider(name = "policyEvalDataProvider")
    private Object[][] policyEvalDataProvider() {
        return new Object[][] { requestEvaluationWithEmptyPolicySet(),
                requestEvaluationWithOnePolicySetAndEmptyPriorityList(),
                requestEvaluationWithOnePolicySetAndPriorityList(), requestEvaluationWithAllOfTwoPolicySets(),
                requestEvaluationWithFirstOfTwoPolicySets(), requestEvaluationWithSecondOfTwoPolicySets() };
    }

    private Object[] requestEvaluationWithEmptyPolicySet() {
        return new Object[] { createPolicyEvalRequest(this.testResource.getResourceIdentifier(),
                this.testSubject.getSubjectIdentifier(), EMPTY_POLICY_EVALUATION_ORDER), Collections.emptyList(),
                Effect.NOT_APPLICABLE };
    }

    private Object[] requestEvaluationWithSecondOfTwoPolicySets() {
        return new Object[] { createPolicyEvalRequest(this.testResource.getResourceIdentifier(),
                this.testSubject.getSubjectIdentifier(), Stream.of(this.notApplicableAndDenyPolicySets.get(1).getName())
                        .collect(Collectors.toCollection(LinkedHashSet::new))), this.notApplicableAndDenyPolicySets,
                Effect.DENY };
    }

    private Object[] requestEvaluationWithFirstOfTwoPolicySets() {
        return new Object[] { createPolicyEvalRequest(this.testResource.getResourceIdentifier(),
                this.testSubject.getSubjectIdentifier(), Stream.of(this.notApplicableAndDenyPolicySets.get(0).getName())
                        .collect(Collectors.toCollection(LinkedHashSet::new))), this.notApplicableAndDenyPolicySets,
                Effect.NOT_APPLICABLE };
    }

    private Object[] requestEvaluationWithOnePolicySetAndPriorityList() {
        return new Object[] { createPolicyEvalRequest(this.testResource.getResourceIdentifier(),
                this.testSubject.getSubjectIdentifier(),
                Stream.of(this.denyPolicySet.get(0).getName()).collect(Collectors.toCollection(LinkedHashSet::new))),
                this.denyPolicySet, Effect.DENY };
    }

    private Object[] requestEvaluationWithOnePolicySetAndEmptyPriorityList() {
        return new Object[] { createPolicyEvalRequest(this.testResource.getResourceIdentifier(),
                this.testSubject.getSubjectIdentifier(), EMPTY_POLICY_EVALUATION_ORDER),
                this.denyPolicySet, Effect.DENY };
    }

    private Object[] requestEvaluationWithAllOfTwoPolicySets() {
        return new Object[] { createPolicyEvalRequest(this.testResource.getResourceIdentifier(),
                this.testSubject.getSubjectIdentifier(), Stream.of(this.notApplicableAndDenyPolicySets.get(0).getName(),
                        this.notApplicableAndDenyPolicySets.get(1).getName())
                        .collect(Collectors.toCollection(LinkedHashSet::new))), this.notApplicableAndDenyPolicySets,
                Effect.DENY };

    }

    @DataProvider(name = "policyEvalBadRequestDataProvider")
    private Object[][] policyEvalBadRequestDataProvider() {
        return new Object[][] { requestEvaluationWithNonExistentPolicySet(),
                requestEvaluationWithTwoPolicySetsAndNoPriorityList(),
                requestEvaluationWithExistentAndNonExistentPolicySets() };
    }

    private Object[] requestEvaluationWithExistentAndNonExistentPolicySets() {
        return new Object[] { createPolicyEvalRequest(this.testResource.getResourceIdentifier(),
                this.testSubject.getSubjectIdentifier(),
                Stream.of(this.notApplicableAndDenyPolicySets.get(0).getName(), "noexistent-policy-set")
                        .collect(Collectors.toCollection(LinkedHashSet::new))), this.notApplicableAndDenyPolicySets };
    }

    private Object[] requestEvaluationWithTwoPolicySetsAndNoPriorityList() {
        return new Object[] { createPolicyEvalRequest(this.testResource.getResourceIdentifier(),
                this.testSubject.getSubjectIdentifier(), EMPTY_POLICY_EVALUATION_ORDER),
                this.notApplicableAndDenyPolicySets };
    }

    private Object[] requestEvaluationWithNonExistentPolicySet() {
        return new Object[] { createPolicyEvalRequest(this.testResource.getResourceIdentifier(),
                this.testSubject.getSubjectIdentifier(),
                Stream.of("nonexistent-policy-set").collect(Collectors.toCollection(LinkedHashSet::new))),
                this.denyPolicySet };
    }

    private PolicyEvaluationRequestV1 createPolicyEvalRequest(final String resourceIdentifier,
            final String subjectIdentifier, final LinkedHashSet<String> policySetsPriority) {
        PolicyEvaluationRequestV1 policyEvalRequest = new PolicyEvaluationRequestV1();
        policyEvalRequest.setAction("GET");
        policyEvalRequest.setResourceIdentifier(resourceIdentifier);
        policyEvalRequest.setSubjectIdentifier(subjectIdentifier);
        policyEvalRequest.setPolicySetsEvaluationOrder(policySetsPriority);
        return policyEvalRequest;
    }

    private List<PolicySet> createDenyPolicySet() {
        List<PolicySet> policySets = new ArrayList<>();
        policySets.add(this.jsonUtils.deserializeFromFile("policies/testPolicyEvalDeny.json", PolicySet.class));
        Assert.assertNotNull(policySets, "Policy set file is not found or invalid");
        return policySets;
    }

    private List<PolicySet> createNotApplicableAndDenyPolicySets() {
        List<PolicySet> policySets = new ArrayList<>();
        policySets
                .add(this.jsonUtils.deserializeFromFile("policies/testPolicyEvalNotApplicable.json", PolicySet.class));
        policySets.add(this.jsonUtils.deserializeFromFile("policies/testPolicyEvalDeny.json", PolicySet.class));
        Assert.assertNotNull(policySets, "Policy set files are not found or invalid");
        Assert.assertTrue(policySets.size() == 2, "One or more policy set files are not found or invalid");
        return policySets;
    }

    private void upsertPolicySet(final PolicySet policySet) {
        this.policyManagementService.upsertPolicySet(policySet);
        Assert.assertNotNull(this.policyManagementService.getPolicySet(policySet.getName()));
    }

    private void upsertMultiplePolicySets(final List<PolicySet> policySets) {
        policySets.forEach(this::upsertPolicySet);
    }
}
