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
 *
 * SPDX-License-Identifier: Apache-2.0
 *******************************************************************************/

package org.eclipse.keti.acs.config;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Configuration file used for in-memory profile.
 *
 * @author acs-engineers@ge.com
 */
@Configuration
@EnableAutoConfiguration
@Profile({ "h2" })
@EnableJpaRepositories({ "org.eclipse.keti.acs.service.policy.admin.dao",
        "org.eclipse.keti.acs.privilege.management.dao", "org.eclipse.keti.acs.zone.management.dao",
        "org.eclipse.keti.acs.attribute.connector.management.dao" })
public class InMemoryDataSourceConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(InMemoryDataSourceConfig.class);

    private final AcsConfigUtil acsConfigUtil = new AcsConfigUtil();

    @Bean
    public DataSource getDataSourceConfig() {
        LOGGER.info("Starting ACS with H2 database"); //$NON-NLS-1$
        return new EmbeddedDatabaseBuilder().setType(EmbeddedDatabaseType.H2).build();
    }

    @Bean
    public LocalContainerEntityManagerFactoryBean entityManagerFactory() {
        return this.acsConfigUtil.entityManagerFactory(this.getDataSourceConfig());
    }

    @Bean
    public PlatformTransactionManager transactionManager(final EntityManagerFactory emf) {
        return this.acsConfigUtil.transactionManager(emf);
    }

}
