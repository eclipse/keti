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

import com.fasterxml.jackson.databind.ObjectMapper
import com.nhaarman.mockito_kotlin.any
import org.eclipse.keti.acs.attribute.readers.AttributeReaderFactory
import org.eclipse.keti.acs.attribute.readers.AttributeRetrievalException
import org.eclipse.keti.acs.attribute.readers.ExternalResourceAttributeReader
import org.eclipse.keti.acs.attribute.readers.ExternalSubjectAttributeReader
import org.eclipse.keti.acs.attribute.readers.ResourceAttributeReader
import org.eclipse.keti.acs.attribute.readers.SubjectAttributeReader
import org.eclipse.keti.acs.commons.policy.condition.groovy.GroovyConditionCache
import org.eclipse.keti.acs.commons.policy.condition.groovy.GroovyConditionShell
import org.eclipse.keti.acs.model.Attribute
import org.eclipse.keti.acs.model.Effect
import org.eclipse.keti.acs.model.PolicySet
import org.eclipse.keti.acs.policy.evaluation.cache.PolicyEvaluationCache
import org.eclipse.keti.acs.rest.BaseResource
import org.eclipse.keti.acs.rest.BaseSubject
import org.eclipse.keti.acs.rest.PolicyEvaluationRequestV1
import org.eclipse.keti.acs.rest.PolicyEvaluationResult
import org.eclipse.keti.acs.service.policy.admin.PolicyManagementService
import org.eclipse.keti.acs.service.policy.matcher.PolicyMatcherImpl
import org.eclipse.keti.acs.service.policy.validation.PolicySetValidator
import org.eclipse.keti.acs.service.policy.validation.PolicySetValidatorImpl
import org.eclipse.keti.acs.zone.management.dao.ZoneEntity
import org.eclipse.keti.acs.zone.resolver.ZoneResolver
import org.mockito.ArgumentMatchers.anyString
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests
import org.springframework.test.util.ReflectionTestUtils
import org.testng.Assert
import org.testng.annotations.BeforeClass
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Test
import java.io.File
import java.util.HashSet

private const val RESOURCE_IDENTIFIER = "/sites/1234"
private const val SUBJECT_IDENTIFIER = "test-subject"
private const val ACTION = "GET"

@ContextConfiguration(
    classes = [(GroovyConditionCache::class), (GroovyConditionShell::class), (PolicySetValidatorImpl::class)]
)
class PolicyEvaluationWithAttributeReaderTest : AbstractTestNGSpringContextTests() {

    @InjectMocks
    private lateinit var evaluationService: PolicyEvaluationServiceImpl
    @Mock
    private lateinit var policyService: PolicyManagementService
    @Mock
    private lateinit var zoneResolver: ZoneResolver
    @Mock
    private lateinit var cache: PolicyEvaluationCache
    @Mock
    private lateinit var attributeReaderFactory: AttributeReaderFactory
    @Mock
    private lateinit var externalResourceAttributeReader: ExternalResourceAttributeReader
    @Mock
    private lateinit var externalSubjectAttributeReader: ExternalSubjectAttributeReader
    @Autowired
    private lateinit var policySetValidator: PolicySetValidator

    private val policyMatcher = PolicyMatcherImpl()

    @BeforeClass
    fun setupClass() {
        val policySetValidatorImpl = (this.policySetValidator as PolicySetValidatorImpl)
        policySetValidatorImpl.setValidAcsPolicyHttpActions("GET, POST, PUT, DELETE, PATCH")
        policySetValidatorImpl.init()
    }

    @BeforeMethod
    @Throws(Exception::class)
    fun setupMethod() {
        this.evaluationService = PolicyEvaluationServiceImpl()
        MockitoAnnotations.initMocks(this)
        ReflectionTestUtils.setField(this.policyMatcher, "attributeReaderFactory", this.attributeReaderFactory)
        ReflectionTestUtils.setField(this.evaluationService, "policyMatcher", this.policyMatcher)
        ReflectionTestUtils.setField(this.evaluationService, "policySetValidator", this.policySetValidator)
        `when`(this.zoneResolver.zoneEntityOrFail).thenReturn(ZoneEntity(0L, "testzone"))
        `when`<PolicyEvaluationResult>(this.cache[any()])
            .thenReturn(null)
        `when`<ResourceAttributeReader>(this.attributeReaderFactory.resourceAttributeReader)
            .thenReturn(this.externalResourceAttributeReader)
        `when`<SubjectAttributeReader>(this.attributeReaderFactory.subjectAttributeReader)
            .thenReturn(this.externalSubjectAttributeReader)
        val policySet = ObjectMapper().readValue(
            File("src/test/resources/policy-set-with-one-policy-one-condition-using-res-attributes.json"),
            PolicySet::class.java
        )
        `when`(this.policyService.allPolicySets).thenReturn(listOf(policySet))
    }

    @Test
    @Throws(Exception::class)
    fun testPolicyEvaluation() {
        val resourceAttributes = HashSet<Attribute>()
        resourceAttributes.add(Attribute("https://acs.attributes.int", "location", "sanramon"))
        resourceAttributes.add(Attribute("https://acs.attributes.int", "role_required", "admin"))
        val testResource = BaseResource(RESOURCE_IDENTIFIER, resourceAttributes)

        val subjectAttributes = HashSet<Attribute>()
        subjectAttributes.add(Attribute("https://acs.attributes.int", "role", "admin"))
        val testSubject = BaseSubject(SUBJECT_IDENTIFIER, subjectAttributes)

        `when`<Set<Attribute>>(this.externalResourceAttributeReader.getAttributes(anyString()))
            .thenReturn(testResource.attributes)
        `when`<Set<Attribute>>(
            this.externalSubjectAttributeReader.getAttributesByScope(
                anyString(),
                any()
            )
        )
            .thenReturn(testSubject.attributes)

        val evalResult = this.evaluationService
            .evalPolicy(createRequest(RESOURCE_IDENTIFIER, SUBJECT_IDENTIFIER, ACTION))
        Assert.assertEquals(evalResult.effect, Effect.PERMIT)
    }

    @Test
    @Throws(Exception::class)
    fun testPolicyEvaluationWhenAdaptersTimeOut() {
        val attributeRetrievalExceptionMessage = "attribute retrieval exception"
        `when`<Set<Attribute>>(this.externalResourceAttributeReader.getAttributes(anyString()))
            .thenAnswer { _ ->
                throw AttributeRetrievalException(
                    attributeRetrievalExceptionMessage,
                    Exception()
                )
            }

        val evalResult = this.evaluationService
            .evalPolicy(createRequest(RESOURCE_IDENTIFIER, SUBJECT_IDENTIFIER, ACTION))
        Assert.assertEquals(evalResult.effect, Effect.INDETERMINATE)
        Assert.assertEquals(evalResult.message, attributeRetrievalExceptionMessage)
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
