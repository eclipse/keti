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

package org.eclipse.keti.integration.test;

import static org.eclipse.keti.integration.test.SubjectResourceFixture.BOB_V1;
import static org.eclipse.keti.integration.test.SubjectResourceFixture.JLO_V1;
import static org.eclipse.keti.integration.test.SubjectResourceFixture.JOE_V1;
import static org.eclipse.keti.integration.test.SubjectResourceFixture.MARISSA_V1;
import static org.eclipse.keti.integration.test.SubjectResourceFixture.PETE_V1;
import static org.eclipse.keti.integration.test.SubjectResourceFixture.SANRAMON;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.security.auth.Subject;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import org.eclipse.keti.acs.model.Attribute;
import org.eclipse.keti.acs.rest.BaseResource;
import org.eclipse.keti.acs.rest.BaseSubject;
import org.eclipse.keti.test.TestConfig;
import org.eclipse.keti.test.utils.ACSITSetUpFactory;
import org.eclipse.keti.test.utils.ACSTestUtil;
import org.eclipse.keti.test.utils.PolicyHelper;
import org.eclipse.keti.test.utils.PrivilegeHelper;
import org.eclipse.keti.test.utils.ZoneFactory;

@ContextConfiguration("classpath:integration-test-spring-context.xml")
// @Test(dependsOnGroups = { "acsHealthCheck.*" })
@Test
public class PrivilegeManagementAccessControlServiceIT extends AbstractTestNGSpringContextTests {

    private String acsZone1Name;

    @Value("${zone1UaaUrl}/oauth/token")
    private String primaryZoneIssuerId;

    private String acsZone3Name;

    @Autowired
    private ZoneFactory zoneFactory;

    @Autowired
    private PrivilegeHelper privilegeHelper;

    @Autowired
    private ACSITSetUpFactory acsitSetUpFactory;

    private String acsUrl;
    private HttpHeaders zone1Headers;
    private HttpHeaders zone3Headers;
    private OAuth2RestTemplate acsAdminRestTemplate;

    @BeforeClass
    public void setup() throws JsonParseException, JsonMappingException, IOException {
        TestConfig.setupForEclipse(); // Starts ACS when running the test in eclipse.
        this.acsitSetUpFactory.setUp();
        this.acsUrl = this.acsitSetUpFactory.getAcsUrl();
        this.zone1Headers = this.acsitSetUpFactory.getZone1Headers();
        this.zone3Headers = this.acsitSetUpFactory.getZone3Headers();
        this.acsAdminRestTemplate = this.acsitSetUpFactory.getAcsZonesAdminRestTemplate();
        this.acsZone1Name = this.acsitSetUpFactory.getZone1().getSubdomain();
        this.acsZone3Name = this.acsitSetUpFactory.getAcsZone3Name();
    }

    public void testBatchCreateSubjectsEmptyList() {
        List<BaseSubject> subjects = new ArrayList<BaseSubject>();
        try {
            this.acsAdminRestTemplate.postForEntity(this.acsUrl + PrivilegeHelper.ACS_SUBJECT_API_PATH,
                    new HttpEntity<>(subjects, this.zone1Headers), BaseSubject[].class);
        } catch (HttpClientErrorException e) {
            Assert.assertEquals(e.getStatusCode(), HttpStatus.UNPROCESSABLE_ENTITY);
            return;
        }
        Assert.fail("Expected unprocessable entity http client error.");
    }

    @Test
    public void testBatchSubjectsDataConstraintViolationSubjectIdentifier() {
        List<BaseSubject> subjects = new ArrayList<BaseSubject>();
        subjects.add(this.privilegeHelper.createSubject("marissa"));
        subjects.add(this.privilegeHelper.createSubject("marissa"));

        try {
            this.acsAdminRestTemplate.postForEntity(this.acsUrl + PrivilegeHelper.ACS_SUBJECT_API_PATH,
                    new HttpEntity<>(subjects, this.zone1Headers), ResponseEntity.class);
        } catch (HttpClientErrorException e) {
            Assert.assertEquals(e.getStatusCode(), HttpStatus.UNPROCESSABLE_ENTITY);
            return;
        }
        this.acsAdminRestTemplate
                .exchange(this.acsUrl + PrivilegeHelper.ACS_SUBJECT_API_PATH + "/marissa", HttpMethod.DELETE,
                        new HttpEntity<>(this.zone1Headers), ResponseEntity.class);
        Assert.fail("Expected unprocessable entity http client error.");
    }

    public void testCreateSubjectWithMalformedJSON() {
        try {
            String badSubject = "{\"subject\": bad-subject-form\"}";
            MultiValueMap<String, String> headers = ACSTestUtil.httpHeaders();
            headers.add("Content-type", "application/json");
            headers.add(PolicyHelper.PREDIX_ZONE_ID, this.acsZone1Name);
            HttpEntity<String> httpEntity = new HttpEntity<String>(badSubject, headers);
            this.acsAdminRestTemplate
                    .put(this.acsUrl + PrivilegeHelper.ACS_SUBJECT_API_PATH + "/bad-subject-form", httpEntity);
        } catch (HttpClientErrorException e) {
            Assert.assertEquals(e.getStatusCode(), HttpStatus.BAD_REQUEST);
            return;
        }
        this.acsAdminRestTemplate
                .exchange(this.acsUrl + PrivilegeHelper.ACS_SUBJECT_API_PATH + "/bad-subject-form", HttpMethod.DELETE,
                        new HttpEntity<>(this.zone1Headers), ResponseEntity.class);
        Assert.fail("testCreateSubjectWithMalformedJSON should have failed!");
    }

    public void testCreateBatchSubjectsWithMalformedJSON() {
        try {
            String badSubject =
                    "{\"subject\":{\"name\" : \"good-subject-brittany\"}," + "{\"subject\": bad-subject-sarah\"}";
            MultiValueMap<String, String> headers = ACSTestUtil.httpHeaders();
            headers.add("Content-type", "application/json");
            headers.add(PolicyHelper.PREDIX_ZONE_ID, this.acsZone1Name);
            HttpEntity<String> httpEntity = new HttpEntity<String>(badSubject, headers);
            this.acsAdminRestTemplate
                    .postForEntity(this.acsUrl + PrivilegeHelper.ACS_SUBJECT_API_PATH, httpEntity, Subject[].class);
        } catch (HttpClientErrorException e) {
            Assert.assertEquals(e.getStatusCode(), HttpStatus.BAD_REQUEST);
            return;
        }
        this.acsAdminRestTemplate
                .exchange(this.acsUrl + PrivilegeHelper.ACS_SUBJECT_API_PATH + "/bad-subject-sarah", HttpMethod.DELETE,
                        new HttpEntity<>(this.zone1Headers), ResponseEntity.class);
        this.acsAdminRestTemplate
                .exchange(this.acsUrl + PrivilegeHelper.ACS_SUBJECT_API_PATH + "/good-subject-brittany",
                        HttpMethod.DELETE, new HttpEntity<>(this.zone1Headers), ResponseEntity.class);
        Assert.fail("testCreateBatchSubjectsWithMalformedJSON should have failed!");
    }

    public void testCreateResourceWithMalformedJSON() {
        try {
            String badResource = "{\"resource\": bad-resource-form\"}";
            MultiValueMap<String, String> headers = ACSTestUtil.httpHeaders();
            headers.add("Content-type", "application/json");
            headers.add(PolicyHelper.PREDIX_ZONE_ID, this.acsZone1Name);
            HttpEntity<String> httpEntity = new HttpEntity<String>(badResource, headers);
            this.acsAdminRestTemplate
                    .put(this.acsUrl + PrivilegeHelper.ACS_RESOURCE_API_PATH + "/bad-resource-form", httpEntity);
        } catch (HttpClientErrorException e) {
            Assert.assertEquals(e.getStatusCode(), HttpStatus.BAD_REQUEST);
            return;
        }
        this.acsAdminRestTemplate
                .exchange(this.acsUrl + PrivilegeHelper.ACS_RESOURCE_API_PATH + "/bad-resource-form", HttpMethod.DELETE,
                        new HttpEntity<>(this.zone1Headers), ResponseEntity.class);
        Assert.fail("testCreateResourceWithMalformedJSON should have failed!");
    }

    public void testCreateBatchResourcesWithMalformedJSON() {
        try {
            String badResource = "{\"resource\":{\"name\" : \"Site\", \"uriTemplate\" : "
                    + "\"/secured-by-value/sites/{site_id}\"},{\"resource\": bad-resource-form\"}";
            MultiValueMap<String, String> headers = ACSTestUtil.httpHeaders();
            headers.add("Content-type", "application/json");
            headers.add(PolicyHelper.PREDIX_ZONE_ID, this.acsZone1Name);
            HttpEntity<String> httpEntity = new HttpEntity<String>(badResource, headers);
            this.acsAdminRestTemplate.postForEntity(this.acsUrl + PrivilegeHelper.ACS_RESOURCE_API_PATH, httpEntity,
                    BaseResource[].class);
        } catch (HttpClientErrorException e) {
            Assert.assertEquals(e.getStatusCode(), HttpStatus.BAD_REQUEST);
            return;
        }
        this.acsAdminRestTemplate
                .exchange(this.acsUrl + PrivilegeHelper.ACS_RESOURCE_API_PATH + "/bad-resource-form", HttpMethod.DELETE,
                        new HttpEntity<>(this.zone1Headers), ResponseEntity.class);
        this.acsAdminRestTemplate
                .exchange(this.acsUrl + PrivilegeHelper.ACS_RESOURCE_API_PATH + "/Site", HttpMethod.DELETE,
                        new HttpEntity<>(this.zone1Headers), ResponseEntity.class);
        Assert.fail("testCreateBatchResourcesWithMalformedJSON should have failed!");
    }

    public void testBatchCreateResourcesEmptyList() {
        List<BaseResource> resources = new ArrayList<BaseResource>();
        try {
            this.acsAdminRestTemplate.postForEntity(this.acsUrl + PrivilegeHelper.ACS_RESOURCE_API_PATH,
                    new HttpEntity<>(resources, this.zone1Headers), BaseResource[].class);
        } catch (HttpClientErrorException e) {
            Assert.assertEquals(e.getStatusCode(), HttpStatus.UNPROCESSABLE_ENTITY);
            return;
        }
        Assert.fail("Expected unprocessable entity http client error.");
    }

    public void testResourceUpdateAttributes() {
        BaseResource resource1 = this.privilegeHelper.createResource("marissa");
        BaseResource resource2 = this.privilegeHelper.createResource("marissa");
        Set<Attribute> attributes = new HashSet<Attribute>();
        Attribute attribute = new Attribute();
        attribute.setName("site");
        attribute.setIssuer("http://attributes.net");
        attribute.setValue("sanfrancisco");
        attributes.add(attribute);
        resource2.setAttributes(attributes);

        this.acsAdminRestTemplate.put(this.acsUrl + PrivilegeHelper.ACS_RESOURCE_API_PATH + "/marissa",
                new HttpEntity<>(resource1, this.zone1Headers));
        this.acsAdminRestTemplate.put(this.acsUrl + PrivilegeHelper.ACS_RESOURCE_API_PATH + "/marissa",
                new HttpEntity<>(resource2, this.zone1Headers));
        ResponseEntity<BaseResource> response = acsAdminRestTemplate
                .exchange(this.acsUrl + PrivilegeHelper.ACS_RESOURCE_API_PATH + "/marissa", HttpMethod.GET,
                        new HttpEntity<>(this.zone1Headers), BaseResource.class);
        Assert.assertEquals(response.getBody(), resource2);
        this.acsAdminRestTemplate
                .exchange(this.acsUrl + PrivilegeHelper.ACS_RESOURCE_API_PATH + "/marissa", HttpMethod.DELETE,
                        new HttpEntity<>(this.zone1Headers), ResponseEntity.class);
    }

    @Test
    public void testBatchResourcesDataConstraintViolationResourceIdentifier() {
        List<BaseResource> resources = new ArrayList<BaseResource>();
        resources.add(this.privilegeHelper.createResource("dupResourceIdentifier"));
        resources.add(this.privilegeHelper.createResource("dupResourceIdentifier"));

        try {
            // This POST causes a data constraint violation on the service bcos
            // of duplicate
            // resource_identifiers which returns a HTTP 422 error.
            this.acsAdminRestTemplate.postForEntity(this.acsUrl + PrivilegeHelper.ACS_RESOURCE_API_PATH,
                    new HttpEntity<>(resources, this.zone1Headers), ResponseEntity.class);
        } catch (HttpClientErrorException e) {
            Assert.assertEquals(e.getStatusCode(), HttpStatus.UNPROCESSABLE_ENTITY);
            return;
        }
        this.acsAdminRestTemplate
                .exchange(this.acsUrl + PrivilegeHelper.ACS_RESOURCE_API_PATH + "/marissa", HttpMethod.DELETE,
                        new HttpEntity<>(this.zone1Headers), ResponseEntity.class);
        Assert.fail("Expected unprocessable entity http client error on post for 2 resources with duplicate resource"
                + "identifiers.");
    }

    @Test(dataProvider = "subjectProvider")
    public void testPutGetDeleteSubject(final BaseSubject subject) throws UnsupportedEncodingException {
        ResponseEntity<BaseSubject> responseEntity = null;
        try {
            this.privilegeHelper.putSubject(this.acsAdminRestTemplate, subject, this.acsUrl, this.zone1Headers,
                    this.privilegeHelper.getDefaultAttribute());
        } catch (HttpClientErrorException e) {
            Assert.fail("Unable to create subject.");
        }
        String encodedSubjectIdentifier = URLEncoder.encode(subject.getSubjectIdentifier(), "UTF-8");
        URI uri = URI.create(this.acsUrl + PrivilegeHelper.ACS_SUBJECT_API_PATH + encodedSubjectIdentifier);
        try {
            responseEntity = this.acsAdminRestTemplate
                    .exchange(uri, HttpMethod.GET, new HttpEntity<>(this.zone1Headers), BaseSubject.class);
            Assert.assertEquals(responseEntity.getStatusCode(), HttpStatus.OK);
        } catch (HttpClientErrorException e) {
            Assert.fail("Unable to get subject.");
        }
        try {
            this.acsAdminRestTemplate
                    .exchange(uri, HttpMethod.DELETE, new HttpEntity<>(this.zone1Headers), ResponseEntity.class);
            responseEntity = this.acsAdminRestTemplate
                    .exchange(uri, HttpMethod.GET, new HttpEntity<>(this.zone1Headers), BaseSubject.class);
            Assert.fail("Subject " + subject.getSubjectIdentifier() + " was not properly deleted");
        } catch (HttpClientErrorException e) {
            Assert.assertEquals(e.getStatusCode(), HttpStatus.NOT_FOUND, "Subject was not deleted.");
        }

    }

    // To test cascade delete for postgres, comment out delete-db-service and delete executions. Run integration with
    // -PCloud. Bind db to pgPhpAdmin and browse the db to ensure all entries with zone 'test-zone-dev3' as a foreign
    // key are deleted respectively.
    public void testPutSubjectDeleteZone() throws JsonParseException, JsonMappingException, IOException {

        this.zoneFactory.createTestZone(this.acsAdminRestTemplate, this.acsZone3Name,
                Collections.singletonList(this.primaryZoneIssuerId));

        ResponseEntity<BaseSubject> responseEntity = null;
        try {
            this.privilegeHelper.putSubject(this.acsAdminRestTemplate, MARISSA_V1, this.acsUrl, this.zone3Headers,
                    this.privilegeHelper.getDefaultAttribute());

        } catch (HttpClientErrorException e) {
            Assert.fail("Unable to create subject.", e);
        }
        try {
            responseEntity = this.acsAdminRestTemplate
                    .exchange(this.acsUrl + PrivilegeHelper.ACS_SUBJECT_API_PATH + MARISSA_V1.getSubjectIdentifier(),
                            HttpMethod.GET, new HttpEntity<>(this.zone3Headers), BaseSubject.class);
            Assert.assertEquals(responseEntity.getStatusCode(), HttpStatus.OK);
        } catch (HttpClientErrorException e) {
            Assert.fail("Unable to get subject.", e);
        }
        try {
            this.zoneFactory.deleteZone(this.acsAdminRestTemplate, this.acsZone3Name);
            this.acsAdminRestTemplate
                    .exchange(this.acsUrl + PrivilegeHelper.ACS_SUBJECT_API_PATH + MARISSA_V1.getSubjectIdentifier(),
                            HttpMethod.GET, new HttpEntity<>(this.zone3Headers), BaseSubject.class);
            Assert.fail("Zone '" + this.acsZone3Name + "' was not properly deleted.");
        } catch (HttpServerErrorException e) {
            // This following lines to be uncommented once TokenService returns the right exception instead of a
            // 500 - Defect url https://rally1.rallydev.com/#/30377833713d/detail/defect/42793900179
            // catch (OAuth2Exception e) {
            // Assert.assertTrue(e.getSummary().contains(HttpStatus.FORBIDDEN.toString()),
            // "Zone deletion did not produce the expected HTTP status code. Failed with: " + e);
        } catch (HttpClientErrorException e) {
            Assert.assertEquals(e.getStatusCode(), HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            Assert.fail("Failed with unexpected exception.", e);
        }
    }

    public void testPutSubjectMismatchURI() {
        try {
            String subjectIdentifier = "marcia";
            URI subjectUri = URI.create(this.acsUrl + PrivilegeHelper.ACS_SUBJECT_API_PATH + URLEncoder
                    .encode(subjectIdentifier, "UTF-8"));
            this.acsAdminRestTemplate.put(subjectUri, new HttpEntity<>(BOB_V1, this.zone1Headers));
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

            this.privilegeHelper.postMultipleSubjects(this.acsAdminRestTemplate, endpoint, this.zone1Headers, subject);

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
            ResponseEntity<Object> responseEntity = this.privilegeHelper
                    .postMultipleSubjects(this.acsAdminRestTemplate, endpoint, this.zone1Headers, subject);
            Assert.assertEquals(responseEntity.getStatusCode(), HttpStatus.NO_CONTENT);
        } catch (Exception e) {
            Assert.fail("Unable to create subject.");
        }
    }

    @Test(dataProvider = "subjectPostProvider")
    public void testPostSubjectsUpdateAttributes(final BaseSubject subject, final String endpoint) {
        // This test was added to test that the graph repo behaves transactionally.
        try {
            BaseSubject subject2 = new BaseSubject(BOB_V1.getSubjectIdentifier());
            subject2.setAttributes(new HashSet<Attribute>(
                    Arrays.asList(new Attribute[] { this.privilegeHelper.getDefaultAttribute() })));
            subject.setAttributes(new HashSet<Attribute>(
                    Arrays.asList(new Attribute[] { this.privilegeHelper.getDefaultAttribute() })));
            ResponseEntity<Object> responseEntity = this.privilegeHelper
                    .postSubjects(this.acsAdminRestTemplate, endpoint, this.zone1Headers, subject, subject2);
            Assert.assertEquals(responseEntity.getStatusCode(), HttpStatus.NO_CONTENT);
            subject2.setAttributes(new HashSet<Attribute>(
                    Arrays.asList(new Attribute[] { this.privilegeHelper.getAlternateAttribute() })));
            subject.setAttributes(new HashSet<Attribute>(
                    Arrays.asList(new Attribute[] { this.privilegeHelper.getAlternateAttribute() })));
            this.privilegeHelper
                    .postSubjects(this.acsAdminRestTemplate, endpoint, this.zone1Headers, subject, subject2);
            String encodedSubjectIdentifier = URLEncoder.encode(subject.getSubjectIdentifier(), "UTF-8");
            URI uri = URI.create(this.acsUrl + PrivilegeHelper.ACS_SUBJECT_API_PATH + encodedSubjectIdentifier);
            ResponseEntity<BaseSubject> forEntity = this.acsAdminRestTemplate
                    .exchange(uri, HttpMethod.GET, new HttpEntity<>(this.zone1Headers), BaseSubject.class);
            Assert.assertTrue(
                    forEntity.getBody().getAttributes().contains(this.privilegeHelper.getAlternateAttribute()));
            encodedSubjectIdentifier = URLEncoder.encode(subject2.getSubjectIdentifier(), "UTF-8");
            uri = URI.create(this.acsUrl + PrivilegeHelper.ACS_SUBJECT_API_PATH + encodedSubjectIdentifier);
            forEntity = this.acsAdminRestTemplate
                    .exchange(uri, HttpMethod.GET, new HttpEntity<>(this.zone1Headers), BaseSubject.class);
            Assert.assertTrue(
                    forEntity.getBody().getAttributes().contains(this.privilegeHelper.getAlternateAttribute()));

            encodedSubjectIdentifier = URLEncoder.encode(subject.getSubjectIdentifier(), "UTF-8");
            uri = URI.create(this.acsUrl + PrivilegeHelper.ACS_SUBJECT_API_PATH + encodedSubjectIdentifier);
            forEntity = this.acsAdminRestTemplate
                    .exchange(uri, HttpMethod.GET, new HttpEntity<>(this.zone1Headers), BaseSubject.class);
            Assert.assertTrue(
                    forEntity.getBody().getAttributes().contains(this.privilegeHelper.getAlternateAttribute()));
            encodedSubjectIdentifier = URLEncoder.encode(subject2.getSubjectIdentifier(), "UTF-8");
            uri = URI.create(this.acsUrl + PrivilegeHelper.ACS_SUBJECT_API_PATH + encodedSubjectIdentifier);
            forEntity = this.acsAdminRestTemplate
                    .exchange(uri, HttpMethod.GET, new HttpEntity<>(this.zone1Headers), BaseSubject.class);
            Assert.assertTrue(
                    forEntity.getBody().getAttributes().contains(this.privilegeHelper.getAlternateAttribute()));
        } catch (Exception e) {
            Assert.fail("Unable to create subject.");
        }
    }

    @Test(dataProvider = "resourcePostProvider")
    public void testPostResourcePostiveCases(final BaseResource resource, final String endpoint) {
        try {
            ResponseEntity<Object> responseEntity = this.privilegeHelper
                    .postResources(this.acsAdminRestTemplate, endpoint, this.zone1Headers, resource);
            Assert.assertEquals(responseEntity.getStatusCode(), HttpStatus.NO_CONTENT);
        } catch (Exception e) {
            Assert.fail("Unable to create resource.");
        }
    }

    @Test(dataProvider = "invalidResourcePostProvider")
    public void testPostResourceNegativeCases(final BaseResource resource, final String endpoint) {
        try {
            this.privilegeHelper.postResources(this.acsAdminRestTemplate, endpoint, this.zone1Headers, resource);
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
            this.privilegeHelper.putResource(this.acsAdminRestTemplate, SANRAMON, this.acsUrl, this.zone1Headers,
                    this.privilegeHelper.getDefaultAttribute());
        } catch (Exception e) {
            Assert.fail("Unable to create resource. " + e.getMessage());
        }
        URI resourceUri = URI.create(this.acsUrl + PrivilegeHelper.ACS_RESOURCE_API_PATH + URLEncoder
                .encode(SANRAMON.getResourceIdentifier(), "UTF-8"));
        try {
            this.acsAdminRestTemplate
                    .exchange(resourceUri, HttpMethod.GET, new HttpEntity<>(this.zone1Headers), BaseResource.class);
        } catch (HttpClientErrorException e) {
            Assert.fail("Unable to get resource.");
        }
        try {
            this.acsAdminRestTemplate.exchange(resourceUri, HttpMethod.DELETE, new HttpEntity<>(this.zone1Headers),
                    ResponseEntity.class);
        } catch (HttpClientErrorException e) {
            Assert.fail("Unable to delete resource.");
        }
        // properly delete
        try {
            this.acsAdminRestTemplate.exchange(resourceUri, HttpMethod.DELETE, new HttpEntity<>(this.zone1Headers),
                    ResponseEntity.class);
            this.acsAdminRestTemplate
                    .exchange(resourceUri, HttpMethod.GET, new HttpEntity<>(this.zone1Headers), BaseResource.class);
        } catch (HttpClientErrorException e) {
            Assert.assertEquals(e.getStatusCode(), HttpStatus.NOT_FOUND);
        }
    }

    public void testUpdateResourceURIMismatch() throws Exception {
        try {
            this.privilegeHelper.putResource(this.acsAdminRestTemplate, SANRAMON, this.acsUrl, this.zone1Headers,
                    this.privilegeHelper.getDefaultAttribute());
            URI resourceUri = URI.create(this.acsUrl + PrivilegeHelper.ACS_RESOURCE_API_PATH + URLEncoder
                    .encode("/different/resource", "UTF-8"));
            this.acsAdminRestTemplate.put(resourceUri, new HttpEntity<>(SANRAMON, this.zone1Headers));
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
                { new BaseSubject(null), this.acsUrl } };
        return data;
    }

    @DataProvider(name = "resourcePostProvider")
    public Object[][] getResourcesPost() {
        Object[][] data = new Object[][] {
                // non empty resourceIdentifier
                { new BaseResource("/sites/sanramon"), this.acsUrl }, };
        return data;
    }

    @DataProvider(name = "invalidResourcePostProvider")
    public Object[][] getInvalidResourcesPost() {
        Object[][] data = new Object[][] {
                // empty resourceIdentifier
                { new BaseResource(null), this.acsUrl }, };
        return data;
    }

    @DataProvider(name = "subjectPostProvider")
    public Object[][] getSubjectsPost() {
        Object[][] data = new Object[][] {
                // non empty subjectIdentifier
                { MARISSA_V1, this.acsUrl } };
        return data;
    }

    @DataProvider(name = "endpointProvider")
    public Object[][] getAcsEndpoint() {
        Object[][] data = new Object[][] { { this.acsUrl } };
        return data;
    }

    @AfterMethod
    public void cleanup() throws Exception {
        this.privilegeHelper.deleteResources(this.acsAdminRestTemplate, this.acsUrl, this.zone1Headers);
        this.privilegeHelper.deleteSubjects(this.acsAdminRestTemplate, this.acsUrl, this.zone1Headers);
    }

    @AfterClass
    public void destroy() {
        this.acsitSetUpFactory.destroy();
    }
}
