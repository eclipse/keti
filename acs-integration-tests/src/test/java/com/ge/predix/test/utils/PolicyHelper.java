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
package com.ge.predix.test.utils;

import static com.ge.predix.test.utils.ACSTestUtil.ACS_VERSION;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

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
import com.ge.predix.acs.model.Attribute;
import com.ge.predix.acs.model.PolicySet;
import com.ge.predix.acs.rest.BaseSubject;
import com.ge.predix.acs.rest.PolicyEvaluationRequestV1;
import com.ge.predix.acs.rest.PolicyEvaluationResult;

@Component
public class PolicyHelper {
    public static final String ACS_POLICY_SET_API_PATH = ACS_VERSION + "/policy-set/";
    public static final String ACS_POLICY_EVAL_API_PATH = ACS_VERSION + "/policy-evaluation";

    public static final String DEFAULT_ACTION = "GET";
    public static final String NOT_MATCHING_ACTION = "HEAD";

    private static final String[] ACTIONS = { "GET", "POST", "DELETE", "PUT" };

    @Autowired
    private ACSRestTemplateFactory acsRestTemplateFactory;

    @Autowired
    private ZoneHelper zoneHelper;

    public String setTestPolicy(final RestTemplate acs, final HttpHeaders headers, final String endpoint,
            final String policyFile) throws JsonParseException, JsonMappingException, IOException {

        PolicySet policySet = new ObjectMapper().readValue(new File(policyFile), PolicySet.class);
        String policyName = policySet.getName();
        acs.put(endpoint + ACS_POLICY_SET_API_PATH + policyName, new HttpEntity<>(policySet, headers));
        return policyName;
    }

    public CreatePolicyStatus createPolicySet(final String policyFile, final RestTemplate restTemplate,
            final String zoneUrl) {
        PolicySet policySet;
        try {
            policySet = new ObjectMapper().readValue(new File(policyFile), PolicySet.class);
            String policyName = policySet.getName();
            restTemplate.put(zoneUrl + ACS_POLICY_SET_API_PATH + policyName, policySet);
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

    public CreatePolicyStatus createPolicySet(final String policyFile) {
        RestTemplate acs = this.acsRestTemplateFactory.getACSTemplateWithPolicyScope();
        return createPolicySet(policyFile, acs, this.zoneHelper.getZone1Url());
    }

    public ResponseEntity<PolicySet> getPolicySet(final String policyName, final RestTemplate restTemplate,
            final String endpoint) {
        ResponseEntity<PolicySet> policySetResponse = restTemplate
                .getForEntity(endpoint + ACS_POLICY_SET_API_PATH + policyName, PolicySet.class);
        return policySetResponse;
    }

    public PolicyEvaluationRequestV1 createRandomEvalRequest() {
        Random r = new Random(System.currentTimeMillis());
        List<Attribute> subjectAttributes = new ArrayList<Attribute>();
        return this.createEvalRequest(ACTIONS[r.nextInt(4)], String.valueOf(r.nextLong()),
                "/alarms/sites/" + String.valueOf(r.nextLong()), subjectAttributes);
    }

    public PolicyEvaluationRequestV1 createEvalRequest(final String action, final String subjectIdentifier,
            final String resourceIdentifier, final List<Attribute> subjectAttributes) {
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

    /**
     * @param createRandomEvalRequest
     * @return
     */
    public ResponseEntity<PolicyEvaluationResult> sendEvaluationRequest(final RestTemplate restTemplate,
            final PolicyEvaluationRequestV1 randomEvalRequest) {
        ResponseEntity<PolicyEvaluationResult> evaluationResponse = restTemplate.postForEntity(
                this.zoneHelper.getZone1Url() + ACS_POLICY_EVAL_API_PATH, randomEvalRequest,
                PolicyEvaluationResult.class);
        return evaluationResponse;
    }

    /**
     * @param testPolicyName
     */
    public void deletePolicySet(final String testPolicyName) {
        if (testPolicyName != null) {
            this.acsRestTemplateFactory.getACSTemplateWithPolicyScope()
                    .delete(this.zoneHelper.getZone1Url() + ACS_POLICY_SET_API_PATH + testPolicyName);
        }
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
