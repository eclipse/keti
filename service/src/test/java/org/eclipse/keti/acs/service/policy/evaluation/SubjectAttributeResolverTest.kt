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

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.eq
import com.nhaarman.mockito_kotlin.isNull
import org.eclipse.keti.acs.attribute.readers.PrivilegeServiceSubjectAttributeReader
import org.eclipse.keti.acs.model.Attribute
import org.eclipse.keti.acs.rest.BaseSubject
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.testng.Assert
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Test
import java.util.HashSet

@Test
class SubjectAttributeResolverTest {

    @Mock
    private lateinit var defaultSubjectAttributeReader: PrivilegeServiceSubjectAttributeReader

    private var testSubject: BaseSubject? = null

    @BeforeMethod
    fun beforeMethod() {
        MockitoAnnotations.initMocks(this)

        this.testSubject = BaseSubject("/test/subject")
        `when`(
            this.defaultSubjectAttributeReader.getAttributesByScope(
                eq(this.testSubject!!.subjectIdentifier!!),
                any()
            )
        ).thenReturn(emptySet())
    }

    @Test
    fun testGetSubjectAttributes() {
        val subjectAttributes = HashSet<Attribute>()
        subjectAttributes.add(Attribute("https://acs.attributes.int", "role", "administrator"))
        this.testSubject!!.attributes = subjectAttributes

        val supplementalSubjectAttributes = HashSet<Attribute>()
        supplementalSubjectAttributes.add(Attribute("https://acs.attributes.int", "site", "sanramon"))

        // mock attribute service for the expected resource URI after attributeURITemplate is applied
        `when`(
            this.defaultSubjectAttributeReader.getAttributesByScope(
                eq(this.testSubject!!.subjectIdentifier!!),
                isNull()
            )
        ).thenReturn(subjectAttributes)
        val resolver = SubjectAttributeResolver(
            this.defaultSubjectAttributeReader,
            this.testSubject!!.subjectIdentifier!!, supplementalSubjectAttributes
        )

        val combinedSubjectAttributes = resolver.getResult(null)
        Assert.assertNotNull(combinedSubjectAttributes)
        Assert.assertTrue(combinedSubjectAttributes.containsAll(subjectAttributes))
        Assert.assertTrue(combinedSubjectAttributes.containsAll(supplementalSubjectAttributes))
    }

    @Test
    fun testGetSubjectAttributesNoSubjectFoundAndNoSupplementalAttributes() {
        val resolver = SubjectAttributeResolver(
            this.defaultSubjectAttributeReader,
            "NonExistingURI", null
        )

        val combinedSubjectAttributes = resolver.getResult(null)
        Assert.assertTrue(combinedSubjectAttributes.isEmpty())
    }

    @Test
    fun testGetSubjectAttributesSubjectFoundWithNoAttributesAndNoSupplementalAttributes() {
        val resolver = SubjectAttributeResolver(
            this.defaultSubjectAttributeReader,
            this.testSubject!!.subjectIdentifier!!, null
        )

        val combinedSubjectAttributes = resolver.getResult(null)
        Assert.assertTrue(combinedSubjectAttributes.isEmpty())
    }

    @Test
    fun testGetSubjectAttributesSubjectFoundWithAttributesAndNoSupplementalAttributes() {
        val subjectAttributes = HashSet<Attribute>()
        subjectAttributes.add(Attribute("https://acs.attributes.int", "role", "administrator"))
        this.testSubject!!.attributes = subjectAttributes

        `when`(
            this.defaultSubjectAttributeReader.getAttributesByScope(
                eq(this.testSubject!!.subjectIdentifier!!),
                isNull()
            )
        ).thenReturn(subjectAttributes)
        val resolver = SubjectAttributeResolver(
            this.defaultSubjectAttributeReader,
            this.testSubject!!.subjectIdentifier!!, null
        )

        val combinedSubjectAttributes = resolver.getResult(null)
        Assert.assertNotNull(combinedSubjectAttributes)
        Assert.assertTrue(combinedSubjectAttributes.containsAll(subjectAttributes))
    }

    @Test
    fun testGetSubjectAttributesSupplementalAttributesOnly() {
        val supplementalSubjectAttributes = HashSet<Attribute>()
        supplementalSubjectAttributes.add(Attribute("https://acs.attributes.int", "site", "sanramon"))

        val resolver = SubjectAttributeResolver(
            this.defaultSubjectAttributeReader,
            this.testSubject!!.subjectIdentifier!!, supplementalSubjectAttributes
        )

        val combinedSubjectAttributes = resolver.getResult(null)
        Assert.assertNotNull(combinedSubjectAttributes)
        Assert.assertTrue(combinedSubjectAttributes.containsAll(supplementalSubjectAttributes))
    }
}
