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

package com.ge.predix.acs.attribute.connector.management;

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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.ge.predix.acs.commons.web.AcsApiUriTemplates;
import com.ge.predix.acs.commons.web.BaseRestApi;
import com.ge.predix.acs.commons.web.RestApiException;
import com.ge.predix.acs.rest.AttributeConnector;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

@RestController
public class AttributeConnectorController extends BaseRestApi {

    @Autowired
    private AttributeConnectorServiceImpl service;

    @ApiOperation(value = "Creates or updates connector configuration for external resource attributes for the given "
            + "zone.", tags = { "Attribute Connector Management" })
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "Connector configuration for the given zone is successfully created.") })
    @RequestMapping(method = PUT, value = V1 + AcsApiUriTemplates.RESOURCE_CONNECTOR_URL,
            consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> putResourceConnector(
            @ApiParam(value = "New or updated connector configuration for external resource attributes",
                    required = true) @RequestBody final AttributeConnector connector) {
        try {
            boolean connectorCreated = this.service.upsertResourceConnector(connector);

            if (connectorCreated) {
                // return 201 with empty response body
                return created(V1 + AcsApiUriTemplates.RESOURCE_CONNECTOR_URL, false);
            }
            // return 200 with empty response body
            return ok();
        } catch (AttributeConnectorException e) {
            throw new RestApiException(HttpStatus.UNPROCESSABLE_ENTITY, e.getMessage(), e);
        }
    }

    @ApiOperation(value = "Retrieves connector configuration for external resource attributes for the given zone.",
            tags = { "Attribute Connector Management" }, response = AttributeConnector.class)
    @ApiResponses(
            value = { @ApiResponse(code = 404, message = "Connector configuration for the given zone is not found.") })
    @RequestMapping(method = GET, value = V1 + AcsApiUriTemplates.RESOURCE_CONNECTOR_URL)
    public ResponseEntity<AttributeConnector> getResourceConnector() {
        try {
            AttributeConnector connector = this.service.retrieveResourceConnector();
            if (connector != null) {
                return ok(obfuscateAdapterSecret(connector));
            }
            return notFound();
        } catch (AttributeConnectorException e) {
            throw new RestApiException(HttpStatus.UNPROCESSABLE_ENTITY, e);
        }
    }

    @ApiOperation(value = "Deletes connector configuration for external resource attributes for the given zone.",
            tags = { "Attribute Connector Management" })
    @ResponseStatus(value = HttpStatus.NO_CONTENT)
    @ApiResponses(value = {
            @ApiResponse(code = 204, message = "Connector configuration for the given zone is successfully deleted."),
            @ApiResponse(code = 404, message = "Connector configuration for the given zone is not found.") })
    @RequestMapping(method = DELETE, value = V1 + AcsApiUriTemplates.RESOURCE_CONNECTOR_URL)
    public ResponseEntity<Void> deleteResourceConnector() {
        try {
            Boolean deleted = this.service.deleteResourceConnector();
            if (deleted) {
                return noContent();
            }
            return notFound();
        } catch (AttributeConnectorException e) {
            throw new RestApiException(HttpStatus.UNPROCESSABLE_ENTITY, e.getMessage(), e);
        }
    }

    @ApiOperation(value = "Creates or updates connector configuration for external subject attributes for the given "
            + "zone.", tags = { "Attribute Connector Management" })
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "Connector configuration for the given zone is successfully created.") })
    @RequestMapping(method = PUT, value = V1 + AcsApiUriTemplates.SUBJECT_CONNECTOR_URL,
            consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> putSubjectConnector(
            @ApiParam(value = "New or updated connector configuration for external subject attributes",
                    required = true) @RequestBody final AttributeConnector connector) {
        try {
            boolean connectorCreated = this.service.upsertSubjectConnector(connector);

            if (connectorCreated) {
                // return 201 with empty response body
                return created(V1 + AcsApiUriTemplates.SUBJECT_CONNECTOR_URL, false);
            }
            // return 200 with empty response body
            return ok();
        } catch (AttributeConnectorException e) {
            throw new RestApiException(HttpStatus.UNPROCESSABLE_ENTITY, e.getMessage(), e);
        }
    }

    @ApiOperation(value = "Retrieves connector configuration for external subject attributes for the given zone.",
            tags = { "Attribute Connector Management" }, response = AttributeConnector.class)
    @ApiResponses(
            value = { @ApiResponse(code = 404, message = "Connector configuration for the given zone is not found.") })
    @RequestMapping(method = GET, value = V1 + AcsApiUriTemplates.SUBJECT_CONNECTOR_URL)
    public ResponseEntity<AttributeConnector> getSubjectConnector() {
        try {
            AttributeConnector connector = this.service.retrieveSubjectConnector();
            if (connector != null) {
                return ok(obfuscateAdapterSecret(connector));
            }
            return notFound();
        } catch (AttributeConnectorException e) {
            throw new RestApiException(HttpStatus.UNPROCESSABLE_ENTITY, e);
        }
    }

    @ApiOperation(value = "Deletes connector configuration for external subject attributes for the given zone.",
            tags = { "Attribute Connector Management" })
    @ResponseStatus(value = HttpStatus.NO_CONTENT)
    @ApiResponses(value = {
            @ApiResponse(code = 204, message = "Connector configuration for the given zone is successfully deleted."),
            @ApiResponse(code = 404, message = "Connector configuration for the given zone is not found.") })
    @RequestMapping(method = DELETE, value = V1 + AcsApiUriTemplates.SUBJECT_CONNECTOR_URL)
    public ResponseEntity<Void> deleteSubjectConnector() {
        try {
            Boolean deleted = this.service.deleteSubjectConnector();
            if (deleted) {
                return noContent();
            }
            return notFound();
        } catch (AttributeConnectorException e) {
            throw new RestApiException(HttpStatus.UNPROCESSABLE_ENTITY, e.getMessage(), e);
        }
    }

    private AttributeConnector obfuscateAdapterSecret(final AttributeConnector connector) {
        connector.getAdapters().forEach(adapter -> adapter.setUaaClientSecret("**********"));
        return connector;
    }
}
