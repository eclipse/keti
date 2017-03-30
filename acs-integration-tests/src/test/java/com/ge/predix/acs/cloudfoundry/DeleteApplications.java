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

@Profile({ "integration" })
@Component
@ContextConfiguration("classpath:integration-test-spring-context.xml")
public final class DeleteApplications extends AbstractTestNGSpringContextTests {

    @Autowired
    private CloudFoundryApplicationHelper cloudFoundryApplicationHelper;

    @Test
    public void deleteAcsApplication() throws Exception {
        this.cloudFoundryApplicationHelper.deleteApplicationAndServices(AcsCloudFoundryUtilities.ACS_APP_NAME,
                new ArrayList<>(Arrays.asList(AcsCloudFoundryUtilities.ACS_DB_SERVICE_INSTANCE_NAME,
                        AcsCloudFoundryUtilities.ACS_REDIS_SERVICE_INSTANCE_NAME)));
    }
}
