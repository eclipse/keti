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

package org.eclipse.keti.acs.policy.evaluation.cache;

import static org.eclipse.keti.acs.testutils.XFiles.AGENT_MULDER;
import static org.eclipse.keti.acs.testutils.XFiles.XFILES_ID;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import java.util.Collections;
import java.util.LinkedHashSet;

import org.eclipse.keti.acs.rest.PolicyEvaluationRequestV1;
import org.testng.annotations.Test;

public class PolicyEvaluationRequestCacheKeyTest {
    private static final String ZONE_NAME = "testzone1";
    private static final String ACTION_GET = "GET";
    private static final String POLICY_ONE = "policyOne";
    private static final LinkedHashSet<String> EVALUATION_ORDER_POLICYONE = new LinkedHashSet<>(
            Collections.singleton(POLICY_ONE));

    @Test
    public void testKeyByRequest() {
        PolicyEvaluationRequestV1 request = new PolicyEvaluationRequestV1();
        request.setAction(ACTION_GET);
        request.setSubjectIdentifier(AGENT_MULDER);
        request.setResourceIdentifier(XFILES_ID);
        request.setPolicySetsEvaluationOrder(EVALUATION_ORDER_POLICYONE);

        PolicyEvaluationRequestCacheKey key = new PolicyEvaluationRequestCacheKey(request, ZONE_NAME);

        assertEquals(key.getZoneId(), ZONE_NAME);
        assertEquals(key.getSubjectId(), request.getSubjectIdentifier());
        assertEquals(key.getResourceId(), request.getResourceIdentifier());
        assertEquals(key.getPolicySetIds(), request.getPolicySetsEvaluationOrder());
        assertEquals(key.getRequest(), request);
    }

    @Test
    public void testKeyByRequestWithEmptyPolicySetEvaluationOrder() {
        PolicyEvaluationRequestV1 request = new PolicyEvaluationRequestV1();
        request.setAction(ACTION_GET);
        request.setSubjectIdentifier(AGENT_MULDER);
        request.setResourceIdentifier(XFILES_ID);

        PolicyEvaluationRequestCacheKey key = new PolicyEvaluationRequestCacheKey(request, ZONE_NAME);

        assertEquals(key.getZoneId(), ZONE_NAME);
        assertEquals(key.getSubjectId(), request.getSubjectIdentifier());
        assertEquals(key.getResourceId(), request.getResourceIdentifier());
        assertEquals(key.getPolicySetIds(), PolicyEvaluationRequestCacheKey.EVALUATION_ORDER_ANY_POLICY_SET_KEY);
        assertEquals(key.getRequest(), request);
    }


    @Test
    public void testKeyEqualsForSameRequests() {
        PolicyEvaluationRequestV1 request = new PolicyEvaluationRequestV1();
        request.setAction(ACTION_GET);
        request.setSubjectIdentifier(AGENT_MULDER);
        request.setResourceIdentifier(XFILES_ID);
        request.setPolicySetsEvaluationOrder(EVALUATION_ORDER_POLICYONE);

        PolicyEvaluationRequestCacheKey key = new PolicyEvaluationRequestCacheKey(request, ZONE_NAME);

        PolicyEvaluationRequestV1 otherRequest = new PolicyEvaluationRequestV1();
        otherRequest.setAction(ACTION_GET);
        otherRequest.setSubjectIdentifier(AGENT_MULDER);
        otherRequest.setResourceIdentifier(XFILES_ID);
        otherRequest.setPolicySetsEvaluationOrder(EVALUATION_ORDER_POLICYONE);

        PolicyEvaluationRequestCacheKey otherKey = new PolicyEvaluationRequestCacheKey(otherRequest, ZONE_NAME);

        assertTrue(key.equals(otherKey));
    }

    @Test
    public void testKeyEqualsForDifferentRequests() {
        PolicyEvaluationRequestV1 request = new PolicyEvaluationRequestV1();
        request.setAction(ACTION_GET);
        request.setSubjectIdentifier(AGENT_MULDER);
        request.setResourceIdentifier(XFILES_ID);
        request.setPolicySetsEvaluationOrder(EVALUATION_ORDER_POLICYONE);

        PolicyEvaluationRequestCacheKey key = new PolicyEvaluationRequestCacheKey(request, ZONE_NAME);

        PolicyEvaluationRequestV1 otherRequest = new PolicyEvaluationRequestV1();
        otherRequest.setAction(ACTION_GET);
        otherRequest.setSubjectIdentifier(AGENT_MULDER);
        otherRequest.setResourceIdentifier(XFILES_ID);

        PolicyEvaluationRequestCacheKey otherKey = new PolicyEvaluationRequestCacheKey(otherRequest, ZONE_NAME);

        assertFalse(key.equals(otherKey));
    }

    @Test
    public void testToRedisKey() {
        PolicyEvaluationRequestV1 request = new PolicyEvaluationRequestV1();
        PolicyEvaluationRequestCacheKey key = new PolicyEvaluationRequestCacheKey(request, ZONE_NAME);
        assertEquals(key.toDecisionKey(), ZONE_NAME + ":*:*:" + Integer.toHexString(request.hashCode()));
    }
}
