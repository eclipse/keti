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
 *******************************************************************************/

package com.ge.predix.acs.service.policy.evaluation;

import static org.mockito.Mockito.anySetOf;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.ge.predix.acs.attribute.readers.PrivilegeServiceSubjectAttributeReader;
import com.ge.predix.acs.model.Attribute;
import com.ge.predix.acs.rest.BaseSubject;

@Test
public class SubjectAttributeResolverTest {

    @Mock
    private PrivilegeServiceSubjectAttributeReader defaultSubjectAttributeReader;

    private BaseSubject testSubject;

    @BeforeMethod
    public void beforeMethod() {
        MockitoAnnotations.initMocks(this);

        this.testSubject = new BaseSubject("/test/subject");
        when(this.defaultSubjectAttributeReader.getAttributesByScope(eq(this.testSubject.getSubjectIdentifier()),
                anySetOf(Attribute.class))).thenReturn(Collections.emptySet());
    }

    @Test
    public void testGetSubjectAttributes() {
        Set<Attribute> subjectAttributes = new HashSet<>();
        subjectAttributes.add(new Attribute("https://acs.attributes.int", "role", "administrator"));
        this.testSubject.setAttributes(subjectAttributes);

        Set<Attribute> supplementalSubjectAttributes = new HashSet<>();
        supplementalSubjectAttributes.add(new Attribute("https://acs.attributes.int", "site", "sanramon"));

        // mock attribute service for the expected resource URI after attributeURITemplate is applied
        when(this.defaultSubjectAttributeReader.getAttributesByScope(eq(this.testSubject.getSubjectIdentifier()),
                anySetOf(Attribute.class))).thenReturn(subjectAttributes);
        SubjectAttributeResolver resolver = new SubjectAttributeResolver(this.defaultSubjectAttributeReader,
                this.testSubject.getSubjectIdentifier(), supplementalSubjectAttributes);

        Set<Attribute> combinedSubjectAttributes = resolver.getResult(null);
        Assert.assertNotNull(combinedSubjectAttributes);
        Assert.assertTrue(combinedSubjectAttributes.containsAll(subjectAttributes));
        Assert.assertTrue(combinedSubjectAttributes.containsAll(supplementalSubjectAttributes));
    }

    @Test
    public void testGetSubjectAttributesNoSubjectFoundAndNoSupplementalAttributes() {
        SubjectAttributeResolver resolver = new SubjectAttributeResolver(this.defaultSubjectAttributeReader,
                "NonExistingURI", null);

        Set<Attribute> combinedSubjectAttributes = resolver.getResult(null);
        Assert.assertTrue(combinedSubjectAttributes.isEmpty());
    }

    @Test
    public void testGetSubjectAttributesSubjectFoundWithNoAttributesAndNoSupplementalAttributes() {
        SubjectAttributeResolver resolver = new SubjectAttributeResolver(this.defaultSubjectAttributeReader,
                this.testSubject.getSubjectIdentifier(), null);

        Set<Attribute> combinedSubjectAttributes = resolver.getResult(null);
        Assert.assertTrue(combinedSubjectAttributes.isEmpty());
    }

    @Test
    public void testGetSubjectAttributesSubjectFoundWithAttributesAndNoSupplementalAttributes() {
        Set<Attribute> subjectAttributes = new HashSet<>();
        subjectAttributes.add(new Attribute("https://acs.attributes.int", "role", "administrator"));
        this.testSubject.setAttributes(subjectAttributes);

        when(this.defaultSubjectAttributeReader.getAttributesByScope(eq(this.testSubject.getSubjectIdentifier()),
                anySetOf(Attribute.class))).thenReturn(subjectAttributes);
        SubjectAttributeResolver resolver = new SubjectAttributeResolver(this.defaultSubjectAttributeReader,
                this.testSubject.getSubjectIdentifier(), null);

        Set<Attribute> combinedSubjectAttributes = resolver.getResult(null);
        Assert.assertNotNull(combinedSubjectAttributes);
        Assert.assertTrue(combinedSubjectAttributes.containsAll(subjectAttributes));
    }

    @Test
    public void testGetSubjectAttributesSupplementalAttributesOnly() {
        Set<Attribute> supplementalSubjectAttributes = new HashSet<>();
        supplementalSubjectAttributes.add(new Attribute("https://acs.attributes.int", "site", "sanramon"));

        SubjectAttributeResolver resolver = new SubjectAttributeResolver(this.defaultSubjectAttributeReader,
                this.testSubject.getSubjectIdentifier(), supplementalSubjectAttributes);

        Set<Attribute> combinedSubjectAttributes = resolver.getResult(null);
        Assert.assertNotNull(combinedSubjectAttributes);
        Assert.assertTrue(combinedSubjectAttributes.containsAll(supplementalSubjectAttributes));
    }
}
