/*******************************************************************************
 * Copyright 2018 General Electric Company
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

package org.eclipse.keti.test.utils

import org.eclipse.keti.acs.rest.Zone
import org.springframework.http.HttpHeaders
import org.springframework.security.oauth2.client.OAuth2RestTemplate
import java.io.IOException

/**
 * @author Sebastian Torres Brown
 *
 * Setup class for Integration tests. Creates several rest clients with desired levels of authorities, it also
 * creates a zone.
 */
interface ACSITSetUpFactory {

    val acsUrl: String

    val zone1: Zone

    val zone2: Zone

    val acsZone1Name: String

    val acsZone2Name: String

    val acsZone3Name: String

    val zone1Headers: HttpHeaders

    val zone3Headers: HttpHeaders

    val acsZoneAdminRestTemplate: OAuth2RestTemplate

    val acsZone2AdminRestTemplate: OAuth2RestTemplate

    val acsReadOnlyRestTemplate: OAuth2RestTemplate

    val acsNoPolicyScopeRestTemplate: OAuth2RestTemplate

    val acsZonesAdminRestTemplate: OAuth2RestTemplate

    fun getAcsAdminRestTemplate(zone: String): OAuth2RestTemplate

    fun getAcsZoneConnectorAdminRestTemplate(zone: String): OAuth2RestTemplate

    fun getAcsZoneConnectorReadRestTemplate(zone: String): OAuth2RestTemplate

    @Throws(IOException::class)
    fun setUp()

    fun destroy()

}
