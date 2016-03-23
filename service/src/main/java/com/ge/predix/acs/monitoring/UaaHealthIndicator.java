/*******************************************************************************
 * Copyright 2016 General Electric Company.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/

package com.ge.predix.acs.monitoring;

import static com.ge.predix.acs.monitoring.AcsMonitoringConstants.FAILED_CHECK;
import static com.ge.predix.acs.monitoring.AcsMonitoringConstants.SUCCESS_CHECK;
import static com.ge.predix.acs.monitoring.AcsMonitoringConstants.UAA_OUT_OF_SERVICE;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 *
 * @author 212360328
 */
@Component
public class UaaHealthIndicator implements HealthIndicator {

    private static final Logger LOGGER = LoggerFactory.getLogger(UaaHealthIndicator.class);

    @Autowired
    private RestTemplate uaaTemplate;

    @Value("${uaaCheckHealthUrl}")
    private String uaaCheckHealthUrl;

    @Override
    public Health health() {
        int errorCode = check(); // perform some specific health check
        if (errorCode != 0) {
            return Health.status(UAA_OUT_OF_SERVICE).withDetail("Error Code for UAA", errorCode).build();
        }
        return Health.up().build();
    }

    private int check() {
        try {

            LOGGER.debug("Checking UAA Status");
            this.uaaTemplate.getForObject(this.uaaCheckHealthUrl, String.class);
            return SUCCESS_CHECK;

        } catch (Exception e) {
            LOGGER.error("Unexpected exception while checking UAA Status", e);
            return FAILED_CHECK;
        }
    }
}
