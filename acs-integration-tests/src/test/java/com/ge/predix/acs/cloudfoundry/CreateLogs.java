package com.ge.predix.acs.cloudfoundry;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import org.cloudfoundry.doppler.LogMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.ge.predix.cloudfoundry.client.CloudFoundryApplicationHelper;
import com.ge.predix.test.TestAnnotationTransformer;

@Profile({ "integration" })
@Component
@ContextConfiguration("classpath:integration-test-spring-context.xml")
public class CreateLogs {

    @Autowired
    private CloudFoundryApplicationHelper cloudFoundryApplicationHelper;

    @Test(dependsOnGroups = { TestAnnotationTransformer.INTEGRATION_TEST_GROUP },
            alwaysRun = true)
    public void getAcsLogs() throws IOException {
        System.out.println("is app helper null: " + this.cloudFoundryApplicationHelper);
        System.out.println(AcsCloudFoundryUtilities.ACS_APP_NAME);
        List<LogMessage> logMessages = this.cloudFoundryApplicationHelper
                .getLogs(AcsCloudFoundryUtilities.ACS_APP_NAME);
        for (LogMessage logMessage : logMessages) {
            System.out.println(logMessage);
        }

        FileWriter writer = new FileWriter(AcsCloudFoundryUtilities.ACS_APP_NAME + ".txt");
        for (LogMessage log : logMessages) {
            writer.write(log.getMessage());
        }
        writer.close();
    }
}
