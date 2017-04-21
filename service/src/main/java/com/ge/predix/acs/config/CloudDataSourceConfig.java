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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.cloud.config.java.AbstractCloudConfig;
import org.springframework.cloud.service.PooledServiceConnectorConfig.PoolConfig;
import org.springframework.cloud.service.relational.DataSourceConfig;
import org.springframework.cloud.service.relational.DataSourceConfig.ConnectionConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * DataSourceConfig used for all cloud profiles.
 */
@Configuration
@Profile({ "cloudDbConfig" })
@EnableAutoConfiguration
@EnableJpaRepositories({ "com.ge.predix.acs.service.policy.admin.dao",
        "com.ge.predix.acs.privilege.management.dao",
        "com.ge.predix.acs.zone.management.dao",
        "com.ge.predix.acs.attribute.connector.management.dao" })
public class CloudDataSourceConfig extends AbstractCloudConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(CloudDataSourceConfig.class);

    private final AcsConfigUtil acsConfigUtil = new AcsConfigUtil();

    @Value("${ACS_DB}")
    private String acsDb;
    @Value("${MIN_ACTIVE:0}")
    private int minActive;
    @Value("${MAX_ACTIVE:100}")
    private int maxActive;
    @Value("${MAX_WAIT_TIME:30000}")
    private int maxWaitTime;

    @Bean
    public DataSourceConfig dataSourceConfig() {
        PoolConfig poolConfig = new PoolConfig(this.minActive, this.maxActive, this.maxWaitTime);
        ConnectionConfig connect = new ConnectionConfig("charset=utf-8");
        return new DataSourceConfig(poolConfig, connect);
    }

    @Bean
    public DataSource dataSource() {
        LOGGER.info("Starting ACS with the database that is bound to it: {}", this.acsDb); //$NON-NLS-1$
        return connectionFactory().dataSource(this.acsDb, dataSourceConfig());
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
