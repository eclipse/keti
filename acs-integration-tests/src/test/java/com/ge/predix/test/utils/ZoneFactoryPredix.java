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

package com.ge.predix.test.utils;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.ge.predix.acs.rest.Zone;

@Component
@Profile({ "predix" })
public class ZoneFactoryPredix extends ZoneFactory {

    @Value("${ACS_CF_DOMAIN:}")
    private String acsCFDomain;

    @Autowired
    private OptionalTestSetup zacHelperUtil;

    @Override
    public Zone createTestZone(final RestTemplate restTemplate, final String zoneId,
            final List<String> trustedIssuerIds) throws IOException {
        this.zacHelperUtil.setup(zoneId, Collections.singletonMap("trustedIssuerIds", trustedIssuerIds));
        return super.createTestZone(restTemplate, zoneId, trustedIssuerIds);
    }

    @Override
    public HttpStatus deleteZone(final RestTemplate restTemplate, final String zoneName) {
        this.zacHelperUtil.tearDown(zoneName);
        return super.deleteZone(restTemplate, zoneName);
    }

    @Override
    public String getServiceDomain() {
        return this.acsCFDomain + '.' + super.getServiceDomain();
    }

}
