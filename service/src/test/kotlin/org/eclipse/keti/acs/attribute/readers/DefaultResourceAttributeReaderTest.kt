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

package org.eclipse.keti.acs.attribute.readers

import com.nhaarman.mockito_kotlin.any
import org.eclipse.keti.acs.model.Attribute
import org.eclipse.keti.acs.privilege.management.PrivilegeManagementService
import org.eclipse.keti.acs.rest.BaseResource
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.springframework.beans.factory.annotation.Autowired
import org.testng.Assert
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Test
import java.util.HashSet

@Test
class DefaultResourceAttributeReaderTest {

    @Mock
    private lateinit var privilegeManagementService: PrivilegeManagementService

    @Autowired
    @InjectMocks
    private lateinit var defaultResourceAttributeReader: PrivilegeServiceResourceAttributeReader

    @BeforeMethod
    fun beforeMethod() {
        MockitoAnnotations.initMocks(this)
    }

    @Test
    @Throws(Exception::class)
    fun testGetAttributes() {
        val resourceAttributes = HashSet<Attribute>()
        resourceAttributes.add(Attribute("https://acs.attributes.int", "site", "sanramon"))
        val testResource = BaseResource("/test/resource", resourceAttributes)

        `when`<BaseResource>(
            this.privilegeManagementService.getByResourceIdentifierWithInheritedAttributes(
                any()
            )
        )
            .thenReturn(testResource)
        Assert.assertTrue(
            this.defaultResourceAttributeReader.getAttributes(testResource.resourceIdentifier!!)
                .containsAll(resourceAttributes)
        )
    }

    @Test
    @Throws(Exception::class)
    fun testGetAttributesForNonExistentResource() {
        `when`<BaseResource>(
            this.privilegeManagementService.getByResourceIdentifierWithInheritedAttributes(
                any()
            )
        ).thenReturn(null)
        Assert.assertTrue(this.defaultResourceAttributeReader.getAttributes("nonexistentResource").isEmpty())
    }
}
