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
package com.ge.predix.integration.test;

import static com.ge.predix.integration.test.SubjectResourceFixture.BOB_V1;
import static com.ge.predix.integration.test.SubjectResourceFixture.JLO_V1;
import static com.ge.predix.integration.test.SubjectResourceFixture.JOE_V1;
import static com.ge.predix.integration.test.SubjectResourceFixture.MARISSA_V1;
import static com.ge.predix.integration.test.SubjectResourceFixture.PETE_V1;
import static com.ge.predix.integration.test.SubjectResourceFixture.SANRAMON;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import javax.security.auth.Subject;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.ge.predix.acs.model.Attribute;
import com.ge.predix.acs.rest.BaseResource;
import com.ge.predix.acs.rest.BaseSubject;
import com.ge.predix.test.utils.ACSRestTemplateFactory;
import com.ge.predix.test.utils.PrivilegeHelper;
import com.ge.predix.test.utils.UaaTestUtil;
import com.ge.predix.test.utils.ZacTestUtil;
import com.ge.predix.test.utils.ZoneHelper;

@ContextConfiguration("classpath:integration-test-spring-context.xml")
// @Test(dependsOnGroups = { "acsHealthCheck.*" })
@Test
public class PrivilegeManagementAccessControlServiceIT extends AbstractTestNGSpringContextTests {

    @Value("${zone1UaaUrl:http://localhost:8080/uaa}")
    private String zone1UaaBaseUrl;

    @Value("${ZONE1_NAME:testzone1}")
    private String acsZone1Name;

    @Value("${ZONE2_NAME:testzone2}")
    private String acsZone2Name;

    @Value("${ZONE3_NAME:testzone3}")
    private String acsZone3Name;

    @Autowired
    private ACSRestTemplateFactory acsRestTemplateFactory;

    @Autowired
    private ZoneHelper zoneHelper;

    @Autowired
    private PrivilegeHelper privilegeHelper;

    @Autowired
    private ZacTestUtil zacTestUtil;

    @Autowired
    Environment env;

    @Value("${UAA_URL:http://localhost:8080/uaa}")
    private String uaaUrl;

    private String zone1Url;
    private OAuth2RestTemplate acsAdminRestTemplate;
    private boolean registerWithZac;

    @BeforeClass
    public void setup() throws JsonParseException, JsonMappingException, IOException {
        this.zone1Url = this.zoneHelper.getZone1Url();
        if (Arrays.asList(this.env.getActiveProfiles()).contains("public")) {
            setupPublicACS();
        } else {
            setupPredixACS();
        }
    }

    private void setupPredixACS() throws JsonParseException, JsonMappingException, IOException {
        this.zacTestUtil.assumeZacServerAvailable();

        this.acsAdminRestTemplate = this.acsRestTemplateFactory.getACSTemplateWithPolicyScope();
        this.registerWithZac = true;
        this.zoneHelper.createTestZone(this.acsAdminRestTemplate, this.acsZone1Name, this.registerWithZac);
    }

    private void setupPublicACS() throws JsonParseException, JsonMappingException, IOException {
        UaaTestUtil uaaTestUtil = new UaaTestUtil(this.acsRestTemplateFactory.getOAuth2RestTemplateForUaaAdmin(),
                this.uaaUrl);
        uaaTestUtil.setup(Arrays.asList(new String[] {this.acsZone1Name, this.acsZone2Name, this.acsZone3Name}));

        this.acsAdminRestTemplate = this.acsRestTemplateFactory.getOAuth2RestTemplateForAcsAdmin();
        this.registerWithZac = false;
        this.zoneHelper.createTestZone(this.acsAdminRestTemplate, this.acsZone1Name, this.registerWithZac);
    }

    public void testBatchCreateSubjectsEmptyList() {
        List<BaseSubject> subjects = new ArrayList<BaseSubject>();
        try {
            this.acsAdminRestTemplate.postForEntity(this.zone1Url + PrivilegeHelper.ACS_SUBJECT_API_PATH, subjects,
                    BaseSubject[].class);
        } catch (HttpClientErrorException e) {
            Assert.assertEquals(e.getStatusCode(), HttpStatus.UNPROCESSABLE_ENTITY);
            return;
        }
        Assert.fail("Expected unprocessable entity http client error.");
    }

    public void testBatchSubjectsDataConstraintViolationSubjectIdentifier() {
        List<BaseSubject> subjects = new ArrayList<BaseSubject>();
        subjects.add(this.privilegeHelper.createSubject("marissa"));
        subjects.add(this.privilegeHelper.createSubject("marissa"));

        try {
            this.acsAdminRestTemplate.postForEntity(this.zone1Url + PrivilegeHelper.ACS_SUBJECT_API_PATH, subjects,
                    ResponseEntity.class);
        } catch (HttpClientErrorException e) {
            Assert.assertEquals(e.getStatusCode(), HttpStatus.UNPROCESSABLE_ENTITY);
            return;
        }
        this.acsAdminRestTemplate.delete(this.zone1Url + PrivilegeHelper.ACS_SUBJECT_API_PATH + "/marissa");
        Assert.fail("Expected unprocessable entity http client error.");
    }

    public void testCreateSubjectWithMalformedJSON() {
        try {
            String badSubject = "{\"subject\": bad-subject-form\"}";
            MultiValueMap<String, String> headers = new HttpHeaders();
            headers.add("Content-type", "application/json");
            HttpEntity<String> httpEntity = new HttpEntity<String>(badSubject, headers);
            this.acsAdminRestTemplate.put(this.zone1Url + PrivilegeHelper.ACS_SUBJECT_API_PATH + "/bad-subject-form",
                    httpEntity);
        } catch (HttpClientErrorException e) {
            Assert.assertEquals(e.getStatusCode(), HttpStatus.BAD_REQUEST);
            return;
        }
        this.acsAdminRestTemplate.delete(this.zone1Url + PrivilegeHelper.ACS_SUBJECT_API_PATH + "/bad-subject-form");
        Assert.fail("testCreateSubjectWithMalformedJSON should have failed!");
    }

    public void testCreateBatchSubjectsWithMalformedJSON() {
        try {
            String badSubject = "{\"subject\":{\"name\" : \"good-subject-brittany\"},"
                    + "{\"subject\": bad-subject-sarah\"}";
            MultiValueMap<String, String> headers = new HttpHeaders();
            headers.add("Content-type", "application/json");
            HttpEntity<String> httpEntity = new HttpEntity<String>(badSubject, headers);
            this.acsAdminRestTemplate.postForEntity(this.zone1Url + PrivilegeHelper.ACS_SUBJECT_API_PATH, httpEntity,
                    Subject[].class);
        } catch (HttpClientErrorException e) {
            Assert.assertEquals(e.getStatusCode(), HttpStatus.BAD_REQUEST);
            return;
        }
        this.acsAdminRestTemplate.delete(this.zone1Url + PrivilegeHelper.ACS_SUBJECT_API_PATH + "/bad-subject-sarah");
        this.acsAdminRestTemplate
                .delete(this.zone1Url + PrivilegeHelper.ACS_SUBJECT_API_PATH + "/good-subject-brittany");
        Assert.fail("testCreateBatchSubjectsWithMalformedJSON should have failed!");
    }

    public void testCreateResourceWithMalformedJSON() {
        try {
            String badResource = "{\"resource\": bad-resource-form\"}";
            MultiValueMap<String, String> headers = new HttpHeaders();
            headers.add("Content-type", "application/json");
            HttpEntity<String> httpEntity = new HttpEntity<String>(badResource, headers);
            this.acsAdminRestTemplate.put(this.zone1Url + PrivilegeHelper.ACS_RESOURCE_API_PATH + "/bad-resource-form",
                    httpEntity);
        } catch (HttpClientErrorException e) {
            Assert.assertEquals(e.getStatusCode(), HttpStatus.BAD_REQUEST);
            return;
        }
        this.acsAdminRestTemplate.delete(this.zone1Url + PrivilegeHelper.ACS_RESOURCE_API_PATH + "/bad-resource-form");
        Assert.fail("testCreateResourceWithMalformedJSON should have failed!");
    }

    public void testCreateBatchResourcesWithMalformedJSON() {
        try {
            String badResource = "{\"resource\":{\"name\" : \"Site\", \"uriTemplate\" : \"/secured-by-value/sites/{site_id}\"},"
                    + "{\"resource\": bad-resource-form\"}";
            MultiValueMap<String, String> headers = new HttpHeaders();
            headers.add("Content-type", "application/json");
            HttpEntity<String> httpEntity = new HttpEntity<String>(badResource, headers);
            this.acsAdminRestTemplate.postForEntity(this.zone1Url + PrivilegeHelper.ACS_RESOURCE_API_PATH, httpEntity,
                    BaseResource[].class);
        } catch (HttpClientErrorException e) {
            Assert.assertEquals(e.getStatusCode(), HttpStatus.BAD_REQUEST);
            return;
        }
        this.acsAdminRestTemplate.delete(this.zone1Url + PrivilegeHelper.ACS_RESOURCE_API_PATH + "/bad-resource-form");
        this.acsAdminRestTemplate.delete(this.zone1Url + PrivilegeHelper.ACS_RESOURCE_API_PATH + "/Site");
        Assert.fail("testCreateBatchResourcesWithMalformedJSON should have failed!");
    }

    public void testBatchCreateResourcesEmptyList() {
        List<BaseResource> resources = new ArrayList<BaseResource>();
        try {
            this.acsAdminRestTemplate.postForEntity(this.zone1Url + PrivilegeHelper.ACS_RESOURCE_API_PATH, resources,
                    BaseResource[].class);
        } catch (HttpClientErrorException e) {
            Assert.assertEquals(e.getStatusCode(), HttpStatus.UNPROCESSABLE_ENTITY);
            return;
        }
        Assert.fail("Expected unprocessable entity http client error.");
    }

    public void testResourceUpdateAttributes() {
        BaseResource resource1 = this.privilegeHelper.createResource("marissa");
        BaseResource resource2 = this.privilegeHelper.createResource("marissa");
        Set<Attribute> attributes = resource2.getAttributes();
        Attribute attribute = new Attribute();
        attribute.setName("updatedName");
        attribute.setIssuer("http://attributes.net");
        attributes.add(attribute);
        resource2.setAttributes(attributes);

        this.acsAdminRestTemplate.put(this.zone1Url + PrivilegeHelper.ACS_RESOURCE_API_PATH + "/marissa", resource1);
        this.acsAdminRestTemplate.put(this.zone1Url + PrivilegeHelper.ACS_RESOURCE_API_PATH + "/marissa", resource2);
        ResponseEntity<BaseResource> response = this.acsAdminRestTemplate
                .getForEntity(this.zone1Url + PrivilegeHelper.ACS_RESOURCE_API_PATH + "/marissa", BaseResource.class);
        Assert.assertEquals(response.getBody(), resource2);
        this.acsAdminRestTemplate.delete(this.zone1Url + PrivilegeHelper.ACS_RESOURCE_API_PATH + "/marissa");
    }

    public void testBatchResourcesDataConstraintViolationResourceIdentifier() {
        List<BaseResource> resources = new ArrayList<BaseResource>();
        resources.add(this.privilegeHelper.createResource("dupResourceIdentifier"));
        resources.add(this.privilegeHelper.createResource("dupResourceIdentifier"));

        try {
            // This POST causes a data constraint violation on the service bcos
            // of duplicate
            // resource_identifiers which returns a HTTP 422 error.
            this.acsAdminRestTemplate.postForEntity(this.zone1Url + PrivilegeHelper.ACS_RESOURCE_API_PATH, resources,
                    ResponseEntity.class);
        } catch (HttpClientErrorException e) {
            Assert.assertEquals(e.getStatusCode(), HttpStatus.UNPROCESSABLE_ENTITY);
            return;
        }
        this.acsAdminRestTemplate.delete(this.zone1Url + PrivilegeHelper.ACS_RESOURCE_API_PATH + "/marissa");
        Assert.fail("Expected unprocessable entity http client error on post for 2 resources with duplicate resource"
                + "identifiers.");
    }

    @Test(dataProvider = "subjectProvider")
    public void testPutGetDeleteSubject(final BaseSubject subject) throws UnsupportedEncodingException {
        ResponseEntity<BaseSubject> responseEntity = null;
        try {
            this.privilegeHelper.putSubject(this.acsAdminRestTemplate, subject, this.zone1Url, null,
                    this.privilegeHelper.getDefaultAttribute());
        } catch (HttpClientErrorException e) {
            Assert.fail("Unable to create subject.");
        }
        String encodedSubjectIdentifier = URLEncoder.encode(subject.getSubjectIdentifier(), "UTF-8");
        URI uri = URI.create(this.zone1Url + PrivilegeHelper.ACS_SUBJECT_API_PATH + encodedSubjectIdentifier);
        try {
            responseEntity = this.acsAdminRestTemplate.getForEntity(uri, BaseSubject.class);
            Assert.assertEquals(responseEntity.getStatusCode(), HttpStatus.OK);
        } catch (HttpClientErrorException e) {
            Assert.fail("Unable to get subject.");
        }
        try {
            this.acsAdminRestTemplate.delete(uri);
            responseEntity = this.acsAdminRestTemplate.getForEntity(uri, BaseSubject.class);
            Assert.fail("Subject " + subject.getSubjectIdentifier() + " was not properly deleted");
        } catch (HttpClientErrorException e) {
            Assert.assertEquals(e.getStatusCode(), HttpStatus.NOT_FOUND, "Subject was not deleted.");
        }

    }

    // To test cascade delete for postgres, comment out delete-db-service and delete executions. Run integration with
    // -PCloud. Bind db to pgPhpAdmin and browse the db to ensure all entries with zone 'test-zone-dev3' as a foreign
    // key are deleted respectively.
    public void testPutSubjectDeleteZone() throws JsonParseException, JsonMappingException, IOException {
        String zone3Url = this.zoneHelper.getZoneSpecificUrl(this.acsZone3Name);
        this.zoneHelper.createTestZone(this.acsAdminRestTemplate, this.acsZone3Name, this.registerWithZac);

        ResponseEntity<BaseSubject> responseEntity = null;
        try {
            this.privilegeHelper.putSubject(this.acsAdminRestTemplate, MARISSA_V1, zone3Url, null,
                    this.privilegeHelper.getDefaultAttribute());

        } catch (HttpClientErrorException e) {
            Assert.fail("Unable to create subject.", e);
        }
        try {
            responseEntity = this.acsAdminRestTemplate.getForEntity(
                    zone3Url + PrivilegeHelper.ACS_SUBJECT_API_PATH + MARISSA_V1.getSubjectIdentifier(),
                    BaseSubject.class);
            Assert.assertEquals(responseEntity.getStatusCode(), HttpStatus.OK);
        } catch (HttpClientErrorException e) {
            Assert.fail("Unable to get subject.", e);
        }
        try {
            this.zoneHelper.deleteZone(this.acsAdminRestTemplate, this.acsZone3Name, this.registerWithZac);
            this.acsAdminRestTemplate.getForEntity(
                    zone3Url + PrivilegeHelper.ACS_SUBJECT_API_PATH + MARISSA_V1.getSubjectIdentifier(),
                    BaseSubject.class);
            Assert.fail("Zone '" + this.acsZone3Name + "' was not properly deleted.");
        } catch (HttpServerErrorException e) {
            // This following lines to be uncommented once ZacTokenService returns the right exception instead of a
            // 500 - Defect url https://rally1.rallydev.com/#/30377833713d/detail/defect/42793900179
            // catch (OAuth2Exception e) {
            // Assert.assertTrue(e.getSummary().contains(HttpStatus.FORBIDDEN.toString()),
            // "Zone deletion did not produce the expected HTTP status code. Failed with: " + e);
        } catch (Exception e) {
            Assert.fail("Failed with unexpected exception.", e);
        }
    }

    public void testPutSubjectMismatchURI() {
        try {
            String subjectIdentifier = "marcia";
            URI subjectUri = URI.create(this.zone1Url + PrivilegeHelper.ACS_SUBJECT_API_PATH
                    + URLEncoder.encode(subjectIdentifier, "UTF-8"));
            this.acsAdminRestTemplate.put(subjectUri, BOB_V1);
            Assert.fail("Subject " + subjectIdentifier + " was not supposed to be created");
        } catch (HttpClientErrorException e) {
            Assert.assertEquals(e.getStatusCode(), HttpStatus.UNPROCESSABLE_ENTITY);
            return;
        } catch (Exception e) {
            Assert.fail("Unable to create subject.");
        }
        Assert.fail("Expected Unprocessible Entity status code in testPutSubjectMismatchURIV1");
    }

    @Test(dataProvider = "invalidSubjectPostProvider")
    public void testPostSubjectNegativeCases(final BaseSubject subject, final String endpoint) {
        try {

            this.privilegeHelper.postMultipleSubjects(this.acsAdminRestTemplate, endpoint, subject);

        } catch (HttpClientErrorException e) {
            Assert.assertEquals(e.getStatusCode(), HttpStatus.UNPROCESSABLE_ENTITY);
            return;
        } catch (Exception e) {
            Assert.fail("Unable to create subject.");
        }
        Assert.fail("Expected Unprocessible Entity status code in testPostSubjectNegativeCases");
    }

    @Test(dataProvider = "subjectPostProvider")
    public void testPostSubjectPostiveCases(final BaseSubject subject, final String endpoint) {
        try {
            ResponseEntity<Object> responseEntity = this.privilegeHelper.postMultipleSubjects(this.acsAdminRestTemplate,
                    endpoint, subject);
            Assert.assertEquals(responseEntity.getStatusCode(), HttpStatus.NO_CONTENT);
        } catch (Exception e) {
            Assert.fail("Unable to create subject.");
        }
    }

    @Test(dataProvider = "resourcePostProvider")
    public void testPostResourcePostiveCases(final BaseResource resource, final String endpoint) {
        try {
            ResponseEntity<Object> responseEntity = this.privilegeHelper.postResources(this.acsAdminRestTemplate,
                    endpoint, resource);
            Assert.assertEquals(responseEntity.getStatusCode(), HttpStatus.NO_CONTENT);
        } catch (Exception e) {
            Assert.fail("Unable to create resource.");
        }
    }

    @Test(dataProvider = "invalidResourcePostProvider")
    public void testPostResourceNegativeCases(final BaseResource resource, final String endpoint) {
        try {
            this.privilegeHelper.postResources(this.acsAdminRestTemplate, endpoint, resource);
        } catch (HttpClientErrorException e) {
            Assert.assertEquals(e.getStatusCode(), HttpStatus.UNPROCESSABLE_ENTITY);
            return;
        } catch (Exception e) {
            Assert.fail("Unable to create resource.");
        }
        Assert.fail("Expected UnprocessibleEntity status code in testPostResourceNegativeCases");
    }

    public void testPutGetDeleteResource() throws Exception {
        try {
            this.privilegeHelper.putResource(this.acsAdminRestTemplate, SANRAMON, this.zone1Url, null,
                    this.privilegeHelper.getDefaultAttribute());
        } catch (Exception e) {
            Assert.fail("Unable to create resource. " + e.getMessage());
        }
        URI resourceUri = URI.create(this.zone1Url + PrivilegeHelper.ACS_RESOURCE_API_PATH
                + URLEncoder.encode(SANRAMON.getResourceIdentifier(), "UTF-8"));
        try {
            this.acsAdminRestTemplate.getForEntity(resourceUri, BaseResource.class);
        } catch (HttpClientErrorException e) {
            Assert.fail("Unable to get resource.");
        }
        try {
            this.acsAdminRestTemplate.delete(resourceUri);
        } catch (HttpClientErrorException e) {
            Assert.fail("Unable to delete resource.");
        }
        // properly delete
        try {
            this.acsAdminRestTemplate.delete(resourceUri);
            this.acsAdminRestTemplate.getForEntity(resourceUri, BaseResource.class);
        } catch (HttpClientErrorException e) {
            Assert.assertEquals(e.getStatusCode(), HttpStatus.NOT_FOUND);
        }
    }

    public void testUpdateResourceURIMismatch() throws Exception {
        try {
            this.privilegeHelper.putResource(this.acsAdminRestTemplate, SANRAMON, this.zone1Url, null,
                    this.privilegeHelper.getDefaultAttribute());
            URI resourceUri = URI.create(this.zone1Url + PrivilegeHelper.ACS_RESOURCE_API_PATH
                    + URLEncoder.encode("/different/resource", "UTF-8"));
            this.acsAdminRestTemplate.put(resourceUri, SANRAMON);
        } catch (HttpClientErrorException e) {
            Assert.assertEquals(e.getStatusCode(), HttpStatus.UNPROCESSABLE_ENTITY);
            return;
        }
        Assert.fail("Expected Unprocessible Entity status code in testUpdateResourceURIMismatchV1");
    }

    @DataProvider(name = "subjectProvider")
    public Object[][] getSubjectProvider() {
        Object[][] data = new Object[][] { { MARISSA_V1 }, { JOE_V1 }, { PETE_V1 }, { JLO_V1 }, { BOB_V1 } };
        return data;
    }

    @DataProvider(name = "invalidSubjectPostProvider")
    public Object[][] getInvalidSubjectsPost() {
        Object[][] data = new Object[][] {
                // empty subjectIdentifier
                { new BaseSubject(null), this.zone1Url } };
        return data;
    }

    @DataProvider(name = "resourcePostProvider")
    public Object[][] getResourcesPost() {
        Object[][] data = new Object[][] {
                // non empty resourceIdentifier
                { new BaseResource("/sites/sanramon"), this.zone1Url }, };
        return data;
    }

    @DataProvider(name = "invalidResourcePostProvider")
    public Object[][] getInvalidResourcesPost() {
        Object[][] data = new Object[][] {
                // empty resourceIdentifier
                { new BaseResource(null), this.zone1Url }, };
        return data;
    }

    @DataProvider(name = "subjectPostProvider")
    public Object[][] getSubjectsPost() {
        Object[][] data = new Object[][] {
                // non empty subjectIdentifier
                { MARISSA_V1, this.zone1Url } };
        return data;
    }

    @DataProvider(name = "endpointProvider")
    public Object[][] getAcsEndpoint() {
        Object[][] data = new Object[][] { { this.zone1Url } };
        return data;
    }
}
