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
// @formatter:off
package com.ge.predix.controller.test;

import static com.ge.predix.acs.testutils.XFiles.ASCENSION_ID;
import static com.ge.predix.acs.testutils.XFiles.BASEMENT_SITE_ID;
import static com.ge.predix.acs.testutils.XFiles.EVIDENCE_SCULLYS_TESTIMONY_ID;
import static com.ge.predix.acs.testutils.XFiles.SITE_BASEMENT;
import static com.ge.predix.acs.testutils.XFiles.SPECIAL_AGENTS_GROUP_ATTRIBUTE;
import static com.ge.predix.acs.testutils.XFiles.TOP_SECRET_CLASSIFICATION;
import static com.ge.predix.acs.testutils.XFiles.TYPE_MYTHARC;
import static com.ge.predix.acs.testutils.XFiles.createThreeLevelResourceHierarchy;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isIn;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.net.URLEncoder;
import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.web.context.WebApplicationContext;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ge.predix.acs.rest.BaseResource;
import com.ge.predix.acs.rest.Zone;
import com.ge.predix.acs.testutils.MockAcsRequestContext;
import com.ge.predix.acs.testutils.MockMvcContext;
import com.ge.predix.acs.testutils.MockSecurityContext;
import com.ge.predix.acs.testutils.TestActiveProfilesResolver;
import com.ge.predix.acs.testutils.TestUtils;
import com.ge.predix.acs.utils.JsonUtils;
import com.ge.predix.acs.zone.management.ZoneService;

@WebAppConfiguration
@ContextConfiguration("classpath:controller-tests-context.xml")
@ActiveProfiles(resolver = TestActiveProfilesResolver.class)
public class ResourcePrivilegeManagementControllerIT extends AbstractTestNGSpringContextTests {

    public static final ConfigureEnvironment CONFIGURE_ENVIRONMENT = new ConfigureEnvironment();

    private static final String RESOURCE_BASE_URL = "/v1/resource";

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Autowired
    private WebApplicationContext wac;

    @Autowired
    private ZoneService zoneService;

    private final JsonUtils jsonUtils = new JsonUtils();

    private final TestUtils testUtils = new TestUtils();

    private Zone testZone;

    private Zone testZone2;

    @Autowired
    ConfigurableEnvironment env;

    @BeforeClass
    public void setup() throws Exception {
        this.testZone = new TestUtils().createTestZone("ResourceMgmtControllerIT");
        this.testZone2 = new TestUtils().createTestZone("ResourceMgmtControllerIT2");
        this.zoneService.upsertZone(this.testZone);
        this.zoneService.upsertZone(this.testZone2);
        MockSecurityContext.mockSecurityContext(this.testZone);
        MockAcsRequestContext.mockAcsRequestContext(this.testZone);
    }

    @Test
    public void testSameResourceDifferentZones() throws Exception {
        BaseResource resource = this.jsonUtils.deserializeFromFile("controller-test/a-resource.json",
                BaseResource.class);
        String thisUri = RESOURCE_BASE_URL + "/%2Fservices%2Fsecured-api";
        // create resource in first zone
        MockMvcContext putContext = this.testUtils.createWACWithCustomPUTRequestBuilder(this.wac,
                this.testZone.getSubdomain(), thisUri);
        putContext.getMockMvc().perform(putContext.getBuilder().contentType(MediaType.APPLICATION_JSON)
                .content(OBJECT_MAPPER.writeValueAsString(resource))).andExpect(status().isCreated());

        // create resource in second zone
        MockSecurityContext.mockSecurityContext(this.testZone2);
        MockAcsRequestContext.mockAcsRequestContext(this.testZone2);
        putContext = this.testUtils.createWACWithCustomPUTRequestBuilder(this.wac, this.testZone2.getSubdomain(),
                thisUri);
        putContext.getMockMvc().perform(putContext.getBuilder().contentType(MediaType.APPLICATION_JSON)
                .content(OBJECT_MAPPER.writeValueAsString(resource))).andExpect(status().isCreated());
        // we expect both resources to be create in each zone
        // set security context back to first test zone
        MockSecurityContext.mockSecurityContext(this.testZone);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testPOSTResources() throws Exception {

        List<BaseResource> resources = this.jsonUtils.deserializeFromFile("controller-test/resources-collection.json",
                List.class);
        Assert.assertNotNull(resources);

        // Append a list of resources
        MockMvcContext postContext = this.testUtils.createWACWithCustomPOSTRequestBuilder(this.wac,
                this.testZone.getSubdomain(), RESOURCE_BASE_URL);
        postContext.getMockMvc().perform(postContext.getBuilder().contentType(MediaType.APPLICATION_JSON)
                .content(OBJECT_MAPPER.writeValueAsString(resources))).andExpect(status().isNoContent());

        // Get the list of resources
        MockMvcContext getContext = this.testUtils.createWACWithCustomGETRequestBuilder(this.wac,
                this.testZone.getSubdomain(), RESOURCE_BASE_URL);
        getContext.getMockMvc().perform(getContext.getBuilder()).andExpect(status().isOk())
                .andExpect(
                        jsonPath("$[0].resourceIdentifier",
                                isIn(new String[] { "/services/secured-api",
                                        "/services/reports/01928374102398741235123" })))
                .andExpect(
                        jsonPath("$[1].resourceIdentifier",
                                isIn(new String[] { "/services/secured-api",
                                        "/services/reports/01928374102398741235123" })))
                .andExpect(jsonPath("$[0].attributes[0].value", isIn(new String[] { "sales", "admin" })))
                .andExpect(jsonPath("$[0].attributes[0].issuer", is("https://acs.attributes.int")))
                .andExpect(jsonPath("$[1].attributes[0].value", isIn(new String[] { "sales", "admin" })))
                .andExpect(jsonPath("$[1].attributes[0].issuer", is("https://acs.attributes.int")));

        BaseResource resource = this.jsonUtils.deserializeFromFile("controller-test/a-resource.json",
                BaseResource.class);
        Assert.assertNotNull(resource);

        String aResourceURI = RESOURCE_BASE_URL + "/%2Fservices%2Fsecured-api";

        // Update a given resource
        MockMvcContext updateContext = this.testUtils.createWACWithCustomPUTRequestBuilder(this.wac,
                this.testZone.getSubdomain(), aResourceURI);
        updateContext.getMockMvc().perform(updateContext.getBuilder().contentType(MediaType.APPLICATION_JSON)
                .content(OBJECT_MAPPER.writeValueAsString(resource))).andExpect(status().isNoContent());

        // Get a given resource
        getContext = this.testUtils.createWACWithCustomGETRequestBuilder(this.wac, this.testZone.getSubdomain(),
                aResourceURI);
        getContext.getMockMvc().perform(getContext.getBuilder()).andExpect(status().isOk())
                .andExpect(jsonPath("resourceIdentifier", is("/services/secured-api")))
                .andExpect(jsonPath("attributes[0].value", isIn(new String[] { "supervisor", "it" })))
                .andExpect(jsonPath("attributes[0].issuer", is("https://acs.attributes.int")));

        MockMvcContext deleteContext = this.testUtils.createWACWithCustomDELETERequestBuilder(this.wac,
                this.testZone.getSubdomain(), aResourceURI);

        // Delete a given resource
        deleteContext.getMockMvc().perform(deleteContext.getBuilder()).andExpect(status().isNoContent());

        // Delete a given resource
        deleteContext = this.testUtils.createWACWithCustomDELETERequestBuilder(this.wac, this.testZone.getSubdomain(),
                RESOURCE_BASE_URL + "/01928374102398741235123");
        deleteContext.getMockMvc().perform(deleteContext.getBuilder()).andExpect(status().isNoContent());

        // Make sure resource does not exist anymore
        getContext = this.testUtils.createWACWithCustomGETRequestBuilder(this.wac, this.testZone.getSubdomain(),
                "/tenant/ge/resource/0192837410239874");
        getContext.getMockMvc().perform(getContext.getBuilder()).andExpect(status().isNotFound());

        // Make sure resource does not exist anymore
        getContext = this.testUtils.createWACWithCustomGETRequestBuilder(this.wac, this.testZone.getSubdomain(),
                "/tenant/ge/resource/01928374102398741235123");
        getContext.getMockMvc().perform(getContext.getBuilder()).andExpect(status().isNotFound());
    }

    @Test
    public void testPostAndGetHierarchicalResources() throws Exception {

        List<BaseResource> resources = createThreeLevelResourceHierarchy();

        // Append a list of resources
        MockMvcContext postContext = this.testUtils.createWACWithCustomPOSTRequestBuilder(this.wac,
                this.testZone.getSubdomain(), RESOURCE_BASE_URL);
        postContext.getMockMvc().perform(postContext.getBuilder().contentType(MediaType.APPLICATION_JSON)
                .content(OBJECT_MAPPER.writeValueAsString(resources))).andExpect(status().isNoContent());

        // Get the child resource without query string specifier.
        MockMvcContext getContext0 = this.testUtils.createWACWithCustomGETRequestBuilder(this.wac,
                this.testZone.getSubdomain(), RESOURCE_BASE_URL + "/"
                        + URLEncoder.encode(EVIDENCE_SCULLYS_TESTIMONY_ID, "UTF-8"));

        getContext0.getMockMvc().perform(getContext0.getBuilder()).andExpect(status().isOk())
            .andExpect(jsonPath("resourceIdentifier", is(EVIDENCE_SCULLYS_TESTIMONY_ID)))
            .andExpect(jsonPath("attributes[0].name", is(TOP_SECRET_CLASSIFICATION.getName())))
            .andExpect(jsonPath("attributes[0].value", is(TOP_SECRET_CLASSIFICATION.getValue())))
            .andExpect(jsonPath("attributes[0].issuer", is(TOP_SECRET_CLASSIFICATION.getIssuer())))
            .andExpect(jsonPath("attributes[1]").doesNotExist());

        // Get the child resource without inherited attributes.
        MockMvcContext getContext1 = this.testUtils.createWACWithCustomGETRequestBuilder(this.wac,
                this.testZone.getSubdomain(), RESOURCE_BASE_URL + "/"
                        + URLEncoder.encode(EVIDENCE_SCULLYS_TESTIMONY_ID, "UTF-8")
                        + "?includeInheritedAttributes=false");

        getContext1.getMockMvc().perform(getContext1.getBuilder()).andExpect(status().isOk())
            .andExpect(jsonPath("resourceIdentifier", is(EVIDENCE_SCULLYS_TESTIMONY_ID)))
            .andExpect(jsonPath("attributes[0].name", is(TOP_SECRET_CLASSIFICATION.getName())))
            .andExpect(jsonPath("attributes[0].value", is(TOP_SECRET_CLASSIFICATION.getValue())))
            .andExpect(jsonPath("attributes[0].issuer", is(TOP_SECRET_CLASSIFICATION.getIssuer())))
            .andExpect(jsonPath("attributes[1]").doesNotExist());

        // Get the child resource with inherited attributes.
        MockMvcContext getContext2 = this.testUtils.createWACWithCustomGETRequestBuilder(this.wac,
                this.testZone.getSubdomain(), RESOURCE_BASE_URL + "/"
                        + URLEncoder.encode(EVIDENCE_SCULLYS_TESTIMONY_ID, "UTF-8")
                        + "?includeInheritedAttributes=true");

        if (!Arrays.asList(this.env.getActiveProfiles()).contains("titan")) {
            getContext2.getMockMvc().perform(getContext2.getBuilder()).andExpect(status().isOk())
                .andExpect(jsonPath("resourceIdentifier", is(EVIDENCE_SCULLYS_TESTIMONY_ID)))
                .andExpect(jsonPath("attributes[0].name", is(TOP_SECRET_CLASSIFICATION.getName())))
                .andExpect(jsonPath("attributes[0].value", is(TOP_SECRET_CLASSIFICATION.getValue())))
                .andExpect(jsonPath("attributes[0].issuer", is(TOP_SECRET_CLASSIFICATION.getIssuer())))
                .andExpect(jsonPath("attributes[1]").doesNotExist());
        } else {
            getContext2.getMockMvc().perform(getContext2.getBuilder()).andExpect(status().isOk())
                .andExpect(jsonPath("resourceIdentifier", is(EVIDENCE_SCULLYS_TESTIMONY_ID)))
                .andExpect(jsonPath("attributes[0].name", is(TOP_SECRET_CLASSIFICATION.getName())))
                .andExpect(jsonPath("attributes[0].value", is(TOP_SECRET_CLASSIFICATION.getValue())))
                .andExpect(jsonPath("attributes[0].issuer", is(TOP_SECRET_CLASSIFICATION.getIssuer())))
                .andExpect(jsonPath("attributes[1].name", is(SPECIAL_AGENTS_GROUP_ATTRIBUTE.getName())))
                .andExpect(jsonPath("attributes[1].value", is(SPECIAL_AGENTS_GROUP_ATTRIBUTE.getValue())))
                .andExpect(jsonPath("attributes[1].issuer", is(SPECIAL_AGENTS_GROUP_ATTRIBUTE.getIssuer())))
                .andExpect(jsonPath("attributes[2].name", is(TYPE_MYTHARC.getName())))
                .andExpect(jsonPath("attributes[2].value", is(TYPE_MYTHARC.getValue())))
                .andExpect(jsonPath("attributes[2].issuer", is(TYPE_MYTHARC.getIssuer())))
                .andExpect(jsonPath("attributes[3].name", is(SITE_BASEMENT.getName())))
                .andExpect(jsonPath("attributes[3].value", is(SITE_BASEMENT.getValue())))
                .andExpect(jsonPath("attributes[3].issuer", is(SITE_BASEMENT.getIssuer())));
        }

        // Clean up after ourselves.
        deleteResource(EVIDENCE_SCULLYS_TESTIMONY_ID);
        deleteResource(ASCENSION_ID);
        deleteResource(BASEMENT_SITE_ID);
    }

    private void deleteResource(String resourceIdentifier) throws Exception {
        String resourceToDeleteURI = RESOURCE_BASE_URL + "/" + URLEncoder.encode(resourceIdentifier, "UTF-8");
        MockMvcContext deleteContext = this.testUtils.createWACWithCustomDELETERequestBuilder(this.wac,
                this.testZone.getSubdomain(), resourceToDeleteURI);
        deleteContext.getMockMvc().perform(deleteContext.getBuilder()).andExpect(status().isNoContent());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testPOSTResourcesMissingResourceId() throws Exception {
        List<BaseResource> resources = this.jsonUtils.deserializeFromFile(
                "controller-test/missing-resourceIdentifier-resources-collection.json", List.class);

        Assert.assertNotNull(resources);

        // Append a list of resources
        MockMvcContext postContext = this.testUtils.createWACWithCustomPOSTRequestBuilder(this.wac,
                this.testZone.getSubdomain(), RESOURCE_BASE_URL);
        postContext.getMockMvc()
                .perform(postContext.getBuilder().contentType(MediaType.APPLICATION_JSON)
                        .content(OBJECT_MAPPER.writeValueAsString(resources)))
                .andExpect(status().isUnprocessableEntity());

    }

    @Test
    @SuppressWarnings("unchecked")
    public void testPOSTResourcesMissingIdentifier() throws Exception {
        List<BaseResource> resources = this.jsonUtils
                .deserializeFromFile("controller-test/missing-identifier-resources-collection.json", List.class);

        Assert.assertNotNull(resources);

        // Append a list of resources
        MockMvcContext postContext = this.testUtils.createWACWithCustomPOSTRequestBuilder(this.wac,
                this.testZone.getSubdomain(), RESOURCE_BASE_URL);
        postContext.getMockMvc()
                .perform(postContext.getBuilder().contentType(MediaType.APPLICATION_JSON)
                        .content(OBJECT_MAPPER.writeValueAsString(resources)))
                .andExpect(status().isUnprocessableEntity());

    }

    @Test
    @SuppressWarnings("unchecked")
    public void testPOSTResourcesEmptyIdentifier() throws Exception {
        List<BaseResource> resources = this.jsonUtils
                .deserializeFromFile("controller-test/empty-identifier-resources-collection.json", List.class);

        Assert.assertNotNull(resources);

        // Append a list of resources
        MockMvcContext postContext = this.testUtils.createWACWithCustomPOSTRequestBuilder(this.wac,
                this.testZone.getSubdomain(), RESOURCE_BASE_URL);
        postContext.getMockMvc()
                .perform(postContext.getBuilder().contentType(MediaType.APPLICATION_JSON)
                        .content(OBJECT_MAPPER.writeValueAsString(resources)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    public void testResourceIdentifierMismatch() throws Exception {

        BaseResource resource = this.jsonUtils.deserializeFromFile("controller-test/a-mismatched-resourceid.json",
                BaseResource.class);
        Assert.assertNotNull(resource);

        String thisUri = RESOURCE_BASE_URL + "/%2Fservices%2Fsecured-api";

        // Update a given resource
        MockMvcContext putContext = this.testUtils.createWACWithCustomPUTRequestBuilder(this.wac,
                this.testZone.getSubdomain(), thisUri);
        putContext.getMockMvc()
                .perform(putContext.getBuilder().contentType(MediaType.APPLICATION_JSON)
                        .content(OBJECT_MAPPER.writeValueAsString(resource)))
                .andExpect(status().isUnprocessableEntity());

    }

    @Test
    public void testPUTResourceNoResourceId() throws Exception {

        BaseResource resource = this.jsonUtils
                .deserializeFromFile("controller-test/no-resourceIdentifier-resource.json", BaseResource.class);
        Assert.assertNotNull(resource);

        String thisUri = RESOURCE_BASE_URL + "/%2Fservices%2Fsecured-api%2Fsubresource";
        // Update a given resource
        MockMvcContext putContext = this.testUtils.createWACWithCustomPUTRequestBuilder(this.wac,
                this.testZone.getSubdomain(), thisUri);
        putContext.getMockMvc().perform(putContext.getBuilder().contentType(MediaType.APPLICATION_JSON)
                .content(OBJECT_MAPPER.writeValueAsString(resource))).andExpect(status().is2xxSuccessful());

        // Get a given resource
        MockMvcContext getContext = this.testUtils.createWACWithCustomGETRequestBuilder(this.wac,
                this.testZone.getSubdomain(), thisUri);
        getContext.getMockMvc().perform(getContext.getBuilder()).andExpect(status().isOk())
                .andExpect(jsonPath("resourceIdentifier", is("/services/secured-api/subresource")))
                .andExpect(jsonPath("attributes[0].value", isIn(new String[] { "supervisor", "it" })))
                .andExpect(jsonPath("attributes[0].issuer", is("https://acs.attributes.int")));

        // Delete a given resource
        MockMvcContext deleteContext = this.testUtils.createWACWithCustomDELETERequestBuilder(this.wac,
                this.testZone.getSubdomain(), thisUri);
        deleteContext.getMockMvc().perform(deleteContext.getBuilder()).andExpect(status().isNoContent());
    }

    @Test
    public void testPUTCreateResourceThenUpdateNoResourceIdentifier() throws Exception {

        BaseResource resource = this.jsonUtils
                .deserializeFromFile("controller-test/with-resourceIdentifier-resource.json", BaseResource.class);
        Assert.assertNotNull(resource);

        String thisUri = RESOURCE_BASE_URL + "/%2Fservices%2Fsecured-api%2Fsubresource";

        MockMvcContext putContext = this.testUtils.createWACWithCustomPUTRequestBuilder(this.wac,
                this.testZone.getSubdomain(), thisUri);
        putContext.getMockMvc().perform(putContext.getBuilder().contentType(MediaType.APPLICATION_JSON)
                .content(OBJECT_MAPPER.writeValueAsString(resource))).andExpect(status().isCreated());

        // Get a given resource
        MockMvcContext getContext = this.testUtils.createWACWithCustomGETRequestBuilder(this.wac,
                this.testZone.getSubdomain(), thisUri);
        getContext.getMockMvc().perform(getContext.getBuilder()).andExpect(status().isOk())
                .andExpect(jsonPath("resourceIdentifier", is(resource.getResourceIdentifier())))
                .andExpect(jsonPath("attributes[0].value", isIn(new String[] { "supervisor", "it" })))
                .andExpect(jsonPath("attributes[0].issuer", is("https://acs.attributes.int")));

        // Ensure we can update resource without a resource identifier in json
        // payload
        // In this case, the resource identifier must be part of the URI.
        BaseResource resourceNoResourceIdentifier = this.jsonUtils
                .deserializeFromFile("controller-test/no-resourceIdentifier-resource.json", BaseResource.class);
        Assert.assertNotNull(resourceNoResourceIdentifier);

        // Update a given resource
        putContext = this.testUtils.createWACWithCustomPUTRequestBuilder(this.wac, this.testZone.getSubdomain(),
                thisUri);
        putContext.getMockMvc()
                .perform(putContext.getBuilder().contentType(MediaType.APPLICATION_JSON)
                        .content(OBJECT_MAPPER.writeValueAsString(resourceNoResourceIdentifier)))
                .andExpect(status().isNoContent());

        // Get a given resource
        getContext = this.testUtils.createWACWithCustomGETRequestBuilder(this.wac, this.testZone.getSubdomain(),
                thisUri);
        getContext.getMockMvc().perform(getContext.getBuilder()).andExpect(status().isOk())
                .andExpect(jsonPath("resourceIdentifier", is(resource.getResourceIdentifier())))
                .andExpect(jsonPath("attributes[0].value", isIn(new String[] { "supervisor", "it" })))
                .andExpect(jsonPath("attributes[0].issuer", is("https://acs.attributes.int")));

        // Delete a given resource
        MockMvcContext deleteContext = this.testUtils.createWACWithCustomDELETERequestBuilder(this.wac,
                this.testZone.getSubdomain(), thisUri);
        deleteContext.getMockMvc().perform(deleteContext.getBuilder()).andExpect(status().isNoContent());

    }

    @Test
    public void testPathVariablesWithSpecialCharacters() throws Exception {

        // If the given string violates RFC&nbsp;2396 it will throw http 422
        // error
        // The following characters are valid: @#!$*&()_-+=[]:;{}'~`,
        String decoded = "/services/special/@#!$*&()_-+=[]:;{}'~`,";
        String encoded = URLEncoder.encode(decoded, "UTF-8");
        String thisUri = RESOURCE_BASE_URL + "/" + encoded;

        BaseResource resource = this.jsonUtils
                .deserializeFromFile("controller-test/special-character-resource-identifier.json", BaseResource.class);
        Assert.assertNotNull(resource);

        MockMvcContext putContext = this.testUtils.createWACWithCustomPUTRequestBuilder(this.wac,
                this.testZone.getSubdomain(), thisUri);
        putContext.getMockMvc().perform(putContext.getBuilder().contentType(MediaType.APPLICATION_JSON)
                .content(OBJECT_MAPPER.writeValueAsString(resource))).andExpect(status().isCreated());

        MockMvcContext getContext = this.testUtils.createWACWithCustomGETRequestBuilder(this.wac,
                this.testZone.getSubdomain(), thisUri);
        getContext.getMockMvc().perform(getContext.getBuilder()).andExpect(status().isOk())
                .andExpect(jsonPath("resourceIdentifier", is(decoded)));
    }

}
// @formatter:on
