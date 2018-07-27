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

package org.eclipse.keti.acs.attribute.readers

import org.apache.commons.lang3.RandomStringUtils
import org.eclipse.keti.acs.attribute.cache.AttributeCache
import org.eclipse.keti.acs.model.Attribute
import org.mockito.InjectMocks
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import org.mockito.Spy
import org.springframework.test.util.ReflectionTestUtils
import org.testng.Assert
import org.testng.annotations.BeforeClass
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Test

private const val IDENTIFIER = "part/03f95db1-4255-4265-a509-f7bca3e1fee4"

private fun generateRandomString(): String {
    return RandomStringUtils.randomAlphanumeric(20)
}

class ExternalResourceAttributeReaderTest {

    @Spy
    private lateinit var attributeCache: AttributeCache

    @InjectMocks
    @Spy
    private lateinit var externalResourceAttributeReader: ExternalResourceAttributeReader

    private var expectedAdapterAttributes: CachedAttributes? = null

    @BeforeClass
    @Throws(Exception::class)
    internal fun beforeClass() {
        this.expectedAdapterAttributes =
            CachedAttributes(setOf(Attribute(generateRandomString(), generateRandomString(), generateRandomString())))
    }

    @BeforeMethod
    internal fun beforeMethod() {
        this.attributeCache = Mockito.mock(AttributeCache::class.java)
        this.externalResourceAttributeReader = ExternalResourceAttributeReader(null, this.attributeCache, 3000)

        MockitoAnnotations.initMocks(this)
    }

    @Test
    @Throws(Exception::class)
    fun testGetAttributesWithCacheMiss() {
        Mockito.`when`<CachedAttributes>(this.attributeCache.getAttributes(IDENTIFIER)).thenReturn(null)

        Mockito.doReturn(this.expectedAdapterAttributes)
            .`when`<ExternalResourceAttributeReader>(this.externalResourceAttributeReader)
            .getAttributesFromAdapters(IDENTIFIER)

        val actualAdapterAttributes = this.externalResourceAttributeReader.getAttributes(IDENTIFIER)

        Mockito.verify<AttributeCache>(this.attributeCache).setAttributes(IDENTIFIER, this.expectedAdapterAttributes!!)

        Assert.assertEquals(this.expectedAdapterAttributes!!.attributes, actualAdapterAttributes)
    }

    @Test
    @Throws(Exception::class)
    fun testGetAttributesWithCacheHit() {
        Mockito.`when`<CachedAttributes>(this.attributeCache.getAttributes(IDENTIFIER))
            .thenReturn(this.expectedAdapterAttributes)

        val actualAdapterAttributes = this.externalResourceAttributeReader.getAttributes(IDENTIFIER)

        Mockito.verify<ExternalResourceAttributeReader>(this.externalResourceAttributeReader, Mockito.times(0))
            .getAttributesFromAdapters(IDENTIFIER)
        Mockito.verify<AttributeCache>(this.attributeCache, Mockito.times(0))
            .setAttributes(IDENTIFIER, this.expectedAdapterAttributes!!)

        Assert.assertEquals(this.expectedAdapterAttributes!!.attributes, actualAdapterAttributes)
    }

    @Test(
        dataProviderClass = ExternalAttributeReaderHelper::class,
        dataProvider = "attributeSizeConstraintDataProvider",
        expectedExceptions = [(AttributeRetrievalException::class)],
        expectedExceptionsMessageRegExp = "Total size of attributes or "
                                          + "number of attributes too large for id: '" + IDENTIFIER + "'.*"
    )
    @Throws(Exception::class)
    fun testGetAttributesThatAreToLarge(
        maxNumberOfAttributes: Int,
        maxSizeOfAttributesInBytes: Int
    ) {
        ExternalAttributeReaderHelper
            .setupMockedAdapterResponse(this.externalResourceAttributeReader, this.attributeCache, IDENTIFIER)
        ReflectionTestUtils
            .setField(this.externalResourceAttributeReader, "maxNumberOfAttributes", maxNumberOfAttributes)
        ReflectionTestUtils.setField(
            this.externalResourceAttributeReader, "maxSizeOfAttributesInBytes",
            maxSizeOfAttributesInBytes
        )
        this.externalResourceAttributeReader.getAttributes(IDENTIFIER)
    }
}
