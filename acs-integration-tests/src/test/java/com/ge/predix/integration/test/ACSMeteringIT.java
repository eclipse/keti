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
package com.ge.predix.integration.test;

import static com.ge.predix.integration.test.SubjectResourceFixture.MARISSA_V1;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.ge.predix.acs.rest.PolicyEvaluationRequestV1;
import com.ge.predix.acs.rest.PolicyEvaluationResult;
import com.ge.predix.acs.rest.Zone;
import com.ge.predix.test.utils.ACSRestTemplateFactory;
import com.ge.predix.test.utils.PolicyHelper;
import com.ge.predix.test.utils.ZacTestUtil;
import com.ge.predix.test.utils.ZoneHelper;
import com.nurego.Nurego;
import com.nurego.model.Entitlement;
import com.nurego.model.Subscription;

@SuppressWarnings({ "nls" })
@ContextConfiguration("classpath:integration-test-spring-context.xml")
@Test
public class ACSMeteringIT extends AbstractTestNGSpringContextTests {

    @Autowired
    private ACSRestTemplateFactory acsRestTemplateFactory;

    @Autowired
    private ZoneHelper zoneHelper;

    @Autowired
    private PolicyHelper policyHelper;

    @Value("${ORG_ID:ff85feb9-be02-4a73-9b13-9e1970abf09c}")
    private String organizationId;

    @Value("${PLAN_ID:pla_1ba8-5fe8-474f-8211-163649417d8e}")
    private String planId;

    @Value("${NUREGO_API_URL}")
    private String nuregoApiBase;

    @Value("${NUREGO_API_KEY}")
    private String nuregoApiKey;

    private static final String POLICY_UPDATE_FEATURE_ID = "policyset_update";
    private static final String POLICY_EVAL_FEATURE_ID = "policy_eval";
    private Subscription subscription;

    private String zoneId;

    private String zoneUrl;

    private Zone createPrimaryTestZone;

    @Autowired
    private ZacTestUtil zacTestUtil;

    @BeforeClass
    public void setup() throws JsonParseException, JsonMappingException, IOException {
        this.zacTestUtil.assumeZacServerAvailable();

        this.createPrimaryTestZone = this.zoneHelper.createPrimaryTestZone();
        this.zoneId = this.createPrimaryTestZone.getSubdomain();
        this.zoneUrl = this.zoneHelper.getZone1Url();
        Nurego.setApiBase(this.nuregoApiBase);
        Nurego.apiKey = this.nuregoApiKey;

        // createNuregoSubscription(this.createPrimaryTestZone.getSubdomain());
    }

    public void testACSMetering() throws Exception {
        String testPolicyName = null;
        PolicyEvaluationRequestV1 policyEvaluationRequest = this.policyHelper
                .createEvalRequest(MARISSA_V1.getSubjectIdentifier(), "sanramon");
        try {
            // Get meter readings before
            Double beforePolicyUpdateMeterCount = getEntitlementUsageByFeatureId(POLICY_UPDATE_FEATURE_ID, this.zoneId);
            Double beforePolicyEvalMeterCount = getEntitlementUsageByFeatureId(POLICY_EVAL_FEATURE_ID, this.zoneId);

            System.out.println("POLICY UPDATE USAGE BEFORE:" + beforePolicyUpdateMeterCount);
            System.out.println("POLICY EVAL USAGE BEFORE:" + beforePolicyEvalMeterCount);

            OAuth2RestTemplate acsRestTemplate = this.acsRestTemplateFactory.getACSTemplateWithPolicyScope();
            String policyFile = "src/test/resources/single-site-based-policy-set.json";
            testPolicyName = this.policyHelper.setTestPolicy(acsRestTemplate, getZoneHeaders(), this.zoneUrl,
                    policyFile);
            ResponseEntity<PolicyEvaluationResult> evalResponse = acsRestTemplate.postForEntity(
                    this.zoneUrl + PolicyHelper.ACS_POLICY_EVAL_API_PATH,
                    new HttpEntity<>(policyEvaluationRequest, getZoneHeaders()), PolicyEvaluationResult.class);

            Assert.assertEquals(evalResponse.getStatusCode(), HttpStatus.OK);

            //Nurego server seems to have a lag before the counts are updated
            Thread.sleep(3000);

            Double afterPolicyUpdateMeterCount = getEntitlementUsageByFeatureId(POLICY_UPDATE_FEATURE_ID, this.zoneId);
            Double afterPolicyEvalMeterCount = getEntitlementUsageByFeatureId(POLICY_EVAL_FEATURE_ID, this.zoneId);

            System.out.println("POLICY UPDATE USAGE AFTER:" + afterPolicyUpdateMeterCount);
            System.out.println("POLICY EVAL USAGE AFTER:" + afterPolicyEvalMeterCount);

            // Assert metering counts incremented by 1
            Assert.assertEquals(afterPolicyUpdateMeterCount - beforePolicyUpdateMeterCount, 1.0);
            Assert.assertEquals(afterPolicyEvalMeterCount - beforePolicyEvalMeterCount, 1.0);
        } finally {
            if (testPolicyName != null) {
                this.policyHelper.deletePolicySet(testPolicyName);
            }
        }
    }

    private HttpHeaders getZoneHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Predix-Zone-Id", this.createPrimaryTestZone.getSubdomain());
        return headers;
    }

    private Entitlement getEntitlementByFeatureId(final String featureId, final String subscriptionId)
            throws Exception {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("provider", "cloud-foundry");
        params.put("plan_id", this.planId);
        params.put("external_subscription_id", subscriptionId);
        List<Entitlement> entitlements = Entitlement.retrieve(subscriptionId, params).getData();
        for (Entitlement entitlement : entitlements) {
            if (entitlement.getFeatureId().equals(featureId)) {
                System.out.println("Matched feature entitlements:" + entitlement.toString());
                return entitlement;
            }
        }

        return null;
    }

    private Double getEntitlementUsageByFeatureId(final String featureId, final String subscriptionId)
            throws Exception {
        Entitlement entitlement = getEntitlementByFeatureId(featureId, subscriptionId);
        if (entitlement == null) {
            throw new IllegalArgumentException(String.format("Feature '%s' does not exist.", featureId));
        }
        return entitlement.getCurrentUsedAmount();
    }

    @AfterClass
    public void cleanUp() {
        this.zoneHelper.deleteZone(this.createPrimaryTestZone.getSubdomain());
        // cancelNuregoSubscription();
    }

    // This method is not being used currently, as we are reusing the subscription and always asserting that the
    // difference between usage amount would be one. If you want to create a new subscription please uncomment
    // the following code.
    @SuppressWarnings("unused")
    private void createNuregoSubscription(String zoneId) {
        Map<String, Object> params = new HashMap<>();
        params.put("plan_id", this.planId);
        params.put("external_subscription_id", zoneId);
        params.put("provider", "cloud-foundry");
        try {
            this.subscription = Subscription.create(this.organizationId, params);
            System.out.println(" Subscription Id: " + this.subscription.getId());
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail("Failed to create Nurego Subscription.");
        }
    }

    @SuppressWarnings("unused")
    private void cancelNuregoSubscription() {
        try {
            Subscription.cancel(this.organizationId, this.subscription.getId());
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail("Failed to cancel Nurego Subscription.");
        }
    }

}
