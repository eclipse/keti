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
 *******************************************************************************/

package com.ge.predix.acs.testutils;

import org.apache.commons.lang.StringUtils;
import org.springframework.test.context.ActiveProfilesResolver;

public class TestActiveProfilesResolver implements ActiveProfilesResolver {

    @Override
    public String[] resolve(final Class<?> arg0) {
        String envSpringProfilesActive = System.getenv("SPRING_PROFILES_ACTIVE");
        String[] profiles = new String[] { "h2", "public", "simple-cache", "titan" };
        if (StringUtils.isNotEmpty(envSpringProfilesActive)) {
            profiles = StringUtils.split(envSpringProfilesActive, ',');
        }
        System.out.println("SPRING_ACTIVE_PROFILES: " + StringUtils.join(profiles, ','));
        return profiles;
    }
}
