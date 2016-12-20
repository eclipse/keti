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

package com.ge.predix.acs.service.policy.evaluation;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ge.predix.acs.PolicyContextResolver;
import com.ge.predix.acs.commons.policy.condition.groovy.GroovyConditionCache;
import com.ge.predix.acs.model.Attribute;
import com.ge.predix.acs.model.Effect;
import com.ge.predix.acs.model.Policy;
import com.ge.predix.acs.model.PolicySet;
import com.ge.predix.acs.policy.evaluation.cache.PolicyEvaluationCacheCircuitBreaker;
import com.ge.predix.acs.policy.evaluation.cache.PolicyEvaluationRequestCacheKey;
import com.ge.predix.acs.privilege.management.PrivilegeManagementService;
import com.ge.predix.acs.rest.BaseResource;
import com.ge.predix.acs.rest.BaseSubject;
import com.ge.predix.acs.rest.PolicyEvaluationRequestV1;
import com.ge.predix.acs.rest.PolicyEvaluationResult;
import com.ge.predix.acs.service.policy.admin.PolicyManagementService;
import com.ge.predix.acs.service.policy.matcher.MatchResult;
import com.ge.predix.acs.service.policy.matcher.PolicyMatchCandidate;
import com.ge.predix.acs.service.policy.matcher.PolicyMatcher;
import com.ge.predix.acs.service.policy.validation.PolicySetValidator;
import com.ge.predix.acs.service.policy.validation.PolicySetValidatorImpl;
import com.ge.predix.acs.utils.JsonUtils;
import com.ge.predix.acs.zone.management.dao.ZoneEntity;
import com.ge.predix.acs.zone.resolver.ZoneResolver;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.internal.util.reflection.Whitebox;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Unit tests for PolicyEvaluationService. Uses mocks, no external dependencies.
 *
 * @author 212406427
 */
@Test
@ContextConfiguration(classes = { GroovyConditionCache.class, PolicySetValidatorImpl.class })
public class PolicyEvaluationServiceTest extends AbstractTestNGSpringContextTests {
    private static final String ISSUER = "https://acs.attributes.int";
    private static final String SUBJECT_ATTRIB_NAME_ROLE = "role";
    private static final String SUBJECT_ATTRIB_VALUE_ANALYST = "analyst";
    private static final String RES_ATTRIB_ROLE_REQUIRED_VALUE = "administrator";
    private static final String SUBJECT_ATTRIB_VALUE_ADMIN = "administrator";
    private static final String RES_ATTRIB_ROLE_REQUIRED = "role_required";
    private static final String RES_ATTRIB_LOCATION = "location";
    private static final String RES_ATTRIB_LOCATION_VALUE = "sanramon";

    private final JsonUtils jsonUtils = new JsonUtils();

    @InjectMocks
    private PolicyEvaluationServiceImpl evaluationService;
    @Mock
    private PolicyManagementService policyService;
    @Mock
    private PrivilegeManagementService privilegeManagementService;
    @Mock
    private PolicyMatcher policyMatcher;
    @Mock
    private PolicyContextResolver policyScopeResolver;
    @Mock
    private ZoneResolver zoneResolver;
    @Mock
    private PolicyEvaluationCacheCircuitBreaker cache;
    @Autowired
    private PolicySetValidator policySetValidator;

    private static final Set<Attribute> EMPTY_ATTRS = Collections.emptySet();

    @BeforeClass
    public void setupClass() {
        ((PolicySetValidatorImpl) this.policySetValidator)
                .setValidAcsPolicyHttpActions("GET, POST, PUT, DELETE, PATCH");
        ((PolicySetValidatorImpl) this.policySetValidator).init();
    }

    @BeforeMethod
    public void setupMethod() throws Exception {
        this.evaluationService = new PolicyEvaluationServiceImpl();
        Whitebox.setInternalState(this.evaluationService, "policySetValidator", this.policySetValidator);
        MockitoAnnotations.initMocks(this);
        when(this.zoneResolver.getZoneEntityOrFail()).thenReturn(new ZoneEntity(0L, "testzone"));
        when(this.cache.get(any(PolicyEvaluationRequestCacheKey.class))).thenReturn(null);
    }

    @Test(dataProvider = "policyRequestParameterProvider", expectedExceptions = IllegalArgumentException.class)
    public void testEvaluateWithNullParameters(final String resource, final String subject, final String action) {
        this.evaluationService.evalPolicy(createRequest(resource, subject, action));
    }

    public void testEvaluateWithNoPolicySet() {
        PolicyEvaluationResult result = this.evaluationService
                .evalPolicy(createRequest("resource1", "subject1", "GET"));
        Assert.assertEquals(result.getEffect(), Effect.NOT_APPLICABLE);
        Assert.assertEquals(result.getResourceAttributes().size(), 0);
        Assert.assertEquals(result.getSubjectAttributes().size(), 0);
    }

    public void testEvaluateWithOnePolicySetNoPolicies() {
        List<PolicySet> policySets = new ArrayList<>();
        policySets.add(new PolicySet());
        when(this.policyService.getAllPolicySets()).thenReturn(policySets);
        List<MatchedPolicy> matchedPolicies = Collections.emptyList();
        when(this.policyMatcher.matchForResult(any(PolicyMatchCandidate.class), anyListOf(Policy.class)))
                .thenReturn(new MatchResult(matchedPolicies, new HashSet<String>()));
        PolicyEvaluationResult evalPolicy = this.evaluationService
                .evalPolicy(createRequest("resource1", "subject1", "GET"));
        Assert.assertEquals(evalPolicy.getEffect(), Effect.NOT_APPLICABLE);
    }

    @Test(dataProvider = "policyDataProvider")
    public void testEvaluateWithPolicy(final File inputPolicy, final Effect effect)
            throws JsonParseException, JsonMappingException, IOException {
        initializePolicyMock(inputPolicy);
        PolicyEvaluationResult evalPolicy = this.evaluationService
                .evalPolicy(createRequest("resource1", "subject1", "GET"));
        Assert.assertEquals(evalPolicy.getEffect(), effect);
    }

    @Test(dataProvider = "policyDataProviderForTestWithAttributes")
    public void testEvaluateWithPolicyAndSubjectResourceAttributes(final String acsSubjectAttributeValue,
            final File inputPolicy, final Effect effect, final Set<Attribute> subjectAttributes)
            throws JsonParseException, JsonMappingException, IOException {

        Set<Attribute> resourceAttributes = new HashSet<>();
        Attribute roleAttribute = new Attribute(ISSUER, RES_ATTRIB_ROLE_REQUIRED, RES_ATTRIB_ROLE_REQUIRED_VALUE);
        resourceAttributes.add(roleAttribute);
        Attribute locationAttribute = new Attribute(ISSUER, RES_ATTRIB_LOCATION, RES_ATTRIB_LOCATION_VALUE);
        resourceAttributes.add(locationAttribute);

        Set<Attribute> mergedSubjectAttributes = new HashSet<>(subjectAttributes);
        mergedSubjectAttributes.addAll(getSubjectAttributes(acsSubjectAttributeValue));
        initializePolicyMock(inputPolicy, resourceAttributes, mergedSubjectAttributes);
        when(this.privilegeManagementService.getByResourceIdentifier(anyString())).thenReturn(this.getResource());
        when(this.privilegeManagementService.getBySubjectIdentifier(anyString()))
                .thenReturn(this.getSubject(acsSubjectAttributeValue));
        PolicyEvaluationResult evalPolicyResponse = this.evaluationService
                .evalPolicy(createRequest("resource1", "subject1", "GET"));
        Assert.assertEquals(evalPolicyResponse.getEffect(), effect);
        Assert.assertTrue(evalPolicyResponse.getResourceAttributes().contains(roleAttribute));
        Assert.assertTrue(evalPolicyResponse.getResourceAttributes().contains(locationAttribute));
        if (acsSubjectAttributeValue != null) {
            Assert.assertTrue(evalPolicyResponse.getSubjectAttributes()
                    .contains(new Attribute(ISSUER, SUBJECT_ATTRIB_NAME_ROLE, acsSubjectAttributeValue)));
        }
        
        for (Attribute attribute : subjectAttributes) {
            Assert.assertTrue(evalPolicyResponse.getSubjectAttributes().contains(attribute));
        }
        
    }

    @Test(
            dataProvider = "filterPolicySetsInvalidRequestDataProvider",
            expectedExceptions = IllegalArgumentException.class)
    public void testFilterPolicySetsByPriorityForInvalidRequest(final List<PolicySet> allPolicySets,
            final LinkedHashSet<String> policySetsPriority) {
        this.evaluationService.filterPolicySetsByPriority("subject1", "resource1", allPolicySets, policySetsPriority);
    }

    @Test(dataProvider = "filterPolicySetsDataProvider")
    public void testFilterPolicySetsByPriority(final List<PolicySet> allPolicySets,
            final LinkedHashSet<String> policySetsPriority, final LinkedHashSet<PolicySet> expectedFilteredPolicySets) {
        LinkedHashSet<PolicySet> actualFilteredPolicySets = this.evaluationService
                .filterPolicySetsByPriority("subject1", "resource1", allPolicySets, policySetsPriority);
        Assert.assertEquals(actualFilteredPolicySets, expectedFilteredPolicySets);
    }

    @Test(dataProvider = "multiplePolicySetsRequestDataProvider")
    public void testEvaluateWithMultiplePolicySets(final List<PolicySet> allPolicySets,
            final LinkedHashSet<String> policySetsPriority, final Effect effect) {
        when(this.policyService.getAllPolicySets()).thenReturn(allPolicySets);
        when(this.policyMatcher.matchForResult(any(PolicyMatchCandidate.class), anyListOf(Policy.class)))
                .thenAnswer(new Answer<MatchResult>() {
                    public MatchResult answer(final InvocationOnMock invocation) {
                        Object[] args = invocation.getArguments();
                        @SuppressWarnings("unchecked")
                        List<Policy> policyList = (List<Policy>) args[1];
                        List<MatchedPolicy> matchedPolicies = new ArrayList<>();
                        // Mocking the policyMatcher to return all policies as matched.
                        policyList.forEach(
                                policy -> matchedPolicies.add(new MatchedPolicy(policy, EMPTY_ATTRS, EMPTY_ATTRS)));
                        return new MatchResult(matchedPolicies, new HashSet<String>());
                    }
                });

        PolicyEvaluationResult result = this.evaluationService
                .evalPolicy(createRequest("anyresource", "anysubject", "GET", policySetsPriority));
        Assert.assertEquals(result.getEffect(), effect);
    }

    @Test
    public void testPolicyEvaluationExceptionHandling() {
        List<PolicySet> twoPolicySets = createNotApplicableAndDenyPolicySets();
        when(this.policyService.getAllPolicySets()).thenReturn(twoPolicySets);
        when(this.policyMatcher.matchForResult(any(PolicyMatchCandidate.class), anyListOf(Policy.class)))
                .thenThrow(new RuntimeException("This policy matcher is designed to throw an exception."));

        PolicyEvaluationResult result = this.evaluationService.evalPolicy(createRequest("anyresource", "anysubject",
                "GET", Stream.of(twoPolicySets.get(0).getName(), twoPolicySets.get(1).getName())
                        .collect(Collectors.toCollection(LinkedHashSet::new))));

        Assert.assertEquals(result.getEffect(), Effect.INDETERMINATE);
    }

    /**
     * @param inputPolicy
     * @throws IOException
     * @throws JsonParseException
     * @throws JsonMappingException
     */
    private void initializePolicyMock(final File inputPolicy)
            throws IOException, JsonParseException, JsonMappingException {
        initializePolicyMock(inputPolicy, Collections.emptySet(), Collections.emptySet());
    }

    /**
     * @param inputPolicy
     * @throws IOException
     * @throws JsonParseException
     * @throws JsonMappingException
     */
    private void initializePolicyMock(final File inputPolicy, final Set<Attribute> resourceAttributes,
            final Set<Attribute> subjectAttributes) throws IOException, JsonParseException, JsonMappingException {
        PolicySet policySet = new ObjectMapper().readValue(inputPolicy, PolicySet.class);
        when(this.policyService.getAllPolicySets()).thenReturn(Arrays.asList(new PolicySet[] { policySet }));
        List<MatchedPolicy> matchedPolicies = new ArrayList<>();
        for (Policy policy : policySet.getPolicies()) {
            matchedPolicies.add(new MatchedPolicy(policy, resourceAttributes, subjectAttributes));
        }
        when(this.policyMatcher.match(any(PolicyMatchCandidate.class), anyListOf(Policy.class)))
                .thenReturn(matchedPolicies);
        when(this.policyMatcher.matchForResult(any(PolicyMatchCandidate.class), anyListOf(Policy.class)))
                .thenReturn(new MatchResult(matchedPolicies, new HashSet<String>()));
    }

    private BaseSubject getSubject(final String roleValue) {
        BaseSubject subject = new BaseSubject("subject1");
        Set<Attribute> attributes = getSubjectAttributes(roleValue);
        subject.setAttributes(attributes);
        return subject;
    }

    /**
     * @param roleValue
     * @return
     */
    private Set<Attribute> getSubjectAttributes(final String roleValue) {
        Set<Attribute> attributes = new HashSet<>();
        if (roleValue != null) {
            attributes.add(new Attribute(ISSUER, SUBJECT_ATTRIB_NAME_ROLE, roleValue));
        }
        return attributes;
    }

    private BaseResource getResource() {
        BaseResource resource = new BaseResource("name");
        Set<Attribute> resourceAttributes = new HashSet<>();
        resourceAttributes.add(new Attribute(ISSUER, RES_ATTRIB_ROLE_REQUIRED, RES_ATTRIB_ROLE_REQUIRED_VALUE));
        resourceAttributes.add(new Attribute(ISSUER, RES_ATTRIB_LOCATION, RES_ATTRIB_LOCATION_VALUE));
        resource.setAttributes(resourceAttributes);
        return resource;
    }

    @DataProvider(name = "policyDataProviderForTestWithAttributes")
    private Object[][] policyDataProviderForTestWithAttributes() {
        return new Object[][] {
                { SUBJECT_ATTRIB_VALUE_ANALYST,
                        new File("src/test/resources/policy-set-with-one-policy-one-condition-using-attributes.json"),
                        Effect.NOT_APPLICABLE, EMPTY_ATTRS },
                { SUBJECT_ATTRIB_VALUE_ANALYST,
                        new File("src/test/resources/policy-set-with-one-policy-one-condition-using-attributes.json"),
                        Effect.PERMIT, getSubjectAttributes(SUBJECT_ATTRIB_VALUE_ADMIN) },
                { null, new File("src/test/resources/policy-set-with-one-policy-one-condition-using-attributes.json"),
                        Effect.NOT_APPLICABLE, getSubjectAttributes(SUBJECT_ATTRIB_VALUE_ADMIN) },
                { null, new File(
                        "src/test/resources/" + "policy-set-with-one-policy-one-condition-using-res-attributes.json"),
                        Effect.PERMIT, getSubjectAttributes(SUBJECT_ATTRIB_VALUE_ADMIN) } };
    }

    @DataProvider(name = "policyDataProvider")
    private Object[][] policyDataProvider() {
        return new Object[][] {
                { new File("src/test/resources/policy-set-with-one-policy-nocondition.json"), Effect.DENY },
                { new File("src/test/resources/policy-set-with-one-policy-one-condition.json"), Effect.PERMIT },
                { new File("src/test/resources/policy-set-with-multiple-policies-first-match.json"), Effect.DENY },
                { new File("src/test/resources/policy-set-with-multiple-policies-permit-with-condition.json"),
                        Effect.PERMIT },
                { new File("src/test/resources/policy-set-with-multiple-policies-deny-with-condition.json"),
                        Effect.DENY },
                { new File("src/test/resources/policy-set-with-multiple-policies-na-with-condition.json"),
                        Effect.NOT_APPLICABLE },
                { new File("src/test/resources/policy-set-with-multiple-policies-default-deny-with-condition.json"),
                        Effect.DENY },
                { new File("src/test/resources/policy-set-with-one-policy-one-condition-indeterminate.json"),
                        Effect.INDETERMINATE },
                { new File("src/test/resources/policy-set-with-multiple-policies-deny-missing-optional-tags.json"),
                        Effect.DENY } };
    }

    @DataProvider(name = "policyRequestParameterProvider")
    private Object[][] policyRequestParameterProvider() {
        return new Object[][] { { null, "s1", "a1" }, { "r1", null, "a1" }, { "r1", "s1", null }, };
    }

    @DataProvider(name = "filterPolicySetsInvalidRequestDataProvider")
    private Object[][] filterPolicySetsInvalidRequestDataProvider()
            throws JsonParseException, JsonMappingException, IOException {
        List<PolicySet> onePolicySet = createDenyPolicySet();
        List<PolicySet> twoPolicySets = createNotApplicableAndDenyPolicySets();
        return new Object[][] { filterOnePolicySetByNonexistentPolicySet(onePolicySet),
                filterTwoPolisySetsByEmptyList(twoPolicySets),
                filterTwoPolicySetsByByNonexistentPolicySet(twoPolicySets) };
    }

    private Object[] filterOnePolicySetByNonexistentPolicySet(final List<PolicySet> onePolicySet) {
        return new Object[] { onePolicySet,
                Stream.of("nonexistent-policy-set").collect(Collectors.toCollection(LinkedHashSet::new)) };
    }

    private Object[] filterTwoPolisySetsByEmptyList(final List<PolicySet> twoPolicySets) {
        return new Object[] { twoPolicySets, PolicyEvaluationRequestV1.EMPTY_POLICY_EVALUATION_ORDER };
    }

    private Object[] filterTwoPolicySetsByByNonexistentPolicySet(final List<PolicySet> twoPolicySets) {
        return new Object[] { twoPolicySets, Stream.of(twoPolicySets.get(0).getName(), "noexistent-policy-set")
                .collect(Collectors.toCollection(LinkedHashSet::new)) };
    }

    @DataProvider(name = "filterPolicySetsDataProvider")
    private Object[][] filterPolicySetsDataProvider() {
        List<PolicySet> denyPolicySet = createDenyPolicySet();
        List<PolicySet> notApplicableAndDenyPolicySets = createNotApplicableAndDenyPolicySets();
        return new Object[][] { filterOnePolicySetByEmptyEvaluationOrder(denyPolicySet),
                                filterOnePolicySetByItself(denyPolicySet),
                                filterTwoPolicySetsByFirstSet(notApplicableAndDenyPolicySets),
                                filterTwoPolicySetsBySecondPolicySet(notApplicableAndDenyPolicySets),
                                filterTwoPolicySetsByItself(notApplicableAndDenyPolicySets) };
    }

    private Object[] filterTwoPolicySetsByItself(final List<PolicySet> twoPolicySets) {
        return new Object[] { twoPolicySets,
                Stream.of(twoPolicySets.get(0).getName(), twoPolicySets.get(1).getName())
                        .collect(Collectors.toCollection(LinkedHashSet::new)),
                twoPolicySets.stream().collect(Collectors.toCollection(LinkedHashSet::new)) };
    }

    private Object[] filterTwoPolicySetsBySecondPolicySet(final List<PolicySet> twoPolicySets) {
        return new Object[] { twoPolicySets,
                Stream.of(twoPolicySets.get(1).getName()).collect(Collectors.toCollection(LinkedHashSet::new)),
                Stream.of(twoPolicySets.get(1)).collect(Collectors.toCollection(LinkedHashSet::new)) };
    }

    private Object[] filterTwoPolicySetsByFirstSet(final List<PolicySet> twoPolicySets) {
        return new Object[] { twoPolicySets,
                Stream.of(twoPolicySets.get(0).getName()).collect(Collectors.toCollection(LinkedHashSet::new)),
                Stream.of(twoPolicySets.get(0)).collect(Collectors.toCollection(LinkedHashSet::new)) };
    }

    private Object[] filterOnePolicySetByItself(final List<PolicySet> onePolicySet) {
        return new Object[] { onePolicySet,
                Stream.of(onePolicySet.get(0).getName()).collect(Collectors.toCollection(LinkedHashSet::new)),
                onePolicySet.stream().collect(Collectors.toCollection(LinkedHashSet::new)) };
    }

    private Object[] filterOnePolicySetByEmptyEvaluationOrder(final List<PolicySet> onePolicySet) {
        return new Object[] { onePolicySet, PolicyEvaluationRequestV1.EMPTY_POLICY_EVALUATION_ORDER,
                onePolicySet.stream().collect(Collectors.toCollection(LinkedHashSet::new)) };
    }

    @DataProvider(name = "multiplePolicySetsRequestDataProvider")
    private Object[][] multiplePolicySetsRequestDataProvider()
            throws JsonParseException, JsonMappingException, IOException {
        List<PolicySet> denyPolicySet = createDenyPolicySet();
        List<PolicySet> notApplicableAndDenyPolicySets = createNotApplicableAndDenyPolicySets();
        return new Object[][] { requestEvaluationWithEmptyPolicySetsListAndEmptyPriorityList(),
                requestEvaluationWithOnePolicySetAndEmptyPriorityList(denyPolicySet),
                requestEvaluationWithFirstOfOnePolicySets(denyPolicySet),
                requestEvaluationWithFirstOfTwoPolicySets(notApplicableAndDenyPolicySets),
                requestEvaluationWithSecondOfTwoPolicySets(notApplicableAndDenyPolicySets),
                requestEvaluationWithAllOfTwoPolicySets(notApplicableAndDenyPolicySets) };
    }

    private Object[] requestEvaluationWithAllOfTwoPolicySets(final List<PolicySet> twoPolicySets) {
        return new Object[] { twoPolicySets, Stream.of(twoPolicySets.get(0).getName(), twoPolicySets.get(1).getName())
                .collect(Collectors.toCollection(LinkedHashSet::new)), Effect.DENY };
    }

    private Object[] requestEvaluationWithSecondOfTwoPolicySets(final List<PolicySet> twoPolicySets) {
        return new Object[] { twoPolicySets,
                Stream.of(twoPolicySets.get(1).getName()).collect(Collectors.toCollection(LinkedHashSet::new)),
                Effect.DENY };
    }

    private Object[] requestEvaluationWithFirstOfTwoPolicySets(final List<PolicySet> twoPolicySets) {
        return new Object[] { twoPolicySets,
                Stream.of(twoPolicySets.get(0).getName()).collect(Collectors.toCollection(LinkedHashSet::new)),
                Effect.NOT_APPLICABLE };
    }

    private Object[] requestEvaluationWithFirstOfOnePolicySets(final List<PolicySet> onePolicySet) {
        return new Object[] { onePolicySet,
                Stream.of(onePolicySet.get(0).getName()).collect(Collectors.toCollection(LinkedHashSet::new)),
                Effect.DENY };
    }

    private Object[] requestEvaluationWithOnePolicySetAndEmptyPriorityList(final List<PolicySet> onePolicySet) {
        return new Object[] { onePolicySet, PolicyEvaluationRequestV1.EMPTY_POLICY_EVALUATION_ORDER, Effect.DENY };
    }

    private Object[] requestEvaluationWithEmptyPolicySetsListAndEmptyPriorityList() {
        return new Object[] { Collections.emptyList(), PolicyEvaluationRequestV1.EMPTY_POLICY_EVALUATION_ORDER,
                Effect.NOT_APPLICABLE };
    }

    private List<PolicySet> createDenyPolicySet() {
        List<PolicySet> policySets = new ArrayList<PolicySet>();
        policySets.add(this.jsonUtils.deserializeFromFile("policies/testPolicyEvalDeny.json", PolicySet.class));
        Assert.assertNotNull(policySets, "Policy set file is not found or invalid");
        return policySets;
    }

    private List<PolicySet> createNotApplicableAndDenyPolicySets() {
        List<PolicySet> policySets = new ArrayList<PolicySet>();
        policySets
                .add(this.jsonUtils.deserializeFromFile("policies/testPolicyEvalNotApplicable.json", PolicySet.class));
        policySets.add(this.jsonUtils.deserializeFromFile("policies/testPolicyEvalDeny.json", PolicySet.class));
        Assert.assertNotNull(policySets, "Policy set files are not found or invalid");
        Assert.assertTrue(policySets.size() == 2, "One or more policy set files are not found or invalid");
        return policySets;
    }

    private PolicyEvaluationRequestV1 createRequest(final String resource, final String subject, final String action) {
        PolicyEvaluationRequestV1 request = new PolicyEvaluationRequestV1();
        request.setAction(action);
        request.setSubjectIdentifier(subject);
        request.setResourceIdentifier(resource);
        return request;
    }

    private PolicyEvaluationRequestV1 createRequest(final String resource, final String subject, final String action,
                                                    final LinkedHashSet<String> policySetsEvaluationOrder) {
        PolicyEvaluationRequestV1 request = new PolicyEvaluationRequestV1();
        request.setAction(action);
        request.setSubjectIdentifier(subject);
        request.setResourceIdentifier(resource);
        request.setPolicySetsEvaluationOrder(policySetsEvaluationOrder);
        return request;
    }
}
