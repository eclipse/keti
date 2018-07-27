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

package org.eclipse.keti.acs.service.policy.evaluation

import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.nhaarman.mockito_kotlin.any
import org.eclipse.keti.acs.attribute.readers.AttributeReaderFactory
import org.eclipse.keti.acs.attribute.readers.PrivilegeServiceResourceAttributeReader
import org.eclipse.keti.acs.attribute.readers.PrivilegeServiceSubjectAttributeReader
import org.eclipse.keti.acs.attribute.readers.ResourceAttributeReader
import org.eclipse.keti.acs.attribute.readers.SubjectAttributeReader
import org.eclipse.keti.acs.model.Attribute
import org.eclipse.keti.acs.model.Effect
import org.eclipse.keti.acs.model.PolicySet
import org.eclipse.keti.acs.policy.evaluation.cache.PolicyEvaluationCache
import org.eclipse.keti.acs.rest.BaseResource
import org.eclipse.keti.acs.rest.BaseSubject
import org.eclipse.keti.acs.rest.PolicyEvaluationRequestV1
import org.eclipse.keti.acs.rest.PolicyEvaluationResult
import org.eclipse.keti.acs.service.policy.admin.PolicyManagementServiceImpl
import org.eclipse.keti.acs.service.policy.matcher.PolicyMatcherImpl
import org.eclipse.keti.acs.zone.management.dao.ZoneEntity
import org.eclipse.keti.acs.zone.resolver.ZoneResolver
import org.mockito.ArgumentMatchers.anyString
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.springframework.test.util.ReflectionTestUtils
import org.testng.Assert
import org.testng.annotations.Test
import java.io.File
import java.io.IOException
import java.util.HashSet

class PolicyEvaluationWithAttributeUriTemplateTest {

    @InjectMocks
    private var evaluationService = PolicyEvaluationServiceImpl()
    @Mock
    private var policyService = PolicyManagementServiceImpl()
    @Mock
    private lateinit var attributeReaderFactory: AttributeReaderFactory
    @Mock
    private lateinit var defaultResourceAttributeReader: PrivilegeServiceResourceAttributeReader
    @Mock
    private lateinit var defaultSubjectAttributeReader: PrivilegeServiceSubjectAttributeReader
    @Mock
    private lateinit var zoneResolver: ZoneResolver
    @Mock
    private lateinit var cache: PolicyEvaluationCache

    private val policyMatcher = PolicyMatcherImpl()

    @Test
    @Throws(JsonParseException::class, JsonMappingException::class, IOException::class)
    fun testEvaluateWithURIAttributeTemplate() {
        MockitoAnnotations.initMocks(this)
        ReflectionTestUtils.setField(this.policyMatcher, "attributeReaderFactory", this.attributeReaderFactory)
        ReflectionTestUtils.setField(this.evaluationService, "policyMatcher", this.policyMatcher)
        `when`(this.zoneResolver.zoneEntityOrFail).thenReturn(ZoneEntity(0L, "testzone"))
        `when`<PolicyEvaluationResult>(this.cache[any()])
            .thenReturn(null)
        `when`<ResourceAttributeReader>(this.attributeReaderFactory.resourceAttributeReader)
            .thenReturn(this.defaultResourceAttributeReader)
        `when`<SubjectAttributeReader>(this.attributeReaderFactory.subjectAttributeReader)
            .thenReturn(this.defaultSubjectAttributeReader)

        // set policy
        val policySet = ObjectMapper()
            .readValue(File("src/test/resources/policy-set-with-attribute-uri-template.json"), PolicySet::class.java)
        `when`(this.policyService.allPolicySets).thenReturn(listOf(policySet))

        // Create 'role' attribute in resource for URI /site/1234. Used in target match for policy 1.
        val testResource = BaseResource("/site/1234")
        val resourceAttributes = HashSet<Attribute>()
        resourceAttributes.add(Attribute("https://acs.attributes.int", "role", "admin"))
        testResource.attributes = resourceAttributes

        `when`(this.defaultResourceAttributeReader.getAttributes(testResource.resourceIdentifier!!))
            .thenReturn(testResource.attributes)

        val testSubject = BaseSubject("test-subject")
        testSubject.attributes = emptySet()
        `when`(
            this.defaultSubjectAttributeReader.getAttributesByScope(
                anyString(),
                any()
            )
        )
            .thenReturn(testSubject.attributes)

        // resourceURI matches attributeURITemplate
        var evalResult = this.evaluationService
            .evalPolicy(createRequest("/v1/site/1234/asset/456", "test-subject", "GET"))
        Assert.assertEquals(evalResult.effect, Effect.PERMIT)

        // resourceURI does NOT match attributeURITemplate
        evalResult = this.evaluationService
            .evalPolicy(createRequest("/v1/no-match/asset/123", "test-subject", "GET"))
        // second policy will match.
        Assert.assertEquals(evalResult.effect, Effect.DENY)
    }

    private fun createRequest(
        resource: String,
        subject: String,
        action: String
    ): PolicyEvaluationRequestV1 {
        val request = PolicyEvaluationRequestV1()
        request.action = action
        request.subjectIdentifier = subject
        request.resourceIdentifier = resource
        return request
    }
}
