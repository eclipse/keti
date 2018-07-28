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

package org.eclipse.keti.acs.monitoring

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import javax.sql.DataSource

private val LOGGER = LoggerFactory.getLogger(AcsMonitoringRepository::class.java)

/**
 * @author acs-engineers@ge.com
 */
@Repository
class AcsMonitoringRepository {

    private lateinit var jdbcTemplate: JdbcTemplate

    @Autowired
    fun setDataSource(dataSource: DataSource) {
        this.jdbcTemplate = JdbcTemplate(dataSource)
    }

    fun queryPolicySetTable() {
        val query = "select policy_set_id from policy_set limit 1"
        val queryResults = this.jdbcTemplate.query(query) { rs, _ -> rs.getString(1) }
        LOGGER.info(
            "Successfully executed health check query on ACS database: {} (result set size: {})", query,
            queryResults.size
        )
    }
}
