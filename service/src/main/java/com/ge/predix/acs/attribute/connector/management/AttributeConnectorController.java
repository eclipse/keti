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
import org.springframework.web.bind.annotation.RestController;

import com.ge.predix.acs.commons.web.AcsApiUriTemplates;
import com.ge.predix.acs.commons.web.BaseRestApi;
import com.ge.predix.acs.commons.web.RestApiException;
import com.ge.predix.acs.rest.AttributeConnector;

@RestController
public class AttributeConnectorController extends BaseRestApi {

    @Autowired
    private AttributeConnectorServiceImpl service;

    @RequestMapping(
            method = PUT,
            value = V1 + AcsApiUriTemplates.RESOURCE_CONNECTOR_URL,
            consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AttributeConnector> putResourceConnector(@RequestBody final AttributeConnector connector) {
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

    @RequestMapping(
            method = GET,
            value = V1 + AcsApiUriTemplates.RESOURCE_CONNECTOR_URL,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AttributeConnector> getResourceConnector() {
        try {
            AttributeConnector connector = this.service.retrieveResourceConnector();
            if (connector != null) {
                return ok(connector);
            }
            return notFound();
        } catch (AttributeConnectorException e) {
            throw new RestApiException(HttpStatus.UNPROCESSABLE_ENTITY, e);
        }
    }

    @RequestMapping(method = DELETE, value = V1 + AcsApiUriTemplates.RESOURCE_CONNECTOR_URL)
    public ResponseEntity<?> deleteResourceConnector() {
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

    @RequestMapping(
            method = PUT,
            value = V1 + AcsApiUriTemplates.SUBJECT_CONNECTOR_URL,
            consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AttributeConnector> putSubjectConnector(@RequestBody final AttributeConnector connector) {
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

    @RequestMapping(
            method = GET,
            value = V1 + AcsApiUriTemplates.SUBJECT_CONNECTOR_URL,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AttributeConnector> getSubjectConnector() {
        try {
            AttributeConnector connector = this.service.retrieveSubjectConnector();
            if (connector != null) {
                return ok(connector);
            }
            return notFound();
        } catch (AttributeConnectorException e) {
            throw new RestApiException(HttpStatus.UNPROCESSABLE_ENTITY, e);
        }
    }

    @RequestMapping(method = DELETE, value = V1 + AcsApiUriTemplates.SUBJECT_CONNECTOR_URL)
    public ResponseEntity<?> deleteSubjectConnector() {
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
}
