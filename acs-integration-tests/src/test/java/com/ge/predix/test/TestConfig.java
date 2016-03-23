package com.ge.predix.test;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.testng.annotations.BeforeSuite;

import com.ge.predix.acs.AccessControlService;

@Component
public class TestConfig {

    @Value("${ZONE1_NAME:testzone1}")
    private String acsZone1Name;

    @Value("${ZONE2_NAME:testzone2}")
    private String acsZone2Name;

    @Value("${ZONE3_NAME:testzone3}")
    private String acsZone3Name;

    @BeforeSuite
    public void setup() {
        AccessControlService.main(new String[] {});
        System.out.println("TestConfig.setup(): acsZone1Name = " + this.acsZone1Name);
    }
}
