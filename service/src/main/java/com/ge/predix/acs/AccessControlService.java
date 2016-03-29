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

package com.ge.predix.acs;

import static com.google.common.base.Predicates.or;
import static springfox.documentation.builders.PathSelectors.regex;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.embedded.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ImportResource;

import com.ge.predix.acs.request.context.AcsRequestEnrichingFilter;
import com.google.common.base.Predicate;

import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;
import org.springframework.cloud.client.circuitbreaker.EnableCircuitBreaker;

/**
 * Access Control Service - Spring Boot Application.
 *
 * @author 212304931
 */
@SpringBootApplication
@EnableCircuitBreaker
@EnableSwagger2
@ImportResource(value = "classpath:security-config.xml")
public class AccessControlService {

    @Autowired
    private AcsRequestEnrichingFilter acsRequestEnrichingFilter;

    private String serviceId;

    public String getServiceId() {
        return this.serviceId;
    }

    public void setServiceId(final String serviceId) {
        this.serviceId = serviceId;
    }

    public static void main(final String[] args) {
        SpringApplication.run(AccessControlService.class, args);
    }

    /**
     * Register acsRequestEnrichingFilter and disable it. If not disabled spring-boot applies this to all requests.
     * This is configured explicitly as a custom filter in security-config.xml.
     */
    @Bean
    public FilterRegistrationBean filterRegistrationBean() {
        FilterRegistrationBean filterRegistrationBean = new FilterRegistrationBean();
        filterRegistrationBean.setEnabled(false);
        filterRegistrationBean.setFilter(this.acsRequestEnrichingFilter);
        return filterRegistrationBean;
    }

    @Bean
    public Docket attributeManagementApi() {
        return new Docket(DocumentationType.SWAGGER_2).groupName("acs").apiInfo(apiInfo()).select()
                .paths(attributeManagementPaths()).build();
    }

    @SuppressWarnings("unchecked")
    private Predicate<String> attributeManagementPaths() {
        return or(regex("/v1/subject.*"), regex("/v1/resource.*"), regex("/v1/policy-set.*"),
                regex("/v1/policy-evaluation.*"), regex("/monitoring/heartbeat.*"));
    }

    private ApiInfo apiInfo() {
        return new ApiInfoBuilder().title("Access Control").description("Access Control Services (ACS). ").version("v1")
                .build();
    }
}
