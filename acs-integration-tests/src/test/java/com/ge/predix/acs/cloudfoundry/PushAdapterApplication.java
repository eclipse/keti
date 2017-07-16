package com.ge.predix.acs.cloudfoundry;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.token.grant.client.ClientCredentialsResourceDetails;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.Test;

import com.ge.predix.cloudfoundry.client.CloudFoundryApplicationHelper;
import com.ge.predix.cloudfoundry.client.CloudFoundryService;
import com.ge.predix.cloudfoundry.client.CloudFoundryUtilities;
import com.ge.predix.test.utils.ACSTestUtil;
import com.ge.predix.test.utils.PolicyHelper;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

@ContextConfiguration("classpath:integration-test-spring-context.xml")
public final class PushAdapterApplication extends AbstractTestNGSpringContextTests {

    private static final Logger LOGGER = LoggerFactory.getLogger(PushAdapterApplication.class);

    @Value("${ASSET_ADAPTER_ARTIFACT_DIR:../../acs-asset-adapter/target}")
    private String assetAdapterArtifactDir;

    @Value("${ASSET_ADAPTER_ARTIFACT_PATTERN:acs-asset-adapter-*.jar}")
    private String assetAdapterArtifactPattern;

    @Value("${ASSET_CLIENT_SECRET}")
    private String clientSecret;

    @Autowired
    private CloudFoundryApplicationHelper cloudFoundryApplicationHelper;

    @Autowired
    private AcsCloudFoundryUtilities.Adapter adapterUtilities;

    @Autowired
    private DeleteApplications deleteApplications;

    @Test(groups = { AcsCloudFoundryUtilities.Adapter.PUSH_ASSET_ADAPTER_APP_TEST_GROUP })
    @SuppressWarnings("unchecked")
    public void pushAssetAdapterApplication() throws Exception {
        this.deleteApplications.deleteAssetAdapterApplication();

        Map<String, Object> assetUaaParameters = new HashMap<String, Object>() {
            {
                put("adminClientSecret", System.getenv("ASSET_CLIENT_SECRET"));
            }
        };
        CloudFoundryService assetUaaService = new CloudFoundryService("Free",
                AcsCloudFoundryUtilities.Adapter.ACS_ASSET_UAA_SERVICE_INSTANCE_NAME,
                AcsCloudFoundryUtilities.UAA_SERVICE_ID, assetUaaParameters);

        this.cloudFoundryApplicationHelper.createServiceInstances(Collections.singletonList(assetUaaService));

        String cfBaseDomain = System.getenv("CF_BASE_DOMAIN");
        String assetUaaZoneId = this.cloudFoundryApplicationHelper
                .getServiceInstance(AcsCloudFoundryUtilities.Adapter.ACS_ASSET_UAA_SERVICE_INSTANCE_NAME).getId();
        String assetUaaUri = String.format("https://%s.%s.%s", assetUaaZoneId,
                AcsCloudFoundryUtilities.UAA_SERVICE_ID, cfBaseDomain);
        String assetTokenUrl = assetUaaUri + "/oauth/token";

        Map<String, Object> assetServiceParameters = new HashMap<String, Object>() {
            {
                put("trustedIssuerIds", Collections.singletonList(assetTokenUrl));
            }
        };
        CloudFoundryService assetService = new CloudFoundryService("Tiered",
                AcsCloudFoundryUtilities.Adapter.ACS_ASSET_SERVICE_INSTANCE_NAME,
                AcsCloudFoundryUtilities.ASSET_SERVICE_ID, assetServiceParameters);

        String assetClientId = System.getenv("ASSET_CLIENT_ID");
        String assetClientSecret = System.getenv("ASSET_CLIENT_SECRET");

        Map<String, String> environmentVariables = new HashMap<>();
        environmentVariables.put("ASSET_CLIENT_ID", assetClientId);
        environmentVariables.put("ASSET_CLIENT_SECRET", assetClientSecret);
        environmentVariables.put("ASSET_ADAPTER_URL", String.format("https://%s.%s/v1/attribute",
                AcsCloudFoundryUtilities.Adapter.ACS_ASSET_ADAPTER_APP_NAME, cfBaseDomain));
        environmentVariables.put("ASSET_ADAPTER_CLIENT_ID", environmentVariables.get("ASSET_CLIENT_ID"));
        environmentVariables.put("ASSET_ADAPTER_CLIENT_SECRET", environmentVariables.get("ASSET_CLIENT_SECRET"));

        Path assetAdapterArtifactFilePath = CloudFoundryUtilities
                .getPathOfFileMatchingPattern(this.assetAdapterArtifactDir, this.assetAdapterArtifactPattern);
        LOGGER.info("Asset adapter artifact file path: '{}'", assetAdapterArtifactFilePath);

        this.cloudFoundryApplicationHelper.createServicesAndPushApplication(
                AcsCloudFoundryUtilities.Adapter.ACS_ASSET_ADAPTER_APP_NAME, assetAdapterArtifactFilePath,
                environmentVariables, new ArrayList<>(Arrays.asList(assetUaaService, assetService)));

        Map<String, Object> systemProvidedEnv = this.cloudFoundryApplicationHelper
                .getApplicationEnvironments(AcsCloudFoundryUtilities.Adapter.ACS_ASSET_ADAPTER_APP_NAME)
                .getSystemProvided();
        Map<String, Object> vcapServicesFromEnv = (Map<String, Object>) systemProvidedEnv.get("VCAP_SERVICES");
        List<Object> predixAssetFromEnv = (List<Object>) vcapServicesFromEnv
                .get(AcsCloudFoundryUtilities.ASSET_SERVICE_ID);
        Map<String, Object> predixAssetCredentialsFromEnv =
            (Map<String, Object>) ((Map<String, Object>) predixAssetFromEnv.get(0)).get("credentials");
        String assetUrl = (String) predixAssetCredentialsFromEnv.get("uri");

        String assetZoneId = this.cloudFoundryApplicationHelper
                .getServiceInstance(AcsCloudFoundryUtilities.Adapter.ACS_ASSET_SERVICE_INSTANCE_NAME).getId();

        this.adapterUtilities.createUaaClient(assetUaaUri, assetClientId, this.clientSecret,
                Collections.singleton(new SimpleGrantedAuthority(String.format("%s.zones.%s.user",
                        AcsCloudFoundryUtilities.ASSET_SERVICE_ID, assetZoneId))));

        environmentVariables = new HashMap<>();
        environmentVariables.put("ASSET_TOKEN_URL", assetTokenUrl);
        environmentVariables.put("ASSET_ZONE_ID", assetZoneId);
        environmentVariables.put("ASSET_URL", assetUrl);
        environmentVariables.put("ASSET_ADAPTER_TOKEN_URL", environmentVariables.get("ASSET_TOKEN_URL"));
        environmentVariables.put("SPRING_PROFILES_ACTIVE", "asset-live");

        this.cloudFoundryApplicationHelper.setEnvironmentVariables(
                AcsCloudFoundryUtilities.Adapter.ACS_ASSET_ADAPTER_APP_NAME, environmentVariables);

        this.cloudFoundryApplicationHelper
                .startApplication(AcsCloudFoundryUtilities.Adapter.ACS_ASSET_ADAPTER_APP_NAME);

        this.configureMockAssetData(assetUrl, this.assetRestTemplate(assetTokenUrl, assetClientId, assetClientSecret),
                this.assetZoneHeader(assetZoneId));
    }

    private void configureMockAssetData(final String assetUrl, final OAuth2RestTemplate assetRestTemplate,
            final HttpHeaders assetZoneHeader) throws IOException {
        JsonObject part = new JsonObject();
        part.addProperty("id", "03f95db1-4255-4265-a509-f7bca3e1fee4");
        part.addProperty("collection", "part");
        part.addProperty("partModel", "/partmodels/9a92831d-42f1-4f9e-86bf-4c0914f481e4");
        part.addProperty("structureModel", "/structureModels/8c787978-bd8b-417a-b759-f63a8a6d43ee");
        part.addProperty("serialNumber", "775277328");
        part.addProperty("parent", "/part/01af94ed-5425-44e4-9f6e-2a58cba7b559");
        part.addProperty("aircraftPart", "/aircraftPart/13a71359-db68-4602-aac5-a8fa401c3194");
        part.addProperty("aircraftPartModel", "/aircraftPartModels/1dc6a36d-a24e-4fec-a181-f576c95a8104");
        part.addProperty("uri", "/part/03f95db1-4255-4265-a509-f7bca3e1fee4");

        JsonArray partArray = new JsonArray();
        partArray.add(part);

        assetRestTemplate.exchange(assetUrl + "/part", HttpMethod.POST,
                new HttpEntity<>(partArray.toString(), assetZoneHeader), String.class);
    }

    private OAuth2RestTemplate assetRestTemplate(final String assetTokenUrl, final String assetClientId,
            final String assetClientSecret) {
        ClientCredentialsResourceDetails clientCredentials = new ClientCredentialsResourceDetails();
        clientCredentials.setAccessTokenUri(assetTokenUrl);
        clientCredentials.setClientId(assetClientId);
        clientCredentials.setClientSecret(assetClientSecret);
        OAuth2RestTemplate assetRestTemplate = new OAuth2RestTemplate(clientCredentials);

        CloseableHttpClient httpClient = HttpClientBuilder.create().useSystemProperties().build();
        HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(httpClient);
        assetRestTemplate.setRequestFactory(requestFactory);
        return assetRestTemplate;
    }

    private HttpHeaders assetZoneHeader(final String assetZoneId) throws IOException {
        HttpHeaders httpHeaders = ACSTestUtil.httpHeaders();
        httpHeaders.set(PolicyHelper.PREDIX_ZONE_ID, assetZoneId);
        httpHeaders.setContentType(MediaType.APPLICATION_JSON);
        return httpHeaders;
    }
}
