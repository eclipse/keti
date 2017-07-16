package com.ge.predix.acs.cloudfoundry;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.cloudfoundry.uaa.tokens.GrantType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.token.grant.client.ClientCredentialsResourceDetails;
import org.springframework.security.oauth2.common.exceptions.InvalidClientException;
import org.springframework.security.oauth2.provider.client.BaseClientDetails;
import org.springframework.stereotype.Component;

import com.ge.predix.test.utils.ACSTestUtil;

public final class AcsCloudFoundryUtilities {

    private AcsCloudFoundryUtilities() {
        throw new AssertionError();
    }

    private static final String APP_AND_SERVICE_NAME_PREFIX = System.getProperty("jenkins.build.number");

    static final String ACS_APP_NAME = "acs-ci-" + APP_AND_SERVICE_NAME_PREFIX;
    static final String ACS_AUDIT_SERVICE_INSTANCE_NAME = "acs-audit-service-int-tests";
    static final String PUSH_ACS_APP_TEST_GROUP = "pushAcsApplication";
    public static final String CHECK_APP_HEALTH_TEST_GROUP = "checkApplicationHealth";
    public static final String UAA_SERVICE_ID = "predix-uaa";
    public static final String ASSET_SERVICE_ID = "predix-asset";

    @Component
    static final class Adapter {

        private static final Logger LOGGER = LoggerFactory.getLogger(Adapter.class);
        static final String ACS_ASSET_ADAPTER_APP_NAME = "acs-asset-adapter-ci-" + APP_AND_SERVICE_NAME_PREFIX;
        static final String ACS_ASSET_SERVICE_INSTANCE_NAME = "acs-asset-ci-" + APP_AND_SERVICE_NAME_PREFIX;
        static final String ACS_ASSET_UAA_SERVICE_INSTANCE_NAME = "acs-asset-uaa-ci-" + APP_AND_SERVICE_NAME_PREFIX;
        static final String PUSH_ASSET_ADAPTER_APP_TEST_GROUP = "pushAssetAdapterApplication";

        @Value("${https.proxyHost}")
        private String proxyHost;

        @Value("${https.proxyPort}")
        private Integer proxyPort;

        void createUaaClient(final String uaaUri, final String clientId, final String clientSecret,
                final Set<GrantedAuthority> authorities) {

            BaseClientDetails client = new BaseClientDetails();
            client.setClientId(clientId);
            client.setClientSecret(clientSecret);
            client.setAuthorizedGrantTypes(new ArrayList<>(Arrays.asList(GrantType.CLIENT_CREDENTIALS.toString(),
                    GrantType.PASSWORD.toString(), GrantType.REFRESH_TOKEN.toString())));
            client.setAuthorities(authorities);
            client.setScope(authorities.stream().map(GrantedAuthority::getAuthority).collect(Collectors.toList()));
            client.addAdditionalInformation("name", clientId);

            HttpHeaders headers = ACSTestUtil.httpHeaders();
            headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);

            HttpEntity<BaseClientDetails> requestEntity = new HttpEntity<>(client, headers);

            ClientCredentialsResourceDetails adminClientCredentials = new ClientCredentialsResourceDetails();
            adminClientCredentials.setAccessTokenUri(uaaUri + "/oauth/token");
            adminClientCredentials.setClientId("admin");
            adminClientCredentials.setClientSecret(clientSecret);
            OAuth2RestTemplate adminRestTemplate = new OAuth2RestTemplate(adminClientCredentials);

            CloseableHttpClient httpClient = HttpClientBuilder.create().useSystemProperties().build();
            HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(
                    httpClient);
            adminRestTemplate.setRequestFactory(requestFactory);

            try {
                adminRestTemplate.exchange(URI.create(uaaUri + "/oauth/clients"), HttpMethod.POST, requestEntity,
                        BaseClientDetails.class);
            } catch (InvalidClientException e) {
                LOGGER.error(e.getMessage());
            }
        }
    }
}
