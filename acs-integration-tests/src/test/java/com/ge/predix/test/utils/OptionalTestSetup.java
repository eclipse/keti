package com.ge.predix.test.utils;

import java.util.Map;

public interface OptionalTestSetup {

    void setup(String zoneId, Map<String, Object> trustedIssuers);

    void tearDown(String zoneId);
}
