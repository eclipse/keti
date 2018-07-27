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
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Scope
import org.springframework.http.HttpHeaders
import org.springframework.security.oauth2.client.OAuth2RestTemplate
import org.springframework.stereotype.Component
import java.io.IOException

private const val OAUTH_ENDPOINT = "/oauth/token"

@Component
@Scope("prototype")
class ACSITSetUpFactoryPublic : ACSITSetUpFactory {

    override lateinit var acsUrl: String

    override lateinit var zone1: Zone

    override lateinit var zone2: Zone

    override lateinit var acsZone1Name: String

    override lateinit var acsZone2Name: String

    override lateinit var acsZone3Name: String

    override lateinit var zone1Headers: HttpHeaders

    override lateinit var zone3Headers: HttpHeaders

    override lateinit var acsZoneAdminRestTemplate: OAuth2RestTemplate

    override lateinit var acsZone2AdminRestTemplate: OAuth2RestTemplate

    override lateinit var acsReadOnlyRestTemplate: OAuth2RestTemplate

    override lateinit var acsNoPolicyScopeRestTemplate: OAuth2RestTemplate

    override lateinit var acsZonesAdminRestTemplate: OAuth2RestTemplate

    @Value("\${ACS_TESTING_UAA}")
    private lateinit var uaaUrl: String

    @Value("\${UAA_ADMIN_SECRET:adminsecret}")
    private lateinit var uaaAdminSecret: String

    @Value("\${ACS_SERVICE_ID:predix-acs}")
    private lateinit var serviceId: String

    private var acsAdminRestTemplate: OAuth2RestTemplate? = null

    private var uaaTestUtil: UaaAcsClientsUtil? = null

    @Autowired
    private lateinit var zoneFactory: ZoneFactory

    @Throws(IOException::class)
    override fun setUp() {
        // TestConfig.setupForEclipse(); // Starts ACS when running the test in eclipse.
        this.acsUrl = this.zoneFactory.acsBaseURL

        this.acsZone1Name = getRandomName(this.javaClass.simpleName)
        this.acsZone2Name = getRandomName(this.javaClass.simpleName)
        this.acsZone3Name = getRandomName(this.javaClass.simpleName)

        this.zone1Headers = httpHeaders()
        this.zone1Headers.set(PREDIX_ZONE_ID, this.acsZone1Name)

        this.zone3Headers = httpHeaders()
        this.zone3Headers.set(PREDIX_ZONE_ID, this.acsZone3Name)

        this.uaaTestUtil = UaaAcsClientsUtil(this.uaaUrl, this.uaaAdminSecret)

        this.acsAdminRestTemplate = this.uaaTestUtil!!.createAcsAdminClientAndGetTemplate(this.acsZone1Name)
        this.acsZonesAdminRestTemplate = this.uaaTestUtil!!
            .createAcsAdminClient(listOf<String>(this.acsZone1Name, this.acsZone2Name, this.acsZone3Name))
        this.acsReadOnlyRestTemplate = this.uaaTestUtil!!.createReadOnlyPolicyScopeClient(this.acsZone1Name)
        this.acsNoPolicyScopeRestTemplate = this.uaaTestUtil!!.createNoPolicyScopeClient(this.acsZone1Name)
        this.zone1 = this.zoneFactory.createTestZone(
            this.acsAdminRestTemplate!!, this.acsZone1Name,
            listOf(this.uaaUrl + OAUTH_ENDPOINT)
        )
        this.acsZoneAdminRestTemplate =
            this.uaaTestUtil!!.createZoneClientAndGetTemplate(this.acsZone1Name, this.serviceId)

        this.zone2 = this.zoneFactory.createTestZone(
            this.acsAdminRestTemplate!!, this.acsZone2Name,
            listOf(this.uaaUrl + OAUTH_ENDPOINT)
        )
        this.acsZone2AdminRestTemplate =
            this.uaaTestUtil!!.createZoneClientAndGetTemplate(this.acsZone2Name, this.serviceId)
    }

    override fun destroy() {
        this.zoneFactory.deleteZone(this.acsAdminRestTemplate!!, this.acsZone1Name)
        this.zoneFactory.deleteZone(this.acsAdminRestTemplate!!, this.acsZone2Name)
        this.uaaTestUtil!!.deleteClient(this.acsAdminRestTemplate!!.resource.clientId)
        this.uaaTestUtil!!.deleteClient(this.acsZoneAdminRestTemplate.resource.clientId)
        this.uaaTestUtil!!.deleteClient(this.acsZone2AdminRestTemplate.resource.clientId)
        this.uaaTestUtil!!.deleteClient(this.acsReadOnlyRestTemplate.resource.clientId)
        this.uaaTestUtil!!.deleteClient(this.acsNoPolicyScopeRestTemplate.resource.clientId)
        this.uaaTestUtil!!.deleteClient(this.acsZonesAdminRestTemplate.resource.clientId)
    }

    override fun getAcsZoneConnectorAdminRestTemplate(zone: String): OAuth2RestTemplate {
        return this.uaaTestUtil!!.createAdminConnectorScopeClient(zone)
    }

    override fun getAcsZoneConnectorReadRestTemplate(zone: String): OAuth2RestTemplate {
        return this.uaaTestUtil!!.createReadOnlyConnectorScopeClient(zone)
    }

    override fun getAcsAdminRestTemplate(zone: String): OAuth2RestTemplate {
        return UaaAcsClientsUtil(this.uaaUrl, this.uaaAdminSecret).createAcsAdminClient(listOf(zone))
    }
}
