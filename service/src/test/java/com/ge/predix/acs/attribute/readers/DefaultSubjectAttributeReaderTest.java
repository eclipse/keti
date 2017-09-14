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

package com.ge.predix.acs.attribute.readers;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.ge.predix.acs.model.Attribute;
import com.ge.predix.acs.privilege.management.PrivilegeManagementService;
import com.ge.predix.acs.rest.BaseSubject;

@Test
public class DefaultSubjectAttributeReaderTest {
    @Mock
    private PrivilegeManagementService privilegeManagementService;

    @Autowired
    @InjectMocks
    private PrivilegeServiceSubjectAttributeReader defaultSubjectAttributeReader;

    @BeforeMethod
    public void beforeMethod() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testGetAttributes() throws Exception {
        Set<Attribute> subjectAttributes = new HashSet<>();
        subjectAttributes.add(new Attribute("https://acs.attributes.int", "role", "administrator"));
        BaseSubject testSubject = new BaseSubject("/test/subject", subjectAttributes);

        when(this.privilegeManagementService.getBySubjectIdentifierAndScopes(any(), eq(Collections.emptySet())))
                .thenReturn(testSubject);
        Assert.assertTrue(this.defaultSubjectAttributeReader.getAttributes(testSubject.getSubjectIdentifier())
                .containsAll(subjectAttributes));
    }

    @Test
    public void testGetAttributesForNonExistentSubject() throws Exception {
        when(this.privilegeManagementService.getBySubjectIdentifierAndScopes(any(), eq(Collections.emptySet())))
                .thenReturn(null);
        Assert.assertTrue(this.defaultSubjectAttributeReader.getAttributes("nonexistentSubject").isEmpty());
    }

    @Test
    public void testGetAttributesByScope() throws Exception {
        Set<Attribute> subjectAttributes = new HashSet<>();
        subjectAttributes.add(new Attribute("https://acs.attributes.int", "role", "administrator"));
        BaseSubject testSubject = new BaseSubject("/test/subject", subjectAttributes);

        when(this.privilegeManagementService.getBySubjectIdentifierAndScopes(any(), any())).thenReturn(testSubject);
        Assert.assertTrue(this.defaultSubjectAttributeReader
                .getAttributesByScope(testSubject.getSubjectIdentifier(), Collections.emptySet())
                .containsAll(subjectAttributes));
    }

    @Test
    public void testGetAttributesByScopeForNonExistentSubject() throws Exception {
        when(this.privilegeManagementService.getBySubjectIdentifierAndScopes(any(), any())).thenReturn(null);
        Assert.assertTrue(this.defaultSubjectAttributeReader
                .getAttributesByScope("nonexistentSubject", Collections.emptySet()).isEmpty());
    }
}
