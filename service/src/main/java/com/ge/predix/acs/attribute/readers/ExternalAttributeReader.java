package com.ge.predix.acs.attribute.readers;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.token.grant.client.ClientCredentialsResourceDetails;
import org.springframework.util.ConcurrentReferenceHashMap;
import org.springframework.web.util.UriComponentsBuilder;

import com.ge.predix.acs.attribute.cache.AttributeCache;
import com.ge.predix.acs.attribute.connector.management.AttributeConnectorService;
import com.ge.predix.acs.model.Attribute;
import com.ge.predix.acs.rest.AttributeAdapterConnection;
import com.ge.predix.acs.rest.attribute.adapter.AttributesResponse;

public abstract class ExternalAttributeReader implements AttributeReader {
    private static final String ID = "id";

    private final int adapterTimeoutMillis;
    private final AttributeConnectorService connectorService;
    private final AttributeCache attributeCache;
    private final Map<AttributeAdapterConnection, OAuth2RestTemplate> adapterRestTemplateCache =
            new ConcurrentReferenceHashMap<>();

    public ExternalAttributeReader(final AttributeConnectorService connectorService,
            final AttributeCache attributeCache, final int adapterTimeoutMillis) {
        this.connectorService = connectorService;
        this.attributeCache = attributeCache;
        this.adapterTimeoutMillis = adapterTimeoutMillis;
    }
    
    AttributeConnectorService getConnectorService() {
        return this.connectorService;
    }

    @Override
    public Set<Attribute> getAttributes(final String identifier) {
        Set<Attribute> attributes = this.attributeCache.getAttributes(identifier);
        if (CollectionUtils.isEmpty(attributes)) {
            attributes = this.getAttributesFromAdapters(identifier);
            this.attributeCache.setAttributes(identifier, attributes);
        }
        return attributes;
    }

    private void setRequestFactory(final OAuth2RestTemplate restTemplate) {
        CloseableHttpClient httpClient = HttpClientBuilder.create().useSystemProperties().build();
        HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(this.adapterTimeoutMillis);
        requestFactory.setConnectTimeout(this.adapterTimeoutMillis);
        requestFactory.setConnectionRequestTimeout(this.adapterTimeoutMillis);
        restTemplate.setRequestFactory(requestFactory);
    }

    private OAuth2RestTemplate getAdapterOauth2RestTemplate(final AttributeAdapterConnection adapterConnection) {
        String uaaTokenUrl = adapterConnection.getUaaTokenUrl();
        String uaaClientId = adapterConnection.getUaaClientId();
        String uaaClientSecret = adapterConnection.getUaaClientSecret();

        OAuth2RestTemplate oAuth2RestTemplate = this.adapterRestTemplateCache.get(adapterConnection);
        if (oAuth2RestTemplate != null) {
            return oAuth2RestTemplate;
        }

        ClientCredentialsResourceDetails clientCredentials = new ClientCredentialsResourceDetails();
        clientCredentials.setAccessTokenUri(uaaTokenUrl);
        clientCredentials.setClientId(uaaClientId);
        clientCredentials.setClientSecret(uaaClientSecret);
        oAuth2RestTemplate = new OAuth2RestTemplate(clientCredentials);
        this.setRequestFactory(oAuth2RestTemplate);
        this.adapterRestTemplateCache.put(adapterConnection, oAuth2RestTemplate);
        return oAuth2RestTemplate;
    }

    @SuppressWarnings("squid:RedundantThrowsDeclarationCheck")
    Set<Attribute> getAttributesFromAdapters(final String identifier) throws AttributeRetrievalException {
        Set<Attribute> attributes = Collections.emptySet();

        Set<AttributeAdapterConnection> adapterConnections = this.getAttributeAdapterConnections();
        AttributeAdapterConnection adapterConnection = matchAdapterConnection(adapterConnections, identifier);

        if (null != adapterConnection) {
            OAuth2RestTemplate adapter = this.getAdapterOauth2RestTemplate(adapterConnection);

            String adapterUrl = UriComponentsBuilder.fromUriString(adapterConnection.getAdapterEndpoint())
                    .queryParam(ID, identifier).toUriString();

            try {
                ResponseEntity<AttributesResponse> attributesResponse = adapter.getForEntity(adapterUrl,
                        AttributesResponse.class);
                attributes = attributesResponse.getBody().getAttributes();
            } catch (final Throwable e) {
                throw new AttributeRetrievalException(AttributeRetrievalException.getAdapterErrorMessage(adapterUrl),
                        e);
            }
        }
        
        return attributes;
    }

    //Matching mechanism of a resource/subject id to a adapter is yet to be defined. Current implementation only 
    //supports exactly one adapterConnection per connector.
    private AttributeAdapterConnection matchAdapterConnection(final Set<AttributeAdapterConnection> adapterConnections,
            final String identifier) {
        if (1 == adapterConnections.size()) {
            return adapterConnections.iterator().next();
        } else {
            throw new IllegalStateException("Connector must have exactly one adapterConnection.");
        }
    }

    abstract Set<AttributeAdapterConnection> getAttributeAdapterConnections();
}
