package com.ge.predix.acs.attribute.readers;

import java.util.Collections;
import java.util.Set;

import org.apache.commons.lang3.RandomStringUtils;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.ge.predix.acs.attribute.cache.AttributeCache;
import com.ge.predix.acs.model.Attribute;

public class ExternalAttributeReaderTest {

    @Mock
    private AttributeCache attributeCache;

    @InjectMocks
    @Spy
    private ExternalAttributeReader externalAttributeReader;

    private static final String ZONE_ID = generateRandomString();
    private static final String IDENTIFIER = generateRandomString();

    private Set<Attribute> expectedAttributes;

    private static String generateRandomString() {
        return RandomStringUtils.randomAlphanumeric(20);
    }

    @BeforeClass
    void beforeClass() {
        this.expectedAttributes = Collections
                .singleton(new Attribute(generateRandomString(), generateRandomString(), generateRandomString()));
        MockitoAnnotations.initMocks(this);
    }

    @BeforeMethod
    void beforeMethod() {
        Mockito.doReturn(ZONE_ID).when(this.externalAttributeReader).getZoneId();
    }

    @Test
    public void testGetAttributesWithCacheMiss() throws Exception {
        Mockito.when(this.attributeCache.getAttributes(IDENTIFIER)).thenReturn(Collections.emptySet());

        Mockito.doReturn(this.expectedAttributes).when(this.externalAttributeReader)
                .getAttributesFromAdapters(IDENTIFIER);

        Set<Attribute> actualAttributes = this.externalAttributeReader.getAttributes(IDENTIFIER);

        Mockito.verify(this.attributeCache).setAttributes(IDENTIFIER, this.expectedAttributes);

        Assert.assertEquals(this.expectedAttributes, actualAttributes);
    }

    @Test
    public void testGetAttributesWithCacheHit() throws Exception {
        Mockito.when(this.attributeCache.getAttributes(IDENTIFIER)).thenReturn(this.expectedAttributes);

        Set<Attribute> actualAttributes = this.externalAttributeReader.getAttributes(IDENTIFIER);

        Mockito.verify(this.externalAttributeReader, Mockito.times(0)).getAttributesFromAdapters(IDENTIFIER);
        Mockito.verify(this.attributeCache, Mockito.times(0)).setAttributes(IDENTIFIER, this.expectedAttributes);

        Assert.assertEquals(this.expectedAttributes, actualAttributes);
    }
}
