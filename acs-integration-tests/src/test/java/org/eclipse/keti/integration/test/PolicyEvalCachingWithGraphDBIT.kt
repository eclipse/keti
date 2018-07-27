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

package org.eclipse.keti.integration.test

import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.databind.JsonMappingException
import org.eclipse.keti.acs.model.Attribute
import org.eclipse.keti.acs.model.Effect
import org.eclipse.keti.acs.rest.BaseResource
import org.eclipse.keti.acs.rest.BaseSubject
import org.eclipse.keti.acs.rest.Parent
import org.eclipse.keti.acs.rest.PolicyEvaluationResult
import org.eclipse.keti.test.utils.ACSITSetUpFactory
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
import java.util.Arrays
import java.util.HashSet

private const val ISSUER_URI = "acs.example.org"
private val TOP_SECRET_CLASSIFICATION = Attribute(
    ISSUER_URI, "classification",
    "top secret"
)
private val SPECIAL_AGENTS_GROUP_ATTRIBUTE = Attribute(
    ISSUER_URI, "group",
    "special-agents"
)

private const val FBI = "fbi"
private const val SPECIAL_AGENTS_GROUP = "special-agents"
private const val AGENT_MULDER = "mulder"
private const val AGENT_SCULLY = "scully"
const val EVIDENCE_SCULLYS_TESTIMONY_ID = "/evidence/scullys-testimony"

@ContextConfiguration("classpath:integration-test-spring-context.xml")
class PolicyEvalCachingWithGraphDBIT : AbstractTestNGSpringContextTests() {

    @Autowired
    private lateinit var policyHelper: PolicyHelper

    @Autowired
    private lateinit var privilegeHelper: PrivilegeHelper

    @Autowired
    private lateinit var acsitSetUpFactory: ACSITSetUpFactory

    private var acsAdminRestTemplate: OAuth2RestTemplate? = null

    private var acsZone1Headers: HttpHeaders? = null
    private var acsUrl: String? = null

    @BeforeClass
    @Throws(JsonParseException::class, JsonMappingException::class, IOException::class)
    fun setup() {
        acsitSetUpFactory.setUp()
        acsZone1Headers = acsitSetUpFactory.zone1Headers
        acsAdminRestTemplate = acsitSetUpFactory.acsZoneAdminRestTemplate
        acsUrl = acsitSetUpFactory.acsUrl
    }

    @AfterMethod
    @Throws(Exception::class)
    fun cleanup() {
        privilegeHelper.deleteResources(acsAdminRestTemplate, acsUrl, acsZone1Headers)
        privilegeHelper.deleteSubjects(acsAdminRestTemplate, acsUrl, acsZone1Headers)
        policyHelper.deletePolicySets(acsAdminRestTemplate, acsUrl, acsZone1Headers)
    }

    /**
     * This test makes sure that cached policy evaluation results are properly invalidated for a subject and its
     * descendants, when attributes are changed for the parent subject.
     */
    @Test
    @Throws(Exception::class)
    fun testPolicyEvalCacheInvalidationWhenSubjectParentChanges() {
        val fbi = BaseSubject(FBI)

        val specialAgentsGroup = BaseSubject(SPECIAL_AGENTS_GROUP)
        specialAgentsGroup
            .parents = HashSet(Arrays.asList(Parent(fbi.subjectIdentifier!!)))

        val agentMulder = BaseSubject(AGENT_MULDER)
        agentMulder.parents = HashSet(Arrays.asList(Parent(specialAgentsGroup.subjectIdentifier!!)))

        val agentScully = BaseSubject(AGENT_SCULLY)
        agentScully.parents = HashSet(Arrays.asList(Parent(specialAgentsGroup.subjectIdentifier!!)))

        val scullysTestimony = BaseResource(EVIDENCE_SCULLYS_TESTIMONY_ID)

        val mulderPolicyEvaluationRequest = policyHelper
            .createEvalRequest("GET", agentMulder.subjectIdentifier, EVIDENCE_SCULLYS_TESTIMONY_ID, null)
        val scullyPolicyEvaluationRequest = policyHelper
            .createEvalRequest("GET", agentScully.subjectIdentifier, EVIDENCE_SCULLYS_TESTIMONY_ID, null)

        val endpoint = acsUrl

        // Set up fbi <-- specialAgentsGroup <-- (agentMulder, agentScully) subject hierarchy
        privilegeHelper.putSubject(acsAdminRestTemplate, fbi, endpoint, acsZone1Headers)
        privilegeHelper.putSubject(
            acsAdminRestTemplate, specialAgentsGroup, endpoint, acsZone1Headers,
            SPECIAL_AGENTS_GROUP_ATTRIBUTE
        )
        privilegeHelper.putSubject(acsAdminRestTemplate, agentMulder, endpoint, acsZone1Headers)
        privilegeHelper.putSubject(acsAdminRestTemplate, agentScully, endpoint, acsZone1Headers)

        // Set up resource
        privilegeHelper.putResource(
            acsAdminRestTemplate, scullysTestimony, endpoint, acsZone1Headers,
            SPECIAL_AGENTS_GROUP_ATTRIBUTE, TOP_SECRET_CLASSIFICATION
        )

        // Set up policy
        val policyFile = "src/test/resources/policies/complete-sample-policy-set-2.json"
        policyHelper.setTestPolicy(acsAdminRestTemplate, acsZone1Headers, endpoint, policyFile)

        // Verify that policy is evaluated to DENY since top secret classification is not set
        var postForEntity = acsAdminRestTemplate!!
            .postForEntity(
                endpoint!! + PolicyHelper.ACS_POLICY_EVAL_API_PATH,
                HttpEntity(mulderPolicyEvaluationRequest, acsZone1Headers),
                PolicyEvaluationResult::class.java
            )
        Assert.assertEquals(postForEntity.statusCode, HttpStatus.OK)
        var responseBody = postForEntity.body
        Assert.assertEquals(responseBody.effect, Effect.DENY)

        postForEntity = acsAdminRestTemplate!!.postForEntity(
            endpoint + PolicyHelper.ACS_POLICY_EVAL_API_PATH,
            HttpEntity(scullyPolicyEvaluationRequest, acsZone1Headers), PolicyEvaluationResult::class.java
        )
        Assert.assertEquals(postForEntity.statusCode, HttpStatus.OK)
        responseBody = postForEntity.body
        Assert.assertEquals(responseBody.effect, Effect.DENY)

        // Change parent subject to add top secret classification
        privilegeHelper.putSubject(
            acsAdminRestTemplate, specialAgentsGroup, endpoint, acsZone1Headers,
            SPECIAL_AGENTS_GROUP_ATTRIBUTE, TOP_SECRET_CLASSIFICATION
        )

        // Verify that policy is evaluated to PERMIT since top secret classification is now propogated from the parent
        postForEntity = acsAdminRestTemplate!!.postForEntity(
            endpoint + PolicyHelper.ACS_POLICY_EVAL_API_PATH,
            HttpEntity(mulderPolicyEvaluationRequest, acsZone1Headers), PolicyEvaluationResult::class.java
        )
        Assert.assertEquals(postForEntity.statusCode, HttpStatus.OK)
        responseBody = postForEntity.body
        Assert.assertEquals(responseBody.effect, Effect.PERMIT)

        postForEntity = acsAdminRestTemplate!!.postForEntity(
            endpoint + PolicyHelper.ACS_POLICY_EVAL_API_PATH,
            HttpEntity(scullyPolicyEvaluationRequest, acsZone1Headers), PolicyEvaluationResult::class.java
        )
        Assert.assertEquals(postForEntity.statusCode, HttpStatus.OK)
        responseBody = postForEntity.body
        Assert.assertEquals(responseBody.effect, Effect.PERMIT)
    }

    /**
     * This test makes sure that cached policy evaluation results are properly invalidated for a resource and its
     * descendants, when attributes are changed for the parent resource.
     */
    @Test
    @Throws(Exception::class)
    fun testPolicyEvalCacheInvalidationWhenResourceParentChanges() {
        val grandparentResource = BaseResource("/secured-by-value/sites/east-bay")
        val parentResource = BaseResource("/secured-by-value/sites/sanramon")
        val childResource = BaseResource("/secured-by-value/sites/basement")

        parentResource.parents = HashSet(Arrays.asList(Parent(grandparentResource.resourceIdentifier!!)))

        childResource.parents = HashSet(Arrays.asList(Parent(parentResource.resourceIdentifier!!)))

        val agentMulder = BaseSubject(AGENT_MULDER)

        val sanramonPolicyEvaluationRequest = policyHelper
            .createEvalRequest(agentMulder.subjectIdentifier, "sanramon")

        val basementPolicyEvaluationRequest = policyHelper
            .createEvalRequest(agentMulder.subjectIdentifier, "basement")

        val endpoint = acsUrl

        privilegeHelper.putResource(
            acsAdminRestTemplate, grandparentResource, endpoint, acsZone1Headers,
            privilegeHelper.defaultOrgAttribute
        )
        privilegeHelper.putResource(acsAdminRestTemplate, parentResource, endpoint, acsZone1Headers)
        privilegeHelper.putResource(acsAdminRestTemplate, childResource, endpoint, acsZone1Headers)
        privilegeHelper.putSubject(
            acsAdminRestTemplate, agentMulder, endpoint, acsZone1Headers,
            privilegeHelper.defaultAttribute, privilegeHelper.defaultOrgAttribute
        )

        val policyFile = "src/test/resources/policies/single-org-based.json"
        policyHelper.setTestPolicy(acsAdminRestTemplate, acsZone1Headers, endpoint, policyFile)

        // Subject policy evaluation request for site "sanramon"
        var postForEntity = acsAdminRestTemplate!!
            .postForEntity(
                endpoint!! + PolicyHelper.ACS_POLICY_EVAL_API_PATH,
                HttpEntity(sanramonPolicyEvaluationRequest, acsZone1Headers),
                PolicyEvaluationResult::class.java
            )

        Assert.assertEquals(postForEntity.statusCode, HttpStatus.OK)
        var responseBody = postForEntity.body
        Assert.assertEquals(responseBody.effect, Effect.PERMIT)

        // Subject policy evaluation request for site "basement"
        postForEntity = acsAdminRestTemplate!!.postForEntity(
            endpoint + PolicyHelper.ACS_POLICY_EVAL_API_PATH,
            HttpEntity(basementPolicyEvaluationRequest, acsZone1Headers), PolicyEvaluationResult::class.java
        )

        Assert.assertEquals(postForEntity.statusCode, HttpStatus.OK)
        responseBody = postForEntity.body
        Assert.assertEquals(responseBody.effect, Effect.PERMIT)

        // Change grandparent resource attributes from DefaultOrgAttribute to AlternateOrgAttribute
        privilegeHelper.putResource(
            acsAdminRestTemplate, grandparentResource, endpoint, acsZone1Headers,
            privilegeHelper.alternateOrgAttribute
        )

        // Subject policy evaluation request for site "sanramon"
        postForEntity = acsAdminRestTemplate!!.postForEntity(
            endpoint + PolicyHelper.ACS_POLICY_EVAL_API_PATH,
            HttpEntity(sanramonPolicyEvaluationRequest, acsZone1Headers), PolicyEvaluationResult::class.java
        )

        Assert.assertEquals(postForEntity.statusCode, HttpStatus.OK)
        responseBody = postForEntity.body
        Assert.assertEquals(responseBody.effect, Effect.NOT_APPLICABLE)

        // Subject policy evaluation request for site "basement"
        postForEntity = acsAdminRestTemplate!!.postForEntity(
            endpoint + PolicyHelper.ACS_POLICY_EVAL_API_PATH,
            HttpEntity(basementPolicyEvaluationRequest, acsZone1Headers), PolicyEvaluationResult::class.java
        )

        Assert.assertEquals(postForEntity.statusCode, HttpStatus.OK)
        responseBody = postForEntity.body
        Assert.assertEquals(responseBody.effect, Effect.NOT_APPLICABLE)
    }

    @AfterClass
    fun destroy() {
        acsitSetUpFactory.destroy()
    }
}
