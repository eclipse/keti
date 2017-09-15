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

package com.ge.predix.acs.monitoring;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.Status;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Component
@Profile({ "predix" })
public class ZacHealthIndicator implements HealthIndicator {

    private static final Logger LOGGER = LoggerFactory.getLogger(ZacHealthIndicator.class);
    private static final String ERROR_MESSAGE_FORMAT = "Unexpected exception while checking ZAC status: {}";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final RestTemplate zacTemplate;

    @Value("${zacCheckHealthUrl}")
    private String zacCheckHealthUrl;

    @Autowired
    public ZacHealthIndicator(final RestTemplate zacTemplate) {
        this.zacTemplate = zacTemplate;
    }

    @Override
    public Health health() {
        return AcsMonitoringUtilities.health(this::check, this.getDescription());
    }

    private AcsMonitoringUtilities.HealthCode check() {
        AcsMonitoringUtilities.HealthCode healthCode = AcsMonitoringUtilities.HealthCode.ERROR;

        try {
            LOGGER.debug("Checking ZAC status using URL: {}", this.zacCheckHealthUrl);
            String response = this.zacTemplate.getForObject(this.zacCheckHealthUrl, String.class);
            JsonNode responseJson = OBJECT_MAPPER.readTree(response);
            if (responseJson.get(AcsMonitoringUtilities.STATUS).asText().equalsIgnoreCase(Status.UP.getCode())) {
                healthCode = AcsMonitoringUtilities.HealthCode.AVAILABLE;
            }
        } catch (JsonParseException | JsonMappingException e) {
            healthCode = AcsMonitoringUtilities.logError(AcsMonitoringUtilities.HealthCode.INVALID_JSON, LOGGER,
                    ERROR_MESSAGE_FORMAT, e);
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
        return String.format("Health check performed by attempting to hit '%s'", this.zacCheckHealthUrl);
    }
}
