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

import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.Set;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.ge.predix.acs.model.Attribute;
import com.ge.predix.acs.privilege.management.PrivilegeManagementService;
import com.ge.predix.acs.rest.BaseSubject;

@Test
public class SubjectAttributeResolverTest {

    @Mock
    private PrivilegeManagementService privilegeManagementService;

    @BeforeMethod
    public void beforeMethod() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testGetSubjectAttributes() {
        BaseSubject testSubject = new BaseSubject();
        testSubject.setSubjectIdentifier("/test/subject");
        Set<Attribute> subjectAttributes = new HashSet<>();
        subjectAttributes.add(new Attribute("https://acs.attributes.int", "role", "administrator"));
        testSubject.setAttributes(subjectAttributes);

        Set<Attribute> supplementalSubjectAttributes = new HashSet<>();
        supplementalSubjectAttributes.add(new Attribute("https://acs.attributes.int", "site", "sanramon"));

        // mock attribute service for the expected resource URI after attributeURITemplate is applied
        when(this.privilegeManagementService.getBySubjectIdentifierAndScopes(testSubject.getSubjectIdentifier(), null))
                .thenReturn(testSubject);
        SubjectAttributeResolver resolver = new SubjectAttributeResolver(this.privilegeManagementService,
                testSubject.getSubjectIdentifier(), supplementalSubjectAttributes);

        Set<Attribute> combinedSubjectAttributes = resolver.getResult(null);
        Assert.assertNotNull(combinedSubjectAttributes);
        Assert.assertTrue(combinedSubjectAttributes.containsAll(subjectAttributes));
        Assert.assertTrue(combinedSubjectAttributes.containsAll(supplementalSubjectAttributes));
    }

    @Test
    public void testGetSubjectAttributesNoSubjectFoundAndNoSupplementalAttributes() {
        BaseSubject testSubject = new BaseSubject();
        testSubject.setSubjectIdentifier("/test/subject");

        // mock attribute service for the expected resource URI after attributeURITemplate is applied
        when(this.privilegeManagementService.getBySubjectIdentifierAndScopes(testSubject.getSubjectIdentifier(), null))
                .thenReturn(null);
        SubjectAttributeResolver resolver = new SubjectAttributeResolver(this.privilegeManagementService,
                testSubject.getSubjectIdentifier(), null);

        Set<Attribute> combinedSubjectAttributes = resolver.getResult(null);
        Assert.assertTrue(combinedSubjectAttributes.isEmpty());
    }

    @Test
    public void testGetSubjectAttributesSubjectFoundWithNoAttributesAndNoSupplementalAttributes() {
        BaseSubject testSubject = new BaseSubject();
        testSubject.setSubjectIdentifier("/test/subject");

        // mock attribute service for the expected resource URI after attributeURITemplate is applied
        when(this.privilegeManagementService.getBySubjectIdentifierAndScopes(testSubject.getSubjectIdentifier(), null))
                .thenReturn(testSubject);
        SubjectAttributeResolver resolver = new SubjectAttributeResolver(this.privilegeManagementService,
                testSubject.getSubjectIdentifier(), null);

        Set<Attribute> combinedSubjectAttributes = resolver.getResult(null);
        Assert.assertTrue(combinedSubjectAttributes.isEmpty());
    }

    @Test
    public void testGetSubjectAttributesSubjectFoundWithAttributesAndNoSupplementalAttributes() {
        BaseSubject testSubject = new BaseSubject();
        testSubject.setSubjectIdentifier("/test/subject");
        Set<Attribute> subjectAttributes = new HashSet<>();
        subjectAttributes.add(new Attribute("https://acs.attributes.int", "role", "administrator"));
        testSubject.setAttributes(subjectAttributes);

        // mock attribute service for the expected resource URI after attributeURITemplate is applied
        when(this.privilegeManagementService.getBySubjectIdentifierAndScopes(testSubject.getSubjectIdentifier(), null))
                .thenReturn(testSubject);
        SubjectAttributeResolver resolver = new SubjectAttributeResolver(this.privilegeManagementService,
                testSubject.getSubjectIdentifier(), null);

        Set<Attribute> combinedSubjectAttributes = resolver.getResult(null);
        Assert.assertNotNull(combinedSubjectAttributes);
        Assert.assertTrue(combinedSubjectAttributes.containsAll(subjectAttributes));
    }

    @Test
    public void testGetSubjectAttributesSupplementalAttributesOnly() {
        BaseSubject testSubject = new BaseSubject();
        testSubject.setSubjectIdentifier("/test/subject");

        Set<Attribute> supplementalSubjectAttributes = new HashSet<>();
        supplementalSubjectAttributes.add(new Attribute("https://acs.attributes.int", "site", "sanramon"));

        // mock attribute service for the expected resource URI after attributeURITemplate is applied
        when(this.privilegeManagementService.getBySubjectIdentifierAndScopes(testSubject.getSubjectIdentifier(), null))
                .thenReturn(null);
        SubjectAttributeResolver resolver = new SubjectAttributeResolver(this.privilegeManagementService,
                testSubject.getSubjectIdentifier(), supplementalSubjectAttributes);

        Set<Attribute> combinedSubjectAttributes = resolver.getResult(null);
        Assert.assertNotNull(combinedSubjectAttributes);
        Assert.assertTrue(combinedSubjectAttributes.containsAll(supplementalSubjectAttributes));
    }

    @Test
    public void testGetSubjectAttributesSubjectFoundWithNoAttributesButSupplementalAttributesProvided() {
        BaseSubject testSubject = new BaseSubject();
        testSubject.setSubjectIdentifier("/test/subject");

        Set<Attribute> supplementalSubjectAttributes = new HashSet<>();
        supplementalSubjectAttributes.add(new Attribute("https://acs.attributes.int", "site", "sanramon"));

        // mock attribute service for the expected resource URI after attributeURITemplate is applied
        when(this.privilegeManagementService.getBySubjectIdentifierAndScopes(testSubject.getSubjectIdentifier(), null))
                .thenReturn(testSubject);
        SubjectAttributeResolver resolver = new SubjectAttributeResolver(this.privilegeManagementService,
                testSubject.getSubjectIdentifier(), supplementalSubjectAttributes);

        Set<Attribute> combinedSubjectAttributes = resolver.getResult(null);
        Assert.assertNotNull(combinedSubjectAttributes);
        Assert.assertTrue(combinedSubjectAttributes.containsAll(supplementalSubjectAttributes));
    }
}
