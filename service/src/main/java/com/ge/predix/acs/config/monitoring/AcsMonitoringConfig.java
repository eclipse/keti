package com.ge.predix.acs.config.monitoring;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class AcsMonitoringConfig {
    @Bean
    public RestTemplate uaaTemplate() {
        return new RestTemplate();
    }
}
