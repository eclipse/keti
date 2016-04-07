/*******************************************************************************
 * Copyright 2016 General Electric Company. 
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
 *******************************************************************************/

package com.ge.predix.test.utils;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.resource.OAuth2ProtectedResourceDetails;
import org.springframework.security.oauth2.client.token.grant.client.ClientCredentialsResourceDetails;
import org.springframework.security.oauth2.client.token.grant.password.ResourceOwnerPasswordResourceDetails;
import org.springframework.stereotype.Component;

/**
 *
 * @author 212319607
 */
@Component
public class ACSRestTemplateFactory {

    private OAuth2RestTemplate authorizedACSTemplate;
    private OAuth2RestTemplate readOnlyPolicyACSTemplate;
    private OAuth2RestTemplate rocketACSTemplate;
    private OAuth2RestTemplate apmACSTemplate;
    private OAuth2RestTemplate acsZone2RogueTemplate;
    private OAuth2RestTemplate zacTemplate;
    private OAuth2RestTemplate uaaAdminTemplate;
    private OAuth2RestTemplate zone1AdminACSTemplate;
    private OAuth2RestTemplate zone2AdminACSTemplate;

    @Value("${zone1UaaUrl}/oauth/token")
    private String accessTokenEndpointUrl;

    @Value("${zone2UaaUrl}/oauth/token")
    private String zone2TokenUrl;

    @Value("${clientId}")
    private String clientId;
    @Value("${clientSecret}")
    private String clientSecret;

    @Value("${apmClientId}")
    private String apmClientId;
    @Value("${apmClientSecret}")
    private String apmClientSecret;

    @Value("${rocketClientId}")
    private String rocketClientId;
    @Value("${rocketClientSecret}")
    private String rocketClientSecret;

    @Value("${zacClientId}")
    private String zacClientId;
    @Value("${zacClientSecret}")
    private String zacClientSecret;

    @Value("${userName}")
    private String userName;
    @Value("${userPassword}")
    private String userPassword;

    @Value("${readOnlyScopeUserName}")
    private String readOnlyUserName;
    @Value("${readOnlyScopeUserPassword}")
    private String readOnlyUserPassword;

    @Value("${noReadScopeUsername}")
    private String noReadScopeUsername;
    @Value("${noReadScopeUserPassword}")
    private String noReadScopeUserPassword;

    @Value("${ACS_ADMIN_CLIENT_ID:acs_admin}")
    private String acsAdminClientId;
    @Value("${ACS_ADMIN_CLIENT_SECRET:acs_admin_secret}")
    private String acsAdminClientSecret;

    @Value("${ACS_READ_ONLY_CLIENT_ID:acs_read_only_client}")
    private String acsReadOnlyClientId;
    @Value("${ACS_READ_ONLY_CLIENT_SECRET:acs_read_only_secret}")
    private String acsReadOnlyClientSecret;

    @Value("${ACS_NO_POLICY_SCOPE_CLIENT_ID:acs_no_policy_client}")
    private String acsNoPolicyScopeClientId;
    @Value("${ACS_NO_POLICY_SCOPE_CLIENT_SECRET:acs_no_policy_secret}")
    private String acsNoPolicyScopeClientSecret;

    @Value("${zone1AdminClientId:zone1AdminClient}")
    private String zone1AdminClientId;
    @Value("${zone1AdminClientSecret:zone1AdminClientSecret}")
    private String zone1AdminClientSecret;

    @Value("${zone2AdminClientId:zone2AdminClient}")
    private String zone2AdminClientId;
    @Value("${zone2AdminClientSecret:zone2AdminClientSecret}")
    private String zone2AdminClientSecret;

    @Value("${UAA_URL:http://localhost:8080/uaa}/oauth/token")
    private String uaaTokenUrl;

    public OAuth2RestTemplate getACSTemplateWithPolicyScope() {
        if (this.authorizedACSTemplate == null) {
            this.authorizedACSTemplate = new OAuth2RestTemplate(getUserWithPolicyScope());
        }

        return this.authorizedACSTemplate;
    }

    public OAuth2RestTemplate getOAuth2RestTemplateForZone1AdminClient() {
        if (this.zone1AdminACSTemplate == null) {
            ClientCredentialsResourceDetails resource = new ClientCredentialsResourceDetails();
            resource.setAccessTokenUri(this.accessTokenEndpointUrl);
            resource.setClientId(this.zone1AdminClientId);
            resource.setClientSecret(this.zone1AdminClientSecret);
            this.zone1AdminACSTemplate = new OAuth2RestTemplate(resource);
        }

        return this.zone1AdminACSTemplate;
    }

    public OAuth2RestTemplate getOAuth2RestTemplateForZone2AdminClient() {
        if (this.zone2AdminACSTemplate == null) {
            ClientCredentialsResourceDetails resource = new ClientCredentialsResourceDetails();
            resource.setAccessTokenUri(this.zone2TokenUrl);
            resource.setClientId(this.zone2AdminClientId);
            resource.setClientSecret(this.zone2AdminClientSecret);
            this.zone2AdminACSTemplate = new OAuth2RestTemplate(resource);
        }

        return this.zone2AdminACSTemplate;
    }
    public OAuth2RestTemplate getOAuth2ResttemplateForZAC() {
        if (this.zacTemplate == null) {
            ClientCredentialsResourceDetails resource = new ClientCredentialsResourceDetails();
            resource.setAccessTokenUri(this.accessTokenEndpointUrl);
            resource.setClientId(this.zacClientId);
            resource.setClientSecret(this.zacClientSecret);
            this.zacTemplate = new OAuth2RestTemplate(resource);
        }

        return this.zacTemplate;
    }

    public OAuth2RestTemplate getOAuth2RestTemplateForUaaAdmin() {
        if (this.uaaAdminTemplate == null) {
            ClientCredentialsResourceDetails resource = new ClientCredentialsResourceDetails();
            resource.setAccessTokenUri(this.uaaTokenUrl);
            resource.setClientId("admin");
            resource.setClientSecret("adminsecret");
            this.uaaAdminTemplate = new OAuth2RestTemplate(resource);
            this.uaaAdminTemplate.setRequestFactory(new HttpComponentsClientHttpRequestFactory());
        }

        return this.uaaAdminTemplate;
    }

    public OAuth2RestTemplate getOAuth2RestTemplateForAcsAdmin() {
        return getOAuth2RestTemplateForClient(this.acsAdminClientId, this.acsAdminClientSecret, this.uaaTokenUrl);
    }

    public OAuth2RestTemplate getOAuth2RestTemplateForReadOnlyClient() {
        return getOAuth2RestTemplateForClient(this.acsReadOnlyClientId, this.acsReadOnlyClientSecret, this.uaaTokenUrl);
    }

    public OAuth2RestTemplate getOAuth2RestTemplateForNoPolicyScopeClient() {
        return getOAuth2RestTemplateForClient(this.acsNoPolicyScopeClientId, this.acsNoPolicyScopeClientSecret, this.uaaTokenUrl);
    }

    public OAuth2RestTemplate getOAuth2RestTemplateForClient(final String clientId, final String clientSecret,
            final String uaaTokenUrl) {
        ClientCredentialsResourceDetails resource = new ClientCredentialsResourceDetails();
        resource.setAccessTokenUri(uaaTokenUrl);
        resource.setClientId(clientId);
        resource.setClientSecret(clientSecret);
        OAuth2RestTemplate restTemplate = new OAuth2RestTemplate(resource);
        restTemplate.setRequestFactory(new HttpComponentsClientHttpRequestFactory());

        return restTemplate;
    }

    public OAuth2RestTemplate getACSZone2Template() {
        if (this.rocketACSTemplate == null) {
            this.rocketACSTemplate = new OAuth2RestTemplate(getZone2RocketClient());

        }
        return this.rocketACSTemplate;
    }

    /**
     * The client used by this template has acs.zones.admin scope from a zone specific issuer.
     * 
     * @return
     */
    public OAuth2RestTemplate getACSZone2RogueTemplate() {
        if (this.acsZone2RogueTemplate == null) {
            this.acsZone2RogueTemplate = new OAuth2RestTemplate(getZone2RogueClient());
        }

        return this.acsZone2RogueTemplate;
    }

    public OAuth2RestTemplate getACSZone1Template() {
        if (this.apmACSTemplate == null) {
            this.apmACSTemplate = new OAuth2RestTemplate(getZone1ApmClient());
        }

        return this.apmACSTemplate;
    }

    public OAuth2RestTemplate getACSTemplateWithReadOnlyPolicyAccess() {
        if (this.readOnlyPolicyACSTemplate == null) {
            this.readOnlyPolicyACSTemplate = new OAuth2RestTemplate(getUserWithReadOnlyPolicyAccess());
        }

        return this.readOnlyPolicyACSTemplate;
    }

    public OAuth2RestTemplate getACSTemplateWithNoAcsScope() {
        return new OAuth2RestTemplate(getUserWithoutAnyAcsScope());
    }

    private OAuth2ProtectedResourceDetails getUserWithPolicyScope() {

        ResourceOwnerPasswordResourceDetails resource = new ResourceOwnerPasswordResourceDetails();
        resource.setAccessTokenUri(this.accessTokenEndpointUrl);
        resource.setClientId(this.clientId);
        resource.setClientSecret(this.clientSecret);
        resource.setUsername(this.userName);
        resource.setPassword(this.userPassword);

        return resource;

    }

    private OAuth2ProtectedResourceDetails getUserWithReadOnlyPolicyAccess() {

        ResourceOwnerPasswordResourceDetails resource = new ResourceOwnerPasswordResourceDetails();
        resource.setAccessTokenUri(this.accessTokenEndpointUrl);
        resource.setClientId(this.clientId);
        resource.setClientSecret(this.clientSecret);

        resource.setUsername(this.readOnlyUserName);
        resource.setPassword(this.readOnlyUserPassword);

        return resource;

    }

    private OAuth2ProtectedResourceDetails getUserWithoutAnyAcsScope() {
        ResourceOwnerPasswordResourceDetails resource = new ResourceOwnerPasswordResourceDetails();
        resource.setAccessTokenUri(this.accessTokenEndpointUrl);
        resource.setClientId(this.clientId);
        resource.setClientSecret(this.clientSecret);
        resource.setUsername(this.noReadScopeUsername);
        resource.setPassword(this.noReadScopeUserPassword);
        return resource;
    }

    private OAuth2ProtectedResourceDetails getZone2RocketClient() {
        ClientCredentialsResourceDetails resource = new ClientCredentialsResourceDetails();
        resource.setAccessTokenUri(this.zone2TokenUrl);
        resource.setClientId(this.rocketClientId);
        resource.setClientSecret(this.rocketClientSecret);
        return resource;
    }

    private OAuth2ProtectedResourceDetails getZone2RogueClient() {
        ClientCredentialsResourceDetails resource = new ClientCredentialsResourceDetails();
        resource.setAccessTokenUri(this.zone2TokenUrl);
        resource.setClientId("z2client");
        resource.setClientSecret("z2client-secret");
        return resource;
    }

    private OAuth2ProtectedResourceDetails getZone1ApmClient() {
        ClientCredentialsResourceDetails resource = new ClientCredentialsResourceDetails();
        resource.setAccessTokenUri(this.accessTokenEndpointUrl);
        resource.setClientId(this.apmClientId);
        resource.setClientSecret(this.apmClientSecret);
        return resource;
    }
}
