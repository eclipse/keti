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

package com.ge.predix.acs.monitoring;

import java.util.List;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 *
 * @author 212360328
 */
@Repository
public class AcsMonitoringRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(AcsMonitoringRepository.class);

    private JdbcTemplate jdbcTemplate;

    @Autowired
    public void setDataSource(final DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    void queryPolicySetTable() {
        String query = "select policy_set_id from policy_set limit 1";
        List<String> queryResults = this.jdbcTemplate.query(query, (rs, rowNum) -> rs.getString(1));
        LOGGER.info("Successfully executed health check query on ACS database: {} (result set size: {})", query,
                queryResults.size());
    }
}
