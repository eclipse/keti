/*******************************************************************************
 * Copyright 2017 General Electric Company
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
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.eclipse.keti.acs.config

import org.apache.tomcat.jdbc.pool.PoolProperties
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean
import org.springframework.transaction.PlatformTransactionManager
import javax.persistence.EntityManagerFactory
import javax.sql.DataSource

private val LOGGER = LoggerFactory.getLogger(EnvDataSourceConfig::class.java)

/**
 * DataSourceConfig used for connecting directly to a postgres database.
 */
@Configuration
@Profile("envDbConfig")
@EnableJpaRepositories(
    "org.eclipse.keti.acs.service.policy.admin.dao",
    "org.eclipse.keti.acs.privilege.management.dao",
    "org.eclipse.keti.acs.zone.management.dao",
    "org.eclipse.keti.acs.attribute.connector.management.dao"
)
class EnvDataSourceConfig {

    private val acsConfigUtil = AcsConfigUtil()

    @Value("\${DB_DRIVER_CLASS_NAME:org.postgresql.Driver}")
    private var driverClassName: String = "org.postgresql.Driver"
    @Value("\${DB_URL:jdbc:postgresql:acs}")
    private var url: String = "jdbc:postgresql:acs"
    @Value("\${DB_USERNAME:postgres}")
    private var username: String = "postgres"
    @Value("\${DB_PASSWORD:}")
    private var password: String? = null
    @Value("\${MIN_ACTIVE:0}")
    private var minActive: Int = 0
    @Value("\${MAX_ACTIVE:100}")
    private var maxActive: Int = 100
    @Value("\${MAX_WAIT_TIME:30000}")
    private var maxWaitTime: Int = 30000

    @Bean
    fun dataSource(): DataSource {
        LOGGER.info("Starting ACS with the database connection: '{}'.", this.url) // $NON-NLS-1$
        val poolProperties = PoolProperties()
        poolProperties.driverClassName = this.driverClassName
        poolProperties.url = this.url
        poolProperties.username = this.username
        poolProperties.password = this.password
        poolProperties.maxActive = this.maxActive
        poolProperties.minIdle = this.minActive
        poolProperties.maxWait = this.maxWaitTime
        return org.apache.tomcat.jdbc.pool.DataSource(poolProperties)
    }

    @Bean
    fun entityManagerFactory(): LocalContainerEntityManagerFactoryBean {
        return this.acsConfigUtil.entityManagerFactory(this.dataSource())
    }

    @Bean
    fun transactionManager(emf: EntityManagerFactory): PlatformTransactionManager {
        return this.acsConfigUtil.transactionManager(emf)
    }
}
