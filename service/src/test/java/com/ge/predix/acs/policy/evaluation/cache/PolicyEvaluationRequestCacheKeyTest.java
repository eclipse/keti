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

package com.ge.predix.acs.policy.evaluation.cache;

import com.ge.predix.acs.rest.PolicyEvaluationRequestV1;
import org.testng.annotations.Test;

import java.util.LinkedHashSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.ge.predix.acs.testutils.XFiles.AGENT_MULDER;
import static com.ge.predix.acs.testutils.XFiles.XFILES_ID;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

public class PolicyEvaluationRequestCacheKeyTest {
    public static final String ZONE_NAME = "testzone1";
    public static final String ACTION_GET = "GET";

    @Test
    public void testBuild() {
        String subjectId = AGENT_MULDER;
        String resourceId = XFILES_ID;
        LinkedHashSet<String> policyEvaluationOrder = Stream.of("policyOne")
                .collect(Collectors.toCollection(LinkedHashSet::new));
        PolicyEvaluationRequestCacheKey key = new PolicyEvaluationRequestCacheKey.Builder().zoneId(ZONE_NAME)
                .resourceId(resourceId).subjectId(subjectId).policySetIds(policyEvaluationOrder).build();

        assertEquals(key.getZoneId(), ZONE_NAME);
        assertEquals(key.getSubjectId(), subjectId);
        assertEquals(key.getResourceId(), resourceId);
        assertEquals(key.getPolicySetIds(), policyEvaluationOrder);
        assertNull(key.getRequest());
    }

    @Test
    public void testBuildByRequest() {
        PolicyEvaluationRequestV1 request = new PolicyEvaluationRequestV1();
        request.setAction(ACTION_GET);
        request.setSubjectIdentifier(AGENT_MULDER);
        request.setResourceIdentifier(XFILES_ID);
        request.setPolicySetsEvaluationOrder(
                Stream.of("policyOne").collect(Collectors.toCollection(LinkedHashSet::new)));
        PolicyEvaluationRequestCacheKey key = new PolicyEvaluationRequestCacheKey.Builder().zoneId(ZONE_NAME)
                .request(request).build();

        assertEquals(key.getZoneId(), ZONE_NAME);
        assertEquals(key.getSubjectId(), request.getSubjectIdentifier());
        assertEquals(key.getResourceId(), request.getResourceIdentifier());
        assertEquals(key.getPolicySetIds(), request.getPolicySetsEvaluationOrder());
        assertEquals(key.getRequest(), request);
    }

    @Test
    public void testBuildByRequestAndPolicySetEvaluationOrder() {
        PolicyEvaluationRequestV1 request = new PolicyEvaluationRequestV1();
        request.setAction(ACTION_GET);
        request.setSubjectIdentifier(AGENT_MULDER);
        request.setResourceIdentifier(XFILES_ID);
        LinkedHashSet<String> policyEvaluationOrder = Stream.of("policyOne")
                .collect(Collectors.toCollection(LinkedHashSet::new));
        PolicyEvaluationRequestCacheKey key = new PolicyEvaluationRequestCacheKey.Builder().zoneId(ZONE_NAME)
                .policySetIds(policyEvaluationOrder).request(request).build();

        assertEquals(key.getZoneId(), ZONE_NAME);
        assertEquals(key.getSubjectId(), request.getSubjectIdentifier());
        assertEquals(key.getResourceId(), request.getResourceIdentifier());
        assertEquals(key.getPolicySetIds(), policyEvaluationOrder);
        assertEquals(key.getRequest(), request);
    }

    @SuppressWarnings("unused")
    @Test(expectedExceptions = IllegalStateException.class)
    public void testIllegalStateExceptionForSettingPolicySetIds() {
        PolicyEvaluationRequestV1 request = new PolicyEvaluationRequestV1();
        request.setPolicySetsEvaluationOrder(
                Stream.of("policyOne").collect(Collectors.toCollection(LinkedHashSet::new)));
        PolicyEvaluationRequestCacheKey policyEvaluationRequestCacheKey =
            new PolicyEvaluationRequestCacheKey.Builder().zoneId(ZONE_NAME)
                                                         .request(request)
                                                         .policySetIds(request.getPolicySetsEvaluationOrder())
                                                         .build();
    }

    @SuppressWarnings("unused")
    @Test(expectedExceptions = IllegalStateException.class)
    public void testIllegalStateExceptionForSettingSubjectId() {
        PolicyEvaluationRequestV1 request = new PolicyEvaluationRequestV1();
        PolicyEvaluationRequestCacheKey policyEvaluationRequestCacheKey =
            new PolicyEvaluationRequestCacheKey.Builder().zoneId(ZONE_NAME)
                                                         .request(request)
                                                         .subjectId("subject")
                                                         .build();
    }

    @SuppressWarnings("unused")
    @Test(expectedExceptions = IllegalStateException.class)
    public void testIllegalStateExceptionForSettingResourceId() {
        PolicyEvaluationRequestV1 request = new PolicyEvaluationRequestV1();
        PolicyEvaluationRequestCacheKey policyEvaluationRequestCacheKey =
            new PolicyEvaluationRequestCacheKey.Builder().zoneId(ZONE_NAME)
                                                         .request(request)
                                                         .resourceId("resource")
                                                         .build();
    }

    @Test
    public void testKeyEqualsForSameRequests() {
        PolicyEvaluationRequestV1 request = new PolicyEvaluationRequestV1();
        request.setAction(ACTION_GET);
        request.setSubjectIdentifier(AGENT_MULDER);
        request.setResourceIdentifier(XFILES_ID);
        request.setPolicySetsEvaluationOrder(
                Stream.of("policyOne").collect(Collectors.toCollection(LinkedHashSet::new)));
        PolicyEvaluationRequestCacheKey key = new PolicyEvaluationRequestCacheKey.Builder().zoneId(ZONE_NAME)
                .request(request).build();

        PolicyEvaluationRequestV1 otherRequest = new PolicyEvaluationRequestV1();
        otherRequest.setAction(ACTION_GET);
        otherRequest.setSubjectIdentifier(AGENT_MULDER);
        otherRequest.setResourceIdentifier(XFILES_ID);
        otherRequest.setPolicySetsEvaluationOrder(
                Stream.of("policyOne").collect(Collectors.toCollection(LinkedHashSet::new)));
        PolicyEvaluationRequestCacheKey otherKey = new PolicyEvaluationRequestCacheKey.Builder().zoneId(ZONE_NAME)
                .request(otherRequest).build();
        assertTrue(key.equals(otherKey));
    }

    @Test
    public void testKeyEqualsForDifferentRequests() {
        PolicyEvaluationRequestV1 request = new PolicyEvaluationRequestV1();
        request.setAction(ACTION_GET);
        request.setSubjectIdentifier(AGENT_MULDER);
        request.setResourceIdentifier(XFILES_ID);
        request.setPolicySetsEvaluationOrder(
                Stream.of("policyOne").collect(Collectors.toCollection(LinkedHashSet::new)));
        PolicyEvaluationRequestCacheKey key = new PolicyEvaluationRequestCacheKey.Builder().zoneId(ZONE_NAME)
                .request(request).build();

        PolicyEvaluationRequestV1 otherRequest = new PolicyEvaluationRequestV1();
        otherRequest.setAction(ACTION_GET);
        otherRequest.setSubjectIdentifier(AGENT_MULDER);
        otherRequest.setResourceIdentifier(XFILES_ID);
        PolicyEvaluationRequestCacheKey otherKey = new PolicyEvaluationRequestCacheKey.Builder().zoneId(ZONE_NAME)
                .request(otherRequest).build();
        assertFalse(key.equals(otherKey));
    }

    @Test
    public void testToRedisKey() {
        PolicyEvaluationRequestV1 request = new PolicyEvaluationRequestV1();
        PolicyEvaluationRequestCacheKey key = new PolicyEvaluationRequestCacheKey.Builder().zoneId(ZONE_NAME)
                .request(request).build();
        assertEquals(key.toDecisionKey(), ZONE_NAME + ":*:*:" + Integer.toHexString(request.hashCode()));
    }
}
