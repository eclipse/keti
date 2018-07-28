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

import org.codehaus.jackson.map.ObjectMapper
import org.eclipse.keti.acs.model.Effect
import org.eclipse.keti.acs.rest.PolicyEvaluationRequestV1
import org.eclipse.keti.acs.rest.PolicyEvaluationResult
import org.eclipse.keti.acs.testutils.AGENT_MULDER
import org.eclipse.keti.acs.testutils.XFILES_ID
import org.joda.time.DateTime
import org.springframework.test.util.ReflectionTestUtils
import org.testng.Assert.assertEquals
import org.testng.annotations.AfterMethod
import org.testng.annotations.Test

private val OBJECT_MAPPER = ObjectMapper()
private const val ZONE_NAME = "testzone1"
private const val ACTION_GET = "GET"

class InMemoryPolicyEvaluationCacheTest {
    private val cache = InMemoryPolicyEvaluationCache()

    @AfterMethod
    fun cleanupTest() {
        this.cache.reset()
    }

    @Test
    @Throws(Exception::class)
    fun testSetPolicyEvalResult() {
        val request = PolicyEvaluationRequestV1()
        request.action = ACTION_GET
        request.subjectIdentifier = AGENT_MULDER
        request.resourceIdentifier = XFILES_ID
        val key = PolicyEvaluationRequestCacheKey.Builder().zoneId(ZONE_NAME)
            .request(request).build()

        val result = PolicyEvaluationResult(Effect.PERMIT)
        val value = OBJECT_MAPPER.writeValueAsString(result)
        this.cache[key.toDecisionKey()] = value

        val evalCache = ReflectionTestUtils.getField(this.cache, "evalCache") as Map<String, String>
        assertEquals(evalCache.size, 1)
    }

    @Test
    @Throws(Exception::class)
    fun testSetPolicySetChangedTimestamp() {
        val key = policySetKey(ZONE_NAME, "testSetPolicyPolicySetChangedTimestamp")
        val value = OBJECT_MAPPER.writeValueAsString(DateTime())
        this.cache[key] = value

        val evalCache = ReflectionTestUtils.getField(this.cache, "evalCache") as Map<String, String>
        assertEquals(evalCache.size, 1)
    }

    @Test
    @Throws(Exception::class)
    fun testSetPolicyResourceChangedTimestamp() {
        val key = resourceKey(ZONE_NAME, XFILES_ID)
        val value = OBJECT_MAPPER.writeValueAsString(DateTime())
        this.cache[key] = value

        val evalCache = ReflectionTestUtils.getField(this.cache, "evalCache") as Map<String, String>
        assertEquals(evalCache.size, 1)
    }

    @Test
    @Throws(Exception::class)
    fun testSetPolicySubjectChangedTimestamp() {
        val key = subjectKey(ZONE_NAME, AGENT_MULDER)
        val value = OBJECT_MAPPER.writeValueAsString(DateTime())
        this.cache[key] = value

        val evalCache = ReflectionTestUtils.getField(this.cache, "evalCache") as Map<String, String>
        assertEquals(evalCache.size, 1)
    }

    @Test(expectedExceptions = [(IllegalArgumentException::class)])
    fun testSetUnsupportedKeyFormat() {
        this.cache["key"] = ""
    }
}
