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

package com.ge.predix.test.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.common.exceptions.InvalidClientException;
import org.springframework.security.oauth2.provider.client.BaseClientDetails;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import com.ge.predix.acs.utils.JsonUtils;

public class UaaTestUtil {

    private static final JsonUtils JSON_UTILS = new JsonUtils();

    private final OAuth2RestTemplate adminRestTemplate;
    private final String uaaUrl;
    private final String zone;

    public UaaTestUtil(final OAuth2RestTemplate adminRestTemplate, final String uaaUrl) {
        this.adminRestTemplate = adminRestTemplate;
        this.uaaUrl = uaaUrl;
        this.zone = null;
    }

    public UaaTestUtil(final OAuth2RestTemplate adminRestTemplate, final String uaaUrl, final String zone) {
        this.adminRestTemplate = adminRestTemplate;
        this.uaaUrl = uaaUrl;
        this.zone = zone;
    }

    public void setup(final List<String> acsZones) {
        if (acsZones == null || acsZones.size() == 0) {
            throw new RuntimeException("At least one ACS zone is expected to setup UAA");
        }
        createAcsAdminClient(acsZones, "acs_admin", "acs_admin_secret");
        createNoPolicyScopeClient(acsZones);
        createReadOnlyPolicyScopeClient(acsZones);
        createReadOnlyConnectorScopeClient(acsZones);
        createAdminConnectorScopeClient(acsZones);
    }

    public void setupAcsZoneClient(final String acsZone, final String clientId, final String clientSecret) {
        createAcsZoneClient(acsZone, clientId, clientSecret);
    }

    private void createAcsZoneClient(final String acsZone, final String clientId, final String clientSecret) {
        BaseClientDetails acsZoneAdminClient = new BaseClientDetails();
        ArrayList<SimpleGrantedAuthority> authorities = new ArrayList<SimpleGrantedAuthority>();
        authorities.add(new SimpleGrantedAuthority("acs.attributes.read"));
        authorities.add(new SimpleGrantedAuthority("acs.attributes.write"));
        authorities.add(new SimpleGrantedAuthority("acs.policies.read"));
        authorities.add(new SimpleGrantedAuthority("acs.policies.write"));
        authorities.add(new SimpleGrantedAuthority("predix-acs.zones." + acsZone + ".admin"));
        authorities.add(new SimpleGrantedAuthority("predix-acs.zones." + acsZone + ".user"));
        acsZoneAdminClient.setAuthorities(authorities);
        acsZoneAdminClient.setAuthorizedGrantTypes(Arrays.asList(new String[] { "client_credentials" }));
        acsZoneAdminClient.setClientId(clientId);
        acsZoneAdminClient.setClientSecret(clientSecret);
        acsZoneAdminClient.setResourceIds(Arrays.asList(new String[] { "uaa.none" }));
        createOrUpdateClient(acsZoneAdminClient);
    }

    private void createAcsAdminClient(final List<String> acsZones, final String clientId, final String clientSecret) {
        BaseClientDetails acsAdminClient = new BaseClientDetails();
        ArrayList<SimpleGrantedAuthority> authorities = new ArrayList<SimpleGrantedAuthority>();
        authorities.add(new SimpleGrantedAuthority("acs.zones.admin"));
        authorities.add(new SimpleGrantedAuthority("acs.attributes.read"));
        authorities.add(new SimpleGrantedAuthority("acs.attributes.write"));
        authorities.add(new SimpleGrantedAuthority("acs.policies.read"));
        authorities.add(new SimpleGrantedAuthority("acs.policies.write"));
        for (int i = 0; i < acsZones.size(); i++) {
            authorities.add(new SimpleGrantedAuthority("predix-acs.zones." + acsZones.get(i) + ".admin"));
            authorities.add(new SimpleGrantedAuthority("predix-acs.zones." + acsZones.get(i) + ".user"));
        }

        acsAdminClient.setAuthorities(authorities);
        acsAdminClient.setAuthorizedGrantTypes(Arrays.asList(new String[] { "client_credentials" }));
        acsAdminClient.setClientId(clientId);
        acsAdminClient.setClientSecret(clientSecret);
        acsAdminClient.setResourceIds(Arrays.asList(new String[] { "uaa.none" }));
        createOrUpdateClient(acsAdminClient);
    }

    private void createReadOnlyPolicyScopeClient(final List<String> acsZones) {
        this.createClientWithAuthorities("acs_read_only_client", "acs_read_only_secret",
                Arrays.asList(new SimpleGrantedAuthority[] {
                        new SimpleGrantedAuthority("predix-acs.zones." + acsZones.get(0) + ".user"),
                        new SimpleGrantedAuthority("acs.policies.read") }));
    }

    private void createNoPolicyScopeClient(final List<String> acsZones) {
        this.createClientWithAuthorities("acs_no_policy_client", "acs_no_policy_secret",
                Arrays.asList(new SimpleGrantedAuthority[] {
                        new SimpleGrantedAuthority("predix-acs.zones." + acsZones.get(0) + ".user") }));
    }

    private void createReadOnlyConnectorScopeClient(final List<String> acsZones) {
        this.createClientWithAuthorities("acs_connector_read_only_client", "s3cr3t",
                Arrays.asList(new SimpleGrantedAuthority[] {
                        new SimpleGrantedAuthority("predix-acs.zones." + acsZones.get(0) + ".user"),
                        new SimpleGrantedAuthority("acs.connectors.read") }));
    }

    private void createAdminConnectorScopeClient(final List<String> acsZones) {
        this.createClientWithAuthorities("acs_connector_admin_client", "s3cr3t",
                Arrays.asList(new SimpleGrantedAuthority[] {
                        new SimpleGrantedAuthority("predix-acs.zones." + acsZones.get(0) + ".user"),
                        new SimpleGrantedAuthority("acs.connectors.read"), 
                        new SimpleGrantedAuthority("acs.connectors.write") }));
    }

    private void createClientWithAuthorities(final String clientId, final String clientSecret,
            final Collection<? extends GrantedAuthority> authorities) {
        BaseClientDetails client = new BaseClientDetails();
        client.setAuthorities(authorities);
        client.setAuthorizedGrantTypes(Arrays.asList(new String[] { "client_credentials" }));
        client.setClientId(clientId);
        client.setClientSecret(clientSecret);
        client.setResourceIds(Arrays.asList(new String[] { "uaa.none" }));
        createOrUpdateClient(client);
    }

    private BaseClientDetails createOrUpdateClient(final BaseClientDetails client) {

        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        headers.add("Accept", MediaType.APPLICATION_JSON_VALUE);
        headers.add("Content-Type", MediaType.APPLICATION_JSON_VALUE);
        if (StringUtils.isNotEmpty(this.zone)) {
            headers.add("X-Identity-Zone-Id", "uaa");
        }

        HttpEntity<String> postEntity = new HttpEntity<String>(JSON_UTILS.serialize(client), headers);

        ResponseEntity<String> clientCreate = null;
        try {
            clientCreate = this.adminRestTemplate.exchange(this.uaaUrl + "/oauth/clients", HttpMethod.POST, postEntity,
                    String.class);
            if (clientCreate.getStatusCode() == HttpStatus.CREATED) {
                return JSON_UTILS.deserialize(clientCreate.getBody(), BaseClientDetails.class);
            } else {
                throw new RuntimeException("Unexpected return code for client create: " + clientCreate.getStatusCode());
            }
        } catch (InvalidClientException ex) {
            if (ex.getMessage().equals("Client already exists: " + client.getClientId())) {
                HttpEntity<String> putEntity = new HttpEntity<String>(JSON_UTILS.serialize(client), headers);
                ResponseEntity<String> clientUpdate = this.adminRestTemplate.exchange(
                        this.uaaUrl + "/oauth/clients/" + client.getClientId(), HttpMethod.PUT, putEntity,
                        String.class);
                if (clientUpdate.getStatusCode() == HttpStatus.OK) {
                    return JSON_UTILS.deserialize(clientUpdate.getBody(), BaseClientDetails.class);
                } else {
                    throw new RuntimeException(
                            "Unexpected return code for client update: " + clientUpdate.getStatusCode());
                }
            }
        }
        throw new RuntimeException("Unexpected return code for client creation: " + clientCreate.getStatusCode());
    }
}
