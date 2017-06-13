package com.ge.predix.acs.cloudfoundry;

import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.Test;

import com.ge.predix.cloudfoundry.client.CloudFoundryApplicationHelper;
import com.ge.predix.cloudfoundry.client.CloudFoundryService;
import com.ge.predix.cloudfoundry.client.CloudFoundryUtilities;

@ContextConfiguration("classpath:integration-test-spring-context.xml")
public final class PushMockedAdapterApplication extends AbstractTestNGSpringContextTests {

    private static final Logger LOGGER = LoggerFactory.getLogger(PushMockedAdapterApplication.class);

    @Value("${ASSET_ADAPTER_ARTIFACT_DIR:../../acs-asset-adapter/target}")
    private String assetAdapterArtifactDir;

    @Value("${ASSET_ADAPTER_ARTIFACT_PATTERN:acs-asset-adapter-*.jar}")
    private String assetAdapterArtifactPattern;

    @Value("${ASSET_CLIENT_ID}")
    private String assetClientId;

    @Value("${ASSET_CLIENT_SECRET}")
    private String assetClientSecret;

    @Autowired
    private CloudFoundryApplicationHelper cloudFoundryApplicationHelper;

    @Autowired
    private AcsCloudFoundryUtilities.Adapter adapterUtilities;

    @Autowired
    private DeleteApplications deleteApplications;

    @Test(groups = { AcsCloudFoundryUtilities.Adapter.PUSH_ASSET_ADAPTER_APP_TEST_GROUP })
    public void pushAssetAdapterApplication() throws Exception {
        this.deleteApplications.deleteAssetAdapterApplication();

        Map<String, Object> assetUaaParameters = new HashMap<String, Object>();
        assetUaaParameters.put("adminClientSecret", this.assetClientSecret);

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

        Map<String, String> environmentVariables = new HashMap<>();
        environmentVariables.put("ASSET_CLIENT_ID", "not-used");
        environmentVariables.put("ASSET_CLIENT_SECRET", "not-used");
        environmentVariables.put("ASSET_URL", "not-used");
        environmentVariables.put("ASSET_TOKEN_URL", "not-used");
        environmentVariables.put("ASSET_ZONE_ID", "not-used");
        environmentVariables.put("ASSET_ADAPTER_TOKEN_URL", assetTokenUrl);
        environmentVariables.put("ASSET_ADAPTER_URL", String.format("https://%s.%s/v1/attribute",
                AcsCloudFoundryUtilities.Adapter.ACS_ASSET_ADAPTER_APP_NAME, cfBaseDomain));
        environmentVariables.put("ASSET_ADAPTER_CLIENT_ID", this.assetClientId);
        environmentVariables.put("ASSET_ADAPTER_CLIENT_SECRET", this.assetClientSecret);

        Path assetAdapterArtifactFilePath = CloudFoundryUtilities
                .getPathOfFileMatchingPattern(this.assetAdapterArtifactDir, this.assetAdapterArtifactPattern);
        LOGGER.info("Asset adapter artifact file path: '{}'", assetAdapterArtifactFilePath);

        this.cloudFoundryApplicationHelper.pushApplication(AcsCloudFoundryUtilities.Adapter.ACS_ASSET_ADAPTER_APP_NAME,
                assetAdapterArtifactFilePath, environmentVariables, Collections.singletonList(assetUaaService));

        this.adapterUtilities.createUaaClient(assetUaaUri, this.assetClientId, this.assetClientSecret,
                Collections.emptySet());

        this.cloudFoundryApplicationHelper
                .startApplication(AcsCloudFoundryUtilities.Adapter.ACS_ASSET_ADAPTER_APP_NAME);
    }
}
