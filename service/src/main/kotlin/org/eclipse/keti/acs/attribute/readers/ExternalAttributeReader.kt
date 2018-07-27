/*******************************************************************************
 * Copyright 2017 General Electric Company
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

import org.apache.http.impl.client.HttpClientBuilder
import org.eclipse.keti.acs.attribute.cache.AttributeCache
import org.eclipse.keti.acs.attribute.connector.management.AttributeConnectorService
import org.eclipse.keti.acs.model.Attribute
import org.eclipse.keti.acs.rest.AttributeAdapterConnection
import org.eclipse.keti.acs.rest.attribute.adapter.AttributesResponse
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory
import org.springframework.security.oauth2.client.OAuth2RestTemplate
import org.springframework.security.oauth2.client.token.grant.client.ClientCredentialsResourceDetails
import org.springframework.util.ConcurrentReferenceHashMap
import org.springframework.web.util.UriComponentsBuilder

private val LOGGER = LoggerFactory.getLogger(ExternalAttributeReader::class.java)
private const val ID = "id"

abstract class ExternalAttributeReader(
    internal val connectorService: AttributeConnectorService?,
    private val attributeCache: AttributeCache,
    private val adapterTimeoutMillis: Int
) : AttributeReader {

    @Value("\${MAX_NUMBER_OF_ATTRIBUTES:1500}")
    private var maxNumberOfAttributes: Int = 1500

    @Value("\${MAX_SIZE_OF_ATTRIBUTES_IN_BYTES:500000}")
    private var maxSizeOfAttributesInBytes: Int = 500000

    private val adapterRestTemplateCache = ConcurrentReferenceHashMap<AttributeAdapterConnection, OAuth2RestTemplate>()

    abstract val attributeAdapterConnections: Set<AttributeAdapterConnection>?

    /**
     * Tries to get the attributes for the identifier in the cache. If the attributes are not in the cache, uses the
     * configured adapters for the zone to retrieve the attributes.
     *
     * @param identifier The identifier of the subject or resource to retrieve attributes for.
     * @return The set of attributes corresponding to the attributes id passed as a parameter.
     * @throws AttributeRetrievalException Throw this exception if the Attributes returned from the adapter are too
     * large or there was a connection problem to the adapter.
     */
    override fun getAttributes(identifier: String): Set<Attribute>? {
        var cachedAttributes: CachedAttributes? = this.attributeCache.getAttributes(identifier)
        if (null == cachedAttributes) {
            LOGGER.trace("Attributes not found in cache")
            // If get returns null then key either doesn't exist in cache or has been evicted.
            // Circuit breaker story to check adapter connection to be done soon.
            cachedAttributes = getAttributesFromAdapters(identifier)
            this.attributeCache.setAttributes(identifier, cachedAttributes)
        }
        if (cachedAttributes.state == CachedAttributes.State.DO_NOT_RETRY) {
            // If get returns CachedAttributes with DO_NOT_RETRY throw the storage exception.
            throw AttributeRetrievalException(getStorageErrorMessage(identifier))
        }
        return cachedAttributes.attributes
    }

    private fun setRequestFactory(restTemplate: OAuth2RestTemplate) {
        val httpClient = HttpClientBuilder.create().useSystemProperties().build()
        val requestFactory = HttpComponentsClientHttpRequestFactory(httpClient)
        requestFactory.setReadTimeout(this.adapterTimeoutMillis)
        requestFactory.setConnectTimeout(this.adapterTimeoutMillis)
        requestFactory.setConnectionRequestTimeout(this.adapterTimeoutMillis)
        restTemplate.requestFactory = requestFactory
    }

    fun getAdapterOauth2RestTemplate(adapterConnection: AttributeAdapterConnection): OAuth2RestTemplate {
        val uaaTokenUrl = adapterConnection.uaaTokenUrl
        val uaaClientId = adapterConnection.uaaClientId
        val uaaClientSecret = adapterConnection.uaaClientSecret

        var oAuth2RestTemplate: OAuth2RestTemplate? = this.adapterRestTemplateCache[adapterConnection]
        if (oAuth2RestTemplate != null) {
            return oAuth2RestTemplate
        }

        val clientCredentials = ClientCredentialsResourceDetails()
        clientCredentials.accessTokenUri = uaaTokenUrl
        clientCredentials.clientId = uaaClientId
        clientCredentials.clientSecret = uaaClientSecret
        oAuth2RestTemplate = OAuth2RestTemplate(clientCredentials)
        this.setRequestFactory(oAuth2RestTemplate)
        this.adapterRestTemplateCache[adapterConnection] = oAuth2RestTemplate
        return oAuth2RestTemplate
    }

    fun getAttributesFromAdapters(identifier: String): CachedAttributes {

        val cachedAttributes = CachedAttributes(emptySet())

        val adapterConnections = this.attributeAdapterConnections
        val adapterConnection = matchAdapterConnection(adapterConnections!!)

        if (null != adapterConnection) {

            val adapterUrl = UriComponentsBuilder.fromUriString(adapterConnection.adapterEndpoint)
                .queryParam(ID, identifier).toUriString()

            val attributesResponse: AttributesResponse

            try {
                attributesResponse = this.getAdapterOauth2RestTemplate(adapterConnection)
                    .getForEntity(adapterUrl, AttributesResponse::class.java).body
            } catch (e: Exception) {
                LOGGER.debug(getAdapterErrorMessage(adapterUrl), e)
                throw AttributeRetrievalException(getAdapterErrorMessage(identifier))
            }

            if (isSizeLimitsExceeded(attributesResponse.attributes!!)) {
                LOGGER.debug(getStorageErrorMessage(identifier))
                cachedAttributes.state = CachedAttributes.State.DO_NOT_RETRY
                return cachedAttributes
            }
            cachedAttributes.attributes = attributesResponse.attributes
        }

        return cachedAttributes
    }

    // Matching mechanism of a resource/subject id to a adapter is yet to be defined. Current implementation only
    // supports exactly one adapterConnection per connector.
    private fun matchAdapterConnection(
        adapterConnections: Set<AttributeAdapterConnection>
    ): AttributeAdapterConnection? {
        return if (1 == adapterConnections.size) {
            adapterConnections.iterator().next()
        } else {
            throw IllegalStateException("Connector must have exactly one adapterConnection.")
        }
    }

    private fun isSizeLimitsExceeded(attributes: Set<Attribute>): Boolean {
        if (attributes.size > this.maxNumberOfAttributes) {
            return true
        }
        var size: Long = 0
        for (attribute in attributes) {
            size += size(attribute).toLong()
            if (size > this.maxSizeOfAttributesInBytes) {
                return true
            }
        }
        return false
    }

    private fun size(attribute: Attribute): Int {
        var size = 0
        val issuer = attribute.issuer
        val name = attribute.name
        val value = attribute.value
        if (null != issuer) {
            size += issuer.length
        }
        if (null != name) {
            size += name.length
        }
        if (null != value) {
            size += value.length
        }
        return size
    }
}
