/*******************************************************************************
 * Copyright 2017 General Electric Company
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

package org.eclipse.keti.acs.zone.management

import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import org.eclipse.keti.acs.commons.web.ACSWebConstants
import org.eclipse.keti.acs.commons.web.AcsApiUriTemplates
import org.eclipse.keti.acs.commons.web.AcsApiUriTemplates.V1
import org.eclipse.keti.acs.commons.web.BaseRestApi
import org.eclipse.keti.acs.commons.web.ResponseEntityBuilder.created
import org.eclipse.keti.acs.commons.web.ResponseEntityBuilder.noContent
import org.eclipse.keti.acs.commons.web.ResponseEntityBuilder.notFound
import org.eclipse.keti.acs.commons.web.ResponseEntityBuilder.ok
import org.eclipse.keti.acs.commons.web.RestApiException
import org.eclipse.keti.acs.rest.Zone
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod.DELETE
import org.springframework.web.bind.annotation.RequestMethod.GET
import org.springframework.web.bind.annotation.RequestMethod.PUT
import org.springframework.web.bind.annotation.RestController

/**
 *
 * @author acs-engineers@ge.com
 */
@RestController
@Api(value = ACSWebConstants.APP_ROOT_PATH, hidden = true)
class ZoneController : BaseRestApi() {

    @Autowired
    private lateinit var service: ZoneService

    @RequestMapping(
        method = [PUT],
        value = [(V1 + AcsApiUriTemplates.ZONE_URL)],
        consumes = [(MediaType.APPLICATION_JSON_VALUE)]
    )
    @ApiOperation(value = "Creates/Updates the zone.", hidden = true)
    fun putZone(@RequestBody zone: Zone, @PathVariable("zoneName") zoneName: String): ResponseEntity<Zone> {

        validateAndSanitizeInputOrFail(zone, zoneName)
        try {

            val zoneCreated = this.service.upsertZone(zone)

            return if (zoneCreated) {
                created(false, V1 + AcsApiUriTemplates.ZONE_URL, "zoneName:$zoneName")
            } else created()
        } catch (e: ZoneManagementException) {
            throw RestApiException(HttpStatus.UNPROCESSABLE_ENTITY, e)
        } catch (e: Exception) {
            val message = String.format(
                "Unexpected Exception while upserting Zone with name %s and subdomain %s", zone.name,
                zone.subdomain
            )
            throw RestApiException(HttpStatus.INTERNAL_SERVER_ERROR, message, e)
        }
    }

    @RequestMapping(
        method = [GET],
        value = [(V1 + AcsApiUriTemplates.ZONE_URL)],
        produces = [(MediaType.APPLICATION_JSON_VALUE)]
    )
    @ApiOperation(
        value = "An ACS zone defines a data partition which encapsulates policy, resource and privelege data" + "for separation between ACS tenants.  Retrieves the zone by a zone name.",
        hidden = true
    )
    fun getZone(@PathVariable("zoneName") zoneName: String): ResponseEntity<Zone> {

        try {

            val zone = this.service.retrieveZone(zoneName)
            return ok(zone)
        } catch (e: ZoneManagementException) {
            throw RestApiException(HttpStatus.NOT_FOUND, e)
        } catch (e: Exception) {
            val message = String.format("Unexpected Exception while retriving Zone with name %s", zoneName)
            throw RestApiException(HttpStatus.INTERNAL_SERVER_ERROR, message, e)
        }
    }

    @RequestMapping(method = [DELETE], value = [(V1 + AcsApiUriTemplates.ZONE_URL)])
    @ApiOperation(value = "Deletes the zone.", hidden = true)
    fun deleteZone(@PathVariable("zoneName") zoneName: String): ResponseEntity<Void> {

        try {

            val deleted = this.service.deleteZone(zoneName)
            return if (deleted!!) {
                noContent()
            } else notFound()
        } catch (e: Exception) {
            val message = String.format("Unexpected Exception while deleting Zone with name %s", zoneName)
            throw RestApiException(HttpStatus.INTERNAL_SERVER_ERROR, message, e)
        }
    }

    /**
     * @param zone
     */
    private fun validateAndSanitizeInputOrFail(zone: Zone, zoneName: String) {
        if (zone.name != null && zoneName != zone.name) {
            throw RestApiException(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "Zone name in URI does not match the Zone Name in Payload"
            )
        }
        if (zone.name == null) {
            zone.name = zoneName
        }
    }
}
