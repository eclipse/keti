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
