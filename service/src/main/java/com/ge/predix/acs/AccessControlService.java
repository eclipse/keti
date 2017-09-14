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

package com.ge.predix.acs;

import static com.google.common.base.Predicates.or;
import static springfox.documentation.builders.PathSelectors.regex;

import java.net.URI;
import java.util.Collections;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ImportResource;

import com.ge.predix.acs.monitoring.ManagementSecurityRoleFilter;
import com.ge.predix.acs.request.context.AcsRequestEnrichingFilter;
import com.google.common.base.Predicate;

import io.swagger.models.Scheme;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

/**
 * Access Control Service - Spring Boot Application.
 *
 * @author 212304931
 */
@SpringBootApplication
@EnableSwagger2
@ImportResource(value = "classpath:security-config.xml")
public class AccessControlService {

    private final AcsRequestEnrichingFilter acsRequestEnrichingFilter;
    private final ManagementSecurityRoleFilter managementSecurityRoleFilter;

    @Value("${ACS_URL:}")
    private String acsUrl;

    @Autowired
    public AccessControlService(final AcsRequestEnrichingFilter acsRequestEnrichingFilter,
            final ManagementSecurityRoleFilter managementSecurityRoleFilter) {
        this.acsRequestEnrichingFilter = acsRequestEnrichingFilter;
        this.managementSecurityRoleFilter = managementSecurityRoleFilter;
    }

    public static void main(final String[] args) {
        SpringApplication.run(AccessControlService.class, args);
    }

    /**
     * Register acsRequestEnrichingFilter and disable it. If not disabled spring-boot applies this to all requests. This
     * is configured explicitly as a custom filter in security-config.xml.
     */
    @Bean
    public FilterRegistrationBean filterRegistrationBean() {
        FilterRegistrationBean filterRegistrationBean = new FilterRegistrationBean();
        filterRegistrationBean.setEnabled(false);
        filterRegistrationBean.setFilter(this.acsRequestEnrichingFilter);
        return filterRegistrationBean;
    }

    @Bean
    public FilterRegistrationBean monitoringFilterRegistrationBean() {
        FilterRegistrationBean filterRegistrationBean = new FilterRegistrationBean(this.managementSecurityRoleFilter);
        filterRegistrationBean.addUrlPatterns("/health*");
        filterRegistrationBean.setOrder(1);
        return filterRegistrationBean;
    }

    @Bean
    public Docket attributeManagementApi() {
        return new Docket(DocumentationType.SWAGGER_2).groupName("acs")
                .protocols(Collections.singleton(StringUtils.isEmpty(this.acsUrl) ? Scheme.HTTPS.toValue()
                        : URI.create(this.acsUrl).getScheme()))
                .apiInfo(apiInfo()).select().paths(attributeManagementPaths()).build();
    }

    @SuppressWarnings("unchecked")
    private static Predicate<String> attributeManagementPaths() {
        return or(regex("/v1/subject.*"), regex("/v1/resource.*"), regex("/v1/policy-set.*"),
                regex("/v1/policy-evaluation.*"), regex("/monitoring/heartbeat.*"), regex("/v1/connector.*"));
    }

    private static ApiInfo apiInfo() {
        return new ApiInfoBuilder().title("Access Control").description("Access Control Services (ACS). ").version("v1")
                .license("Apache 2.0").licenseUrl("https://github.com/predix/acs/blob/develop/LICENSE").build();
    }
}
