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

package com.ge.predix.acs.zone.management;

import static com.ge.predix.acs.commons.web.AcsApiUriTemplates.V1;
import static com.ge.predix.acs.commons.web.ResponseEntityBuilder.created;
import static com.ge.predix.acs.commons.web.ResponseEntityBuilder.noContent;
import static com.ge.predix.acs.commons.web.ResponseEntityBuilder.notFound;
import static com.ge.predix.acs.commons.web.ResponseEntityBuilder.ok;
import static org.springframework.web.bind.annotation.RequestMethod.DELETE;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.PUT;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ge.predix.acs.commons.web.ACSWebConstants;
import com.ge.predix.acs.commons.web.AcsApiUriTemplates;
import com.ge.predix.acs.commons.web.BaseRestApi;
import com.ge.predix.acs.commons.web.RestApiException;
import com.ge.predix.acs.rest.Zone;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

/**
 *
 * @author 212319607
 */
@RestController
@Api(value = ACSWebConstants.APP_ROOT_PATH, hidden = true)
public class ZoneController extends BaseRestApi {

    @Autowired
    private ZoneService service;

    @RequestMapping(method = PUT, value = V1 + AcsApiUriTemplates.ZONE_URL, consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Creates/Updates the zone.", hidden = true)
    public ResponseEntity<Zone> putZone(@RequestBody final Zone zone, @PathVariable("zoneName") final String zoneName) {

        validateAndSanitizeInputOrFail(zone, zoneName);
        try {

            boolean zoneCreated = this.service.upsertZone(zone);

            if (zoneCreated) {
                return created(false, V1 + AcsApiUriTemplates.ZONE_URL, "zoneName:" + zoneName);
            }

            return created();

        } catch (ZoneManagementException e) {
            throw new RestApiException(HttpStatus.UNPROCESSABLE_ENTITY, e);
        } catch (Exception e) {
            String message = String.format(
                    "Unexpected Exception while upserting Zone with name %s and subdomain %s", zone.getName(),
                    zone.getSubdomain());
            throw new RestApiException(HttpStatus.INTERNAL_SERVER_ERROR, message, e);
        }
    }

    @RequestMapping(method = GET, value = V1 + AcsApiUriTemplates.ZONE_URL, produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(
            value = "An ACS zone defines a data partition which encapsulates policy, resource and privelege data"
                    + "for separation between ACS tenants.  Retrieves the zone by a zone name.",
            hidden = true)
    public ResponseEntity<Zone> getZone(@PathVariable("zoneName") final String zoneName) {

        try {

            Zone zone = this.service.retrieveZone(zoneName);
            return ok(zone);

        } catch (ZoneManagementException e) {
            throw new RestApiException(HttpStatus.NOT_FOUND, e);
        } catch (Exception e) {
            String message = String.format("Unexpected Exception while retriving Zone with name %s", zoneName);
            throw new RestApiException(HttpStatus.INTERNAL_SERVER_ERROR, message, e);
        }
    }

    @RequestMapping(method = DELETE, value = V1 + AcsApiUriTemplates.ZONE_URL)
    @ApiOperation(value = "Deletes the zone.", hidden = true)
    public ResponseEntity<Void> deleteZone(@PathVariable("zoneName") final String zoneName) {

        try {

            Boolean deleted = this.service.deleteZone(zoneName);
            if (deleted) {
                return noContent();
            }
            return notFound();

        } catch (Exception e) {
            String message = String.format("Unexpected Exception while deleting Zone with name %s", zoneName);
            throw new RestApiException(HttpStatus.INTERNAL_SERVER_ERROR, message, e);
        }
    }

    /**
     * @param zone
     */
    private void validateAndSanitizeInputOrFail(final Zone zone, final String zoneName) {
        if (zone.getName() != null && !zoneName.equals(zone.getName())) {
            throw new RestApiException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Zone name in URI does not match the Zone Name in Payload");
        }
        if (zone.getName() == null) {
            zone.setName(zoneName);
        }
    }

}
