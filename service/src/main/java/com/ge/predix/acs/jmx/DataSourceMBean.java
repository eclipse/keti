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
package com.ge.predix.acs.jmx;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.stereotype.Component;

@Profile({ "performance" })
@ManagedResource(objectName = DataSourceMBean.OBJECTNAME, description = "Connection pool propteries")
@Component
public class DataSourceMBean {
    public static final String OBJECTNAME = "com.ge.predix.acs.jmx:name=DataSourceMBean";
    private static final String DELIMITER = ";|,";
    private static final String PROP_DELIMITER = "=|:";
    private static final String TOMCAT_POOL_DATASOURCE = "org.apache.tomcat.jdbc.pool.DataSource";

    @Autowired
    private LocalContainerEntityManagerFactoryBean entityManagerFactory;

    @ManagedAttribute
    public String getDataSourceImpl() {
        if (this.entityManagerFactory == null || this.entityManagerFactory.getDataSource() == null) {
            return "";
        }
        return this.entityManagerFactory.getDataSource().getClass().getName();
    }

    @ManagedAttribute
    public Map<String, Object> getConnectionPool() {
        Map<String, Object> connectionPool = new HashMap<>();
        if (this.entityManagerFactory == null || this.entityManagerFactory.getDataSource() == null) {
            return connectionPool;
        }
        // For TOMCAT_POOL_DATASOURCE
        if (this.entityManagerFactory.getDataSource().getClass().getName().equals(TOMCAT_POOL_DATASOURCE)) {
            org.apache.tomcat.jdbc.pool.DataSource tomcatDs =
                    (org.apache.tomcat.jdbc.pool.DataSource) this.entityManagerFactory.getDataSource();
            connectionPool.put("driverClassName", tomcatDs.getDriverClassName());
            connectionPool.put("numActive", tomcatDs.getNumActive());
            connectionPool.put("maxActive", tomcatDs.getMaxActive());
            connectionPool.put("numIdle", tomcatDs.getNumIdle());
            connectionPool.put("minIdle", tomcatDs.getMinIdle());
            connectionPool.put("maxIdle", tomcatDs.getMaxIdle());
            connectionPool.put("maxWait", tomcatDs.getMaxWait());
            return connectionPool;
        }

        // in case it is not TOMCAT_POOL_DATASOURCE
        String str = this.entityManagerFactory.getDataSource().toString();
        int start = str.indexOf("[");
        if (start == -1) {
            start = str.indexOf("=") - 16;
            if (start <= 0) {
                return connectionPool;
            }
        }
        String poolString = str.substring(start + 1);
        String[] array = poolString.split(DELIMITER);
        for (String prop : array) {
            if (prop.contains("=") || prop.contains(":")) {
                String[] nameValue = prop.trim().split(PROP_DELIMITER);
                if (nameValue.length == 2) {
                    connectionPool.put(nameValue[0], nameValue[1]);
                } else {
                    connectionPool.put(nameValue[0], "null");
                }
            }
        }
        return connectionPool;
    }
}
