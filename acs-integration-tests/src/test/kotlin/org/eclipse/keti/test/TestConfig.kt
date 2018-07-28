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

package org.eclipse.keti.test

import org.apache.commons.lang.StringUtils
import org.eclipse.keti.acs.AccessControlService
import org.testng.annotations.BeforeSuite

object TestConfig {

    @get:Synchronized
    private var isAcsStarted: Boolean = false

    @BeforeSuite
    @Synchronized
    fun setup() {
        if (!isAcsStarted) {
            AccessControlService.main(arrayOf())
            isAcsStarted = true
        }
    }

    @BeforeSuite
    @Synchronized
    fun setupForEclipse() {
        val runInEclipse = System.getenv("RUN_IN_ECLIPSE")
        if (StringUtils.isEmpty(runInEclipse) || !runInEclipse.equals("true", ignoreCase = true)) {
            return
        }
        if (!isAcsStarted) {
            println("*** Setting up test for Eclipse ***")
            var springProfilesActive = System.getenv("SPRING_PROFILES_ACTIVE")
            if (StringUtils.isEmpty(springProfilesActive)) {
                springProfilesActive = "h2,public,simple-cache"
            }
            System.setProperty("spring.profiles.active", springProfilesActive)
            AccessControlService.main(arrayOf())
            isAcsStarted = true
        }
    }
} // not called
