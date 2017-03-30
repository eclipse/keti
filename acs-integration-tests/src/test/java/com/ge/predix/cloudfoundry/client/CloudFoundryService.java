package com.ge.predix.cloudfoundry.client;

import java.util.Map;

public final class CloudFoundryService {

    private String planName;
    private String serviceInstanceName;
    private String serviceName;
    private Map<String, Object> parameters;

    public CloudFoundryService(final String planName, final String serviceInstanceName, final String serviceName,
            final Map<String, Object> parameters) {
        this.planName = planName;
        this.serviceInstanceName = serviceInstanceName;
        this.serviceName = serviceName;
        this.parameters = parameters;
    }

    String getPlanName() {
        return this.planName;
    }

    String getServiceInstanceName() {
        return this.serviceInstanceName;
    }

    String getServiceName() {
        return this.serviceName;
    }

    Map<String, Object> getParameters() {
        return this.parameters;
    }
}
