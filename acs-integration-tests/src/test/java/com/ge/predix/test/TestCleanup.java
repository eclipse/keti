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

package com.ge.predix.test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.Test;

import com.ge.predix.test.utils.ACSRestTemplateFactory;
import com.ge.predix.test.utils.PrivilegeHelper;
import com.ge.predix.test.utils.ZoneHelper;

@ContextConfiguration("classpath:integration-test-spring-context.xml")
@Test
public class TestCleanup extends AbstractTestNGSpringContextTests {

    @Autowired
    private ACSRestTemplateFactory acsRestTemplateFactory;

    @Autowired
    private PrivilegeHelper privilegeHelper;

    @Autowired
    private ZoneHelper zoneHelper;

    @AfterSuite
    public void afterSuite() throws Exception {
        OAuth2RestTemplate acsAdminRestTemplate = this.acsRestTemplateFactory.getACSTemplateWithPolicyScope();
        this.privilegeHelper.deleteResources(acsAdminRestTemplate, this.zoneHelper.getZone1Url(), null);
        this.privilegeHelper.deleteSubjects(acsAdminRestTemplate, this.zoneHelper.getZone1Url(), null);
        this.privilegeHelper.deleteResources(acsAdminRestTemplate, this.zoneHelper.getZone2Url(), null);
        this.privilegeHelper.deleteSubjects(acsAdminRestTemplate, this.zoneHelper.getZone2Url(), null);
    }
}
