package com.ge.predix.acs.attribute.readers;

import java.util.Collections;
import java.util.Set;

import org.apache.commons.lang3.RandomStringUtils;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.ge.predix.acs.attribute.cache.AttributeCache;
import com.ge.predix.acs.model.Attribute;

public class ExternalSubjectAttributeReaderTest {

    @Spy
    private ExternalSubjectAttributeReader externalSubjectAttributeReader;

    private static final String IDENTIFIER = generateRandomString();

    private Set<Attribute> expectedAttributes;

    private static String generateRandomString() {
        return RandomStringUtils.randomAlphanumeric(20);
    }

    @BeforeClass
    void beforeClass() {
        this.externalSubjectAttributeReader = new ExternalSubjectAttributeReader(Mockito.mock(AttributeCache.class));
        this.expectedAttributes = Collections
                .singleton(new Attribute(generateRandomString(), generateRandomString(), generateRandomString()));
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testGetAttributesByScope() throws Exception {
        Mockito.doReturn(this.expectedAttributes).when(this.externalSubjectAttributeReader)
                .getAttributesFromAdapters(IDENTIFIER);

        Set<Attribute> actualAttributes = this.externalSubjectAttributeReader.getAttributes(IDENTIFIER);

        Assert.assertEquals(this.expectedAttributes, actualAttributes);

        actualAttributes = this.externalSubjectAttributeReader.getAttributesByScope(IDENTIFIER,
                Collections.singleton(new Attribute("test-issuer", "test-scope", "test-value")));

        Assert.assertEquals(this.expectedAttributes, actualAttributes);
    }
}
