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
 *
 * SPDX-License-Identifier: Apache-2.0
 *******************************************************************************/

package com.ge.predix.acs.service.policy.evaluation;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anySetOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.internal.util.reflection.Whitebox;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ge.predix.acs.PolicyContextResolver;
import com.ge.predix.acs.attribute.readers.AttributeReaderFactory;
import com.ge.predix.acs.attribute.readers.AttributeRetrievalException;
import com.ge.predix.acs.attribute.readers.ExternalResourceAttributeReader;
import com.ge.predix.acs.attribute.readers.ExternalSubjectAttributeReader;
import com.ge.predix.acs.commons.policy.condition.groovy.GroovyConditionCache;
import com.ge.predix.acs.commons.policy.condition.groovy.GroovyConditionShell;
import com.ge.predix.acs.model.Attribute;
import com.ge.predix.acs.model.Effect;
import com.ge.predix.acs.model.PolicySet;
import com.ge.predix.acs.policy.evaluation.cache.PolicyEvaluationCache;
import com.ge.predix.acs.policy.evaluation.cache.PolicyEvaluationRequestCacheKey;
import com.ge.predix.acs.rest.BaseResource;
import com.ge.predix.acs.rest.BaseSubject;
import com.ge.predix.acs.rest.PolicyEvaluationRequestV1;
import com.ge.predix.acs.rest.PolicyEvaluationResult;
import com.ge.predix.acs.service.policy.admin.PolicyManagementService;
import com.ge.predix.acs.service.policy.matcher.PolicyMatcherImpl;
import com.ge.predix.acs.service.policy.validation.PolicySetValidator;
import com.ge.predix.acs.service.policy.validation.PolicySetValidatorImpl;
import com.ge.predix.acs.zone.management.dao.ZoneEntity;
import com.ge.predix.acs.zone.resolver.ZoneResolver;

@ContextConfiguration(
        classes = { GroovyConditionCache.class, GroovyConditionShell.class, PolicySetValidatorImpl.class })
public class PolicyEvaluationWithAttributeReaderTest extends AbstractTestNGSpringContextTests {

    @InjectMocks
    private PolicyEvaluationServiceImpl evaluationService;
    @Mock
    private PolicyManagementService policyService;
    @Mock
    private PolicyContextResolver policyScopeResolver;
    @Mock
    private ZoneResolver zoneResolver;
    @Mock
    private PolicyEvaluationCache cache;
    @Mock
    private AttributeReaderFactory attributeReaderFactory;
    @Mock
    private ExternalResourceAttributeReader externalResourceAttributeReader;
    @Mock
    private ExternalSubjectAttributeReader externalSubjectAttributeReader;
    @Autowired
    private PolicySetValidator policySetValidator;

    private static final String RESOURCE_IDENTIFIER = "/sites/1234";
    private static final String SUBJECT_IDENTIFIER = "test-subject";
    private static final String ACTION = "GET";

    private final PolicyMatcherImpl policyMatcher = new PolicyMatcherImpl();

    @BeforeClass
    public void setupClass() {
        ((PolicySetValidatorImpl) this.policySetValidator)
                .setValidAcsPolicyHttpActions("GET, POST, PUT, DELETE, PATCH");
        ((PolicySetValidatorImpl) this.policySetValidator).init();
    }

    @BeforeMethod
    public void setupMethod() throws Exception {
        this.evaluationService = new PolicyEvaluationServiceImpl();
        MockitoAnnotations.initMocks(this);
        Whitebox.setInternalState(this.policyMatcher, "attributeReaderFactory", this.attributeReaderFactory);
        Whitebox.setInternalState(this.evaluationService, "policyMatcher", this.policyMatcher);
        Whitebox.setInternalState(this.evaluationService, "policySetValidator", this.policySetValidator);
        when(this.zoneResolver.getZoneEntityOrFail()).thenReturn(new ZoneEntity(0L, "testzone"));
        when(this.cache.get(any(PolicyEvaluationRequestCacheKey.class))).thenReturn(null);
        when(this.attributeReaderFactory.getResourceAttributeReader()).thenReturn(this.externalResourceAttributeReader);
        when(this.attributeReaderFactory.getSubjectAttributeReader()).thenReturn(this.externalSubjectAttributeReader);
        PolicySet policySet = new ObjectMapper().readValue(
                new File("src/test/resources/policy-set-with-one-policy-one-condition-using-res-attributes.json"),
                PolicySet.class);
        when(this.policyService.getAllPolicySets()).thenReturn(Collections.singletonList(policySet));
    }

    @Test
    public void testPolicyEvaluation() throws Exception {
        Set<Attribute> resourceAttributes = new HashSet<>();
        resourceAttributes.add(new Attribute("https://acs.attributes.int", "location", "sanramon"));
        resourceAttributes.add(new Attribute("https://acs.attributes.int", "role_required", "admin"));
        BaseResource testResource = new BaseResource(RESOURCE_IDENTIFIER, resourceAttributes);

        Set<Attribute> subjectAttributes = new HashSet<>();
        subjectAttributes.add(new Attribute("https://acs.attributes.int", "role", "admin"));
        BaseSubject testSubject = new BaseSubject(SUBJECT_IDENTIFIER, subjectAttributes);

        when(this.externalResourceAttributeReader.getAttributes(anyString())).thenReturn(testResource.getAttributes());
        when(this.externalSubjectAttributeReader.getAttributesByScope(anyString(), anySetOf(Attribute.class)))
                .thenReturn(testSubject.getAttributes());

        PolicyEvaluationResult evalResult = this.evaluationService
                .evalPolicy(createRequest(RESOURCE_IDENTIFIER, SUBJECT_IDENTIFIER, ACTION));
        Assert.assertEquals(evalResult.getEffect(), Effect.PERMIT);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testPolicyEvaluationWhenAdaptersTimeOut() throws Exception {
        String attributeRetrievalExceptionMessage = "attribute retrieval exception";
        when(this.externalResourceAttributeReader.getAttributes(Mockito.anyString()))
                .thenThrow(new AttributeRetrievalException(attributeRetrievalExceptionMessage, new Exception()));

        PolicyEvaluationResult evalResult = this.evaluationService
                .evalPolicy(createRequest(RESOURCE_IDENTIFIER, SUBJECT_IDENTIFIER, ACTION));
        Assert.assertEquals(evalResult.getEffect(), Effect.INDETERMINATE);
        Assert.assertEquals(evalResult.getMessage(), attributeRetrievalExceptionMessage);
    }

    private PolicyEvaluationRequestV1 createRequest(final String resource, final String subject, final String action) {
        PolicyEvaluationRequestV1 request = new PolicyEvaluationRequestV1();
        request.setAction(action);
        request.setSubjectIdentifier(subject);
        request.setResourceIdentifier(resource);
        return request;
    }
}
