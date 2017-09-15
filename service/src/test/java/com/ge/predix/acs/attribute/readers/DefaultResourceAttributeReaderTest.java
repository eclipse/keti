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

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;

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
import com.ge.predix.acs.rest.BaseResource;

@Test
public class DefaultResourceAttributeReaderTest {
    @Mock
    private PrivilegeManagementService privilegeManagementService;

    @Autowired
    @InjectMocks
    private PrivilegeServiceResourceAttributeReader defaultResourceAttributeReader;

    @BeforeMethod
    public void beforeMethod() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testGetAttributes() throws Exception {
        Set<Attribute> resourceAttributes = new HashSet<>();
        resourceAttributes.add(new Attribute("https://acs.attributes.int", "site", "sanramon"));
        BaseResource testResource = new BaseResource("/test/resource", resourceAttributes);

        when(this.privilegeManagementService.getByResourceIdentifierWithInheritedAttributes(any()))
                .thenReturn(testResource);
        Assert.assertTrue(this.defaultResourceAttributeReader.getAttributes(testResource.getResourceIdentifier())
                .containsAll(resourceAttributes));
    }

    @Test
    public void testGetAttributesForNonExistentResource() throws Exception {
        when(this.privilegeManagementService.getByResourceIdentifierWithInheritedAttributes(any())).thenReturn(null);
        Assert.assertTrue(this.defaultResourceAttributeReader.getAttributes("nonexistentResource").isEmpty());
    }
}
