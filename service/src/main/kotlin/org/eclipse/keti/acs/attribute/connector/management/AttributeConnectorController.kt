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

package org.eclipse.keti.acs.attribute.connector.management

import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import io.swagger.annotations.ApiResponse
import io.swagger.annotations.ApiResponses
import org.eclipse.keti.acs.commons.web.BaseRestApi
import org.eclipse.keti.acs.commons.web.RESOURCE_CONNECTOR_URL
import org.eclipse.keti.acs.commons.web.RestApiException
import org.eclipse.keti.acs.commons.web.SUBJECT_CONNECTOR_URL
import org.eclipse.keti.acs.commons.web.V1
import org.eclipse.keti.acs.commons.web.created
import org.eclipse.keti.acs.commons.web.noContent
import org.eclipse.keti.acs.commons.web.notFound
import org.eclipse.keti.acs.commons.web.ok
import org.eclipse.keti.acs.rest.AttributeConnector
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod.DELETE
import org.springframework.web.bind.annotation.RequestMethod.GET
import org.springframework.web.bind.annotation.RequestMethod.PUT
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
class AttributeConnectorController : BaseRestApi() {

    @Autowired
    private lateinit var service: AttributeConnectorServiceImpl

    @ApiOperation(
        value = "Retrieves connector configuration for external resource attributes for the given zone.",
        tags = ["Attribute Connector Management"],
        response = AttributeConnector::class
    )
    @ApiResponses(
        value = [(ApiResponse(
            code = 404,
            message = "Connector configuration for the given zone is not found."
        ))]
    )
    @RequestMapping(method = [GET], value = [(V1 + RESOURCE_CONNECTOR_URL)])
    fun getResourceConnector(): ResponseEntity<AttributeConnector> {
        try {
            val connector = this.service.retrieveResourceConnector()
            return if (connector != null) {
                ok(obfuscateAdapterSecret(connector))
            } else notFound()
        } catch (e: AttributeConnectorException) {
            throw RestApiException(HttpStatus.UNPROCESSABLE_ENTITY, e)
        }
    }

    @ApiOperation(
        value = "Retrieves connector configuration for external subject attributes for the given zone.",
        tags = ["Attribute Connector Management"],
        response = AttributeConnector::class
    )
    @ApiResponses(
        value = [(ApiResponse(
            code = 404,
            message = "Connector configuration for the given zone is not found."
        ))]
    )
    @RequestMapping(method = [GET], value = [(V1 + SUBJECT_CONNECTOR_URL)])
    fun getSubjectConnector(): ResponseEntity<AttributeConnector> {
        try {
            val connector = this.service.retrieveSubjectConnector()
            return if (connector != null) {
                ok(obfuscateAdapterSecret(connector))
            } else notFound()
        } catch (e: AttributeConnectorException) {
            throw RestApiException(HttpStatus.UNPROCESSABLE_ENTITY, e)
        }
    }

    @ApiOperation(
        value = "Creates or updates connector configuration for external resource attributes for the given " + "zone.",
        tags = ["Attribute Connector Management"]
    )
    @ApiResponses(
        value = [(ApiResponse(
            code = 201,
            message = "Connector configuration for the given zone is successfully created."
        ))]
    )
    @RequestMapping(
        method = [PUT],
        value = [(V1 + RESOURCE_CONNECTOR_URL)],
        consumes = [(MediaType.APPLICATION_JSON_VALUE)]
    )
    fun putResourceConnector(
        @ApiParam(value = "New or updated connector configuration for external resource attributes", required = true)
        @RequestBody
        connector: AttributeConnector
    ): ResponseEntity<String> {
        try {
            val connectorCreated = this.service.upsertResourceConnector(connector)

            return if (connectorCreated) {
                // return 201 with empty response body
                created(V1 + RESOURCE_CONNECTOR_URL, false)
            } else ok()
            // return 200 with empty response body
        } catch (e: AttributeConnectorException) {
            throw RestApiException(HttpStatus.UNPROCESSABLE_ENTITY, e.message!!, e)
        }
    }

    @ApiOperation(
        value = "Deletes connector configuration for external resource attributes for the given zone.",
        tags = ["Attribute Connector Management"]
    )
    @ResponseStatus(value = HttpStatus.NO_CONTENT)
    @ApiResponses(
        value = [(ApiResponse(
            code = 204,
            message = "Connector configuration for the given zone is successfully deleted."
        )), (ApiResponse(code = 404, message = "Connector configuration for the given zone is not found."))]
    )
    @RequestMapping(method = [DELETE], value = [(V1 + RESOURCE_CONNECTOR_URL)])
    fun deleteResourceConnector(): ResponseEntity<Void> {
        try {
            val deleted = this.service.deleteResourceConnector()
            return if (deleted) {
                noContent()
            } else notFound()
        } catch (e: AttributeConnectorException) {
            throw RestApiException(HttpStatus.UNPROCESSABLE_ENTITY, e.message!!, e)
        }
    }

    @ApiOperation(
        value = "Creates or updates connector configuration for external subject attributes for the given " + "zone.",
        tags = ["Attribute Connector Management"]
    )
    @ApiResponses(
        value = [(ApiResponse(
            code = 201,
            message = "Connector configuration for the given zone is successfully created."
        ))]
    )
    @RequestMapping(
        method = [PUT],
        value = [(V1 + SUBJECT_CONNECTOR_URL)],
        consumes = [(MediaType.APPLICATION_JSON_VALUE)]
    )
    fun putSubjectConnector(
        @ApiParam(value = "New or updated connector configuration for external subject attributes", required = true)
        @RequestBody
        connector: AttributeConnector
    ): ResponseEntity<String> {
        try {
            val connectorCreated = this.service.upsertSubjectConnector(connector)

            return if (connectorCreated) {
                // return 201 with empty response body
                created(V1 + SUBJECT_CONNECTOR_URL, false)
            } else ok()
            // return 200 with empty response body
        } catch (e: AttributeConnectorException) {
            throw RestApiException(HttpStatus.UNPROCESSABLE_ENTITY, e.message!!, e)
        }
    }

    @ApiOperation(
        value = "Deletes connector configuration for external subject attributes for the given zone.",
        tags = ["Attribute Connector Management"]
    )
    @ResponseStatus(value = HttpStatus.NO_CONTENT)
    @ApiResponses(
        value = [(ApiResponse(
            code = 204,
            message = "Connector configuration for the given zone is successfully deleted."
        )), (ApiResponse(code = 404, message = "Connector configuration for the given zone is not found."))]
    )
    @RequestMapping(method = [DELETE], value = [(V1 + SUBJECT_CONNECTOR_URL)])
    fun deleteSubjectConnector(): ResponseEntity<Void> {
        try {
            val deleted = this.service.deleteSubjectConnector()
            return if (deleted) {
                noContent()
            } else notFound()
        } catch (e: AttributeConnectorException) {
            throw RestApiException(HttpStatus.UNPROCESSABLE_ENTITY, e.message!!, e)
        }
    }

    private fun obfuscateAdapterSecret(connector: AttributeConnector): AttributeConnector {
        connector.adapters!!.forEach { adapter -> adapter.uaaClientSecret = "**********" }
        return connector
    }
}
