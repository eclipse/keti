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

package org.eclipse.keti.acs.policy.evaluation.cache

import org.eclipse.keti.acs.rest.PolicyEvaluationRequestV1
import org.eclipse.keti.acs.testutils.AGENT_MULDER
import org.eclipse.keti.acs.testutils.XFILES_ID
import org.testng.Assert.assertEquals
import org.testng.Assert.assertFalse
import org.testng.Assert.assertNull
import org.testng.Assert.assertTrue
import org.testng.annotations.Test
import java.util.LinkedHashSet

private const val ZONE_NAME = "testzone1"
private const val ACTION_GET = "GET"

class PolicyEvaluationRequestCacheKeyTest {

    @Test
    fun testBuild() {
        val subjectId = AGENT_MULDER
        val resourceId = XFILES_ID
        val policyEvaluationOrder = LinkedHashSet(listOf("policyOne"))
        val key = PolicyEvaluationRequestCacheKey.Builder().zoneId(ZONE_NAME)
            .resourceId(resourceId).subjectId(subjectId).policySetIds(policyEvaluationOrder).build()

        assertEquals(key.zoneId, ZONE_NAME)
        assertEquals(key.subjectId, subjectId)
        assertEquals(key.resourceId, resourceId)
        assertEquals(key.policySetIds, policyEvaluationOrder)
        assertNull(key.request)
    }

    @Test
    fun testBuildByRequest() {
        val request = PolicyEvaluationRequestV1()
        request.action = ACTION_GET
        request.subjectIdentifier = AGENT_MULDER
        request.resourceIdentifier = XFILES_ID
        request.policySetsEvaluationOrder = LinkedHashSet(listOf("policyOne"))
        val key = PolicyEvaluationRequestCacheKey.Builder().zoneId(ZONE_NAME)
            .request(request).build()

        assertEquals(key.zoneId, ZONE_NAME)
        assertEquals(key.subjectId, request.subjectIdentifier)
        assertEquals(key.resourceId, request.resourceIdentifier)
        assertEquals(key.policySetIds, request.policySetsEvaluationOrder)
        assertEquals(key.request, request)
    }

    @Test
    fun testBuildByRequestAndPolicySetEvaluationOrder() {
        val request = PolicyEvaluationRequestV1()
        request.action = ACTION_GET
        request.subjectIdentifier = AGENT_MULDER
        request.resourceIdentifier = XFILES_ID
        val policyEvaluationOrder = LinkedHashSet(listOf("policyOne"))
        val key = PolicyEvaluationRequestCacheKey.Builder().zoneId(ZONE_NAME)
            .policySetIds(policyEvaluationOrder).request(request).build()

        assertEquals(key.zoneId, ZONE_NAME)
        assertEquals(key.subjectId, request.subjectIdentifier)
        assertEquals(key.resourceId, request.resourceIdentifier)
        assertEquals(key.policySetIds, policyEvaluationOrder)
        assertEquals(key.request, request)
    }

    @Test(expectedExceptions = [(IllegalStateException::class)])
    fun testIllegalStateExceptionForSettingPolicySetIds() {
        val request = PolicyEvaluationRequestV1()
        request.policySetsEvaluationOrder = LinkedHashSet(listOf("policyOne"))
        PolicyEvaluationRequestCacheKey.Builder().zoneId(ZONE_NAME)
            .request(request)
            .policySetIds(request.policySetsEvaluationOrder)
            .build()
    }

    @Test(expectedExceptions = [(IllegalStateException::class)])
    fun testIllegalStateExceptionForSettingSubjectId() {
        val request = PolicyEvaluationRequestV1()
        PolicyEvaluationRequestCacheKey.Builder().zoneId(ZONE_NAME)
            .request(request)
            .subjectId("subject")
            .build()
    }

    @Test(expectedExceptions = [(IllegalStateException::class)])
    fun testIllegalStateExceptionForSettingResourceId() {
        val request = PolicyEvaluationRequestV1()
        PolicyEvaluationRequestCacheKey.Builder().zoneId(ZONE_NAME)
            .request(request)
            .resourceId("resource")
            .build()
    }

    @Test
    fun testKeyEqualsForSameRequests() {
        val request = PolicyEvaluationRequestV1()
        request.action = ACTION_GET
        request.subjectIdentifier = AGENT_MULDER
        request.resourceIdentifier = XFILES_ID
        request.policySetsEvaluationOrder = LinkedHashSet(listOf("policyOne"))
        val key = PolicyEvaluationRequestCacheKey.Builder().zoneId(ZONE_NAME)
            .request(request).build()

        val otherRequest = PolicyEvaluationRequestV1()
        otherRequest.action = ACTION_GET
        otherRequest.subjectIdentifier = AGENT_MULDER
        otherRequest.resourceIdentifier = XFILES_ID
        otherRequest.policySetsEvaluationOrder = LinkedHashSet(listOf("policyOne"))
        val otherKey = PolicyEvaluationRequestCacheKey.Builder().zoneId(ZONE_NAME)
            .request(otherRequest).build()
        assertTrue(key == otherKey)
    }

    @Test
    fun testKeyEqualsForDifferentRequests() {
        val request = PolicyEvaluationRequestV1()
        request.action = ACTION_GET
        request.subjectIdentifier = AGENT_MULDER
        request.resourceIdentifier = XFILES_ID
        request.policySetsEvaluationOrder = LinkedHashSet(listOf("policyOne"))
        val key = PolicyEvaluationRequestCacheKey.Builder().zoneId(ZONE_NAME)
            .request(request).build()

        val otherRequest = PolicyEvaluationRequestV1()
        otherRequest.action = ACTION_GET
        otherRequest.subjectIdentifier = AGENT_MULDER
        otherRequest.resourceIdentifier = XFILES_ID
        val otherKey = PolicyEvaluationRequestCacheKey.Builder().zoneId(ZONE_NAME)
            .request(otherRequest).build()
        assertFalse(key == otherKey)
    }

    @Test
    fun testToRedisKey() {
        val request = PolicyEvaluationRequestV1()
        val key = PolicyEvaluationRequestCacheKey.Builder().zoneId(ZONE_NAME)
            .request(request).build()
        assertEquals(key.toDecisionKey(), ZONE_NAME + ":*:*:" + Integer.toHexString(request.hashCode()))
    }
}
