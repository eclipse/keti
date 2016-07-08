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

package com.ge.predix.acs.privilege.management;

import static com.ge.predix.acs.commons.web.AcsApiUriTemplates.MANAGED_RESOURCES_URL;
import static com.ge.predix.acs.commons.web.AcsApiUriTemplates.MANAGED_RESOURCE_URL;
import static com.ge.predix.acs.commons.web.AcsApiUriTemplates.V1;
import static com.ge.predix.acs.commons.web.ResponseEntityBuilder.created;
import static com.ge.predix.acs.commons.web.ResponseEntityBuilder.noContent;
import static com.ge.predix.acs.commons.web.ResponseEntityBuilder.notFound;
import static com.ge.predix.acs.commons.web.ResponseEntityBuilder.ok;
import static org.springframework.web.bind.annotation.RequestMethod.DELETE;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;
import static org.springframework.web.bind.annotation.RequestMethod.PUT;

import java.net.URI;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ge.predix.acs.commons.web.BaseRestApi;
import com.ge.predix.acs.commons.web.RestApiException;
import com.ge.predix.acs.commons.web.UriTemplateUtils;
import com.ge.predix.acs.rest.BaseResource;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

/**
 *
 * @author 212319607
 */
@RestController
public class ResourcePrivilegeManagementController extends BaseRestApi {
    @Autowired
    private PrivilegeManagementService service;

    @ApiOperation(
            value = "Creates a list of resources for the given zone. "
                    + "Existing resources will be updated with the provided values.",
            tags = { "Attribute Management" })
    @ApiResponses(value = { @ApiResponse(code = 204, message = "Resource objects appended successfully."), })
    @RequestMapping(method = POST, value = { V1 + MANAGED_RESOURCES_URL }, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> appendResources(@RequestBody final List<BaseResource> resources) {
        try {
            this.service.appendResources(resources);

            return noContent();
        } catch (Exception e) {
            throw new RestApiException(HttpStatus.UNPROCESSABLE_ENTITY, e);
        }
    }

    @ApiOperation(value = "Retrieves the list of all resources for the given zone.", tags = { "Attribute Management" })
    @RequestMapping(method = GET, value = { V1 + MANAGED_RESOURCES_URL }, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<BaseResource>> getResources() {

        List<BaseResource> resources = this.service.getResources();
        return ok(resources);
    }

    @ApiOperation(
            value = "Retrieves the resource for the given zone. The resourceIdentifier must be URL encoded in "
                    + "application/x-www-form-urlencoded format with UTF-8.",
            tags = { "Attribute Management" })
    @RequestMapping(method = GET, value = V1 + MANAGED_RESOURCE_URL, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<BaseResource> getResourceV1(
            @PathVariable("resourceIdentifier") final String resourceIdentifier,
            @RequestParam(
                    name = "includeInheritedAttributes",
                    defaultValue = "false") final boolean includeInheritedAttributes) {
        BaseResource resource;
        if (includeInheritedAttributes) {
            resource = this.service.getByResourceIdentifierWithInheritedAttributes(resourceIdentifier);
        } else {
            resource = this.service.getByResourceIdentifier(resourceIdentifier);
        }

        if (resource == null) {
            return notFound();
        }
        return ok(resource);
    }

    @ApiOperation(
            value = "Creates/Updates a given resource for a given zone. "
                    + "The resourceIdentifier must be URL encoded in application/x-www-form-urlencoded format with "
                    + "UTF-8.",
            tags = { "Attribute Management" })
    @RequestMapping(method = PUT, value = { V1 + MANAGED_RESOURCE_URL }, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<BaseResource> putResourceV1(@RequestBody final BaseResource resource,
            @PathVariable("resourceIdentifier") final String resourceIdentifier) {
        try {
            // resource identifier is optional, setting it to URI resource id if
            // missing from payload
            if (StringUtils.isEmpty(resource.getResourceIdentifier())) {
                resource.setResourceIdentifier(resourceIdentifier);
            }

            validResourceIdentifierOrFail(resource, resourceIdentifier);

            return doPutResource(resource, resourceIdentifier);
        } catch (RestApiException e) {
            throw e;
        } catch (Exception e) {
            throw new RestApiException(HttpStatus.UNPROCESSABLE_ENTITY, e);
        }

    }

    private ResponseEntity<BaseResource> doPutResource(final BaseResource resource, final String resourceIdentifier) {
        try {
            boolean createdResource = this.service.upsertResource(resource);

            URI resourceUri = UriTemplateUtils.expand(MANAGED_RESOURCE_URL, "resourceIdentifier:" + resourceIdentifier);

            if (createdResource) {
                return created(resourceUri.getPath(), false);
            }

            // CHECK if path returns the right info
            return created(resourceUri.getPath(), true);
        } catch (Exception e) {
            throw new RestApiException(HttpStatus.UNPROCESSABLE_ENTITY, e);
        }
    }

    @ApiOperation(
            value = "Deletes the resource for a given zone. The resourceIdentifier must be URL encoded in "
                    + "application/x-www-form-urlencoded format with UTF-8.",
            tags = { "Attribute Management" })
    @RequestMapping(method = DELETE, value = V1 + MANAGED_RESOURCE_URL)
    public ResponseEntity<Void> deleteResourceV1(@PathVariable("resourceIdentifier") final String resourceIdentifier) {
        this.service.deleteResource(resourceIdentifier);

        return noContent();
    }

    private void validResourceIdentifierOrFail(final BaseResource resource, final String resourceIdentifier) {
        if (!resourceIdentifier.equals(resource.getResourceIdentifier())) {
            throw new RestApiException(HttpStatus.UNPROCESSABLE_ENTITY,
                    String.format("Resource identifier = %s, does not match the one provided in URI = %s",
                            resource.getResourceIdentifier(), resourceIdentifier));
        }
    }

}
