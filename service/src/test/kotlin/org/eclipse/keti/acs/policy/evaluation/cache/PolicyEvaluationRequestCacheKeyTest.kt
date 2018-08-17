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

package org.eclipse.keti.acs.policy.evaluation.cache

import org.eclipse.keti.acs.rest.PolicyEvaluationRequestV1
import org.eclipse.keti.acs.testutils.AGENT_MULDER
import org.eclipse.keti.acs.testutils.XFILES_ID
import org.testng.Assert.assertEquals
import org.testng.Assert.assertFalse
import org.testng.Assert.assertTrue
import org.testng.annotations.Test
import java.util.LinkedHashSet

private const val ZONE_NAME = "testzone1"
private const val ACTION_GET = "GET"
private const val POLICY_ONE = "policyOne"
private val EVALUATION_ORDER_POLICYONE = LinkedHashSet(listOf(POLICY_ONE))

class PolicyEvaluationRequestCacheKeyTest {

    @Test
    fun testKeyByRequest() {
        val request = PolicyEvaluationRequestV1()
        request.action = ACTION_GET
        request.subjectIdentifier = AGENT_MULDER
        request.resourceIdentifier = XFILES_ID
        request.policySetsEvaluationOrder = EVALUATION_ORDER_POLICYONE
        val key = PolicyEvaluationRequestCacheKey(request, ZONE_NAME)

        assertEquals(key.zoneId, ZONE_NAME)
        assertEquals(key.subjectId, request.subjectIdentifier)
        assertEquals(key.resourceId, request.resourceIdentifier)
        assertEquals(key.policySetIds, request.policySetsEvaluationOrder)
        assertEquals(key.request, request)
    }

    @Test
    fun testKeyByRequestWithEmptyPolicySetEvaluationOrder() {
        val request = PolicyEvaluationRequestV1()
        request.action = ACTION_GET
        request.subjectIdentifier = AGENT_MULDER
        request.resourceIdentifier = XFILES_ID
        val key = PolicyEvaluationRequestCacheKey(request, ZONE_NAME)

        assertEquals(key.zoneId, ZONE_NAME)
        assertEquals(key.subjectId, request.subjectIdentifier)
        assertEquals(key.resourceId, request.resourceIdentifier)
        assertEquals(key.policySetIds, EVALUATION_ORDER_ANY_POLICY_SET_KEY)
        assertEquals(key.request, request)
    }

    @Test
    fun testKeyEqualsForSameRequests() {
        val request = PolicyEvaluationRequestV1()
        request.action = ACTION_GET
        request.subjectIdentifier = AGENT_MULDER
        request.resourceIdentifier = XFILES_ID
        request.policySetsEvaluationOrder = EVALUATION_ORDER_POLICYONE
        val key = PolicyEvaluationRequestCacheKey(request, ZONE_NAME)

        val otherRequest = PolicyEvaluationRequestV1()
        otherRequest.action = ACTION_GET
        otherRequest.subjectIdentifier = AGENT_MULDER
        otherRequest.resourceIdentifier = XFILES_ID
        otherRequest.policySetsEvaluationOrder = EVALUATION_ORDER_POLICYONE
        val otherKey = PolicyEvaluationRequestCacheKey(otherRequest, ZONE_NAME)
        assertTrue(key == otherKey)
    }

    @Test
    fun testKeyEqualsForDifferentRequests() {
        val request = PolicyEvaluationRequestV1()
        request.action = ACTION_GET
        request.subjectIdentifier = AGENT_MULDER
        request.resourceIdentifier = XFILES_ID
        request.policySetsEvaluationOrder = EVALUATION_ORDER_POLICYONE
        val key = PolicyEvaluationRequestCacheKey(request, ZONE_NAME)

        val otherRequest = PolicyEvaluationRequestV1()
        otherRequest.action = ACTION_GET
        otherRequest.subjectIdentifier = AGENT_MULDER
        otherRequest.resourceIdentifier = XFILES_ID
        val otherKey = PolicyEvaluationRequestCacheKey(otherRequest, ZONE_NAME)
        assertFalse(key == otherKey)
    }

    @Test
    fun testToRedisKey() {
        val request = PolicyEvaluationRequestV1()
        val key = PolicyEvaluationRequestCacheKey(request, ZONE_NAME)
        assertEquals(key.toDecisionKey(), ZONE_NAME + ":*:*:" + Integer.toHexString(request.hashCode()))
    }
}
