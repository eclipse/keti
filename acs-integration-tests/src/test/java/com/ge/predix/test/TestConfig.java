package com.ge.predix.test;

import org.apache.commons.lang.StringUtils;
import org.testng.annotations.BeforeSuite;

import com.ge.predix.acs.AccessControlService;

public class TestConfig {

    private static boolean acsStarted;

    public static synchronized boolean isAcsStarted() {
        return acsStarted;
    }

    @BeforeSuite
    public static synchronized void setup() {
        if (!acsStarted) {
            AccessControlService.main(new String[] {});
            acsStarted = true;
        }
    }

    @BeforeSuite
    public static synchronized void setupForEclipse() {
        String runInEclipse = System.getenv("RUN_IN_ECLIPSE");
        if (StringUtils.isEmpty(runInEclipse) || !runInEclipse.equalsIgnoreCase("true")) {
            return;
        }
        if (!acsStarted) {
            System.out.println("*** Setting up test for Eclipse ***");
            String springProfilesActive = System.getenv("SPRING_PROFILES_ACTIVE");
            if (StringUtils.isEmpty(springProfilesActive)) {
                springProfilesActive = "h2,public,simple-cache";
            }
            System.setProperty("spring.profiles.active", springProfilesActive);
            AccessControlService.main(new String[] {});
            acsStarted = true;
        }
    }
}
