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
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestTemplate
import java.io.IOException
import java.net.URI
import java.net.URISyntaxException
import java.util.UUID

const val ACS_ZONE_API_PATH = "$ACS_VERSION/zone/"

/**
 * Creates a random zone name. Having a random zone name avoids test collisions when executing in parallel
 *
 * @param clazz
 * a classname to pass
 * @return String randomize name of the zone
 */
internal fun getRandomName(clazz: String): String {
    return clazz + UUID.randomUUID().toString()
}

@Component
@Profile("public", "public-graph")
class ZoneFactory {

    @Value("\${ACS_URL}")
    lateinit var acsBaseURL: String

    @Value("\${CF_BASE_DOMAIN:localhost}")
    lateinit var serviceDomain: String

    /**
     * Creates desired Zone. Makes a call to the zone API and creates a zone in order to execute your set of test
     * against it.
     *
     * @param restTemplate
     * @param zoneId
     * @param trustedIssuerIds
     * @return Zone
     * @throws IOException
     */
    @Throws(IOException::class)
    fun createTestZone(
        restTemplate: RestTemplate,
        zoneId: String,
        trustedIssuerIds: List<String>
    ): Zone {
        val zone = Zone(zoneId, zoneId, "Zone for integration testing.")
        restTemplate.put(this.acsBaseURL + ACS_ZONE_API_PATH + zoneId, zone)
        return zone
    }

    /**
     * Deletes desired Zone. Makes a client call that deletes the desired zone. This method should be use after the
     * set of tests for that zone are finished.
     *
     * @param restTemplate
     * @param zoneName
     * @return HttpStatus
     */
    fun deleteZone(
        restTemplate: RestTemplate,
        zoneName: String
    ): HttpStatus {
        return try {
            restTemplate.delete(this.acsBaseURL + ACS_ZONE_API_PATH + zoneName)
            HttpStatus.NO_CONTENT
        } catch (httpException: HttpClientErrorException) {
            httpException.statusCode
        } catch (e: RestClientException) {
            HttpStatus.INTERNAL_SERVER_ERROR
        }
    }

    fun getZoneSpecificUrl(zoneId: String): String {
        var uri: URI? = null
        var zoneurl: String
        try {
            uri = URI(this.acsBaseURL)
        } catch (e: URISyntaxException) {
            e.printStackTrace()
        }

        zoneurl = uri!!.scheme + "://" + zoneId + '.'.toString() + serviceDomain
        if (uri.port != -1) {
            zoneurl += ':'.toInt() + uri.port
        }
        return zoneurl
    }
}
