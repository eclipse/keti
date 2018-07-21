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

package org.eclipse.keti.acs.jmx

import org.apache.tomcat.jdbc.pool.DataSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Profile
import org.springframework.jmx.export.annotation.ManagedAttribute
import org.springframework.jmx.export.annotation.ManagedResource
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean
import org.springframework.stereotype.Component
import java.util.HashMap

const val OBJECTNAME = "org.eclipse.keti.acs.jmx:name=DataSourceMBean"
private const val DELIMITER = "[;,]"
private const val PROP_DELIMITER = "[=:]"

@Profile("performance")
@ManagedResource(objectName = OBJECTNAME, description = "Connection pool propteries")
@Component
class DataSourceMBean {

    @Autowired
    private lateinit var entityManagerFactory: LocalContainerEntityManagerFactoryBean

    val dataSourceImpl: String
        @ManagedAttribute get() = if (this.entityManagerFactory == null || this.entityManagerFactory.dataSource == null) {
            ""
        } else this.entityManagerFactory.dataSource.javaClass.name

    // For TOMCAT_POOL_DATASOURCE
    // in case it is not TOMCAT_POOL_DATASOURCE
    val connectionPool: Map<String, Any>
        @ManagedAttribute get() {
            val connectionPool = HashMap<String, Any>()
            if (this.entityManagerFactory == null || this.entityManagerFactory.dataSource == null) {
                return connectionPool
            }
            if (this.entityManagerFactory.dataSource.javaClass.isAssignableFrom(DataSource::class.java)) {
                val tomcatDs = this.entityManagerFactory.dataSource as org.apache.tomcat.jdbc.pool.DataSource
                connectionPool["driverClassName"] = tomcatDs.driverClassName
                connectionPool["numActive"] = tomcatDs.numActive
                connectionPool["maxActive"] = tomcatDs.maxActive
                connectionPool["numIdle"] = tomcatDs.numIdle
                connectionPool["minIdle"] = tomcatDs.minIdle
                connectionPool["maxIdle"] = tomcatDs.maxIdle
                connectionPool["maxWait"] = tomcatDs.maxWait
                return connectionPool
            }
            val str = this.entityManagerFactory.dataSource.toString()
            var start = str.indexOf('[')
            if (start == -1) {
                start = str.indexOf('=') - 16
                if (start <= 0) {
                    return connectionPool
                }
            }
            val poolString = str.substring(start + 1)
            val array = poolString.split(DELIMITER.toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            for (prop in array) {
                if (prop.contains("=") || prop.contains(":")) {
                    val nameValue =
                        prop.trim { it <= ' ' }.split(PROP_DELIMITER.toRegex()).dropLastWhile { it.isEmpty() }
                            .toTypedArray()
                    if (nameValue.size == 2) {
                        connectionPool[nameValue[0]] = nameValue[1]
                    } else {
                        connectionPool[nameValue[0]] = "null"
                    }
                }
            }
            return connectionPool
        }
}
