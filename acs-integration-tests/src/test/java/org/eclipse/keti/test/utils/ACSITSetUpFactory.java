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

package org.eclipse.keti.test.utils;

import java.io.IOException;

import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;

import org.eclipse.keti.acs.rest.Zone;

/**
 * @author Sebastian Torres Brown
 *
 *         Setup class for Integration tests. Creates several rest clients with desired levels of authorities, it also
 *         creates a zone.
 *
 */
public interface ACSITSetUpFactory {


    String getAcsUrl();

    Zone getZone1();

    Zone getZone2();

    String getAcsZone1Name();

    String getAcsZone2Name();

    String getAcsZone3Name();

    HttpHeaders getZone1Headers();

    HttpHeaders getZone3Headers();

    OAuth2RestTemplate getAcsZoneAdminRestTemplate();

    OAuth2RestTemplate getAcsAdminRestTemplate(String zone);

    OAuth2RestTemplate getAcsZone2AdminRestTemplate();

    OAuth2RestTemplate getAcsReadOnlyRestTemplate();

    OAuth2RestTemplate getAcsNoPolicyScopeRestTemplate();

    OAuth2RestTemplate getAcsZonesAdminRestTemplate();

    OAuth2RestTemplate getAcsZoneConnectorAdminRestTemplate(String zone);

    OAuth2RestTemplate getAcsZoneConnectorReadRestTemplate(String zone);

    void setUp() throws IOException;

    void destroy();

}
