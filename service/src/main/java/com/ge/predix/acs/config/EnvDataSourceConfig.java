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

package com.ge.predix.acs.config;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;

import org.apache.tomcat.jdbc.pool.PoolProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * DataSourceConfig used for connecting directly to a postgres database.
 */
@Configuration
@Profile({ "envDbConfig" })
@EnableAutoConfiguration
@EnableJpaRepositories({ "com.ge.predix.acs.service.policy.admin.dao",
    "com.ge.predix.acs.privilege.management.dao",
    "com.ge.predix.acs.zone.management.dao" })
public class EnvDataSourceConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(EnvDataSourceConfig.class);

    private final AcsConfigUtil acsConfigUtil = new AcsConfigUtil();

    @Value("${DB_DRIVER_CLASS_NAME:org.postgresql.Driver}")
    private String driverClassName;
    @Value("${DB_URL:jdbc:postgresql:acs}")
    private String url;
    @Value("${DB_USERNAME:postgres}")
    private String username;
    @Value("${DB_PASSWORD:}")
    private String password;
    @Value("${MIN_ACTIVE:0}")
    private int minActive;
    @Value("${MAX_ACTIVE:100}")
    private int maxActive;
    @Value("${MAX_WAIT_TIME:30000}")
    private int maxWaitTime;

    @Bean
    public DataSource dataSource() {
        LOGGER.info("Starting ACS with the database connection: '" + this.url + "'."); //$NON-NLS-1$
        PoolProperties poolProperties = new PoolProperties();
        poolProperties.setDriverClassName(this.driverClassName);
        poolProperties.setUrl(this.url);
        poolProperties.setUsername(this.username);
        poolProperties.setPassword(this.password);
        poolProperties.setMaxActive(this.maxActive);
        poolProperties.setMinIdle(this.minActive);
        poolProperties.setMaxWait(this.maxWaitTime);
        return new org.apache.tomcat.jdbc.pool.DataSource(poolProperties);
    }

    @Bean
    public LocalContainerEntityManagerFactoryBean entityManagerFactory() {
        return this.acsConfigUtil.entityManagerFactory(this.dataSource());
    }

    @Bean
    public PlatformTransactionManager transactionManager(final EntityManagerFactory emf) {
        return this.acsConfigUtil.transactionManager(emf);
    }
}
