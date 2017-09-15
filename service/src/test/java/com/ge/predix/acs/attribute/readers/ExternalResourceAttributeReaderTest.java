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

import java.util.Collections;
import java.util.Set;

import org.apache.commons.lang3.RandomStringUtils;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.springframework.test.util.ReflectionTestUtils;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.ge.predix.acs.attribute.cache.AttributeCache;
import com.ge.predix.acs.model.Attribute;

public class ExternalResourceAttributeReaderTest {

    @Spy
    private AttributeCache attributeCache;

    @InjectMocks
    @Spy
    private ExternalResourceAttributeReader externalResourceAttributeReader;

    private static final String IDENTIFIER = "part/03f95db1-4255-4265-a509-f7bca3e1fee4";

    private CachedAttributes expectedAdapterAttributes;

    private static String generateRandomString() {
        return RandomStringUtils.randomAlphanumeric(20);
    }

    @BeforeClass
    void beforeClass() throws Exception {
        this.expectedAdapterAttributes = new CachedAttributes(Collections
                .singleton(new Attribute(generateRandomString(), generateRandomString(), generateRandomString())));
    }

    @BeforeMethod
    void beforeMethod() {
        this.attributeCache = Mockito.mock(AttributeCache.class);
        this.externalResourceAttributeReader = new ExternalResourceAttributeReader(null, this.attributeCache, 3000);

        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testGetAttributesWithCacheMiss() throws Exception {
        Mockito.when(this.attributeCache.getAttributes(IDENTIFIER)).thenReturn(null);

        Mockito.doReturn(this.expectedAdapterAttributes).when(this.externalResourceAttributeReader)
                .getAttributesFromAdapters(IDENTIFIER);

        Set<Attribute> actualAdapterAttributes = this.externalResourceAttributeReader.getAttributes(IDENTIFIER);

        Mockito.verify(this.attributeCache).setAttributes(IDENTIFIER, this.expectedAdapterAttributes);

        Assert.assertEquals(this.expectedAdapterAttributes.getAttributes(), actualAdapterAttributes);
    }

    @Test
    public void testGetAttributesWithCacheHit() throws Exception {
        Mockito.when(this.attributeCache.getAttributes(IDENTIFIER)).thenReturn(this.expectedAdapterAttributes);

        Set<Attribute> actualAdapterAttributes = this.externalResourceAttributeReader.getAttributes(IDENTIFIER);

        Mockito.verify(this.externalResourceAttributeReader, Mockito.times(0)).getAttributesFromAdapters(IDENTIFIER);
        Mockito.verify(this.attributeCache, Mockito.times(0)).setAttributes(IDENTIFIER, this.expectedAdapterAttributes);

        Assert.assertEquals(this.expectedAdapterAttributes.getAttributes(), actualAdapterAttributes);
    }

    @Test(dataProviderClass = ExternalAttributeReaderHelper.class,
            dataProvider = "attributeSizeConstraintDataProvider",
            expectedExceptions = { AttributeRetrievalException.class },
            expectedExceptionsMessageRegExp = "Total size of attributes or "
                    + "number of attributes too large for id: '" + IDENTIFIER + "'.*")
    public void testGetAttributesThatAreToLarge(final int maxNumberOfAttributes, final int maxSizeOfAttributesInBytes)
            throws Exception {
        ExternalAttributeReaderHelper
                .setupMockedAdapterResponse(this.externalResourceAttributeReader, this.attributeCache, IDENTIFIER);
        ReflectionTestUtils
                .setField(this.externalResourceAttributeReader, "maxNumberOfAttributes", maxNumberOfAttributes);
        ReflectionTestUtils.setField(this.externalResourceAttributeReader, "maxSizeOfAttributesInBytes",
                maxSizeOfAttributesInBytes);
        this.externalResourceAttributeReader.getAttributes(IDENTIFIER);
    }

}
