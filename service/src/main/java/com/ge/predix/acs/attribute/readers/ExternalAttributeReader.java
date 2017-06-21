package com.ge.predix.acs.attribute.readers;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${MAX_NUMBER_OF_ATTRIBUTES:1500}")
    private int maxNumberOfAttributes = 1500;

    @Value("${MAX_SIZE_OF_ATTRIBUTES_IN_BYTES:500000}")
    private int maxSizeOfAttributesInBytes = 500000;

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(ExternalAttributeReader.class);
    private static final String ID = "id";

    private final int adapterTimeoutMillis;
    private final AttributeConnectorService connectorService;
    private final AttributeCache attributeCache;
    private final Map<AttributeAdapterConnection, OAuth2RestTemplate> adapterRestTemplateCache = new
            ConcurrentReferenceHashMap<>();

    public ExternalAttributeReader(final AttributeConnectorService connectorService,
            final AttributeCache attributeCache, final int adapterTimeoutMillis) {
        this.connectorService = connectorService;
        this.attributeCache = attributeCache;
        this.adapterTimeoutMillis = adapterTimeoutMillis;
    }

    AttributeConnectorService getConnectorService() {
        return this.connectorService;
    }

    /**
     * Tries to get the attributes for the identifier in the cache. If the attributes are not in the cache, uses the
     * configured adapters for the zone to retrieve the attributes.
     *
     * @param identifier The identifier of the subject or resource to retrieve attributes for.
     * @return The set of attributes corresponding to the attributes id passed as a parameter.
     * @throws AttributeRetrievalException Throw this exception if the Attributes returned from the adapter are too
     *                                     large or there was a connection problem to the adapter.
     */
    @Override
    public Set<Attribute> getAttributes(final String identifier) {
        CachedAttributes cachedAttributes = this.attributeCache.getAttributes(identifier);
        if (null == cachedAttributes) {
            LOGGER.trace("Attributes not found in cache");
            // If get returns null then key either doesn't exist in cache or has been evicted.
            // Circuit breaker story to check adapter connection to be done soon.
            cachedAttributes = getAttributesFromAdapters(identifier);
            this.attributeCache.setAttributes(identifier, cachedAttributes);
        }
        if (cachedAttributes.getState().equals(CachedAttributes.State.DO_NOT_RETRY)) {
            // If get returns CachedAttributes with DO_NOT_RETRY throw the storage exception.
            throw new AttributeRetrievalException(AttributeRetrievalException.getStorageErrorMessage(identifier));
        }
        return cachedAttributes.getAttributes();
    }

    private void setRequestFactory(final OAuth2RestTemplate restTemplate) {
        CloseableHttpClient httpClient = HttpClientBuilder.create().useSystemProperties().build();
        HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(this.adapterTimeoutMillis);
        requestFactory.setConnectTimeout(this.adapterTimeoutMillis);
        requestFactory.setConnectionRequestTimeout(this.adapterTimeoutMillis);
        restTemplate.setRequestFactory(requestFactory);
    }

    OAuth2RestTemplate getAdapterOauth2RestTemplate(final AttributeAdapterConnection adapterConnection) {
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

    CachedAttributes getAttributesFromAdapters(final String identifier) {

        CachedAttributes cachedAttributes = new CachedAttributes(Collections.emptySet());

        Set<AttributeAdapterConnection> adapterConnections = this.getAttributeAdapterConnections();
        AttributeAdapterConnection adapterConnection = matchAdapterConnection(adapterConnections, identifier);

        if (null != adapterConnection) {

            String adapterUrl = UriComponentsBuilder.fromUriString(adapterConnection.getAdapterEndpoint())
                    .queryParam(ID, identifier).toUriString();

            AttributesResponse attributesResponse;

            try {
                attributesResponse = this.getAdapterOauth2RestTemplate(adapterConnection).
                        getForEntity(adapterUrl, AttributesResponse.class).getBody();
            } catch (final Exception e) {
                LOGGER.debug(AttributeRetrievalException.getAdapterErrorMessage(adapterUrl), e);
                throw new AttributeRetrievalException(AttributeRetrievalException.getAdapterErrorMessage(identifier));
            }

            if (isSizeLimitsExceeded(attributesResponse.getAttributes())) {
                LOGGER.debug(AttributeRetrievalException.getStorageErrorMessage(identifier));
                cachedAttributes.setState(CachedAttributes.State.DO_NOT_RETRY);
                return cachedAttributes;
            }
            cachedAttributes.setAttributes(attributesResponse.getAttributes());
        }

        return cachedAttributes;
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

    private boolean isSizeLimitsExceeded(final Set<Attribute> attributes) {
        if (attributes.size() > this.maxNumberOfAttributes) {
            return true;
        }
        long size = 0;
        for (Attribute attribute : attributes) {
            size += size(attribute);
            if (size > this.maxSizeOfAttributesInBytes) {
                return true;
            }
        }
        return false;
    }

    private int size(final Attribute attribute) {
        int size = 0;
        String issuer = attribute.getIssuer();
        String name = attribute.getName();
        String value = attribute.getValue();
        if (null != issuer) {
            size += issuer.length();
        }
        if (null != name) {
            size += name.length();
        }
        if (null != value) {
            size += value.length();
        }
        return size;
    }

    abstract Set<AttributeAdapterConnection> getAttributeAdapterConnections();
}
