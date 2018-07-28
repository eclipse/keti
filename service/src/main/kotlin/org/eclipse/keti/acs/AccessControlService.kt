/*******************************************************************************
 * Copyright 2018 General Electric Company
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
 *
 * SPDX-License-Identifier: Apache-2.0
 *******************************************************************************/

package org.eclipse.keti.acs

import com.google.common.base.Predicate
import com.google.common.base.Predicates.or
import io.swagger.models.Scheme
import org.apache.commons.lang.StringUtils
import org.eclipse.keti.acs.monitoring.ManagementSecurityRoleFilter
import org.eclipse.keti.acs.request.context.AcsRequestEnrichingFilter
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ImportResource
import springfox.documentation.builders.ApiInfoBuilder
import springfox.documentation.builders.PathSelectors.regex
import springfox.documentation.service.ApiInfo
import springfox.documentation.spi.DocumentationType
import springfox.documentation.spring.web.plugins.Docket
import springfox.documentation.swagger2.annotations.EnableSwagger2
import java.net.URI

private fun attributeManagementPaths(): Predicate<String> {
    return or(
        regex("/v1/subject.*"),
        regex("/v1/resource.*"),
        regex("/v1/policy-set.*"),
        regex("/v1/policy-evaluation.*"),
        regex("/monitoring/heartbeat.*"),
        regex("/v1/connector.*")
    )
}

private fun apiInfo(): ApiInfo {
    return ApiInfoBuilder().title("Access Control").description("Access Control Services (ACS). ").version("v1")
        .license("Apache 2.0").licenseUrl("https://github.com/eclipse/keti/blob/master/LICENSE").build()
}

/**
 * Access Control Service - Spring Boot Application.
 *
 * @author acs-engineers@ge.com
 */
@SpringBootApplication
@EnableSwagger2
@ImportResource(value = ["classpath:security-config.xml"])
class AccessControlService @Autowired constructor(
    private val acsRequestEnrichingFilter: AcsRequestEnrichingFilter,
    private val managementSecurityRoleFilter: ManagementSecurityRoleFilter
) {

    @Value("\${ACS_URL:}")
    private val acsUrl: String? = null

    /**
     * Register acsRequestEnrichingFilter and disable it. If not disabled spring-boot applies this to all requests. This
     * is configured explicitly as a custom filter in security-config.xml.
     */
    @Bean
    fun filterRegistrationBean(): FilterRegistrationBean {
        val filterRegistrationBean = FilterRegistrationBean()
        filterRegistrationBean.isEnabled = false
        filterRegistrationBean.filter = this.acsRequestEnrichingFilter
        return filterRegistrationBean
    }

    @Bean
    fun monitoringFilterRegistrationBean(): FilterRegistrationBean {
        val filterRegistrationBean = FilterRegistrationBean(this.managementSecurityRoleFilter)
        filterRegistrationBean.addUrlPatterns("/health*")
        filterRegistrationBean.order = 1
        return filterRegistrationBean
    }

    @Bean
    fun attributeManagementApi(): Docket {
        return Docket(DocumentationType.SWAGGER_2).groupName("acs").protocols(
            setOf(
                if (StringUtils.isEmpty(this.acsUrl)) Scheme.HTTPS.toValue()
                else URI.create(this.acsUrl!!).scheme
            )
        ).apiInfo(apiInfo()).select().paths(attributeManagementPaths()).build()
    }

    companion object {

        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplication.run(AccessControlService::class.java, *args)
        }
    }
}
