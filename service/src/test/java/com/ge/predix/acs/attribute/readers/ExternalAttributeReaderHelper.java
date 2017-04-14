package com.ge.predix.acs.attribute.readers;

import java.util.Collections;

import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.testng.annotations.DataProvider;

import com.ge.predix.acs.attribute.cache.AttributeCache;
import com.ge.predix.acs.model.Attribute;
import com.ge.predix.acs.rest.AttributeAdapterConnection;
import com.ge.predix.acs.rest.attribute.adapter.AttributesResponse;

public final class ExternalAttributeReaderHelper {

    private ExternalAttributeReaderHelper() {
        // Hiding constructor because this a test utility class
    }

    static void setupMockedAdapterResponse(final ExternalAttributeReader externalAttributeReader,
            final AttributeCache attributeCache, final String identifier) {
        ResponseEntity<AttributesResponse> attributeResponseResponseEntity = new ResponseEntity<>(
                new AttributesResponse(Collections.singleton(new Attribute("issuer", "name", "value")),
                        "attributesResponse"), HttpStatus.OK);
        OAuth2RestTemplate mockRestTemplate = Mockito.mock(OAuth2RestTemplate.class);
        Mockito.doReturn(attributeResponseResponseEntity).when(mockRestTemplate)
                .getForEntity(Mockito.anyString(), Mockito.eq(AttributesResponse.class));

        Mockito.doReturn(Collections.singleton(
                new AttributeAdapterConnection("https://my-url", "https://my-uaa", "my-client", "my-secret")))
                .when(externalAttributeReader).getAttributeAdapterConnections();

        Mockito.doReturn(mockRestTemplate).when(externalAttributeReader)
                .getAdapterOauth2RestTemplate(Mockito.any(AttributeAdapterConnection.class));
        Mockito.when(attributeCache.getAttributes(identifier)).thenReturn(null);
    }

    @DataProvider
    static Object[][] attributeSizeConstraintDataProvider() {
        return new Integer[][] { { 0, 100 }, { 100, 0 } };
    }

}
