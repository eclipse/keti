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

package org.eclipse.keti.test.utils

import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.ObjectMapper
import org.eclipse.keti.acs.model.Attribute
import org.eclipse.keti.acs.model.PolicySet
import org.eclipse.keti.acs.rest.BaseSubject
import org.eclipse.keti.acs.rest.PolicyEvaluationRequestV1
import org.eclipse.keti.acs.rest.PolicyEvaluationResult
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestTemplate
import java.io.File
import java.io.IOException
import java.net.URI
import java.util.LinkedHashSet
import java.util.Random

const val ACS_POLICY_SET_API_PATH = "$ACS_VERSION/policy-set/"
const val ACS_POLICY_EVAL_API_PATH = "$ACS_VERSION/policy-evaluation"
const val DEFAULT_ACTION = "GET"
const val NOT_MATCHING_ACTION = "HEAD"
const val PREDIX_ZONE_ID = "Predix-Zone-Id"

private val ACTIONS = arrayOf("GET", "POST", "DELETE", "PUT")

@Component
class PolicyHelper {

    @Autowired
    private lateinit var zoneFactory: ZoneFactory

    @Throws(JsonParseException::class, JsonMappingException::class, IOException::class)
    fun setTestPolicy(
        acs: RestTemplate,
        headers: HttpHeaders,
        endpoint: String,
        policyFile: String
    ): String? {

        val policySet = ObjectMapper().readValue(File(policyFile), PolicySet::class.java)
        val policyName = policySet.name
        acs.put(endpoint + ACS_POLICY_SET_API_PATH + policyName, HttpEntity(policySet, headers))
        return policyName
    }

    fun createPolicySet(
        policyFile: String,
        restTemplate: RestTemplate,
        headers: HttpHeaders
    ): CreatePolicyStatus {
        val policySet: PolicySet
        try {
            policySet = ObjectMapper().readValue(File(policyFile), PolicySet::class.java)
            val policyName = policySet.name
            restTemplate.put(
                zoneFactory.acsBaseURL + ACS_POLICY_SET_API_PATH + policyName,
                HttpEntity(policySet, headers)
            )
            return CreatePolicyStatus.SUCCESS
        } catch (e: IOException) {
            return CreatePolicyStatus.JSON_ERROR
        } catch (httpException: HttpClientErrorException) {
            return if (httpException.statusCode != null && httpException.statusCode == HttpStatus.UNPROCESSABLE_ENTITY)
                CreatePolicyStatus.INVALID_POLICY_SET
            else
                CreatePolicyStatus.ACS_ERROR
        } catch (e: RestClientException) {
            return CreatePolicyStatus.ACS_ERROR
        }

    }

    fun getPolicySet(
        policyName: String,
        restTemplate: RestTemplate,
        endpoint: String
    ): ResponseEntity<PolicySet> {
        return restTemplate
            .getForEntity(endpoint + ACS_POLICY_SET_API_PATH + policyName, PolicySet::class.java)
    }

    fun createRandomEvalRequest(): PolicyEvaluationRequestV1 {
        val r = Random(System.currentTimeMillis())
        val subjectAttributes = emptySet<Attribute>()
        return this.createEvalRequest(
            ACTIONS[r.nextInt(4)], r.nextLong().toString(),
            "/alarms/sites/" + r.nextLong().toString(), subjectAttributes
        )
    }

    fun createMultiplePolicySetsEvalRequest(
        action: String,
        subjectIdentifier: String?,
        resourceIdentifier: String,
        subjectAttributes: Set<Attribute>?,
        policySetIds: LinkedHashSet<String?>
    ): PolicyEvaluationRequestV1 {
        val policyEvaluationRequest = PolicyEvaluationRequestV1()
        policyEvaluationRequest.action = action
        policyEvaluationRequest.subjectIdentifier = subjectIdentifier
        policyEvaluationRequest.resourceIdentifier = resourceIdentifier
        policyEvaluationRequest.subjectAttributes = subjectAttributes
        policyEvaluationRequest.policySetsEvaluationOrder = policySetIds
        return policyEvaluationRequest
    }

    fun createMultiplePolicySetsEvalRequest(
        subjectIdentifier: String,
        site: String,
        policySetIds: LinkedHashSet<String?>
    ): PolicyEvaluationRequestV1 {
        return createMultiplePolicySetsEvalRequest(
            "GET", subjectIdentifier, "/secured-by-value/sites/$site", null,
            policySetIds
        )
    }

    fun createMultiplePolicySetsEvalRequest(
        subject: BaseSubject,
        site: String,
        policySetIds: LinkedHashSet<String?>
    ): PolicyEvaluationRequestV1 {
        return createMultiplePolicySetsEvalRequest(
            "GET", subject.subjectIdentifier,
            "/secured-by-value/sites/$site", null, policySetIds
        )
    }

    fun createEvalRequest(
        action: String,
        subjectIdentifier: String?,
        resourceIdentifier: String,
        subjectAttributes: Set<Attribute>?
    ): PolicyEvaluationRequestV1 {
        val policyEvaluationRequest = PolicyEvaluationRequestV1()
        policyEvaluationRequest.action = action
        policyEvaluationRequest.subjectIdentifier = subjectIdentifier
        policyEvaluationRequest.resourceIdentifier = resourceIdentifier
        policyEvaluationRequest.subjectAttributes = subjectAttributes
        return policyEvaluationRequest
    }

    fun createEvalRequest(
        subjectIdentifier: String,
        site: String
    ): PolicyEvaluationRequestV1 {
        return createEvalRequest("GET", subjectIdentifier, "/secured-by-value/sites/$site", null)
    }

    fun createEvalRequest(
        subject: BaseSubject,
        site: String
    ): PolicyEvaluationRequestV1 {
        return createEvalRequest("GET", subject.subjectIdentifier, "/secured-by-value/sites/$site", null)
    }

    fun sendEvaluationRequest(
        restTemplate: RestTemplate,
        headers: HttpHeaders,
        randomEvalRequest: PolicyEvaluationRequestV1
    ): ResponseEntity<PolicyEvaluationResult> {
        return restTemplate.postForEntity(
            zoneFactory.acsBaseURL + ACS_POLICY_EVAL_API_PATH,
            HttpEntity(randomEvalRequest, headers), PolicyEvaluationResult::class.java
        )
    }

    fun deletePolicySet(
        restTemplate: RestTemplate,
        acsUrl: String,
        testPolicyName: String?
    ) {
        if (testPolicyName != null) {
            restTemplate.delete(acsUrl + ACS_POLICY_SET_API_PATH + testPolicyName)
        }
    }

    fun deletePolicySet(
        restTemplate: RestTemplate,
        acsUrl: String,
        testPolicyName: String?,
        headers: HttpHeaders
    ) {
        if (testPolicyName != null) {
            restTemplate.exchange(
                acsUrl + ACS_POLICY_SET_API_PATH + testPolicyName, HttpMethod.DELETE,
                HttpEntity<Any>(headers), String::class.java
            )
        }
    }

    fun listPolicySets(
        restTemplate: RestTemplate,
        acsUrl: String,
        headers: HttpHeaders
    ): Array<PolicySet> {
        val uri = URI.create(acsUrl + ACS_POLICY_SET_API_PATH)
        val response = restTemplate.exchange(
            uri, HttpMethod.GET, HttpEntity<Any>(headers),
            Array<PolicySet>::class.java
        )
        return response.body
    }

    @Throws(Exception::class)
    fun deletePolicySets(
        restTemplate: RestTemplate,
        acsUrl: String,
        headers: HttpHeaders
    ) {
        val policySets = listPolicySets(restTemplate, acsUrl, headers)
        for (policySet in policySets) {
            deletePolicySet(restTemplate, acsUrl, policySet.name, headers)
        }
    }
}
