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
package com.ge.predix.test.utils;

import static com.ge.predix.test.utils.ACSTestUtil.isServerListening;

import java.net.URI;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.testng.SkipException;

@Component
public class ZacTestUtil {

    @Value("${zac.url}")
    private String zacUrl;

    public void assumeZacServerAvailable() {
        // Not all tests use Spring so try to get the URL from the environment.
        if (StringUtils.isEmpty(this.zacUrl)) {
            this.zacUrl = System.getenv("ZAC_URL");
        }
        if (!isServerListening(URI.create(this.zacUrl))) {
            throw new SkipException("Skipping tests because ZAC is not available.");
        }
    }

    public void setZacUrl(final String zacUrl) {
        this.zacUrl = zacUrl;
    }
}
