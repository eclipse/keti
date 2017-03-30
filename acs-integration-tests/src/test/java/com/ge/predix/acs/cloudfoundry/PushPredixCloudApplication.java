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

    @Test
    public void pushAcsApplication() throws Exception {
        this.pushAcsApplication.pushApplication(Collections.emptyMap());
    }
}
