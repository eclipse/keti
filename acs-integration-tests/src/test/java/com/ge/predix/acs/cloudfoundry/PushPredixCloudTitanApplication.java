package com.ge.predix.acs.cloudfoundry;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.Test;

@ContextConfiguration("classpath:integration-test-spring-context.xml")
public final class PushPredixCloudTitanApplication extends AbstractTestNGSpringContextTests {

    @Autowired
    private PushAcsApplication pushAcsApplication;

    @Test(groups = { AcsCloudFoundryUtilities.PUSH_ACS_APP_TEST_GROUP },
          dependsOnGroups = { AcsCloudFoundryUtilities.Adapter.PUSH_ASSET_ADAPTER_APP_TEST_GROUP })
    public void pushAcsApplication() throws Exception {
        Map<String, String> additionalEnvironmentVariables = new HashMap<>();
        additionalEnvironmentVariables.put("TITAN_ENABLE_CASSANDRA", System.getenv("TITAN_ENABLE_CASSANDRA"));
        additionalEnvironmentVariables.put("TITAN_STORAGE_CASSANDRA_KEYSPACE",
                System.getenv("TITAN_STORAGE_CASSANDRA_KEYSPACE"));
        additionalEnvironmentVariables.put("TITAN_STORAGE_HOSTNAME", System.getenv("TITAN_STORAGE_HOSTNAME"));
        additionalEnvironmentVariables.put("TITAN_STORAGE_PORT", System.getenv("TITAN_STORAGE_PORT"));
        additionalEnvironmentVariables.put("TITAN_STORAGE_USERNAME", System.getenv("TITAN_STORAGE_USERNAME"));
        additionalEnvironmentVariables.put("TITAN_STORAGE_PASSWORD", System.getenv("TITAN_STORAGE_PASSWORD"));

        this.pushAcsApplication.pushApplication(additionalEnvironmentVariables);
    }
}
