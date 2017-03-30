package com.ge.predix.acs.cloudfoundry;

final class AcsCloudFoundryUtilities {

    private AcsCloudFoundryUtilities() {
        throw new AssertionError();
    }

    private static final String APP_AND_SERVICE_NAME_PREFIX = System.getProperty("jenkins.build.number");

    static final String ACS_APP_NAME = "acs-ci-" + APP_AND_SERVICE_NAME_PREFIX;
    static final String ACS_DB_SERVICE_INSTANCE_NAME = "acs-db-ci-" + APP_AND_SERVICE_NAME_PREFIX;
    static final String ACS_REDIS_SERVICE_INSTANCE_NAME = "acs-redis-ci-" + APP_AND_SERVICE_NAME_PREFIX;
    static final String ACS_AUDIT_SERVICE_INSTANCE_NAME = "acs-audit-service-int-tests";
}
