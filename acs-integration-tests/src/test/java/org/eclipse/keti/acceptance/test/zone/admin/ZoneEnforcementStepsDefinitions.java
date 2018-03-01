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

package org.eclipse.keti.acceptance.test.zone.admin;

import static org.eclipse.keti.test.utils.ACSTestUtil.ACS_VERSION;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.http.AccessTokenRequiredException;
import org.springframework.web.client.HttpClientErrorException;
import org.testng.Assert;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import org.eclipse.keti.acs.model.PolicySet;
import org.eclipse.keti.acs.rest.BaseResource;
import org.eclipse.keti.acs.rest.BaseSubject;
import org.eclipse.keti.test.utils.ACSITSetUpFactory;
import org.eclipse.keti.test.utils.ACSTestUtil;
import org.eclipse.keti.test.utils.CreatePolicyStatus;
import org.eclipse.keti.test.utils.PolicyHelper;
import org.eclipse.keti.test.utils.PrivilegeHelper;

import cucumber.api.java.After;
import cucumber.api.java.Before;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;

//CHECKSTYLE:OFF
//Turning checkstyle off because the way these cucumber tests are named do not conform to the checkstyle rules.
public class ZoneEnforcementStepsDefinitions {

    private String zone1Name;
    private String zone2Name;

    @Autowired
    private PolicyHelper policyHelper;

    @Autowired
    private PrivilegeHelper privilegeHelper;

    @Autowired
    private ACSITSetUpFactory acsitSetUpFactory;

    @Autowired
    Environment env;

    private String acsUrl;

    private HttpHeaders zone1Headers;

    private final BaseSubject subject = new BaseSubject("subject_id_1");

    private final BaseResource resource = new BaseResource("resource_id_1");

    private ResponseEntity<BaseSubject> responseEntity = null;

    private ResponseEntity<BaseResource> responseEntityForResource = null;

    private int status;

    private String testPolicyName;

    private ResponseEntity<PolicySet> policyset;
    private OAuth2RestTemplate acsZone1Template;
    private OAuth2RestTemplate acsZone2Template;

    @Before
    public void setup() throws JsonParseException, JsonMappingException, IOException {
        this.acsitSetUpFactory.setUp();
        this.acsUrl = this.acsitSetUpFactory.getAcsUrl();
        this.zone1Headers = this.acsitSetUpFactory.getZone1Headers();
        this.acsZone1Template = this.acsitSetUpFactory.getAcsZoneAdminRestTemplate();
        this.acsZone2Template = this.acsitSetUpFactory.getAcsZone2AdminRestTemplate();
        this.zone1Name = this.acsitSetUpFactory.getZone1().getName();
        this.zone2Name = this.acsitSetUpFactory.getZone2().getName();
    }

    @Given("^zone 1 and zone (.*?)")
    public void given_zone_1_and_zone(final String subdomainSuffix) throws Throwable {
        // just checking zones are created ok
        Assert.assertNotNull(this.acsitSetUpFactory.getZone1().getName());
        Assert.assertNotNull(this.acsitSetUpFactory.getZone2().getName());
    }

    @When("^client_two does a PUT on (.*?) with (.*?) in zone (.*?)$")
    public void client_two_does_a_PUT_on_subject_with_subject_id__in_zone(final String api, final String identifier,
            final String subdomainSuffix) throws Throwable {
        HttpHeaders zoneHeaders = ACSTestUtil.httpHeaders();
        OAuth2RestTemplate acsTemplate = this.acsZone2Template;
        zoneHeaders.set(PolicyHelper.PREDIX_ZONE_ID, getZoneName(subdomainSuffix));

        try {
            switch (api) {
            case "subject":
                this.privilegeHelper.putSubject(acsTemplate, this.subject, this.acsUrl, zoneHeaders,
                        this.privilegeHelper.getDefaultAttribute());
                break;
            case "resource":
                this.privilegeHelper.putResource(acsTemplate, this.resource, this.acsUrl, zoneHeaders,
                        this.privilegeHelper.getDefaultAttribute());
                break;
            case "policy-set":
                this.testPolicyName = "single-action-defined-policy-set";
                CreatePolicyStatus s = this.policyHelper.createPolicySet(
                        "src/test/resources/single-action-defined-policy-set.json", acsTemplate, zoneHeaders);
                Assert.assertEquals(s, CreatePolicyStatus.SUCCESS);
                break;
            default:
                Assert.fail("Api " + api + " does not match/is not yet implemented for this test code.");
            }

        } catch (HttpClientErrorException e) {
            Assert.fail("Unable to PUT identifier: " + identifier + " for api: " + api, e);
        }
    }

    @When("^client_two does a GET on (.*?) with (.*?) in zone (.*?)$")
    public void client_two_does_a_GET_on_subject_with_subject_id__in_zone(final String api, final String identifier,
            final String subdomainSuffix) throws Throwable {

        OAuth2RestTemplate acsTemplate = this.acsZone2Template;
        String encodedIdentifier = URLEncoder.encode(identifier, "UTF-8");
        HttpHeaders zoneHeaders = ACSTestUtil.httpHeaders();
        // differentiate between zone 1 and zone 2, which will have slightly different uris
        zoneHeaders.set(PolicyHelper.PREDIX_ZONE_ID, getZoneName(subdomainSuffix));

        URI uri = URI.create(this.acsUrl + ACS_VERSION + "/" + api + "/" + encodedIdentifier);
        try {
            switch (api) {
            case "subject":
                this.responseEntity = acsTemplate.exchange(uri, HttpMethod.GET, new HttpEntity<>(zoneHeaders),
                        BaseSubject.class);
                this.status = this.responseEntity.getStatusCode().value();
                break;
            case "resource":
                this.responseEntityForResource = acsTemplate.exchange(uri, HttpMethod.GET,
                        new HttpEntity<>(zoneHeaders), BaseResource.class);
                this.status = this.responseEntityForResource.getStatusCode().value();
                break;
            case "policy-set":
                this.policyset = acsTemplate.exchange(
                        this.acsUrl + PolicyHelper.ACS_POLICY_SET_API_PATH + this.testPolicyName, HttpMethod.GET,
                        new HttpEntity<>(zoneHeaders), PolicySet.class);
                this.status = this.policyset.getStatusCode().value();
                break;
            default:
                Assert.fail("Api " + api + " does not match/is not yet implemented for this test code.");
            }
        } catch (AccessTokenRequiredException | HttpClientErrorException e) {
            this.status = HttpStatus.FORBIDDEN.value();
        }

    }

    @When("^client_one does a PUT on (.*?) with (.*?) in zone 1$")
    public void client_one_does_a_PUT_on_identifier_in_test_zone(final String api, final String identifier)
            throws Throwable {
        OAuth2RestTemplate acsTemplate = this.acsZone1Template;
        try {
            switch (api) {
            case "subject":
                this.privilegeHelper.putSubject(acsTemplate, this.subject, this.acsUrl, this.zone1Headers,
                        this.privilegeHelper.getDefaultAttribute());
                break;
            case "resource":
                this.privilegeHelper.putResource(acsTemplate, this.resource, this.acsUrl, this.zone1Headers,
                        this.privilegeHelper.getDefaultAttribute());
                break;
            case "policy-set":
                this.testPolicyName = "single-action-defined-policy-set";
                this.policyHelper.createPolicySet("src/test/resources/single-action-defined-policy-set.json",
                        acsTemplate, this.zone1Headers);
                break;
            default:
                Assert.fail("Api " + api + " does not match/is not yet implemented for this test code.");
            }
        } catch (HttpClientErrorException e) {
            Assert.fail("Unable to PUT identifier: " + identifier + " for api: " + api);
        }
    }

    @When("^client_one does a GET on (.*?) with (.*?) in zone 1$")
    public void client_one_does_a_GET_on_api_with_identifier_in_test_zone_dev(final String api, final String identifier)
            throws Throwable {
        OAuth2RestTemplate acsTemplate = this.acsZone1Template;
        String encodedIdentifier = URLEncoder.encode(identifier, "UTF-8");
        URI uri = URI.create(this.acsUrl + ACS_VERSION + "/" + api + "/" + encodedIdentifier);

        try {
            switch (api) {
            case "subject":
                this.responseEntity = acsTemplate.exchange(uri, HttpMethod.GET, new HttpEntity<>(this.zone1Headers),
                        BaseSubject.class);
                this.status = this.responseEntity.getStatusCode().value();
                break;
            case "resource":
                this.responseEntityForResource = acsTemplate.exchange(uri, HttpMethod.GET,
                        new HttpEntity<>(this.zone1Headers), BaseResource.class);
                this.status = this.responseEntityForResource.getStatusCode().value();
                break;
            case "policy-set":
                this.policyset = acsTemplate.exchange(
                        this.acsUrl + PolicyHelper.ACS_POLICY_SET_API_PATH + this.testPolicyName, HttpMethod.GET,
                        new HttpEntity<>(this.zone1Headers), PolicySet.class);
                this.status = this.policyset.getStatusCode().value();
                break;
            default:
                Assert.fail("Api " + api + " does not match/is not yet implemented for this test code.");
            }
        } catch (HttpClientErrorException e) {
            e.printStackTrace();
            Assert.fail("Unable to GET identifier: " + identifier + " for api: " + api);
        }
    }

    @When("^client_one does a DELETE on (.*?) with (.*?) in zone 1$")
    public void client_one_does_a_DELETE_on_api_with_identifier_in_test_zone_dev(final String api,
            final String identifier) throws Throwable {
        String encodedIdentifier = URLEncoder.encode(identifier, "UTF-8");
        URI uri = URI.create(this.acsUrl + ACS_VERSION + "/" + api + "/" + encodedIdentifier);
        try {
            this.status = this.acsZone1Template
                    .exchange(uri, HttpMethod.DELETE, new HttpEntity<>(this.zone1Headers), ResponseEntity.class)
                    .getStatusCode().value();
        } catch (HttpClientErrorException e) {
            Assert.fail("Unable to DELETE identifier: " + identifier + " for api: " + api);
        }
    }

    @When("^client_two does a DELETE on (.*?) with (.*?) in zone (.*?)$")
    public void client_two_does_a_DELETE_on_api_with_identifier_in_test_zone_dev(final String api,
            final String identifier, final String zone) throws Throwable {

        OAuth2RestTemplate acsTemplate = this.acsZone2Template;
        String zoneName = getZoneName(zone);

        HttpHeaders zoneHeaders = ACSTestUtil.httpHeaders();
        zoneHeaders.set(PolicyHelper.PREDIX_ZONE_ID, zoneName);

        String encodedIdentifier = URLEncoder.encode(identifier, "UTF-8");
        URI uri = URI.create(this.acsUrl + ACS_VERSION + "/" + api + "/" + encodedIdentifier);
        try {
            this.status = acsTemplate
                    .exchange(uri, HttpMethod.DELETE, new HttpEntity<>(zoneHeaders), ResponseEntity.class)
                    .getStatusCode().value();
        } catch (HttpClientErrorException e) {
            Assert.fail("Unable to DELETE identifier: " + identifier + " for api: " + api);
        }
    }

    private String getZoneName(final String subdomain) {
        String zoneName;
        if (subdomain.equals("1")) {
            zoneName = this.zone1Name;
        } else if (subdomain.equals("2")) {
            zoneName = this.zone2Name;
        } else {
            throw new IllegalArgumentException("Unexpected zone id from feature file");
        }
        return zoneName;
    }

    @Then("^the request has status code (\\d+)$")
    public void the_request_has_status_code(final int statusCode) throws Throwable {
        // Asserts are done in when statements because global status variable
        // gets reset before this check is done
        Assert.assertEquals(this.status, statusCode);
    }

    @After
    public void cleanAfterScenario() {
        this.acsitSetUpFactory.destroy();
    }
}
