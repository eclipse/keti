/*
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
 */

package com.ge.predix.acs.monitoring;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Component
public class UaaHealthIndicator implements HealthIndicator {

    private static final Logger LOGGER = LoggerFactory.getLogger(UaaHealthIndicator.class);
    private static final String ERROR_MESSAGE_FORMAT = "Unexpected exception while checking UAA status: {}";

    private final RestTemplate uaaTemplate;

    @Value("${uaaCheckHealthUrl}")
    private String uaaCheckHealthUrl;

    @Autowired
    public UaaHealthIndicator(final RestTemplate uaaTemplate) {
        this.uaaTemplate = uaaTemplate;
    }

    @Override
    public Health health() {
        return AcsMonitoringUtilities.health(this::check, this.getDescription());
    }

    private AcsMonitoringUtilities.HealthCode check() {
        AcsMonitoringUtilities.HealthCode healthCode;

        try {
            LOGGER.debug("Checking UAA status using URL: {}", this.uaaCheckHealthUrl);
            this.uaaTemplate.getForObject(this.uaaCheckHealthUrl, String.class);
            healthCode = AcsMonitoringUtilities.HealthCode.AVAILABLE;
        } catch (RestClientException e) {
            healthCode = AcsMonitoringUtilities.logError(AcsMonitoringUtilities.HealthCode.UNREACHABLE, LOGGER,
                    ERROR_MESSAGE_FORMAT, e);
        } catch (Exception e) {
            healthCode = AcsMonitoringUtilities.logError(AcsMonitoringUtilities.HealthCode.ERROR, LOGGER,
                    ERROR_MESSAGE_FORMAT, e);
        }

        return healthCode;
    }

    String getDescription() {
        return String.format("Health check performed by attempting to hit '%s'", this.uaaCheckHealthUrl);
    }
}
