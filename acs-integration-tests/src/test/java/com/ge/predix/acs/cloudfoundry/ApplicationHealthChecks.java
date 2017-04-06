package com.ge.predix.acs.cloudfoundry;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.Test;

import com.ge.predix.cloudfoundry.client.CloudFoundryApplicationHelper;

final class ApplicationNotStartedException extends RuntimeException {

    ApplicationNotStartedException(final String applicationName) {
        super(String.format("Application '%s' not started", applicationName));
    }
}

@ContextConfiguration("classpath:integration-test-spring-context.xml")
public final class ApplicationHealthChecks extends AbstractTestNGSpringContextTests {

    @Autowired
    private CloudFoundryApplicationHelper cloudFoundryApplicationHelper;

    @Test(groups = { AcsCloudFoundryUtilities.CHECK_APP_HEALTH_TEST_GROUP },
          dependsOnGroups = { AcsCloudFoundryUtilities.Adapter.PUSH_ASSET_ADAPTER_APP_TEST_GROUP },
          ignoreMissingDependencies = true, alwaysRun = true)
    public void checkAssetAdapterApplicationHealth() throws Exception {
        if (!this.cloudFoundryApplicationHelper.applicationStarted(
                AcsCloudFoundryUtilities.Adapter.ACS_ASSET_ADAPTER_APP_NAME)) {
            throw new ApplicationNotStartedException(AcsCloudFoundryUtilities.Adapter.ACS_ASSET_ADAPTER_APP_NAME);
        }
    }

    @Test(groups = { AcsCloudFoundryUtilities.CHECK_APP_HEALTH_TEST_GROUP },
          dependsOnGroups = { AcsCloudFoundryUtilities.PUSH_ACS_APP_TEST_GROUP },
          ignoreMissingDependencies = true, alwaysRun = true)
    public void checkAcsApplicationHealth() throws Exception {
        if (!this.cloudFoundryApplicationHelper.applicationStarted(AcsCloudFoundryUtilities.ACS_APP_NAME)) {
            throw new ApplicationNotStartedException(AcsCloudFoundryUtilities.ACS_APP_NAME);
        }
    }
}
