package com.ge.predix.acs.config.metering;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile({ "predix" })
@ImportResource(value = "classpath:META-INF/spring-metering-filter.xml")
public class PredixAcsMeteringConfig {
    //Nurego Metering Filter
}
