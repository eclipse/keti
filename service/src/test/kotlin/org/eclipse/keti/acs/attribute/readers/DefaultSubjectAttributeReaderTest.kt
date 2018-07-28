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
import com.nhaarman.mockito_kotlin.eq
import org.eclipse.keti.acs.model.Attribute
import org.eclipse.keti.acs.privilege.management.PrivilegeManagementService
import org.eclipse.keti.acs.rest.BaseSubject
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
class DefaultSubjectAttributeReaderTest {

    @Mock
    private lateinit var privilegeManagementService: PrivilegeManagementService

    @Autowired
    @InjectMocks
    private lateinit var defaultSubjectAttributeReader: PrivilegeServiceSubjectAttributeReader

    @BeforeMethod
    fun beforeMethod() {
        MockitoAnnotations.initMocks(this)
    }

    @Test
    @Throws(Exception::class)
    fun testGetAttributes() {
        val subjectAttributes = HashSet<Attribute>()
        subjectAttributes.add(Attribute("https://acs.attributes.int", "role", "administrator"))
        val testSubject = BaseSubject("/test/subject", subjectAttributes)

        `when`<BaseSubject>(
            this.privilegeManagementService.getBySubjectIdentifierAndScopes(
                any(),
                eq(emptySet())
            )
        )
            .thenReturn(testSubject)
        Assert.assertTrue(
            this.defaultSubjectAttributeReader.getAttributes(testSubject.subjectIdentifier!!)
                .containsAll(subjectAttributes)
        )
    }

    @Test
    @Throws(Exception::class)
    fun testGetAttributesForNonExistentSubject() {
        `when`<BaseSubject>(
            this.privilegeManagementService.getBySubjectIdentifierAndScopes(
                any(),
                eq(emptySet())
            )
        )
            .thenReturn(null)
        Assert.assertTrue(this.defaultSubjectAttributeReader.getAttributes("nonexistentSubject").isEmpty())
    }

    @Test
    @Throws(Exception::class)
    fun testGetAttributesByScope() {
        val subjectAttributes = HashSet<Attribute>()
        subjectAttributes.add(Attribute("https://acs.attributes.int", "role", "administrator"))
        val testSubject = BaseSubject("/test/subject", subjectAttributes)

        `when`<BaseSubject>(
            this.privilegeManagementService.getBySubjectIdentifierAndScopes(
                any(),
                any()
            )
        ).thenReturn(testSubject)
        Assert.assertTrue(
            this.defaultSubjectAttributeReader
                .getAttributesByScope(testSubject.subjectIdentifier!!, emptySet())
                .containsAll(subjectAttributes)
        )
    }

    @Test
    @Throws(Exception::class)
    fun testGetAttributesByScopeForNonExistentSubject() {
        `when`<BaseSubject>(
            this.privilegeManagementService.getBySubjectIdentifierAndScopes(
                any(),
                any()
            )
        ).thenReturn(null)
        Assert.assertTrue(
            this.defaultSubjectAttributeReader
                .getAttributesByScope("nonexistentSubject", emptySet()).isEmpty()
        )
    }
}
