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
