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

import java.util.ArrayList;
import java.util.Arrays;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.Test;

import com.ge.predix.cloudfoundry.client.CloudFoundryApplicationHelper;
import com.ge.predix.test.TestAnnotationTransformer;

@Profile({ "integration" })
@Component
@ContextConfiguration("classpath:integration-test-spring-context.xml")
public final class DeleteApplications extends AbstractTestNGSpringContextTests {

    @Autowired
    private CloudFoundryApplicationHelper cloudFoundryApplicationHelper;

    @Test(dependsOnGroups = { TestAnnotationTransformer.INTEGRATION_TEST_GROUP },
            alwaysRun = true)
    public void deleteAcsApplication() throws Exception {
        this.cloudFoundryApplicationHelper.unbindServicesAndDeleteApplication(AcsCloudFoundryUtilities.ACS_APP_NAME,
                new ArrayList<>(Arrays.asList(PushAcsApplication.getAcsDbServiceName(),
                        PushAcsApplication.getAcsDecisionRedisServiceName(),
                        PushAcsApplication.getAcsResourceRedisServiceName(),
                        PushAcsApplication.getAcsSubjectRedisServiceName())));
    }

    @Test(dependsOnGroups = { TestAnnotationTransformer.INTEGRATION_TEST_GROUP },
            alwaysRun = true)
    public void deleteAssetAdapterApplication() throws Exception {
        this.cloudFoundryApplicationHelper
                .unbindAndDeleteServicesAndApplication(AcsCloudFoundryUtilities.Adapter.ACS_ASSET_ADAPTER_APP_NAME,
                        new ArrayList<>(
                                Arrays.asList(AcsCloudFoundryUtilities.Adapter.ACS_ASSET_UAA_SERVICE_INSTANCE_NAME,
                                        AcsCloudFoundryUtilities.Adapter.ACS_ASSET_SERVICE_INSTANCE_NAME)));
    }
}
