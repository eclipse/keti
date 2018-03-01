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

package org.eclipse.keti.test.utils;

import static org.eclipse.keti.test.utils.ACSTestUtil.ACS_VERSION;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Random;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.keti.acs.model.Attribute;
import org.eclipse.keti.acs.model.PolicySet;
import org.eclipse.keti.acs.rest.BaseSubject;
import org.eclipse.keti.acs.rest.PolicyEvaluationRequestV1;
import org.eclipse.keti.acs.rest.PolicyEvaluationResult;

@Component
public class PolicyHelper {
    public static final String ACS_POLICY_SET_API_PATH = ACS_VERSION + "/policy-set/";
    public static final String ACS_POLICY_EVAL_API_PATH = ACS_VERSION + "/policy-evaluation";

    public static final String DEFAULT_ACTION = "GET";
    public static final String NOT_MATCHING_ACTION = "HEAD";
    public static final String PREDIX_ZONE_ID = "Predix-Zone-Id";

    private static final String[] ACTIONS = { "GET", "POST", "DELETE", "PUT" };

    @Autowired
    private ZoneFactory zoneFactory;

    public String setTestPolicy(final RestTemplate acs, final HttpHeaders headers, final String endpoint,
            final String policyFile) throws JsonParseException, JsonMappingException, IOException {

        PolicySet policySet = new ObjectMapper().readValue(new File(policyFile), PolicySet.class);
        String policyName = policySet.getName();
        acs.put(endpoint + ACS_POLICY_SET_API_PATH + policyName, new HttpEntity<>(policySet, headers));
        return policyName;
    }

    public CreatePolicyStatus createPolicySet(final String policyFile, final RestTemplate restTemplate,
            final HttpHeaders headers) {
        PolicySet policySet;
        try {
            policySet = new ObjectMapper().readValue(new File(policyFile), PolicySet.class);
            String policyName = policySet.getName();
            restTemplate.put(zoneFactory.getAcsBaseURL() + ACS_POLICY_SET_API_PATH + policyName,
                    new HttpEntity<>(policySet, headers));
            return CreatePolicyStatus.SUCCESS;
        } catch (IOException e) {
            return CreatePolicyStatus.JSON_ERROR;
        } catch (HttpClientErrorException httpException) {
            return httpException.getStatusCode() != null
                    && httpException.getStatusCode().equals(HttpStatus.UNPROCESSABLE_ENTITY)
                            ? CreatePolicyStatus.INVALID_POLICY_SET : CreatePolicyStatus.ACS_ERROR;
        } catch (RestClientException e) {
            return CreatePolicyStatus.ACS_ERROR;
        }
    }

    public ResponseEntity<PolicySet> getPolicySet(final String policyName, final RestTemplate restTemplate,
            final String endpoint) {
        ResponseEntity<PolicySet> policySetResponse = restTemplate
                .getForEntity(endpoint + ACS_POLICY_SET_API_PATH + policyName, PolicySet.class);
        return policySetResponse;
    }

    public PolicyEvaluationRequestV1 createRandomEvalRequest() {
        Random r = new Random(System.currentTimeMillis());
        Set<Attribute> subjectAttributes = Collections.emptySet();
        return this.createEvalRequest(ACTIONS[r.nextInt(4)], String.valueOf(r.nextLong()),
                "/alarms/sites/" + String.valueOf(r.nextLong()), subjectAttributes);
    }

    public PolicyEvaluationRequestV1 createMultiplePolicySetsEvalRequest(final String action,
            final String subjectIdentifier, final String resourceIdentifier, final Set<Attribute> subjectAttributes,
            final LinkedHashSet<String> policySetIds) {
        PolicyEvaluationRequestV1 policyEvaluationRequest = new PolicyEvaluationRequestV1();
        policyEvaluationRequest.setAction(action);
        policyEvaluationRequest.setSubjectIdentifier(subjectIdentifier);
        policyEvaluationRequest.setResourceIdentifier(resourceIdentifier);
        policyEvaluationRequest.setSubjectAttributes(subjectAttributes);
        policyEvaluationRequest.setPolicySetsEvaluationOrder(policySetIds);
        return policyEvaluationRequest;
    }

    public PolicyEvaluationRequestV1 createMultiplePolicySetsEvalRequest(final String subjectIdentifier,
            final String site, final LinkedHashSet<String> policySetIds) {
        return createMultiplePolicySetsEvalRequest("GET", subjectIdentifier, "/secured-by-value/sites/" + site, null,
                policySetIds);
    }

    public PolicyEvaluationRequestV1 createMultiplePolicySetsEvalRequest(final BaseSubject subject, final String site,
            final LinkedHashSet<String> policySetIds) {
        return createMultiplePolicySetsEvalRequest("GET", subject.getSubjectIdentifier(),
                "/secured-by-value/sites/" + site, null, policySetIds);
    }

    public PolicyEvaluationRequestV1 createEvalRequest(final String action, final String subjectIdentifier,
            final String resourceIdentifier, final Set<Attribute> subjectAttributes) {
        PolicyEvaluationRequestV1 policyEvaluationRequest = new PolicyEvaluationRequestV1();
        policyEvaluationRequest.setAction(action);
        policyEvaluationRequest.setSubjectIdentifier(subjectIdentifier);
        policyEvaluationRequest.setResourceIdentifier(resourceIdentifier);
        policyEvaluationRequest.setSubjectAttributes(subjectAttributes);
        return policyEvaluationRequest;
    }

    public PolicyEvaluationRequestV1 createEvalRequest(final String subjectIdentifier, final String site) {
        return createEvalRequest("GET", subjectIdentifier, "/secured-by-value/sites/" + site, null);
    }

    public PolicyEvaluationRequestV1 createEvalRequest(final BaseSubject subject, final String site) {
        return createEvalRequest("GET", subject.getSubjectIdentifier(), "/secured-by-value/sites/" + site, null);
    }

    public ResponseEntity<PolicyEvaluationResult> sendEvaluationRequest(final RestTemplate restTemplate,
            final HttpHeaders headers, final PolicyEvaluationRequestV1 randomEvalRequest) {
        ResponseEntity<PolicyEvaluationResult> evaluationResponse = restTemplate.postForEntity(
                this.zoneFactory.getAcsBaseURL() + ACS_POLICY_EVAL_API_PATH,
                new HttpEntity<>(randomEvalRequest, headers), PolicyEvaluationResult.class);
        return evaluationResponse;
    }

    public void deletePolicySet(final RestTemplate restTemplate, final String acsUrl, final String testPolicyName) {
        if (testPolicyName != null) {
            restTemplate.delete(acsUrl + ACS_POLICY_SET_API_PATH + testPolicyName);
        }
    }

    public void deletePolicySet(final RestTemplate restTemplate, final String acsUrl, final String testPolicyName,
            final HttpHeaders headers) {
        if (testPolicyName != null) {
            restTemplate.exchange(acsUrl + ACS_POLICY_SET_API_PATH + testPolicyName, HttpMethod.DELETE,
                    new HttpEntity<>(headers), String.class);
        }
    }

    public PolicySet[] listPolicySets(final RestTemplate restTemplate, final String acsUrl, final HttpHeaders headers) {
        URI uri = URI.create(acsUrl + ACS_POLICY_SET_API_PATH);
        ResponseEntity<PolicySet[]> response = restTemplate.exchange(uri, HttpMethod.GET, new HttpEntity<>(headers),
                PolicySet[].class);
        return response.getBody();
    }

    public void deletePolicySets(final RestTemplate restTemplate, final String acsUrl, final HttpHeaders headers)
            throws Exception {
        PolicySet[] policySets = listPolicySets(restTemplate, acsUrl, headers);
        for (PolicySet policySet : policySets) {
            deletePolicySet(restTemplate, acsUrl, policySet.getName(), headers);
        }
    }
}
