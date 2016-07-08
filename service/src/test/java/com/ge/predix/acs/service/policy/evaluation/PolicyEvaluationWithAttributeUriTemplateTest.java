/*******************************************************************************
 * Copyright 2016 General Electric Company.
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
 *******************************************************************************/
package com.ge.predix.acs.service.policy.evaluation;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.internal.util.reflection.Whitebox;
import org.testng.Assert;
import org.testng.annotations.Test;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ge.predix.acs.model.Attribute;
import com.ge.predix.acs.model.Effect;
import com.ge.predix.acs.model.PolicySet;
import com.ge.predix.acs.privilege.management.PrivilegeManagementService;
import com.ge.predix.acs.privilege.management.PrivilegeManagementServiceImpl;
import com.ge.predix.acs.rest.BaseResource;
import com.ge.predix.acs.rest.BaseSubject;
import com.ge.predix.acs.rest.PolicyEvaluationResult;
import com.ge.predix.acs.service.policy.admin.PolicyManagementService;
import com.ge.predix.acs.service.policy.admin.PolicyManagementServiceImpl;
import com.ge.predix.acs.service.policy.matcher.PolicyMatcherImpl;

public class PolicyEvaluationWithAttributeUriTemplateTest {

    @InjectMocks
    private final PolicyEvaluationService evaluationService = new PolicyEvaluationServiceImpl();

    @Mock
    private final PolicyManagementService policyService = new PolicyManagementServiceImpl();

    @Mock
    private final PrivilegeManagementService privilegeManagementService = new PrivilegeManagementServiceImpl();

    private final PolicyMatcherImpl policyMatcher = new PolicyMatcherImpl();

    @SuppressWarnings("unchecked")
    @Test(enabled = true)
    public void testEvaluateWithURIAttributeTemplate() throws JsonParseException, JsonMappingException, IOException {
        MockitoAnnotations.initMocks(this);
        Whitebox.setInternalState(this.policyMatcher, "privilegeManagementService", this.privilegeManagementService);
        Whitebox.setInternalState(this.evaluationService, "policyMatcher", this.policyMatcher);

        // set policy
        PolicySet policySet = new ObjectMapper()
                .readValue(new File("src/test/resources/policy-set-with-attribute-uri-template.json"), PolicySet.class);
        when(this.policyService.getAllPolicySets()).thenReturn(Arrays.asList(policySet));

        // Create 'role' attribute in resource for URI /site/1234. Used in target match for policy 1.
        BaseResource testResource = new BaseResource("/site/1234");
        Set<Attribute> resourceAttributes = new HashSet<>();
        resourceAttributes.add(new Attribute("https://acs.attributes.int", "role", "admin"));
        testResource.setAttributes(resourceAttributes);

        when(this.privilegeManagementService.getByResourceIdentifier("/site/1234")).thenReturn(testResource);
        when(this.privilegeManagementService.getByResourceIdentifierWithInheritedAttributes("/site/1234")).thenReturn(testResource);

        BaseSubject testSubject = new BaseSubject("test-subject");
        testSubject.setAttributes(Collections.emptySet());
        when(this.privilegeManagementService.getBySubjectIdentifierAndScopes(any(String.class), any(Set.class)))
                .thenReturn(testSubject);

        // resourceURI matches attributeURITemplate
        PolicyEvaluationResult evalResult = this.evaluationService.evalPolicy("/v1/site/1234/asset/456", "test-subject",
                "GET", null, null);
        Assert.assertEquals(evalResult.getEffect(), Effect.PERMIT);

        // resourceURI does NOT match attributeURITemplate
        evalResult = this.evaluationService.evalPolicy("/v1/no-match/asset/123", "test-subject", "GET", null, null);
        // second policy will match.
        Assert.assertEquals(evalResult.getEffect(), Effect.DENY);

    }
}
