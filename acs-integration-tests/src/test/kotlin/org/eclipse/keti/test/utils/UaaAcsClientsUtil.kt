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

package org.eclipse.keti.test.utils

import org.apache.http.impl.client.HttpClientBuilder
import org.eclipse.keti.acs.utils.JsonUtils
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.client.OAuth2RestTemplate
import org.springframework.security.oauth2.client.token.grant.client.ClientCredentialsResourceDetails
import org.springframework.security.oauth2.common.exceptions.InvalidClientException
import org.springframework.security.oauth2.provider.client.BaseClientDetails
import org.springframework.util.LinkedMultiValueMap
import java.util.ArrayList

private val JSON_UTILS = JsonUtils()

class UaaAcsClientsUtil(
    private val uaaUrl: String,
    adminSecret: String
) {

    private val uaaAdminTemplate: OAuth2RestTemplate

    init {
        this.uaaAdminTemplate = getOAuth2RestTemplateForClient(this.uaaUrl + "/oauth/token", "admin", adminSecret)
    }

    fun createAcsAdminClient(acsZones: List<String>): OAuth2RestTemplate {

        val authorities = ArrayList<SimpleGrantedAuthority>()
        authorities.add(SimpleGrantedAuthority("acs.zones.admin"))
        authorities.add(SimpleGrantedAuthority("acs.attributes.read"))
        authorities.add(SimpleGrantedAuthority("acs.attributes.write"))
        authorities.add(SimpleGrantedAuthority("acs.policies.read"))
        authorities.add(SimpleGrantedAuthority("acs.policies.write"))
        authorities.add(SimpleGrantedAuthority("acs.connectors.read"))
        authorities.add(SimpleGrantedAuthority("acs.connectors.write"))
        for (i in acsZones.indices) {
            authorities.add(SimpleGrantedAuthority("predix-acs.zones." + acsZones[i] + ".admin"))
            authorities.add(SimpleGrantedAuthority("predix-acs.zones." + acsZones[i] + ".user"))
        }
        val restTemplate = createScopeClient(acsZones[0], "super-admin")
        this.createClientWithAuthorities(
            restTemplate.resource.clientId,
            restTemplate.resource.clientSecret, authorities
        )
        return restTemplate
    }

    fun createAcsAdminClientAndGetTemplate(zoneName: String): OAuth2RestTemplate {
        val restTemplate = createScopeClient(zoneName, "admin")
        this.createAcsAdminClient(
            restTemplate.resource.clientId,
            restTemplate.resource.clientSecret
        )
        return restTemplate
    }

    fun createZoneClientAndGetTemplate(
        zoneName: String,
        serviceId: String
    ): OAuth2RestTemplate {
        val restTemplate = createScopeClient(zoneName, "zoneAdmin")
        this.createAcsZoneClient(
            zoneName, restTemplate.resource.clientId,
            restTemplate.resource.clientSecret, serviceId
        )
        return restTemplate
    }

    fun createReadOnlyPolicyScopeClient(zoneName: String): OAuth2RestTemplate {
        val restTemplate = createScopeClient(zoneName, "admin", "readonly")
        this.createReadOnlyPolicyScopeClient(
            restTemplate.resource.clientId,
            restTemplate.resource.clientSecret, zoneName
        )
        return restTemplate
    }

    fun createNoPolicyScopeClient(zoneName: String): OAuth2RestTemplate {
        val restTemplate = createScopeClient(zoneName, "admin", "nopolicy")
        this.createNoPolicyScopeClient(
            restTemplate.resource.clientId,
            restTemplate.resource.clientSecret, zoneName
        )
        return restTemplate
    }

    fun createReadOnlyConnectorScopeClient(zoneName: String): OAuth2RestTemplate {
        val restTemplate = createScopeClient(zoneName, "admin-connector", "readonly")
        this.createReadOnlyConnectorScopeClient(
            restTemplate.resource.clientId,
            restTemplate.resource.clientSecret, zoneName
        )
        return restTemplate
    }

    fun createAdminConnectorScopeClient(zoneName: String): OAuth2RestTemplate {
        val restTemplate = createScopeClient(zoneName, "admin-connector")
        this.createAdminConnectorScopeClient(
            restTemplate.resource.clientId,
            restTemplate.resource.clientSecret, zoneName
        )
        return restTemplate
    }

    fun createAcsAdminClient(
        clientId: String,
        clientSecret: String
    ) {
        createClientWithAuthorities(
            clientId, clientSecret,
            listOf(SimpleGrantedAuthority("acs.zones.admin"))
        )
    }

    fun createNoPolicyScopeClient(
        clientId: String,
        clientSecret: String,
        zone: String
    ) {
        this.createClientWithAuthorities(
            clientId, clientSecret,
            listOf(SimpleGrantedAuthority("predix-acs.zones.$zone.user"))
        )
    }

    fun createReadOnlyPolicyScopeClient(
        clientId: String,
        clientSecret: String,
        zone: String
    ) {
        this.createClientWithAuthorities(
            clientId, clientSecret,
            listOf(
                SimpleGrantedAuthority("predix-acs.zones.$zone.user"),
                SimpleGrantedAuthority("acs.policies.read")
            )
        )
    }

    fun createReadOnlyConnectorScopeClient(
        clientId: String,
        clientSecret: String,
        zone: String
    ) {
        this.createClientWithAuthorities(
            clientId, clientSecret,
            listOf(
                SimpleGrantedAuthority("predix-acs.zones.$zone.user"),
                SimpleGrantedAuthority("acs.connectors.read")
            )
        )
    }

    fun createAdminConnectorScopeClient(
        clientId: String,
        clientSecret: String,
        zone: String
    ) {
        this.createClientWithAuthorities(
            clientId, clientSecret,
            listOf(
                SimpleGrantedAuthority("predix-acs.zones.$zone.user"),
                SimpleGrantedAuthority("acs.connectors.read"),
                SimpleGrantedAuthority("acs.connectors.write")
            )
        )
    }

    fun createAcsZoneClient(
        acsZone: String,
        clientId: String,
        clientSecret: String,
        serviceId: String
    ) {
        val authorities = object : ArrayList<SimpleGrantedAuthority>() {
            init {
                add(SimpleGrantedAuthority("acs.attributes.read"))
                add(SimpleGrantedAuthority("acs.attributes.write"))
                add(SimpleGrantedAuthority("acs.policies.read"))
                add(SimpleGrantedAuthority("acs.policies.write"))
                add(SimpleGrantedAuthority("$serviceId.zones.$acsZone.user"))
                add(SimpleGrantedAuthority("$serviceId.zones.$acsZone.admin"))
            }
        }
        createClientWithAuthorities(clientId, clientSecret, authorities)
    }

    private fun createClientWithAuthorities(
        clientId: String,
        clientSecret: String,
        authorities: Collection<GrantedAuthority>
    ) {
        val client = BaseClientDetails()
        client.authorities = authorities
        client.setAuthorizedGrantTypes(listOf("client_credentials"))
        client.clientId = clientId
        client.clientSecret = clientSecret
        client.setResourceIds(listOf("uaa.none"))
        createOrUpdateClient(client)
    }

    private fun createOrUpdateClient(client: BaseClientDetails): BaseClientDetails? {

        val headers = LinkedMultiValueMap<String, String>()
        headers.add("Accept", MediaType.APPLICATION_JSON_VALUE)
        headers.add("Content-Type", MediaType.APPLICATION_JSON_VALUE)
        val postEntity = HttpEntity<String>(JSON_UTILS.serialize(client), headers)

        var clientCreate: ResponseEntity<String>? = null
        try {
            clientCreate = this.uaaAdminTemplate.exchange(
                this.uaaUrl + "/oauth/clients", HttpMethod.POST, postEntity,
                String::class.java
            )
            return if (clientCreate!!.statusCode == HttpStatus.CREATED) {
                JSON_UTILS.deserialize(clientCreate.body, BaseClientDetails::class.java)
            } else {
                throw RuntimeException("Unexpected return code for client create: " + clientCreate.statusCode)
            }
        } catch (ex: InvalidClientException) {
            if (ex.message == "Client already exists: " + client.clientId) {
                val putEntity = HttpEntity<String>(JSON_UTILS.serialize(client), headers)
                val clientUpdate = this.uaaAdminTemplate.exchange(
                    this.uaaUrl + "/oauth/clients/" + client.clientId, HttpMethod.PUT, putEntity,
                    String::class.java
                )
                return if (clientUpdate.statusCode == HttpStatus.OK) {
                    JSON_UTILS.deserialize(clientUpdate.body, BaseClientDetails::class.java)
                } else {
                    throw RuntimeException(
                        "Unexpected return code for client update: " + clientUpdate.statusCode
                    )
                }
            }
        }

        throw RuntimeException("Unexpected return code for client creation: " + clientCreate!!.statusCode)
    }

    fun deleteClient(clientId: String) {
        this.uaaAdminTemplate.delete(this.uaaUrl + "/oauth/clients/" + clientId)
    }

    private fun createScopeClient(
        zoneName: String,
        clientRole: String
    ): OAuth2RestTemplate {
        return this.createScopeClient(zoneName, clientRole, null)
    }

    private fun createScopeClient(
        zoneName: String,
        clientRole: String,
        clientType: String?
    ): OAuth2RestTemplate {
        var clientId = clientRole + '-'.toString() + zoneName
        if (clientType != null) {
            clientId += "-$clientType"
        }
        val clientSecret = "$clientId-secret"
        return this.getOAuth2RestTemplateForClient(this.uaaUrl + "/oauth/token", clientId, clientSecret)
    }

    private fun getOAuth2RestTemplateForClient(
        tokenUrl: String,
        clientId: String,
        clientSecret: String
    ): OAuth2RestTemplate {
        val resource = ClientCredentialsResourceDetails()
        resource.accessTokenUri = tokenUrl
        resource.clientId = clientId
        resource.clientSecret = clientSecret
        val restTemplate = OAuth2RestTemplate(resource)
        val httpClient = HttpClientBuilder.create().useSystemProperties().build()
        val requestFactory = HttpComponentsClientHttpRequestFactory(httpClient)
        restTemplate.requestFactory = requestFactory

        return restTemplate
    }
}
