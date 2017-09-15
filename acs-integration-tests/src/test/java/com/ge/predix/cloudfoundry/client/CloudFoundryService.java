/*******************************************************************************
 * Copyright 2017 General Electric Company
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/

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
