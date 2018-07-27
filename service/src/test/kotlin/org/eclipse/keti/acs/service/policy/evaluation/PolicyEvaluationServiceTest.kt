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

package org.eclipse.keti.acs.service.policy.evaluation

import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.nhaarman.mockito_kotlin.any
import org.eclipse.keti.acs.commons.policy.condition.groovy.GroovyConditionCache
import org.eclipse.keti.acs.commons.policy.condition.groovy.GroovyConditionShell
import org.eclipse.keti.acs.model.Attribute
import org.eclipse.keti.acs.model.Effect
import org.eclipse.keti.acs.model.Policy
import org.eclipse.keti.acs.model.PolicySet
import org.eclipse.keti.acs.policy.evaluation.cache.PolicyEvaluationCache
import org.eclipse.keti.acs.privilege.management.PrivilegeManagementService
import org.eclipse.keti.acs.rest.BaseResource
import org.eclipse.keti.acs.rest.BaseSubject
import org.eclipse.keti.acs.rest.PolicyEvaluationRequestV1
import org.eclipse.keti.acs.rest.PolicyEvaluationResult
import org.eclipse.keti.acs.service.policy.admin.PolicyManagementService
import org.eclipse.keti.acs.service.policy.matcher.MatchResult
import org.eclipse.keti.acs.service.policy.matcher.PolicyMatcher
import org.eclipse.keti.acs.service.policy.validation.PolicySetValidator
import org.eclipse.keti.acs.service.policy.validation.PolicySetValidatorImpl
import org.eclipse.keti.acs.utils.JsonUtils
import org.eclipse.keti.acs.zone.management.dao.ZoneEntity
import org.eclipse.keti.acs.zone.resolver.ZoneResolver
import org.mockito.ArgumentMatchers.anyString
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests
import org.springframework.test.util.ReflectionTestUtils
import org.testng.Assert
import org.testng.annotations.BeforeClass
import org.testng.annotations.BeforeMethod
import org.testng.annotations.DataProvider
import org.testng.annotations.Test
import java.io.File
import java.io.IOException
import java.util.ArrayList
import java.util.Arrays
import java.util.HashSet
import java.util.LinkedHashSet

private const val ISSUER = "https://acs.attributes.int"
private const val SUBJECT_ATTRIB_NAME_ROLE = "role"
private const val SUBJECT_ATTRIB_VALUE_ANALYST = "analyst"
private const val RES_ATTRIB_ROLE_REQUIRED_VALUE = "administrator"
private const val SUBJECT_ATTRIB_VALUE_ADMIN = "administrator"
private const val RES_ATTRIB_ROLE_REQUIRED = "role_required"
private const val RES_ATTRIB_LOCATION = "location"
private const val RES_ATTRIB_LOCATION_VALUE = "sanramon"

private val EMPTY_ATTRS = emptySet<Attribute>()

/**
 * Unit tests for PolicyEvaluationService. Uses mocks, no external dependencies.
 *
 * @author acs-engineers@ge.com
 */
@Test
@ContextConfiguration(
    classes = [
        GroovyConditionCache::class,
        GroovyConditionShell::class,
        PolicySetValidatorImpl::class
    ]
)
class PolicyEvaluationServiceTest : AbstractTestNGSpringContextTests() {

    private val jsonUtils = JsonUtils()

    @InjectMocks
    private lateinit var evaluationService: PolicyEvaluationServiceImpl
    @Mock
    private lateinit var policyService: PolicyManagementService
    @Mock
    private lateinit var privilegeManagementService: PrivilegeManagementService
    @Mock
    private lateinit var policyMatcher: PolicyMatcher
    @Mock
    private lateinit var zoneResolver: ZoneResolver
    @Mock
    private lateinit var cache: PolicyEvaluationCache
    @Autowired
    private lateinit var policySetValidator: PolicySetValidator

    private val resource: BaseResource
        get() {
            val resource = BaseResource("name")
            val resourceAttributes = HashSet<Attribute>()
            resourceAttributes.add(Attribute(ISSUER, RES_ATTRIB_ROLE_REQUIRED, RES_ATTRIB_ROLE_REQUIRED_VALUE))
            resourceAttributes.add(Attribute(ISSUER, RES_ATTRIB_LOCATION, RES_ATTRIB_LOCATION_VALUE))
            resource.attributes = resourceAttributes
            return resource
        }

    @BeforeClass
    fun setupClass() {
        val policySetValidatorImpl = (this.policySetValidator as PolicySetValidatorImpl)
        policySetValidatorImpl.setValidAcsPolicyHttpActions("GET, POST, PUT, DELETE, PATCH")
        policySetValidatorImpl.init()
    }

    @BeforeMethod
    @Throws(Exception::class)
    fun setupMethod() {
        this.evaluationService = PolicyEvaluationServiceImpl()
        ReflectionTestUtils.setField(this.evaluationService, "policySetValidator", this.policySetValidator)
        MockitoAnnotations.initMocks(this)
        `when`(this.zoneResolver.zoneEntityOrFail).thenReturn(ZoneEntity(0L, "testzone"))
        `when`<PolicyEvaluationResult>(this.cache[any()]).thenReturn(null)
    }

    @Test(
        dataProvider = "policyRequestParameterProvider",
        expectedExceptions = [(IllegalArgumentException::class)]
    )
    fun testEvaluateWithNullParameters(
        resource: String,
        subject: String,
        action: String
    ) {
        this.evaluationService.evalPolicy(createRequest(resource, subject, action))
    }

    fun testEvaluateWithNoPolicySet() {
        val result = this.evaluationService
            .evalPolicy(createRequest("resource1", "subject1", "GET"))
        Assert.assertEquals(result.effect, Effect.NOT_APPLICABLE)
        Assert.assertEquals(result.resourceAttributes.size, 0)
        Assert.assertEquals(result.subjectAttributes.size, 0)
    }

    fun testEvaluateWithOnePolicySetNoPolicies() {
        val policySets = ArrayList<PolicySet>()
        policySets.add(PolicySet())
        `when`(this.policyService.allPolicySets).thenReturn(policySets)
        val matchedPolicies = emptyList<MatchedPolicy>()
        `when`(this.policyMatcher.matchForResult(any(), any()))
            .thenReturn(MatchResult(matchedPolicies, HashSet()))
        val evalPolicy = this.evaluationService
            .evalPolicy(createRequest("resource1", "subject1", "GET"))
        Assert.assertEquals(evalPolicy.effect, Effect.NOT_APPLICABLE)
    }

    @Test(dataProvider = "policyDataProvider")
    @Throws(JsonParseException::class, JsonMappingException::class, IOException::class)
    fun testEvaluateWithPolicy(
        inputPolicy: File,
        effect: Effect
    ) {
        initializePolicyMock(inputPolicy)
        val evalPolicy = this.evaluationService
            .evalPolicy(createRequest("resource1", "subject1", "GET"))
        Assert.assertEquals(evalPolicy.effect, effect)
    }

    @Test(dataProvider = "policyDataProviderForTestWithAttributes")
    @Throws(JsonParseException::class, JsonMappingException::class, IOException::class)
    fun testEvaluateWithPolicyAndSubjectResourceAttributes(
        acsSubjectAttributeValue: String?,
        inputPolicy: File,
        effect: Effect,
        subjectAttributes: Set<Attribute>
    ) {

        val resourceAttributes = HashSet<Attribute>()
        val roleAttribute = Attribute(ISSUER, RES_ATTRIB_ROLE_REQUIRED, RES_ATTRIB_ROLE_REQUIRED_VALUE)
        resourceAttributes.add(roleAttribute)
        val locationAttribute = Attribute(ISSUER, RES_ATTRIB_LOCATION, RES_ATTRIB_LOCATION_VALUE)
        resourceAttributes.add(locationAttribute)

        val mergedSubjectAttributes = HashSet(subjectAttributes)
        mergedSubjectAttributes.addAll(getSubjectAttributes(acsSubjectAttributeValue))
        initializePolicyMock(inputPolicy, resourceAttributes, mergedSubjectAttributes)
        `when`<BaseResource>(this.privilegeManagementService.getByResourceIdentifier(anyString()))
            .thenReturn(this.resource)
        `when`<BaseSubject>(this.privilegeManagementService.getBySubjectIdentifier(anyString()))
            .thenReturn(this.getSubject(acsSubjectAttributeValue))
        val evalPolicyResponse = this.evaluationService
            .evalPolicy(createRequest("resource1", "subject1", "GET"))
        Assert.assertEquals(evalPolicyResponse.effect, effect)
        Assert.assertTrue(evalPolicyResponse.resourceAttributes.contains(roleAttribute))
        Assert.assertTrue(evalPolicyResponse.resourceAttributes.contains(locationAttribute))
        if (acsSubjectAttributeValue != null) {
            Assert.assertTrue(
                evalPolicyResponse.subjectAttributes
                    .contains(Attribute(ISSUER, SUBJECT_ATTRIB_NAME_ROLE, acsSubjectAttributeValue))
            )
        }

        for (attribute in subjectAttributes) {
            Assert.assertTrue(evalPolicyResponse.subjectAttributes.contains(attribute))
        }

    }

    @Test(
        dataProvider = "filterPolicySetsInvalidRequestDataProvider",
        expectedExceptions = [(IllegalArgumentException::class)]
    )
    fun testFilterPolicySetsByPriorityForInvalidRequest(
        allPolicySets: List<PolicySet>,
        policySetsPriority: LinkedHashSet<String?>
    ) {
        this.evaluationService.filterPolicySetsByPriority("subject1", "resource1", allPolicySets, policySetsPriority)
    }

    @Test(dataProvider = "filterPolicySetsDataProvider")
    fun testFilterPolicySetsByPriority(
        allPolicySets: List<PolicySet>,
        policySetsPriority: LinkedHashSet<String?>,
        expectedFilteredPolicySets: LinkedHashSet<PolicySet>
    ) {
        val actualFilteredPolicySets = this.evaluationService
            .filterPolicySetsByPriority("subject1", "resource1", allPolicySets, policySetsPriority)
        Assert.assertEquals(actualFilteredPolicySets, expectedFilteredPolicySets)
    }

    @Test(dataProvider = "multiplePolicySetsRequestDataProvider")
    fun testEvaluateWithMultiplePolicySets(
        allPolicySets: List<PolicySet>,
        policySetsPriority: LinkedHashSet<String?>,
        effect: Effect
    ) {
        `when`(this.policyService.allPolicySets).thenReturn(allPolicySets)
        `when`(this.policyMatcher.matchForResult(any(), any()))
            .thenAnswer { invocation ->
                val args = invocation.arguments
                val policyList = args[1] as List<Policy>
                val matchedPolicies = ArrayList<MatchedPolicy>()
                // Mocking the policyMatcher to return all policies as matched.
                policyList.forEach { policy -> matchedPolicies.add(MatchedPolicy(policy, EMPTY_ATTRS, EMPTY_ATTRS)) }
                MatchResult(matchedPolicies, HashSet())
            }

        val result = this.evaluationService
            .evalPolicy(createRequest("anyresource", "anysubject", "GET", policySetsPriority))
        Assert.assertEquals(result.effect, effect)
    }

    @Test
    fun testPolicyEvaluationExceptionHandling() {
        val twoPolicySets = createNotApplicableAndDenyPolicySets()
        `when`(this.policyService.allPolicySets).thenReturn(twoPolicySets)
        `when`(this.policyMatcher.matchForResult(any(), any()))
            .thenAnswer { _ -> throw RuntimeException("This policy matcher is designed to throw an exception.") }

        val result = this.evaluationService.evalPolicy(
            createRequest(
                "anyresource", "anysubject", "GET", LinkedHashSet(listOf(twoPolicySets[0].name, twoPolicySets[1].name))
            )
        )

        Mockito.verify<PolicyEvaluationCache>(this.cache, Mockito.times(0))[any()] = any()

        Assert.assertEquals(result.effect, Effect.INDETERMINATE)
    }

    @Test(dataProvider = "policyDataProvider")
    @Throws(JsonParseException::class, JsonMappingException::class, IOException::class)
    fun testEvaluateWithPolicyWithCacheGetException(
        inputPolicy: File,
        effect: Effect
    ) {
        `when`<PolicyEvaluationResult>(this.cache[any()])
            .thenAnswer { _ -> throw RuntimeException() }
        testEvaluateWithPolicy(inputPolicy, effect)
    }

    @Test(dataProvider = "policyDataProvider")
    @Throws(JsonParseException::class, JsonMappingException::class, IOException::class)
    fun testEvaluateWithPolicyWithCacheSetException(
        inputPolicy: File,
        effect: Effect
    ) {
        Mockito.doAnswer { _ -> throw RuntimeException() }.`when`<PolicyEvaluationCache>(this.cache)[any()] = any()
        testEvaluateWithPolicy(inputPolicy, effect)
    }

    /**
     * @param inputPolicy
     * @throws IOException
     * @throws JsonParseException
     * @throws JsonMappingException
     */
    @Throws(IOException::class, JsonParseException::class, JsonMappingException::class)
    private fun initializePolicyMock(
        inputPolicy: File,
        resourceAttributes: Set<Attribute> = emptySet(),
        subjectAttributes: Set<Attribute> = emptySet()
    ) {
        val policySet = ObjectMapper().readValue(inputPolicy, PolicySet::class.java)
        `when`(this.policyService.allPolicySets).thenReturn(Arrays.asList(policySet))
        val matchedPolicies = ArrayList<MatchedPolicy>()
        for (policy in policySet.policies) {
            matchedPolicies.add(MatchedPolicy(policy, resourceAttributes, subjectAttributes))
        }
        `when`(this.policyMatcher.match(any(), any()))
            .thenReturn(matchedPolicies)
        `when`(this.policyMatcher.matchForResult(any(), any()))
            .thenReturn(MatchResult(matchedPolicies, HashSet()))
    }

    private fun getSubject(roleValue: String?): BaseSubject {
        val subject = BaseSubject("subject1")
        val attributes = getSubjectAttributes(roleValue)
        subject.attributes = attributes
        return subject
    }

    /**
     * @param roleValue
     * @return
     */
    private fun getSubjectAttributes(roleValue: String?): Set<Attribute> {
        val attributes = HashSet<Attribute>()
        if (roleValue != null) {
            attributes.add(Attribute(ISSUER, SUBJECT_ATTRIB_NAME_ROLE, roleValue))
        }
        return attributes
    }

    @DataProvider(name = "policyDataProviderForTestWithAttributes")
    private fun policyDataProviderForTestWithAttributes(): Array<Array<out Any?>> {
        return arrayOf(
            arrayOf(
                SUBJECT_ATTRIB_VALUE_ANALYST,
                File("src/test/resources/policy-set-with-one-policy-one-condition-using-attributes.json"),
                Effect.NOT_APPLICABLE,
                EMPTY_ATTRS
            ),
            arrayOf(
                SUBJECT_ATTRIB_VALUE_ANALYST,
                File("src/test/resources/policy-set-with-one-policy-one-condition-using-attributes.json"),
                Effect.PERMIT,
                getSubjectAttributes(SUBJECT_ATTRIB_VALUE_ADMIN)
            ),
            arrayOf(
                null,
                File("src/test/resources/policy-set-with-one-policy-one-condition-using-attributes.json"),
                Effect.NOT_APPLICABLE,
                getSubjectAttributes(SUBJECT_ATTRIB_VALUE_ADMIN)
            ),
            arrayOf(
                null,
                File("src/test/resources/" + "policy-set-with-one-policy-one-condition-using-res-attributes.json"),
                Effect.PERMIT,
                getSubjectAttributes(SUBJECT_ATTRIB_VALUE_ADMIN)
            )
        )
    }

    @DataProvider(name = "policyDataProvider")
    private fun policyDataProvider(): Array<Array<Any?>> {
        return arrayOf<Array<Any?>>(
            arrayOf(File("src/test/resources/policy-set-with-one-policy-nocondition.json"), Effect.DENY),
            arrayOf(File("src/test/resources/policy-set-with-one-policy-one-condition.json"), Effect.PERMIT),
            arrayOf(File("src/test/resources/policy-set-with-multiple-policies-first-match.json"), Effect.DENY),
            arrayOf(
                File("src/test/resources/policy-set-with-multiple-policies-permit-with-condition.json"),
                Effect.PERMIT
            ),
            arrayOf(File("src/test/resources/policy-set-with-multiple-policies-deny-with-condition.json"), Effect.DENY),
            arrayOf(
                File("src/test/resources/policy-set-with-multiple-policies-na-with-condition.json"),
                Effect.NOT_APPLICABLE
            ),
            arrayOf(
                File("src/test/resources/policy-set-with-multiple-policies-default-deny-with-condition.json"),
                Effect.DENY
            ),
            arrayOf(
                File("src/test/resources/policy-set-with-one-policy-one-condition-indeterminate.json"),
                Effect.INDETERMINATE
            ),
            arrayOf(
                File("src/test/resources/policy-set-with-multiple-policies-deny-missing-optional-tags.json"),
                Effect.DENY
            )
        )
    }

    @DataProvider(name = "policyRequestParameterProvider")
    private fun policyRequestParameterProvider(): Array<Array<Any?>> {
        return arrayOf<Array<Any?>>(arrayOf(null, "s1", "a1"), arrayOf("r1", null, "a1"), arrayOf("r1", "s1", null))
    }

    @DataProvider(name = "filterPolicySetsInvalidRequestDataProvider")
    @Throws(JsonParseException::class, JsonMappingException::class, IOException::class)
    private fun filterPolicySetsInvalidRequestDataProvider(): Array<Array<Any?>> {
        val onePolicySet = createDenyPolicySet()
        val twoPolicySets = createNotApplicableAndDenyPolicySets()
        return arrayOf(
            filterOnePolicySetByNonexistentPolicySet(onePolicySet),
            filterTwoPolisySetsByEmptyList(twoPolicySets),
            filterTwoPolicySetsByByNonexistentPolicySet(twoPolicySets)
        )
    }

    private fun filterOnePolicySetByNonexistentPolicySet(onePolicySet: List<PolicySet>): Array<Any?> {
        return arrayOf(
            onePolicySet,
            LinkedHashSet(listOf("nonexistent-policy-set"))
        )
    }

    private fun filterTwoPolisySetsByEmptyList(twoPolicySets: List<PolicySet>): Array<Any?> {
        return arrayOf(twoPolicySets, LinkedHashSet<String?>())
    }

    private fun filterTwoPolicySetsByByNonexistentPolicySet(twoPolicySets: List<PolicySet>): Array<Any?> {
        return arrayOf(
            twoPolicySets,
            LinkedHashSet(listOf(twoPolicySets[0].name, "noexistent-policy-set"))
        )
    }

    @DataProvider(name = "filterPolicySetsDataProvider")
    private fun filterPolicySetsDataProvider(): Array<Array<Any?>> {
        val denyPolicySet = createDenyPolicySet()
        val notApplicableAndDenyPolicySets = createNotApplicableAndDenyPolicySets()
        return arrayOf(
            filterOnePolicySetByEmptyEvaluationOrder(denyPolicySet),
            filterOnePolicySetByItself(denyPolicySet),
            filterTwoPolicySetsByFirstSet(notApplicableAndDenyPolicySets),
            filterTwoPolicySetsBySecondPolicySet(notApplicableAndDenyPolicySets),
            filterTwoPolicySetsByItself(notApplicableAndDenyPolicySets)
        )
    }

    private fun filterTwoPolicySetsByItself(twoPolicySets: List<PolicySet>): Array<Any?> {
        return arrayOf(
            twoPolicySets,
            LinkedHashSet(listOf(twoPolicySets[0].name, twoPolicySets[1].name)),
            LinkedHashSet(twoPolicySets)
        )
    }

    private fun filterTwoPolicySetsBySecondPolicySet(twoPolicySets: List<PolicySet>): Array<Any?> {
        return arrayOf(
            twoPolicySets,
            LinkedHashSet(listOf(twoPolicySets[1].name)),
            LinkedHashSet(listOf(twoPolicySets[1]))
        )
    }

    private fun filterTwoPolicySetsByFirstSet(twoPolicySets: List<PolicySet>): Array<Any?> {
        return arrayOf(
            twoPolicySets,
            LinkedHashSet(listOf(twoPolicySets[0].name)),
            LinkedHashSet(listOf(twoPolicySets[0]))
        )
    }

    private fun filterOnePolicySetByItself(onePolicySet: List<PolicySet>): Array<Any?> {
        return arrayOf(
            onePolicySet,
            LinkedHashSet(listOf(onePolicySet[0].name)),
            LinkedHashSet(onePolicySet)
        )
    }

    private fun filterOnePolicySetByEmptyEvaluationOrder(onePolicySet: List<PolicySet>): Array<Any?> {
        return arrayOf(
            onePolicySet,
            LinkedHashSet<String?>(),
            LinkedHashSet(onePolicySet)
        )
    }

    @DataProvider(name = "multiplePolicySetsRequestDataProvider")
    @Throws(JsonParseException::class, JsonMappingException::class, IOException::class)
    private fun multiplePolicySetsRequestDataProvider(): Array<Array<Any?>> {
        val denyPolicySet = createDenyPolicySet()
        val notApplicableAndDenyPolicySets = createNotApplicableAndDenyPolicySets()
        return arrayOf(
            requestEvaluationWithEmptyPolicySetsListAndEmptyPriorityList(),
            requestEvaluationWithOnePolicySetAndEmptyPriorityList(denyPolicySet),
            requestEvaluationWithFirstOfOnePolicySets(denyPolicySet),
            requestEvaluationWithFirstOfTwoPolicySets(notApplicableAndDenyPolicySets),
            requestEvaluationWithSecondOfTwoPolicySets(notApplicableAndDenyPolicySets),
            requestEvaluationWithAllOfTwoPolicySets(notApplicableAndDenyPolicySets)
        )
    }

    private fun requestEvaluationWithAllOfTwoPolicySets(twoPolicySets: List<PolicySet>): Array<Any?> {
        return arrayOf(
            twoPolicySets,
            LinkedHashSet(listOf(twoPolicySets[0].name, twoPolicySets[1].name)),
            Effect.DENY
        )
    }

    private fun requestEvaluationWithSecondOfTwoPolicySets(twoPolicySets: List<PolicySet>): Array<Any?> {
        return arrayOf(
            twoPolicySets,
            LinkedHashSet(listOf(twoPolicySets[1].name)),
            Effect.DENY
        )
    }

    private fun requestEvaluationWithFirstOfTwoPolicySets(twoPolicySets: List<PolicySet>): Array<Any?> {
        return arrayOf(
            twoPolicySets,
            LinkedHashSet(listOf(twoPolicySets[0].name)),
            Effect.NOT_APPLICABLE
        )
    }

    private fun requestEvaluationWithFirstOfOnePolicySets(onePolicySet: List<PolicySet>): Array<Any?> {
        return arrayOf(
            onePolicySet,
            LinkedHashSet(listOf(onePolicySet[0].name)),
            Effect.DENY
        )
    }

    private fun requestEvaluationWithOnePolicySetAndEmptyPriorityList(onePolicySet: List<PolicySet>): Array<Any?> {
        return arrayOf(onePolicySet, LinkedHashSet<String?>(), Effect.DENY)
    }

    private fun requestEvaluationWithEmptyPolicySetsListAndEmptyPriorityList(): Array<Any?> {
        return arrayOf(emptyList<Any>(), LinkedHashSet<String?>(), Effect.NOT_APPLICABLE)
    }

    private fun createDenyPolicySet(): List<PolicySet> {
        val policySets = ArrayList<PolicySet>()
        policySets.add(this.jsonUtils.deserializeFromFile("policies/testPolicyEvalDeny.json", PolicySet::class.java)!!)
        Assert.assertNotNull(policySets, "Policy set file is not found or invalid")
        return policySets
    }

    private fun createNotApplicableAndDenyPolicySets(): List<PolicySet> {
        val policySets = ArrayList<PolicySet>()
        policySets
            .add(
                this.jsonUtils.deserializeFromFile(
                    "policies/testPolicyEvalNotApplicable.json",
                    PolicySet::class.java
                )!!
            )
        policySets.add(this.jsonUtils.deserializeFromFile("policies/testPolicyEvalDeny.json", PolicySet::class.java)!!)
        Assert.assertNotNull(policySets, "Policy set files are not found or invalid")
        Assert.assertTrue(policySets.size == 2, "One or more policy set files are not found or invalid")
        return policySets
    }

    private fun createRequest(
        resource: String,
        subject: String,
        action: String
    ): PolicyEvaluationRequestV1 {
        val request = PolicyEvaluationRequestV1()
        request.action = action
        request.subjectIdentifier = subject
        request.resourceIdentifier = resource
        return request
    }

    private fun createRequest(
        resource: String,
        subject: String,
        action: String,
        policySetsEvaluationOrder: LinkedHashSet<String?>
    ): PolicyEvaluationRequestV1 {
        val request = PolicyEvaluationRequestV1()
        request.action = action
        request.subjectIdentifier = subject
        request.resourceIdentifier = resource
        request.policySetsEvaluationOrder = policySetsEvaluationOrder
        return request
    }
}
