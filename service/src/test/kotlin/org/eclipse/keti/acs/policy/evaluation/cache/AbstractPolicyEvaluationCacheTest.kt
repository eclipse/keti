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

import com.nhaarman.mockito_kotlin.any
import org.eclipse.keti.acs.attribute.connector.management.AttributeConnectorService
import org.eclipse.keti.acs.attribute.connector.management.AttributeConnectorServiceImpl
import org.eclipse.keti.acs.model.Effect
import org.eclipse.keti.acs.privilege.management.dao.ResourceEntity
import org.eclipse.keti.acs.privilege.management.dao.SubjectEntity
import org.eclipse.keti.acs.rest.AttributeConnector
import org.eclipse.keti.acs.rest.PolicyEvaluationRequestV1
import org.eclipse.keti.acs.rest.PolicyEvaluationResult
import org.eclipse.keti.acs.testutils.AGENT_MULDER
import org.eclipse.keti.acs.testutils.XFILES_ID
import org.eclipse.keti.acs.zone.management.dao.ZoneEntity
import org.joda.time.DateTime
import org.mockito.Mockito
import org.springframework.test.util.ReflectionTestUtils
import org.testng.Assert
import org.testng.Assert.assertEquals
import org.testng.Assert.assertNotNull
import org.testng.Assert.assertNull
import org.testng.annotations.AfterMethod
import org.testng.annotations.BeforeClass
import org.testng.annotations.DataProvider
import org.testng.annotations.Test
import java.util.LinkedHashSet

private const val ZONE_NAME = "testzone1"
private val ZONE_ENTITY = ZoneEntity(1L, ZONE_NAME)
private const val ACTION_GET = "GET"
private const val POLICY_ONE = "policyOne"
private const val POLICY_TWO = "policyTwo"
private val EVALUATION_ORDER_POLICYONE_POLICYTWO = LinkedHashSet(listOf(POLICY_ONE, POLICY_TWO))
private val EVALUATION_ORDER_POLICYTWO_POLICYONE = LinkedHashSet(listOf(POLICY_TWO, POLICY_ONE))
private val EVALUATION_ORDER_POLICYONE = LinkedHashSet(listOf(POLICY_ONE))

private fun mockPermitResult(): PolicyEvaluationResult {
    val result = PolicyEvaluationResult(Effect.PERMIT)
    result.resolvedResourceUris = setOf(XFILES_ID)
    return result
}

class AbstractPolicyEvaluationCacheTest {

    private val cache = InMemoryPolicyEvaluationCache()

    @BeforeClass
    internal fun beforeClass() {
        val connectorService = Mockito.mock(AttributeConnectorService::class.java)
        ReflectionTestUtils.setField(this.cache, "connectorService", connectorService)
    }

    @AfterMethod
    fun cleanupTest() {
        this.cache.reset()

        this.cache.resetForSubject(ZONE_NAME, AGENT_MULDER)
        this.cache.resetForResource(ZONE_NAME, XFILES_ID)
        this.cache.resetForPolicySet(ZONE_NAME, POLICY_ONE)
        this.cache.resetForPolicySet(ZONE_NAME, POLICY_TWO)
    }

    @Test
    fun testGetWithCacheMissForPolicyEvaluation() {

        val request = PolicyEvaluationRequestV1()
        request.action = ACTION_GET
        request.subjectIdentifier = AGENT_MULDER
        request.resourceIdentifier = XFILES_ID
        val key = PolicyEvaluationRequestCacheKey(request, ZONE_NAME)
        assertNull(this.cache[key])
    }

    @Test
    fun testGetWithCacheMissForResource() {
        val request = PolicyEvaluationRequestV1()
        request.action = ACTION_GET
        request.subjectIdentifier = AGENT_MULDER
        request.resourceIdentifier = XFILES_ID
        val key = PolicyEvaluationRequestCacheKey(request, ZONE_NAME)
        val result = mockPermitResult()
        this.cache[key] = result
        this.cache.delete(resourceKey(ZONE_NAME, XFILES_ID))
        assertNull(this.cache[key])
    }

    @Test
    fun testGetWithCacheHit() {

        val request = PolicyEvaluationRequestV1()
        request.action = ACTION_GET
        request.subjectIdentifier = AGENT_MULDER
        request.resourceIdentifier = XFILES_ID
        request.policySetsEvaluationOrder = EVALUATION_ORDER_POLICYONE
        val key = PolicyEvaluationRequestCacheKey(request, ZONE_NAME)

        val result = mockPermitResult()
        this.cache[key] = result

        val cachedResult = this.cache[key]
        assertEquals(cachedResult!!.effect, result.effect)
    }

    @Test
    @Throws(Exception::class)
    fun testGetWithPolicyInvalidation() {

        val request = PolicyEvaluationRequestV1()
        request.action = ACTION_GET
        request.subjectIdentifier = AGENT_MULDER
        request.resourceIdentifier = XFILES_ID
        request.policySetsEvaluationOrder = EVALUATION_ORDER_POLICYONE

        val key = PolicyEvaluationRequestCacheKey(request, ZONE_NAME)

        val result = mockPermitResult()
        this.cache[key] = result

        val cachedResult = this.cache[key]
        assertEquals(cachedResult!!.effect, result.effect)

        Thread.sleep(1)
        this.cache.resetForPolicySet(ZONE_NAME, POLICY_ONE)
        assertNull(this.cache[key])
    }

    @Test
    @Throws(Exception::class)
    fun testGetWithResetForSinglePolicySet() {

        val request = PolicyEvaluationRequestV1()
        request.action = ACTION_GET
        request.subjectIdentifier = AGENT_MULDER
        request.resourceIdentifier = XFILES_ID

        val key = PolicyEvaluationRequestCacheKey(request, ZONE_NAME)

        val result = mockPermitResult()
        this.cache[key] = result

        val cachedResult = this.cache[key]
        assertEquals(cachedResult!!.effect, result.effect)

        Thread.sleep(1)
        this.cache.resetForPolicySet(ZONE_NAME, POLICY_ONE)
        assertNull(this.cache[key])
    }

    @Test
    @Throws(Exception::class)
    fun testGetWithResetForPolicySetNotIncludedInEvaluationOrder() {

        val request = PolicyEvaluationRequestV1()
        request.action = ACTION_GET
        request.subjectIdentifier = AGENT_MULDER
        request.resourceIdentifier = XFILES_ID
        request.policySetsEvaluationOrder = EVALUATION_ORDER_POLICYONE

        val key = PolicyEvaluationRequestCacheKey(request, ZONE_NAME)

        val result = mockPermitResult()
        this.cache[key] = result

        var cachedResult = this.cache[key]
        assertEquals(cachedResult!!.effect, result.effect)

        Thread.sleep(1)
        this.cache.resetForPolicySet(ZONE_NAME, POLICY_TWO)

        cachedResult = this.cache[key]
        assertNotNull(cachedResult)
        assertEquals(cachedResult!!.effect, result.effect)
    }

    @Test
    @Throws(Exception::class)
    fun testGetWithResetForMultiplePolicySets() {

        val request = PolicyEvaluationRequestV1()
        request.action = ACTION_GET
        request.subjectIdentifier = AGENT_MULDER
        request.resourceIdentifier = XFILES_ID
        request.policySetsEvaluationOrder = EVALUATION_ORDER_POLICYONE_POLICYTWO
        val key = PolicyEvaluationRequestCacheKey(request, ZONE_NAME)

        val result = mockPermitResult()
        this.cache[key] = result

        val cachedResult = this.cache[key]
        assertNotNull(cachedResult)
        assertEquals(cachedResult!!.effect, result.effect)

        Thread.sleep(1)
        this.cache.resetForPolicySet(ZONE_NAME, POLICY_TWO)
        assertNull(this.cache[key])
    }

    @Test
    @Throws(Exception::class)
    fun testGetWithPolicyEvaluationOrderChange() {

        val request = PolicyEvaluationRequestV1()
        request.action = ACTION_GET
        request.subjectIdentifier = AGENT_MULDER
        request.resourceIdentifier = XFILES_ID
        request.policySetsEvaluationOrder = EVALUATION_ORDER_POLICYONE_POLICYTWO
        var key = PolicyEvaluationRequestCacheKey(request, ZONE_NAME)

        val result = mockPermitResult()
        this.cache[key] = result

        val cachedResult = this.cache[key]
        assertNotNull(cachedResult)
        assertEquals(cachedResult!!.effect, result.effect)

        Thread.sleep(1)

        request.policySetsEvaluationOrder = EVALUATION_ORDER_POLICYTWO_POLICYONE
        key = PolicyEvaluationRequestCacheKey(request, ZONE_NAME)

        assertNull(this.cache[key])
    }

    @Test
    @Throws(Exception::class)
    fun testGetWithResetForResource() {

        val request = PolicyEvaluationRequestV1()
        request.action = ACTION_GET
        request.subjectIdentifier = AGENT_MULDER
        request.resourceIdentifier = XFILES_ID
        val key = PolicyEvaluationRequestCacheKey(request, ZONE_NAME)

        val result = mockPermitResult()
        this.cache[key] = result

        val cachedResult = this.cache[key]
        assertEquals(cachedResult!!.effect, result.effect)

        Thread.sleep(1)
        this.cache.resetForResource(ZONE_NAME, XFILES_ID)
        assertNull(this.cache[key])
    }

    @Test
    @Throws(Exception::class)
    fun testGetWithResetForLongResource() {

        val request = PolicyEvaluationRequestV1()
        request.action = ACTION_GET
        request.subjectIdentifier = AGENT_MULDER
        request.resourceIdentifier = "/v1/x-files"
        val key = PolicyEvaluationRequestCacheKey(request, ZONE_NAME)

        val result = mockPermitResult()
        this.cache[key] = result

        val cachedResult = this.cache[key]
        assertEquals(cachedResult!!.effect, result.effect)

        Thread.sleep(1)
        this.cache.resetForResource(ZONE_NAME, XFILES_ID)
        assertNull(this.cache[key])
    }

    @Test
    @Throws(Exception::class)
    fun testGetWithResetForResources() {

        val request = PolicyEvaluationRequestV1()
        request.action = ACTION_GET
        request.subjectIdentifier = AGENT_MULDER
        request.resourceIdentifier = XFILES_ID
        val key = PolicyEvaluationRequestCacheKey(request, ZONE_NAME)

        val result = mockPermitResult()
        this.cache[key] = result

        val cachedResult = this.cache[key]
        assertEquals(cachedResult!!.effect, result.effect)

        Thread.sleep(1)
        this.cache.resetForResources(ZONE_NAME, listOf(ResourceEntity(ZONE_ENTITY, XFILES_ID)))
        assertNull(this.cache[key])
    }

    @Test
    @Throws(Exception::class)
    fun testGetWithResetForResolvedResources() {

        val request = PolicyEvaluationRequestV1()
        val resolvedResourceUri = "/resolved-resource"
        request.action = ACTION_GET
        request.subjectIdentifier = AGENT_MULDER
        request.resourceIdentifier = XFILES_ID
        val key = PolicyEvaluationRequestCacheKey(request, ZONE_NAME)

        val result = mockPermitResult()
        result.resolvedResourceUris = setOf(resolvedResourceUri)
        this.cache[key] = result

        val cachedResult = this.cache[key]
        assertEquals(cachedResult!!.effect, result.effect)

        Thread.sleep(1)
        this.cache.resetForResources(
            ZONE_NAME,
            listOf(ResourceEntity(ZONE_ENTITY, resolvedResourceUri))
        )
        assertNull(this.cache[key])
    }

    @Test
    @Throws(Exception::class)
    fun testGetWithResetForResourcesByIds() {

        val request = PolicyEvaluationRequestV1()
        request.action = ACTION_GET
        request.subjectIdentifier = AGENT_MULDER
        request.resourceIdentifier = XFILES_ID
        val key = PolicyEvaluationRequestCacheKey(request, ZONE_NAME)

        val result = mockPermitResult()
        this.cache[key] = result

        val cachedResult = this.cache[key]
        assertEquals(cachedResult!!.effect, result.effect)

        Thread.sleep(1)
        this.cache.resetForResourcesByIds(ZONE_NAME, setOf(XFILES_ID))
        assertNull(this.cache[key])
    }

    @Test
    @Throws(Exception::class)
    fun testGetWithResetForSubject() {

        val request = PolicyEvaluationRequestV1()
        request.action = ACTION_GET
        request.subjectIdentifier = AGENT_MULDER
        request.resourceIdentifier = XFILES_ID
        val key = PolicyEvaluationRequestCacheKey(request, ZONE_NAME)

        val result = mockPermitResult()
        this.cache[key] = result

        val cachedResult = this.cache[key]
        assertEquals(cachedResult!!.effect, result.effect)

        Thread.sleep(1)
        this.cache.resetForSubject(ZONE_NAME, AGENT_MULDER)
        assertNull(this.cache[key])
    }

    @Test
    @Throws(Exception::class)
    fun testGetWithResetForSubjectsByIds() {

        val request = PolicyEvaluationRequestV1()
        request.action = ACTION_GET
        request.subjectIdentifier = AGENT_MULDER
        request.resourceIdentifier = XFILES_ID
        val key = PolicyEvaluationRequestCacheKey(request, ZONE_NAME)

        val result = mockPermitResult()
        this.cache[key] = result

        val cachedResult = this.cache[key]
        assertEquals(cachedResult!!.effect, result.effect)

        Thread.sleep(1)
        this.cache.resetForSubjectsByIds(ZONE_NAME, setOf(AGENT_MULDER))
        assertNull(this.cache[key])
    }

    @Test
    @Throws(Exception::class)
    fun testGetWithResetForSubjects() {

        val request = PolicyEvaluationRequestV1()
        request.action = ACTION_GET
        request.subjectIdentifier = AGENT_MULDER
        request.resourceIdentifier = XFILES_ID
        val key = PolicyEvaluationRequestCacheKey(request, ZONE_NAME)

        val result = mockPermitResult()
        this.cache[key] = result

        val cachedResult = this.cache[key]
        assertEquals(cachedResult!!.effect, result.effect)

        Thread.sleep(1)
        this.cache.resetForSubjects(ZONE_NAME, listOf(SubjectEntity(ZONE_ENTITY, AGENT_MULDER)))
        assertNull(this.cache[key])
    }

    @Test
    fun testGetWithReset() {

        val request = PolicyEvaluationRequestV1()
        request.action = ACTION_GET
        request.subjectIdentifier = AGENT_MULDER
        request.resourceIdentifier = XFILES_ID
        val key = PolicyEvaluationRequestCacheKey(request, ZONE_NAME)

        val result = mockPermitResult()
        this.cache[key] = result

        val cachedResult = this.cache[key]
        assertEquals(cachedResult!!.effect, result.effect)

        this.cache.reset()
        assertNull(this.cache[key])
    }

    @Test(dataProvider = "intervalProvider")
    fun testHaveConnectorIntervalsLapsed(
        resourceConnector: AttributeConnector?,
        subjectConnector: AttributeConnector?,
        currentTime: DateTime,
        haveConnectorCacheIntervalsLapsed: Boolean
    ) {
        val connectorService = Mockito.mock(AttributeConnectorServiceImpl::class.java)

        Mockito.doReturn(resourceConnector).`when`<AttributeConnectorService>(connectorService)
            .resourceAttributeConnector
        Mockito.doReturn(subjectConnector).`when`<AttributeConnectorService>(connectorService).subjectAttributeConnector
        this.cache.resetForPolicySet(ZONE_NAME, POLICY_ONE)

        val isResourceConnectorConfigured = resourceConnector != null
        val isSubjectConnectorConfigured = subjectConnector != null
        Mockito.doReturn(isResourceConnectorConfigured).`when`<AttributeConnectorService>(connectorService)
            .isResourceAttributeConnectorConfigured
        Mockito.doReturn(isSubjectConnectorConfigured).`when`<AttributeConnectorService>(connectorService)
            .isSubjectAttributeConnectorConfigured

        val spiedCache = Mockito.spy(this.cache)
        ReflectionTestUtils.setField(spiedCache, "connectorService", connectorService)

        val request = PolicyEvaluationRequestV1()
        request.action = ACTION_GET
        request.subjectIdentifier = AGENT_MULDER
        request.resourceIdentifier = XFILES_ID
        request.policySetsEvaluationOrder = EVALUATION_ORDER_POLICYONE
        val key = PolicyEvaluationRequestCacheKey(request, ZONE_NAME)

        val expectedResult = mockPermitResult()
        spiedCache[key] = expectedResult

        val actualResult = spiedCache[key]
        Assert.assertEquals(actualResult!!.effect, expectedResult.effect)
        Assert.assertEquals(actualResult.resourceAttributes, expectedResult.resourceAttributes)
        Assert.assertEquals(actualResult.subjectAttributes, expectedResult.subjectAttributes)

        Mockito.verify(
            spiedCache,
            Mockito.times(if (isResourceConnectorConfigured || isSubjectConnectorConfigured) 1 else 2)
        )
            .haveEntitiesChanged(any<List<String>>(), any())
        Mockito.verify(
            spiedCache,
            Mockito.times(if (isResourceConnectorConfigured || isSubjectConnectorConfigured) 1 else 0)
        )
            .haveConnectorCacheIntervalsLapsed(any(), any())
        Assert.assertEquals(
            this.cache.haveConnectorCacheIntervalsLapsed(connectorService, currentTime),
            haveConnectorCacheIntervalsLapsed
        )
    }

    @DataProvider
    private fun intervalProvider(): Array<Array<out Any?>> {
        return arrayOf(
            allConnectorsConfiguredNoneElapsed(),
            allConnectorsConfiguredOnlyResourceElapsed(),
            allConnectorsConfiguredOnlySubjectElapsed(),
            connectorsNotConfigured(),
            onlyResourceConnectorConfiguredAndElapsed(),
            onlySubjectConnectorConfiguredAndElapsed()
        )
    }

    private fun allConnectorsConfiguredNoneElapsed(): Array<Any?> {
        val resourceConnector = AttributeConnector()
        val subjectConnector = AttributeConnector()
        resourceConnector.maxCachedIntervalMinutes = 1
        subjectConnector.maxCachedIntervalMinutes = 1

        return arrayOf(resourceConnector, subjectConnector, DateTime.now(), false)
    }

    private fun allConnectorsConfiguredOnlyResourceElapsed(): Array<Any?> {
        val resourceConnector = AttributeConnector()
        val subjectConnector = AttributeConnector()
        resourceConnector.maxCachedIntervalMinutes = 1
        subjectConnector.maxCachedIntervalMinutes = 4

        return arrayOf(resourceConnector, subjectConnector, DateTime.now().minusMinutes(3), true)
    }

    private fun allConnectorsConfiguredOnlySubjectElapsed(): Array<Any?> {
        val resourceConnector = AttributeConnector()
        val subjectConnector = AttributeConnector()
        resourceConnector.maxCachedIntervalMinutes = 4
        subjectConnector.maxCachedIntervalMinutes = 1

        return arrayOf(resourceConnector, subjectConnector, DateTime.now().minusMinutes(3), true)
    }

    private fun onlyResourceConnectorConfiguredAndElapsed(): Array<Any?> {
        val resourceConnector = AttributeConnector()
        resourceConnector.maxCachedIntervalMinutes = 1

        return arrayOf(resourceConnector, null, DateTime.now().minusMinutes(3), true)
    }

    private fun onlySubjectConnectorConfiguredAndElapsed(): Array<Any?> {
        val subjectConnector = AttributeConnector()
        subjectConnector.maxCachedIntervalMinutes = 1

        return arrayOf(null, subjectConnector, DateTime.now().minusMinutes(3), true)
    }

    private fun connectorsNotConfigured(): Array<Any?> {
        return arrayOf(null, null, DateTime.now().minusMinutes(3), false)
    }
}
