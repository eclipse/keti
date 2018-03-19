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
 *******************************************************************************/

package com.ge.predix.integration.test;

import static org.eclipse.keti.integration.test.SubjectResourceFixture.MARISSA_V1;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.keti.acs.rest.PolicyEvaluationRequestV1;
import org.eclipse.keti.acs.rest.PolicyEvaluationResult;
import org.eclipse.keti.test.utils.ACSITSetUpFactory;
import org.eclipse.keti.test.utils.ACSTestUtil;
import org.eclipse.keti.test.utils.PolicyHelper;
import org.eclipse.keti.test.utils.ZoneFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

import com.nurego.Nurego;
import com.nurego.model.Entitlement;
import com.nurego.model.Subscription;

/* TODO
 * Reverted the implementation where we do not use ACSITSetupfactory for setting up the testcase because of the below
 * error with nurego subscription model
 *
 * The metering-related test failure in the latest run of the acs-integration-pullrequest job for the predix-cloud
 * and predix-cloud-graph profiles is because test-zone-pipe3 is the Nurego subscription ID used in CF but what's
 * actually generated here is ACSITSetUpFactoryPublic<UUID>. We should either keep the subscription ID unchanged or
 * somehow use Nurego APIs to dynamically create the subscription, run tests in this class, then delete the
 * subscription  (the former is probably simpler).
 *
 * Need to change the implementation in sync with other test cases
 * */

@SuppressWarnings({ "nls" })
@ContextConfiguration("classpath:integration-test-spring-context.xml")
public class ACSMeteringIT extends AbstractTestNGSpringContextTests {
    private static final Logger LOGGER = LoggerFactory.getLogger(ACSMeteringIT.class);

    @Autowired
    private ACSITSetUpFactory acsitSetUpFactory;

    @Autowired
    private ZoneFactory zoneFactory;

    @Autowired
    private PolicyHelper policyHelper;

    @Value("${ORG_ID:050e3f85-4706-4d88-8e87-7488cc84089c}")
    private String organizationId;

    @Value("${PLAN_ID:pla_b70d-6248-4a0b-8018-9fc6b9de29e6}")
    private String planId;

    @Value("${NUREGO_API_URL}")
    private String nuregoApiBase;

    @Value("${NUREGO_USERNAME}")
    private String nuregoUsername;

    @Value("${NUREGO_PASSWORD}")
    private String nuregoPassword;

    @Value("${NUREGO_INSTANCE_ID}")
    private String nuregoInstanceId;

    @Value("${STEADY_STATE_SLEEP_MS:10000}")
    private int steadyStateSleepMS;

    @Value("${zone1UaaUrl}/oauth/token")
    private String zoneTrustedIssuer;

    private static final String POLICY_UPDATE_FEATURE_ID = "policyset_update";
    private static final String POLICY_EVAL_FEATURE_ID = "policy_eval";
    private static final int MAX_ITERATIONS = 10;
    private static final int NUREGO_UPDATE_SLEEP_MS = 1000;
    private Subscription subscription;

    private String zoneId;
    private String acsUrl;
    private OAuth2RestTemplate acsAdminRestTemplate;

    @BeforeClass
    public void setup() throws Exception {

        this.zoneId = "test-zone-pipe3";
        this.acsAdminRestTemplate = this.acsitSetUpFactory.getAcsAdminRestTemplate(this.zoneId);
        this.zoneFactory.createTestZone(this.acsAdminRestTemplate, this.zoneId,
                Collections.singletonList(this.zoneTrustedIssuer));
        this.acsUrl = this.zoneFactory.getAcsBaseURL();

        Nurego.setApiBase(this.nuregoApiBase);
        Nurego.setApiCredentials(nuregoUsername, nuregoPassword, nuregoInstanceId);

        // Busy wait for the meter to achieve steady state for POLICY_UPDATE and POLICY_EVAL
        waitForSteadyStateEntitlementUsage(POLICY_UPDATE_FEATURE_ID, this.zoneId);
        waitForSteadyStateEntitlementUsage(POLICY_EVAL_FEATURE_ID, this.zoneId);
    }

    @Test(enabled = true)
    public void testACSMetering() throws Exception {
        String testPolicyName = null;
        PolicyEvaluationRequestV1 policyEvaluationRequest = this.policyHelper
                .createEvalRequest(MARISSA_V1.getSubjectIdentifier(), "sanramon");
        try {
            // Get meter readings before
            Double beforePolicyUpdateMeterCount = getEntitlementUsageByFeatureId(POLICY_UPDATE_FEATURE_ID, this.zoneId);
            Double beforePolicyEvalMeterCount = getEntitlementUsageByFeatureId(POLICY_EVAL_FEATURE_ID, this.zoneId);

            LOGGER.info("POLICY UPDATE USAGE BEFORE:" + beforePolicyUpdateMeterCount);
            LOGGER.info("POLICY EVAL USAGE BEFORE:" + beforePolicyEvalMeterCount);

            String policyFile = "src/test/resources/single-site-based-policy-set.json";
            testPolicyName = this.policyHelper.setTestPolicy(this.acsAdminRestTemplate, getZoneHeaders(), this.acsUrl,
                    policyFile);
            ResponseEntity<PolicyEvaluationResult> evalResponse = this.acsAdminRestTemplate.postForEntity(
                    this.acsUrl + PolicyHelper.ACS_POLICY_EVAL_API_PATH,
                    new HttpEntity<>(policyEvaluationRequest, getZoneHeaders()), PolicyEvaluationResult.class);

            Assert.assertEquals(evalResponse.getStatusCode(), HttpStatus.OK);

            // Nurego server seems to have a lag before the counts are updated
            // Wait for Nurego to be updated before proceeding with assertion
            Double afterPolicyUpdateMeterCount = 0.0;
            Double afterPolicyEvalMeterCount = 0.0;
            Double updateMeterCount = 0.0;
            Double evalMeterCount = 0.0;
            for (int i = 0; i < MAX_ITERATIONS; i++) {
                Thread.sleep(NUREGO_UPDATE_SLEEP_MS);
                afterPolicyUpdateMeterCount = getEntitlementUsageByFeatureId(POLICY_UPDATE_FEATURE_ID, this.zoneId);
                afterPolicyEvalMeterCount = getEntitlementUsageByFeatureId(POLICY_EVAL_FEATURE_ID, this.zoneId);

                if (isMeterUpdated(beforePolicyUpdateMeterCount, afterPolicyUpdateMeterCount)
                        && isMeterUpdated(beforePolicyEvalMeterCount, afterPolicyEvalMeterCount)) {
                    LOGGER.debug("POLICY UPDATE USAGE AFTER: " + afterPolicyUpdateMeterCount);
                    LOGGER.debug("POLICY EVAL USAGE AFTER: " + afterPolicyEvalMeterCount);
                    break;
                }
            }

            // Assert metering counts incremented by 1
            updateMeterCount = afterPolicyUpdateMeterCount - beforePolicyUpdateMeterCount;
            evalMeterCount = afterPolicyEvalMeterCount - beforePolicyEvalMeterCount;
            Assert.assertEquals(updateMeterCount, 1.0);
            Assert.assertEquals(evalMeterCount, 1.0);
        } finally {
            if (testPolicyName != null) {
                this.policyHelper.deletePolicySet(this.acsAdminRestTemplate, this.acsUrl, testPolicyName,
                        getZoneHeaders());
            }
        }
    }

    private HttpHeaders getZoneHeaders() {
        HttpHeaders headers = ACSTestUtil.httpHeaders();
        headers.set("Predix-Zone-Id", this.zoneId);
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

    private boolean waitForSteadyStateEntitlementUsage(final String featureId, final String subscriptionId)
            throws Exception {
        double usageBeforeWait = 0.0;
        double usageAfterWait = 0.0;
        int iteration;

        for (iteration = 0; iteration < MAX_ITERATIONS; iteration++) {
            usageBeforeWait = getEntitlementUsageByFeatureId(featureId, subscriptionId);

            Thread.sleep(steadyStateSleepMS);

            usageAfterWait = getEntitlementUsageByFeatureId(featureId, subscriptionId);

            if (isUsageSteady(usageBeforeWait, usageAfterWait)) {
                String msg = String.format("Meter has reached a steady state of '%f' for '%s' with subscription '%s'",
                        usageAfterWait, POLICY_UPDATE_FEATURE_ID, this.planId);
                LOGGER.info(msg);
                break;
            }
        }

        return isUsageSteady(usageBeforeWait, usageAfterWait);
    }

    private boolean isMeterUpdated(final double readBeforeSleep, final double readAfterSleep) {
        return !isUsageSteady(readBeforeSleep, readAfterSleep);
    }

    private boolean isUsageSteady(final double readBeforeSleep, final double readAfterSleep) {
        return readBeforeSleep == readAfterSleep;
    }

    @AfterClass
    public void cleanUp() {
        this.zoneFactory.deleteZone(this.acsAdminRestTemplate, this.zoneId);
        // cancelNuregoSubscription();
    }

    // This method is not being used currently, as we are reusing the subscription and always asserting that the
    // difference between usage amount would be one. If you want to create a new subscription please uncomment
    // the following code.
    @SuppressWarnings("unused")
    private void createNuregoSubscription(final String subscriptionId) {
        Map<String, Object> params = new HashMap<>();
        params.put("plan_id", this.planId);
        params.put("external_subscription_id", subscriptionId);
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
