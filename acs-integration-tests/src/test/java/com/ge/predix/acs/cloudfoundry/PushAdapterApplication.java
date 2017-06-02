package com.ge.predix.acs.cloudfoundry;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.Test;

import com.ge.predix.cloudfoundry.client.CloudFoundryApplicationHelper;
import com.ge.predix.cloudfoundry.client.CloudFoundryService;
import com.ge.predix.cloudfoundry.client.CloudFoundryUtilities;

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

        String assetUaaServiceName = "predix-uaa";
        String assetServiceName = "predix-asset";

        Map<String, Object> assetUaaParameters = new HashMap<String, Object>() {
            {
                put("adminClientSecret", System.getenv("ASSET_CLIENT_SECRET"));
            }
        };
        CloudFoundryService assetUaaService = new CloudFoundryService("Free",
                AcsCloudFoundryUtilities.Adapter.ACS_ASSET_UAA_SERVICE_INSTANCE_NAME, assetUaaServiceName,
                assetUaaParameters);

        this.cloudFoundryApplicationHelper.createServiceInstances(Collections.singletonList(assetUaaService));

        String cfBaseDomain = System.getenv("CF_BASE_DOMAIN");
        String assetUaaZoneId = this.cloudFoundryApplicationHelper
                .getServiceInstance(AcsCloudFoundryUtilities.Adapter.ACS_ASSET_UAA_SERVICE_INSTANCE_NAME).getId();
        String assetUaaUri = String.format("https://%s.%s.%s", assetUaaZoneId, assetUaaServiceName, cfBaseDomain);
        String assetTokenUrl = assetUaaUri + "/oauth/token";

        Map<String, Object> assetServiceParameters = new HashMap<String, Object>() {
            {
                put("trustedIssuerIds", Collections.singletonList(assetTokenUrl));
            }
        };
        CloudFoundryService assetService = new CloudFoundryService("Tiered",
                AcsCloudFoundryUtilities.Adapter.ACS_ASSET_SERVICE_INSTANCE_NAME, assetServiceName,
                assetServiceParameters);

        String assetClientId = System.getenv("ASSET_CLIENT_ID");

        Map<String, String> environmentVariables = new HashMap<>();
        environmentVariables.put("ASSET_CLIENT_ID", assetClientId);
        environmentVariables.put("ASSET_CLIENT_SECRET", System.getenv("ASSET_CLIENT_SECRET"));
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
        List<Object> predixAssetFromEnv = (List<Object>) vcapServicesFromEnv.get(assetServiceName);
        Map<String, Object> predixAssetCredentialsFromEnv =
            (Map<String, Object>) ((Map<String, Object>) predixAssetFromEnv.get(0)).get("credentials");
        String assetUrl = (String) predixAssetCredentialsFromEnv.get("uri");

        String assetZoneId = this.cloudFoundryApplicationHelper
                .getServiceInstance(AcsCloudFoundryUtilities.Adapter.ACS_ASSET_SERVICE_INSTANCE_NAME).getId();

        this.adapterUtilities.createUaaClient(assetUaaUri, assetClientId, this.clientSecret, Collections.singleton(
                new SimpleGrantedAuthority(String.format("%s.zones.%s.user", assetServiceName, assetZoneId))));

        environmentVariables = new HashMap<>();
        environmentVariables.put("ASSET_TOKEN_URL", assetTokenUrl);
        environmentVariables.put("ASSET_ZONE_ID", assetZoneId);
        environmentVariables.put("ASSET_URL", assetUrl);
        environmentVariables.put("ASSET_ADAPTER_TOKEN_URL", environmentVariables.get("ASSET_TOKEN_URL"));

        this.cloudFoundryApplicationHelper.setEnvironmentVariables(
                AcsCloudFoundryUtilities.Adapter.ACS_ASSET_ADAPTER_APP_NAME, environmentVariables);

        this.cloudFoundryApplicationHelper
                .startApplication(AcsCloudFoundryUtilities.Adapter.ACS_ASSET_ADAPTER_APP_NAME);
    }
}
