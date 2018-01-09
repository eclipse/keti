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

package com.ge.predix.test;

import org.apache.commons.lang.StringUtils;
import org.testng.annotations.BeforeSuite;

import com.ge.predix.acs.AccessControlService;

public final class TestConfig {

    private TestConfig() {
        //not called
     }

    private static boolean acsStarted;

    public static synchronized boolean isAcsStarted() {
        return acsStarted;
    }

    @BeforeSuite
    public static synchronized void setup() {
        if (!acsStarted) {
            AccessControlService.main(new String[] {});
            acsStarted = true;
        }
    }

    @BeforeSuite
    public static synchronized void setupForEclipse() {
        String runInEclipse = System.getenv("RUN_IN_ECLIPSE");
        if (StringUtils.isEmpty(runInEclipse) || !runInEclipse.equalsIgnoreCase("true")) {
            return;
        }
        if (!acsStarted) {
            System.out.println("*** Setting up test for Eclipse ***");
            String springProfilesActive = System.getenv("SPRING_PROFILES_ACTIVE");
            if (StringUtils.isEmpty(springProfilesActive)) {
                springProfilesActive = "h2,public,simple-cache";
            }
            System.setProperty("spring.profiles.active", springProfilesActive);
            AccessControlService.main(new String[] {});
            acsStarted = true;
        }
    }
}
