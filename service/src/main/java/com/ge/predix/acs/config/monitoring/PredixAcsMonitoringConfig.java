package com.ge.predix.acs.config.monitoring;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.client.RestTemplate;

@Configuration
@Profile({ "predix" })
public class PredixAcsMonitoringConfig {
    @Bean
    public RestTemplate zacTemplate() {
        return new RestTemplate();
    }
}
