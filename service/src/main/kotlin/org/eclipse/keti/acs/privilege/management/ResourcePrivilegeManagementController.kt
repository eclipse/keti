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

package org.eclipse.keti.acs.privilege.management

import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiResponse
import io.swagger.annotations.ApiResponses
import org.apache.commons.collections4.CollectionUtils
import org.apache.commons.lang.StringUtils
import org.eclipse.keti.acs.commons.web.BaseRestApi
import org.eclipse.keti.acs.commons.web.MANAGED_RESOURCES_URL
import org.eclipse.keti.acs.commons.web.MANAGED_RESOURCE_URL
import org.eclipse.keti.acs.commons.web.PARENTS_ATTR_NOT_SUPPORTED_MSG
import org.eclipse.keti.acs.commons.web.RestApiException
import org.eclipse.keti.acs.commons.web.V1
import org.eclipse.keti.acs.commons.web.created
import org.eclipse.keti.acs.commons.web.expand
import org.eclipse.keti.acs.commons.web.noContent
import org.eclipse.keti.acs.commons.web.notFound
import org.eclipse.keti.acs.commons.web.ok
import org.eclipse.keti.acs.rest.BaseResource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod.DELETE
import org.springframework.web.bind.annotation.RequestMethod.GET
import org.springframework.web.bind.annotation.RequestMethod.POST
import org.springframework.web.bind.annotation.RequestMethod.PUT
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * @author acs-engineers@ge.com
 */
@RestController
class ResourcePrivilegeManagementController : BaseRestApi() {

    @Autowired
    private lateinit var service: PrivilegeManagementService

    private var graphProfileActive: Boolean? = null

    @ApiOperation(
        value = "Retrieves the list of all resources for the given zone.",
        tags = ["Attribute Management"]
    )
    @RequestMapping(
        method = [GET],
        value = [(V1 + MANAGED_RESOURCES_URL)],
        produces = [(MediaType.APPLICATION_JSON_VALUE)]
    )
    fun getResources(): ResponseEntity<List<BaseResource>> {

        val resources = this.service.resources
        return ok(resources)
    }

    private fun getGraphProfileActive(): Boolean? {
        if (this.graphProfileActive == null) {
            this.graphProfileActive = listOf(*this.environment.activeProfiles).contains("graph")
        }

        return this.graphProfileActive
    }

    private fun failIfParentsSpecified(resources: List<BaseResource>) {
        if (this.getGraphProfileActive()!!) {
            return
        }

        for (resource in resources) {
            if (!CollectionUtils.isEmpty(resource.parents)) {
                throw RestApiException(HttpStatus.NOT_IMPLEMENTED, PARENTS_ATTR_NOT_SUPPORTED_MSG)
            }
        }
    }

    @ApiOperation(
        value = "Creates a list of resources for the given zone. " + "Existing resources will be updated with the provided values.",
        tags = ["Attribute Management"]
    )
    @ApiResponses(value = [(ApiResponse(code = 204, message = "Resource objects appended successfully."))])
    @RequestMapping(
        method = [POST],
        value = [(V1 + MANAGED_RESOURCES_URL)],
        consumes = [(MediaType.APPLICATION_JSON_VALUE)]
    )
    fun appendResources(@RequestBody resources: List<BaseResource>): ResponseEntity<Void> {
        try {
            this.failIfParentsSpecified(resources)

            this.service.appendResources(resources)

            return noContent()
        } catch (e: RestApiException) {
            // NOTE: This block is necessary to avoid accidentally
            // converting the HTTP status code to an unintended one
            throw e
        } catch (e: Exception) {
            throw RestApiException(HttpStatus.UNPROCESSABLE_ENTITY, e)
        }
    }

    @ApiOperation(
        value = "Retrieves the resource for the given zone. The resourceIdentifier must be URL encoded in " + "application/x-www-form-urlencoded format with UTF-8.",
        tags = ["Attribute Management"]
    )
    @RequestMapping(method = [GET], value = [(V1 + MANAGED_RESOURCE_URL)])
    fun getResourceV1(
        @PathVariable("resourceIdentifier") resourceIdentifier: String,
        @RequestParam(name = "includeInheritedAttributes", defaultValue = "false")
        includeInheritedAttributes: Boolean
    ): ResponseEntity<BaseResource> {
        val resource: BaseResource? = if (includeInheritedAttributes) {
            this.service.getByResourceIdentifierWithInheritedAttributes(resourceIdentifier)
        } else {
            this.service.getByResourceIdentifier(resourceIdentifier)
        }

        return if (resource == null) {
            notFound()
        } else ok(resource)
    }

    @ApiOperation(
        value = "Creates/Updates a given resource for a given zone. " +
                "The resourceIdentifier must be URL encoded in application/x-www-form-urlencoded format with " + "UTF-8.",
        tags = ["Attribute Management"]
    )
    @RequestMapping(
        method = [PUT],
        value = [(V1 + MANAGED_RESOURCE_URL)],
        consumes = [(MediaType.APPLICATION_JSON_VALUE)]
    )
    fun putResourceV1(
        @RequestBody resource: BaseResource,
        @PathVariable("resourceIdentifier") resourceIdentifier: String
    ): ResponseEntity<BaseResource> {
        try {
            this.failIfParentsSpecified(listOf(resource))

            // resource identifier is optional, setting it to URI resource id if
            // missing from payload
            if (StringUtils.isEmpty(resource.resourceIdentifier)) {
                resource.resourceIdentifier = resourceIdentifier
            }

            validResourceIdentifierOrFail(resource, resourceIdentifier)

            return doPutResource(resource, resourceIdentifier)
        } catch (e: RestApiException) {
            // NOTE: This block is necessary to avoid accidentally
            // converting the HTTP status code to an unintended one
            throw e
        } catch (e: Exception) {
            throw RestApiException(HttpStatus.UNPROCESSABLE_ENTITY, e)
        }
    }

    private fun doPutResource(
        resource: BaseResource,
        resourceIdentifier: String
    ): ResponseEntity<BaseResource> {
        try {
            val createdResource = this.service.upsertResource(resource)

            val resourceUri = expand(MANAGED_RESOURCE_URL, "resourceIdentifier:$resourceIdentifier")

            return if (createdResource) {
                created(resourceUri.path, false)
            } else created(resourceUri.path, true)

            // CHECK if path returns the right info
        } catch (e: Exception) {
            throw RestApiException(HttpStatus.UNPROCESSABLE_ENTITY, e)
        }
    }

    @ApiOperation(
        value = "Deletes the resource for a given zone. The resourceIdentifier must be URL encoded in " + "application/x-www-form-urlencoded format with UTF-8.",
        tags = ["Attribute Management"]
    )
    @RequestMapping(method = [DELETE], value = [(V1 + MANAGED_RESOURCE_URL)])
    fun deleteResourceV1(@PathVariable("resourceIdentifier") resourceIdentifier: String): ResponseEntity<Void> {
        this.service.deleteResource(resourceIdentifier)

        return noContent()
    }

    private fun validResourceIdentifierOrFail(
        resource: BaseResource,
        resourceIdentifier: String
    ) {
        if (resourceIdentifier != resource.resourceIdentifier) {
            throw RestApiException(
                HttpStatus.UNPROCESSABLE_ENTITY,
                String.format(
                    "Resource identifier = %s, does not match the one provided in URI = %s",
                    resource.resourceIdentifier, resourceIdentifier
                )
            )
        }
    }
}
