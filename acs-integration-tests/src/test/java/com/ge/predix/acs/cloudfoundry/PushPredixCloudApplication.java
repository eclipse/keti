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

import java.util.Collections;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.Test;

@ContextConfiguration("classpath:integration-test-spring-context.xml")
public final class PushPredixCloudApplication extends AbstractTestNGSpringContextTests {

    @Autowired
    private PushAcsApplication pushAcsApplication;

    @Test(groups = { AcsCloudFoundryUtilities.PUSH_ACS_APP_TEST_GROUP },
          dependsOnGroups = { AcsCloudFoundryUtilities.Adapter.PUSH_ASSET_ADAPTER_APP_TEST_GROUP })
    public void pushAcsApplication() throws Exception {
        this.pushAcsApplication.pushApplication(Collections.emptyMap());
    }
}
