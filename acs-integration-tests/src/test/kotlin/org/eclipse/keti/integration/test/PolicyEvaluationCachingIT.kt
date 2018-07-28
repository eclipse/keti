/*******************************************************************************
 * Copyright 2018 General Electric Company
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

package org.eclipse.keti.integration.test

import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.databind.JsonMappingException
import org.eclipse.keti.acs.model.Effect
import org.eclipse.keti.acs.rest.BaseResource
import org.eclipse.keti.acs.rest.PolicyEvaluationResult
import org.eclipse.keti.test.TestConfig
import org.eclipse.keti.test.utils.ACSITSetUpFactory
import org.eclipse.keti.test.utils.ACS_POLICY_EVAL_API_PATH
import org.eclipse.keti.test.utils.PolicyHelper
import org.eclipse.keti.test.utils.PrivilegeHelper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.security.oauth2.client.OAuth2RestTemplate
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests
import org.testng.Assert
import org.testng.annotations.AfterClass
import org.testng.annotations.AfterMethod
import org.testng.annotations.BeforeClass
import org.testng.annotations.Test
import java.io.IOException
import java.util.HashSet
import java.util.LinkedHashSet

@ContextConfiguration("classpath:integration-test-spring-context.xml")
class PolicyEvaluationCachingIT : AbstractTestNGSpringContextTests() {

    private var acsUrl: String? = null

    @Autowired
    private lateinit var policyHelper: PolicyHelper

    @Autowired
    private lateinit var privilegeHelper: PrivilegeHelper

    @Autowired
    private lateinit var acsitSetUpFactory: ACSITSetUpFactory

    private var acsAdminRestTemplate: OAuth2RestTemplate? = null
    private var acsZone1Headers: HttpHeaders? = null

    @BeforeClass
    @Throws(JsonParseException::class, JsonMappingException::class, IOException::class)
    fun setup() {
        TestConfig.setupForEclipse() // Starts ACS when running the test in eclipse.
        this.acsitSetUpFactory.setUp()
        this.acsUrl = this.acsitSetUpFactory.acsUrl
        this.acsZone1Headers = this.acsitSetUpFactory.zone1Headers
        this.acsAdminRestTemplate = this.acsitSetUpFactory.acsZoneAdminRestTemplate
    }

    @AfterMethod
    @Throws(Exception::class)
    fun cleanup() {
        this.privilegeHelper.deleteResources(this.acsAdminRestTemplate!!, this.acsUrl!!, this.acsZone1Headers!!)
        this.privilegeHelper.deleteSubjects(this.acsAdminRestTemplate!!, this.acsUrl!!, this.acsZone1Headers!!)
        this.policyHelper.deletePolicySets(this.acsAdminRestTemplate!!, this.acsUrl!!, this.acsZone1Headers!!)
    }

    /**
     * This test makes sure that cached policy evaluation results are properly invalidated when a policy set changes.
     * Policy set name that is used to derive part of cache key does not change.
     */
    @Test
    @Throws(Exception::class)
    fun testPolicyEvalCacheWhenPolicySetChanges() {
        val subject = MARISSA_V1
        val policyEvaluationRequest = this.policyHelper
            .createEvalRequest(MARISSA_V1.subjectIdentifier!!, "sanramon")
        val endpoint = this.acsUrl

        this.privilegeHelper.putSubject(
            this.acsAdminRestTemplate!!, subject, endpoint, this.acsZone1Headers!!,
            this.privilegeHelper.defaultAttribute
        )
        var policyFile = "src/test/resources/policies/single-site-based.json"
        this.policyHelper.setTestPolicy(this.acsAdminRestTemplate!!, this.acsZone1Headers!!, endpoint!!, policyFile)

        var postForEntity = this.acsAdminRestTemplate!!
            .postForEntity(
                endpoint + ACS_POLICY_EVAL_API_PATH,
                HttpEntity(policyEvaluationRequest, this.acsZone1Headers), PolicyEvaluationResult::class.java
            )

        Assert.assertEquals(postForEntity.statusCode, HttpStatus.OK)
        var responseBody = postForEntity.body
        Assert.assertEquals(responseBody.effect, Effect.PERMIT)

        policyFile = "src/test/resources/policies/deny-all.json"
        this.policyHelper.setTestPolicy(this.acsAdminRestTemplate!!, this.acsZone1Headers!!, endpoint, policyFile)

        postForEntity = this.acsAdminRestTemplate!!.postForEntity(
            endpoint + ACS_POLICY_EVAL_API_PATH,
            HttpEntity(policyEvaluationRequest, this.acsZone1Headers), PolicyEvaluationResult::class.java
        )

        Assert.assertEquals(postForEntity.statusCode, HttpStatus.OK)
        responseBody = postForEntity.body
        Assert.assertEquals(responseBody.effect, Effect.DENY)
    }

    /**
     * This test makes sure that cached policy evaluation results are properly invalidated when one of the policies in
     * a multiple policy set evaluation order list changes.
     */
    @Test
    @Throws(Exception::class)
    fun testPolicyEvalCacheWithMultiplePolicySets() {

        val indeterminatePolicyFile = "src/test/resources/policies/indeterminate.json"
        val denyAllPolicyFile = "src/test/resources/policies/deny-all.json"
        val siteBasedPolicyFile = "src/test/resources/policies/single-site-based.json"
        val endpoint = this.acsUrl

        this.privilegeHelper.putSubject(
            this.acsAdminRestTemplate!!, MARISSA_V1, endpoint, this.acsZone1Headers!!,
            this.privilegeHelper.defaultAttribute
        )

        val indeterminatePolicySet = this.policyHelper
            .setTestPolicy(this.acsAdminRestTemplate!!, this.acsZone1Headers!!, endpoint!!, indeterminatePolicyFile)
        val denyAllPolicySet = this.policyHelper
            .setTestPolicy(this.acsAdminRestTemplate!!, this.acsZone1Headers!!, endpoint, denyAllPolicyFile)

        // test with a valid policy set evaluation order list
        var policyEvaluationRequest = this.policyHelper
            .createMultiplePolicySetsEvalRequest(
                MARISSA_V1.subjectIdentifier!!, "sanramon",
                LinkedHashSet(listOf(indeterminatePolicySet, denyAllPolicySet))
            )

        var postForEntity = this.acsAdminRestTemplate!!
            .postForEntity(
                endpoint + ACS_POLICY_EVAL_API_PATH,
                HttpEntity(policyEvaluationRequest, this.acsZone1Headers), PolicyEvaluationResult::class.java
            )

        Assert.assertEquals(postForEntity.statusCode, HttpStatus.OK)
        var responseBody = postForEntity.body
        Assert.assertEquals(responseBody.effect, Effect.DENY)

        // test with one of the policy sets changed from the evaluation order list
        val siteBasedPolicySet = this.policyHelper
            .setTestPolicy(this.acsAdminRestTemplate!!, this.acsZone1Headers!!, endpoint, siteBasedPolicyFile)
        policyEvaluationRequest = this.policyHelper
            .createMultiplePolicySetsEvalRequest(
                MARISSA_V1.subjectIdentifier!!, "sanramon",
                LinkedHashSet(listOf(indeterminatePolicySet, siteBasedPolicySet))
            )

        postForEntity = this.acsAdminRestTemplate!!.postForEntity(
            endpoint + ACS_POLICY_EVAL_API_PATH,
            HttpEntity(policyEvaluationRequest, this.acsZone1Headers), PolicyEvaluationResult::class.java
        )

        Assert.assertEquals(postForEntity.statusCode, HttpStatus.OK)
        responseBody = postForEntity.body
        Assert.assertEquals(responseBody.effect, Effect.PERMIT)
    }

    /**
     * This test makes sure that cached policy evaluation results are properly invalidated when an affected resource
     * is created.
     */
    @Test
    @Throws(Exception::class)
    fun testPolicyEvalCacheWhenResourceAdded() {
        val endpoint = this.acsUrl

        // create test subject
        val subject = MARISSA_V1
        this.privilegeHelper.putSubject(
            this.acsAdminRestTemplate!!, subject, endpoint, this.acsZone1Headers!!,
            this.privilegeHelper.defaultAttribute, this.privilegeHelper.defaultOrgAttribute
        )

        // create test policy set
        val policyFile = "src/test/resources/policies/single-org-based.json"
        this.policyHelper.setTestPolicy(this.acsAdminRestTemplate!!, this.acsZone1Headers!!, endpoint!!, policyFile)

        // post policy evaluation request
        val policyEvaluationRequest = this.policyHelper
            .createEvalRequest(MARISSA_V1.subjectIdentifier!!, "sanramon")
        var postForEntity = this.acsAdminRestTemplate!!
            .postForEntity(
                endpoint + ACS_POLICY_EVAL_API_PATH,
                HttpEntity(policyEvaluationRequest, this.acsZone1Headers), PolicyEvaluationResult::class.java
            )

        Assert.assertEquals(postForEntity.statusCode, HttpStatus.OK)
        var responseBody = postForEntity.body
        Assert.assertEquals(responseBody.effect, Effect.NOT_APPLICABLE)

        // at this point evaluation decision is cached
        // timestamps for subject and resource involved in the decision are also cached even though resource doesn't
        // exist yet

        // add a resource which is expected to reset resource cached timestamp and invalidate cached decision
        val resource = BaseResource("/secured-by-value/sites/sanramon")
        this.privilegeHelper.putResource(
            this.acsAdminRestTemplate!!, resource, endpoint, this.acsZone1Headers!!,
            this.privilegeHelper.defaultOrgAttribute
        )

        // post policy evaluation request; decision should be reevaluated.
        postForEntity = this.acsAdminRestTemplate!!.postForEntity(
            endpoint + ACS_POLICY_EVAL_API_PATH,
            HttpEntity(policyEvaluationRequest, this.acsZone1Headers), PolicyEvaluationResult::class.java
        )

        Assert.assertEquals(postForEntity.statusCode, HttpStatus.OK)
        responseBody = postForEntity.body
        Assert.assertEquals(responseBody.effect, Effect.PERMIT)
    }

    /**
     * This test makes sure that cached policy evaluation results are properly invalidated when an affected resource
     * is updated with different attributes.
     */
    @Test
    @Throws(Exception::class)
    fun testPolicyEvalCacheWhenResourceChanges() {
        val resource = BaseResource("/secured-by-value/sites/sanramon")
        val subject = MARISSA_V1
        val policyEvaluationRequest = this.policyHelper
            .createEvalRequest(MARISSA_V1.subjectIdentifier!!, "sanramon")
        val endpoint = this.acsUrl

        this.privilegeHelper.putResource(
            this.acsAdminRestTemplate!!, resource, endpoint!!, this.acsZone1Headers!!,
            this.privilegeHelper.defaultOrgAttribute
        )
        this.privilegeHelper.putSubject(
            this.acsAdminRestTemplate!!, subject, endpoint, this.acsZone1Headers!!,
            this.privilegeHelper.defaultAttribute, this.privilegeHelper.defaultOrgAttribute
        )
        val policyFile = "src/test/resources/policies/single-org-based.json"
        this.policyHelper.setTestPolicy(this.acsAdminRestTemplate!!, this.acsZone1Headers!!, endpoint, policyFile)

        var postForEntity = this.acsAdminRestTemplate!!
            .postForEntity(
                endpoint + ACS_POLICY_EVAL_API_PATH,
                HttpEntity(policyEvaluationRequest, this.acsZone1Headers), PolicyEvaluationResult::class.java
            )

        Assert.assertEquals(postForEntity.statusCode, HttpStatus.OK)
        var responseBody = postForEntity.body
        Assert.assertEquals(responseBody.effect, Effect.PERMIT)

        // update resource with different attributes
        this.privilegeHelper.putResource(
            this.acsAdminRestTemplate!!, resource, endpoint, this.acsZone1Headers!!,
            this.privilegeHelper.alternateOrgAttribute
        )

        postForEntity = this.acsAdminRestTemplate!!.postForEntity(
            endpoint + ACS_POLICY_EVAL_API_PATH,
            HttpEntity(policyEvaluationRequest, this.acsZone1Headers), PolicyEvaluationResult::class.java
        )

        Assert.assertEquals(postForEntity.statusCode, HttpStatus.OK)
        responseBody = postForEntity.body
        Assert.assertEquals(responseBody.effect, Effect.NOT_APPLICABLE)
    }

    /**
     * This test makes sure that cached policy evaluation results are properly invalidated when an affected subject
     * is created.
     */
    @Test
    @Throws(Exception::class)
    fun testPolicyEvalCacheWhenSubjectAdded() {
        val endpoint = this.acsUrl

        // create test resource
        val resource = BaseResource("/secured-by-value/sites/sanramon")
        this.privilegeHelper.putResource(
            this.acsAdminRestTemplate!!, resource, endpoint!!, this.acsZone1Headers!!,
            this.privilegeHelper.defaultAttribute
        )

        // create test policy set
        val policyFile = "src/test/resources/policies/single-site-based.json"
        this.policyHelper.setTestPolicy(this.acsAdminRestTemplate!!, this.acsZone1Headers!!, endpoint, policyFile)

        // post policy evaluation request
        val policyEvaluationRequest = this.policyHelper
            .createEvalRequest(MARISSA_V1.subjectIdentifier!!, "sanramon")
        var postForEntity = this.acsAdminRestTemplate!!
            .postForEntity(
                endpoint + ACS_POLICY_EVAL_API_PATH,
                HttpEntity(policyEvaluationRequest, this.acsZone1Headers), PolicyEvaluationResult::class.java
            )

        Assert.assertEquals(postForEntity.statusCode, HttpStatus.OK)
        var responseBody = postForEntity.body
        Assert.assertEquals(responseBody.effect, Effect.NOT_APPLICABLE)

        // at this point evaluation decision is cached
        // timestamps for resource and subject involved in the decision are also cached even though subject doesn't
        // exist yet

        // add a subject which is expected to reset subject cached timestamp and invalidate cached decision
        val subject = MARISSA_V1
        this.privilegeHelper.putSubject(
            this.acsAdminRestTemplate!!, subject, endpoint, this.acsZone1Headers!!,
            this.privilegeHelper.defaultAttribute, this.privilegeHelper.defaultAttribute
        )

        // post policy evaluation request; decision should be reevaluated.
        postForEntity = this.acsAdminRestTemplate!!.postForEntity(
            endpoint + ACS_POLICY_EVAL_API_PATH,
            HttpEntity(policyEvaluationRequest, this.acsZone1Headers), PolicyEvaluationResult::class.java
        )

        Assert.assertEquals(postForEntity.statusCode, HttpStatus.OK)
        responseBody = postForEntity.body
        Assert.assertEquals(responseBody.effect, Effect.PERMIT)
    }

    /**
     * This test makes sure that cached policy evaluation results are properly invalidated when an affected subject is
     * updated or deleted.
     */
    @Test
    @Throws(Exception::class)
    fun testPolicyEvalCacheWhenSubjectChanges() {
        val policyEvaluationRequest = this.policyHelper
            .createEvalRequest(MARISSA_V1.subjectIdentifier!!, "sanramon")
        val endpoint = this.acsUrl

        this.privilegeHelper.putSubject(
            this.acsAdminRestTemplate!!, MARISSA_V1, endpoint, this.acsZone1Headers!!,
            this.privilegeHelper.defaultAttribute
        )
        val policyFile = "src/test/resources/single-site-based-policy-set.json"
        this.policyHelper.setTestPolicy(this.acsAdminRestTemplate!!, this.acsZone1Headers!!, endpoint!!, policyFile)

        var postForEntity = this.acsAdminRestTemplate!!
            .postForEntity(
                endpoint + ACS_POLICY_EVAL_API_PATH,
                HttpEntity(policyEvaluationRequest, this.acsZone1Headers), PolicyEvaluationResult::class.java
            )

        Assert.assertEquals(postForEntity.statusCode, HttpStatus.OK)
        var responseBody = postForEntity.body
        Assert.assertEquals(responseBody.effect, Effect.PERMIT)

        this.privilegeHelper.putSubject(
            this.acsAdminRestTemplate!!, MARISSA_V1, endpoint, this.acsZone1Headers!!,
            this.privilegeHelper.alternateAttribute
        )

        postForEntity = this.acsAdminRestTemplate!!.postForEntity(
            endpoint + ACS_POLICY_EVAL_API_PATH,
            HttpEntity(policyEvaluationRequest, this.acsZone1Headers), PolicyEvaluationResult::class.java
        )

        Assert.assertEquals(postForEntity.statusCode, HttpStatus.OK)
        responseBody = postForEntity.body
        Assert.assertEquals(responseBody.effect, Effect.NOT_APPLICABLE)
    }

    /**
     * This test makes sure that cached policy evaluation results are properly invalidated when a policy has multiple
     * resource attribute URI templates that match the request.
     */
    @Test
    @Throws(Exception::class)
    fun testPolicyWithMultAttrUriTemplatatesEvalCache() {
        val subject = MARISSA_V1
        val policyEvaluationRequest = this.policyHelper
            .createEvalRequest("GET", MARISSA_V1.subjectIdentifier, "/v1/site/1/plant/asset/1", null)
        val endpoint = this.acsUrl

        this.privilegeHelper.putSubject(
            this.acsAdminRestTemplate!!, subject, endpoint, this.acsZone1Headers!!,
            this.privilegeHelper.defaultAttribute, this.privilegeHelper.defaultOrgAttribute
        )
        val policyFile = "src/test/resources/policies/multiple-attribute-uri-templates.json"
        this.policyHelper.setTestPolicy(this.acsAdminRestTemplate!!, this.acsZone1Headers!!, endpoint!!, policyFile)

        var postForEntity = this.acsAdminRestTemplate!!
            .postForEntity(
                endpoint + ACS_POLICY_EVAL_API_PATH,
                HttpEntity(policyEvaluationRequest, this.acsZone1Headers), PolicyEvaluationResult::class.java
            )

        Assert.assertEquals(postForEntity.statusCode, HttpStatus.OK)
        var responseBody = postForEntity.body
        Assert.assertEquals(responseBody.effect, Effect.NOT_APPLICABLE)

        val siteResource = BaseResource("/site/1")
        siteResource.attributes = HashSet(
            listOf(this.privilegeHelper.defaultOrgAttribute)
        )
        this.privilegeHelper.putResource(
            this.acsAdminRestTemplate!!, siteResource, endpoint, this.acsZone1Headers!!,
            this.privilegeHelper.defaultOrgAttribute
        )

        postForEntity = this.acsAdminRestTemplate!!.postForEntity(
            endpoint + ACS_POLICY_EVAL_API_PATH,
            HttpEntity(policyEvaluationRequest, this.acsZone1Headers), PolicyEvaluationResult::class.java
        )

        Assert.assertEquals(postForEntity.statusCode, HttpStatus.OK)
        responseBody = postForEntity.body
        Assert.assertEquals(responseBody.effect, Effect.PERMIT)
    }

    @AfterClass
    fun destroy() {
        this.acsitSetUpFactory.destroy()
    }
}
