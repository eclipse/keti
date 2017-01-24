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
package com.ge.predix.controller.test;

import org.apache.commons.lang3.StringUtils;

public class ConfigureEnvironment {
    static {
        setPropertyIfNotExist("ACS_DEFAULT_ISSUER_ID",
                              "http://acs.localhost:" + System.getenv("UAA_LOCAL_PORT") + "/uaa");
        setPropertyIfNotExist("ACS_SERVICE_ID", "predix-acs");
        setPropertyIfNotExist("ACS_UAA_URL", "http://localhost:" + System.getenv("UAA_LOCAL_PORT") + "/uaa");
        setPropertyIfNotExist("ZAC_UAA_URL", "http://localhost:" + System.getenv("UAA_LOCAL_PORT") + "/uaa");
        setPropertyIfNotExist("ZAC_URL", "http://localhost:" + System.getenv("ZAC_LOCAL_PORT"));
        setPropertyIfNotExist("ZAC_CLIENT_ID", "fake-client");
        setPropertyIfNotExist("ZAC_CLIENT_SECRET", "fake-client");
    }

    private static void setPropertyIfNotExist(final String name, final String value) {
        if (StringUtils.isEmpty(System.getProperty(name))) {
            System.setProperty(name, value);
        }
    }
}
