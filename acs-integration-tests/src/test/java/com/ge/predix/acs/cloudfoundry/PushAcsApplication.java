package com.ge.predix.acs.cloudfoundry;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import com.ge.predix.cloudfoundry.client.CloudFoundryApplicationHelper;
import com.ge.predix.cloudfoundry.client.CloudFoundryService;
import com.ge.predix.cloudfoundry.client.CloudFoundryUtilities;

@Profile({ "integration" })
@Component
final class PushAcsApplication {

    @Value("${ACS_SERVICE_ARTIFACT_DIR:../service/target}")
    private String serviceArtifactDir;

    @Value("${ACS_SERVICE_ARTIFACT_PATTERN:acs-service-*-exec.jar}")
    private String serviceArtifactPattern;

    @Autowired
    private CloudFoundryApplicationHelper cloudFoundryApplicationHelper;

    @Autowired
    private Environment environment;

    private static final Logger LOGGER = LoggerFactory.getLogger(PushAcsApplication.class);
    private static final String ACS_SERVICE_ID = "predix-acs";
    private static final String CACHE_SERVICE_NAME = "aws-redis";
    private static final String CACHE_SERVICE_PLAN = "2-3GB";

    private static final Map<String, String> COMMON_ENVIRONMENT_VARIABLES = new HashMap<String, String>() {
        {
            put("ACS_BASE_DOMAIN", String.format("%s-integration.%s", ACS_SERVICE_ID, System.getenv("CF_BASE_DOMAIN")));
            put("ACS_DB", System.getenv("ACS_DB"));
            put("ACS_DEFAULT_ISSUER_ID", System.getenv("ACS_DEFAULT_ISSUER_ID"));
            put("DECISION_CACHE_NAME", System.getenv("DECISION_CACHE_NAME"));
            put("RESOURCE_CACHE_NAME", System.getenv("RESOURCE_CACHE_NAME"));
            put("SUBJECT_CACHE_NAME", System.getenv("SUBJECT_CACHE_NAME"));
            put("ACS_SERVICE_ID", ACS_SERVICE_ID);
            put("NUREGO_USERNAME", System.getenv("NUREGO_USERNAME"));
            put("NUREGO_PASSWORD", System.getenv("NUREGO_PASSWORD"));
            put("NUREGO_INSTANCE_ID", System.getenv("NUREGO_INSTANCE_ID"));
            put("NUREGO_API_URL", System.getenv("NUREGO_API_URL"));
            put("NUREGO_BATCH_MAX_MAP_SIZE", "1");
            put("UAA_CHECK_HEALTH_URL", System.getenv("UAA_CHECK_HEALTH_URL"));
            put("ZAC_CLIENT_ID", System.getenv("ZAC_CLIENT_ID"));
            put("ZAC_CLIENT_SECRET", System.getenv("ZAC_CLIENT_SECRET"));
            put("ZAC_UAA_URL", System.getenv("ZAC_UAA_URL"));
            put("ACS_UAA_URL", System.getenv("ACS_UAA_URL"));
            put("ZAC_URL", System.getenv("ZAC_URL"));
            put("ENABLED_REDIS_HEALTH_CHECK", "true");
            put("DEPLOYMENT_TYPE", System.getenv("DEPLOYMENT_TYPE"));
            put("BASE_APP_NAME", System.getenv("BASE_APP_NAME"));
            put("CONFIG_CLIENT_ID", System.getenv("CONFIG_CLIENT_ID"));
            put("CONFIG_CLIENT_SECRET", System.getenv("CONFIG_CLIENT_SECRET"));
            put("CONFIG_SERVER_URI", System.getenv("CONFIG_SERVER_URI"));
            put("CONFIG_OAUTH_TOKEN_URI", System.getenv("CONFIG_OAUTH_TOKEN_URI"));
            put("ENCRYPTION_KEY", System.getenv("ENCRYPTION_KEY"));
            put("AUDIT_SERVICE_NAME", System.getenv("AUDIT_SERVICE_NAME"));
            put("AUDIT_UAA_URL", System.getenv("AUDIT_UAA_URL"));
            put("AUDIT_UAA_CLIENT_ID", System.getenv("AUDIT_UAA_CLIENT_ID"));
            put("AUDIT_UAA_CLIENT_SECRET", System.getenv("AUDIT_UAA_CLIENT_SECRET"));
            put("SPRING_PROFILES_ACTIVE", System.getenv("SPRING_PROFILES_ACTIVE"));
            put("JAVA_OPTS", System.getenv("JAVA_OPTS"));
        }
    };

    void pushApplication(final Map<String, String> additionalEnvironmentVariables) throws Exception {

        Map<String, Object> userProvidedAssetAdapterEnv = this.cloudFoundryApplicationHelper
                .getApplicationEnvironments(AcsCloudFoundryUtilities.Adapter.ACS_ASSET_ADAPTER_APP_NAME)
                .getUserProvided();

        String assetAdapterUrl = (String) userProvidedAssetAdapterEnv.get("ASSET_ADAPTER_URL");
        String assetAdapterTokenUrl = (String) userProvidedAssetAdapterEnv.get("ASSET_ADAPTER_TOKEN_URL");
        String assetTokenUrl = (String) userProvidedAssetAdapterEnv.get("ASSET_TOKEN_URL");
        String assetZoneId = (String) userProvidedAssetAdapterEnv.get("ASSET_ZONE_ID");
        String assetUrl = (String) userProvidedAssetAdapterEnv.get("ASSET_URL");

        COMMON_ENVIRONMENT_VARIABLES.put("ADAPTER_ENDPOINT", assetAdapterUrl);
        COMMON_ENVIRONMENT_VARIABLES.put("ADAPTER_UAA_TOKEN_URL", assetAdapterTokenUrl);
        COMMON_ENVIRONMENT_VARIABLES.put("ADAPTER_UAA_CLIENT_ID",
                (String) userProvidedAssetAdapterEnv.get("ASSET_ADAPTER_CLIENT_ID"));
        COMMON_ENVIRONMENT_VARIABLES.put("ADAPTER_UAA_CLIENT_SECRET",
                (String) userProvidedAssetAdapterEnv.get("ASSET_ADAPTER_CLIENT_SECRET"));
        COMMON_ENVIRONMENT_VARIABLES.put("ASSET_TOKEN_URL", assetTokenUrl);
        COMMON_ENVIRONMENT_VARIABLES
                .put("ASSET_CLIENT_ID", (String) userProvidedAssetAdapterEnv.get("ASSET_CLIENT_ID"));
        COMMON_ENVIRONMENT_VARIABLES
                .put("ASSET_CLIENT_SECRET", (String) userProvidedAssetAdapterEnv.get("ASSET_CLIENT_SECRET"));
        COMMON_ENVIRONMENT_VARIABLES.put("ASSET_ZONE_ID", assetZoneId);
        COMMON_ENVIRONMENT_VARIABLES.put("ASSET_URL", assetUrl);

        CloudFoundryService postgresService = new CloudFoundryService("dedicated-10.1", getAcsDbServiceName(),
                "postgres-2.0", Collections.emptyMap());
        CloudFoundryService decisionRedisService = new CloudFoundryService(CACHE_SERVICE_PLAN,
                getAcsDecisionRedisServiceName(), CACHE_SERVICE_NAME, Collections.emptyMap());
        CloudFoundryService resourceRedisService = new CloudFoundryService(CACHE_SERVICE_PLAN,
                getAcsResourceRedisServiceName(), CACHE_SERVICE_NAME, Collections.emptyMap());
        CloudFoundryService subjectRedisService = new CloudFoundryService(CACHE_SERVICE_PLAN,
                getAcsSubjectRedisServiceName(), CACHE_SERVICE_NAME, Collections.emptyMap());

        Path acsServiceArtifactFilePath = CloudFoundryUtilities
                .getPathOfFileMatchingPattern(this.serviceArtifactDir, this.serviceArtifactPattern);
        LOGGER.info("ACS service artifact file path: '{}'", acsServiceArtifactFilePath);

        this.cloudFoundryApplicationHelper.deleteApplication(AcsCloudFoundryUtilities.ACS_APP_NAME);

        Map<String, String> environmentVariables = new HashMap<>(COMMON_ENVIRONMENT_VARIABLES);
        environmentVariables.putAll(additionalEnvironmentVariables);
        environmentVariables.values().removeIf(Objects::isNull);
        this.cloudFoundryApplicationHelper
                .pushApplication(AcsCloudFoundryUtilities.ACS_APP_NAME, acsServiceArtifactFilePath,
                        environmentVariables, new ArrayList<>(
                                Arrays.asList(postgresService, decisionRedisService, resourceRedisService,
                                        subjectRedisService)));

        if (Arrays.asList(this.environment.getActiveProfiles()).contains("predixAudit")) {
            this.cloudFoundryApplicationHelper.bindServiceInstances(AcsCloudFoundryUtilities.ACS_APP_NAME,
                    Collections.singletonList(AcsCloudFoundryUtilities.ACS_AUDIT_SERVICE_INSTANCE_NAME));
        }

        this.cloudFoundryApplicationHelper.startApplication(AcsCloudFoundryUtilities.ACS_APP_NAME);

        System.setProperty("ASSET_ADAPTER_URL", assetAdapterUrl);
        System.setProperty("ASSET_TOKEN_URL", assetTokenUrl);
        System.setProperty("ASSET_ZONE_ID", assetZoneId);
        System.setProperty("ASSET_URL", assetUrl);
        System.setProperty("ADAPTER_UAA_TOKEN_URL", assetAdapterTokenUrl);
    }

    static String getAcsDecisionRedisServiceName() {
        return COMMON_ENVIRONMENT_VARIABLES.get("DECISION_CACHE_NAME");
    }

    static String getAcsResourceRedisServiceName() {
        return COMMON_ENVIRONMENT_VARIABLES.get("RESOURCE_CACHE_NAME");
    }

    static String getAcsSubjectRedisServiceName() {
        return COMMON_ENVIRONMENT_VARIABLES.get("SUBJECT_CACHE_NAME");
    }

    static String getAcsDbServiceName() {
        return COMMON_ENVIRONMENT_VARIABLES.get("ACS_DB");
    }
}
