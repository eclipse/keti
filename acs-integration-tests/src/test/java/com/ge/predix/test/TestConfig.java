package com.ge.predix.test;

import org.testng.annotations.BeforeSuite;

import com.ge.predix.acs.AccessControlService;

public class TestConfig {

    @BeforeSuite
    public void setup() {
        AccessControlService.main(new String[] {});
    }
}
