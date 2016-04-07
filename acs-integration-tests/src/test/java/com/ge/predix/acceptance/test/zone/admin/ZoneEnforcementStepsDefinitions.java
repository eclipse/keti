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
package com.ge.predix.acceptance.test.zone.admin;

import static com.ge.predix.test.utils.ACSTestUtil.ACS_VERSION;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.common.exceptions.OAuth2Exception;
import org.springframework.web.client.HttpClientErrorException;
import org.testng.Assert;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.ge.predix.acs.model.PolicySet;
import com.ge.predix.acs.rest.BaseResource;
import com.ge.predix.acs.rest.BaseSubject;
import com.ge.predix.test.utils.ACSRestTemplateFactory;
import com.ge.predix.test.utils.CreatePolicyStatus;
import com.ge.predix.test.utils.PolicyHelper;
import com.ge.predix.test.utils.PrivilegeHelper;
import com.ge.predix.test.utils.UaaTestUtil;
import com.ge.predix.test.utils.ZacTestUtil;
import com.ge.predix.test.utils.ZoneHelper;

import cucumber.api.java.After;
import cucumber.api.java.Before;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;

public class ZoneEnforcementStepsDefinitions {

    @Value("${UAA_URL:http://localhost:8080/uaa}")
    private String uaaUrl;

    @Value("${zone1UaaUrl:http://localhost:8080/uaa}")
    private String zone1UaaBaseUrl;

    @Value("${zone2UaaUrl:http://localhost:8080/uaa}")
    private String zone2UaaBaseUrl;

    @Value("${ZONE1_NAME:testzone1}")
    private String zone1Name;

    @Value("${ZONE2_NAME:testzone2}")
    private String zone2Name;

    @Autowired
    private ACSRestTemplateFactory acsRestTemplateFactory;

    @Autowired
    private ZoneHelper zoneHelper;

    @Autowired
    private PolicyHelper policyHelper;

    @Autowired
    private PrivilegeHelper privilegeHelper;

    @Autowired
    private ZacTestUtil zacTestUtil;

    @Autowired
    Environment env;

    private String zone1Url;

    // used when building URI in this class
    private String zoneUrlWithV1;

    private final BaseSubject subject = new BaseSubject("subject_id_1");

    private final BaseResource resource = new BaseResource("resource_id_1");

    private ResponseEntity<BaseSubject> responseEntity = null;

    private ResponseEntity<BaseResource> responseEntityForResource = null;

    private int status;

    private String testPolicyName;

    private ResponseEntity<PolicySet> policyset;
    private OAuth2RestTemplate acsAdminRestTemplate;
    private OAuth2RestTemplate acsZone1Template;
    private OAuth2RestTemplate acsZone2Template;
    private boolean registerWithZac;

    @Before
    public void setup() throws JsonParseException, JsonMappingException, IOException {
        this.zone1Url = this.zoneHelper.getZone1Url();
        this.zoneUrlWithV1 = this.zone1Url + ACS_VERSION;
        if (Arrays.asList(this.env.getActiveProfiles()).contains("public")) {
            setupPublicACS();
        } else {
            setupPredixACS();
        }
    }

    private void setupPredixACS() throws JsonParseException, JsonMappingException, IOException {
        this.zacTestUtil.assumeZacServerAvailable();
        this.acsAdminRestTemplate = this.acsRestTemplateFactory.getACSTemplateWithPolicyScope();
        this.acsZone1Template = this.acsRestTemplateFactory.getACSZone1Template();
        this.acsZone2Template = this.acsRestTemplateFactory.getACSZone2Template();
        this.registerWithZac = true;
        // each acs zone trusts a different issuer
        // create acs zone 1 which trusts the primary zone issuer
        this.zoneHelper.createPrimaryTestZone();
    }

    private void setupPublicACS() throws JsonParseException, JsonMappingException, IOException {
        UaaTestUtil uaaTestUtil = new UaaTestUtil(this.acsRestTemplateFactory.getOAuth2RestTemplateForUaaAdmin(),
                this.uaaUrl);
        uaaTestUtil.setup(Arrays.asList(new String[] { this.zone1Name, this.zone2Name }));
        uaaTestUtil.setupAcsZoneClient(this.zone1Name,"zone1AdminClient", "zone1AdminClientSecret");
        uaaTestUtil.setupAcsZoneClient(this.zone2Name,"zone2AdminClient", "zone2AdminClientSecret");
        this.acsAdminRestTemplate = this.acsRestTemplateFactory.getOAuth2RestTemplateForAcsAdmin();
        this.acsZone1Template = this.acsRestTemplateFactory.getOAuth2RestTemplateForZone1AdminClient();
        this.acsZone2Template = this.acsRestTemplateFactory.getOAuth2RestTemplateForZone2AdminClient();
        this.registerWithZac = false;
        // both acs zones trust the same issuer
        // create acs zone 1 which trusts the primary zone issuer
        this.zoneHelper.createTestZone(this.acsAdminRestTemplate, this.zone1Name, this.registerWithZac);
    }

    @Given("^zone 1 and zone (.*?)")
    public void given_zone_1_and_zone(final String subdomainSuffix) throws Throwable {
        Map<String, Object> trustedIssuers = new HashMap<>();
        // create acs zone 2 which trusts the zone 2 issuer
        // for the public profile, the zone 2 issuer is the same as the primary zone issuer
        trustedIssuers.put("trustedIssuerIds", Arrays.asList(this.zone2UaaBaseUrl + "/oauth/token"));
        this.zoneHelper.createTestZone(this.acsAdminRestTemplate, this.zone2Name, this.registerWithZac, trustedIssuers);
    }

    @When("^client_two does a PUT on (.*?) with (.*?) in zone (.*?)$")
    public void client_two_does_a_PUT_on_subject_with_subject_id__in_zone(final String api, final String identifier,
            final String subdomainSuffix) throws Throwable {
        String clientZoneUrl;
        OAuth2RestTemplate acsTemplate = this.acsZone2Template;
        
        if (subdomainSuffix.equals("1")) {
            clientZoneUrl = this.zoneHelper.getZone1Url();
        } else {
            clientZoneUrl = this.zoneHelper.getZoneSpecificUrl(getZoneName(subdomainSuffix));
        }

        try {
            switch (api) {
            case "subject":
                this.privilegeHelper.putSubject(acsTemplate, this.subject, clientZoneUrl, null,
                        this.privilegeHelper.getDefaultAttribute());
                break;
            case "resource":
                this.privilegeHelper.putResource(acsTemplate, this.resource, clientZoneUrl, null,
                        this.privilegeHelper.getDefaultAttribute());
                break;
            case "policy-set":
                this.testPolicyName = "single-action-defined-policy-set";
                CreatePolicyStatus s = this.policyHelper.createPolicySet(
                        "src/test/resources/single-action-defined-policy-set.json", acsTemplate, clientZoneUrl);
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
        String clientZoneUrl = null;
        // differentiate between zone 1 and zone 2, which will have slightly different uris
        if (subdomainSuffix.equals("1")) {
            clientZoneUrl = this.zoneHelper.getZone1Url();
        } else {
            clientZoneUrl = this.zoneHelper.getZoneSpecificUrl(getZoneName(subdomainSuffix));
        }

        // "uri" is used to do a get on subject and resource, thus needing the V1
        URI uri = URI.create(clientZoneUrl + ACS_VERSION + "/" + api + "/" + encodedIdentifier);
        try {
            switch (api) {
            case "subject":
                this.responseEntity = acsTemplate.getForEntity(uri, BaseSubject.class);
                this.status = this.responseEntity.getStatusCode().value();
                break;
            case "resource":
                this.responseEntityForResource = acsTemplate.getForEntity(uri, BaseResource.class);
                this.status = this.responseEntityForResource.getStatusCode().value();
                break;
            case "policy-set":
                this.policyset = this.policyHelper.getPolicySet(this.testPolicyName, acsTemplate, clientZoneUrl);
                this.status = this.policyset.getStatusCode().value();
                break;
            default:
                Assert.fail("Api " + api + " does not match/is not yet implemented for this test code.");
            }
        } catch (OAuth2Exception e) {
            this.status = e.getHttpErrorCode();
        }
    }

    @When("^client_one does a PUT on (.*?) with (.*?) in zone 1$")
    public void client_one_does_a_PUT_on_identifier_in_test_zone(final String api, final String identifier)
            throws Throwable {
        OAuth2RestTemplate acsTemplate = this.acsZone1Template;
        try {
            switch (api) {
            case "subject":
                this.privilegeHelper.putSubject(acsTemplate, this.subject, this.zone1Url, null,
                        this.privilegeHelper.getDefaultAttribute());
                break;
            case "resource":
                this.privilegeHelper.putResource(acsTemplate, this.resource, this.zone1Url, null,
                        this.privilegeHelper.getDefaultAttribute());
                break;
            case "policy-set":
                this.testPolicyName = "single-action-defined-policy-set";
                this.policyHelper.createPolicySet("src/test/resources/single-action-defined-policy-set.json",
                        acsTemplate, this.zone1Url);
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
        URI uri = URI.create(this.zoneUrlWithV1 + "/" + api + "/" + encodedIdentifier);
        try {
            switch (api) {
            case "subject":
                this.responseEntity = acsTemplate.getForEntity(uri, BaseSubject.class);
                this.status = this.responseEntity.getStatusCode().value();
                break;
            case "resource":
                this.responseEntityForResource = acsTemplate.getForEntity(uri, BaseResource.class);
                this.status = this.responseEntityForResource.getStatusCode().value();
                break;
            case "policy-set":
                this.policyset = this.policyHelper.getPolicySet(this.testPolicyName, acsTemplate, this.zone1Url);
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
        URI uri = URI.create(this.zoneUrlWithV1 + "/" + api + "/" + encodedIdentifier);
        try {
            this.status = this.acsZone1Template
                    .exchange(uri, HttpMethod.DELETE, null, ResponseEntity.class).getStatusCode().value();
        } catch (HttpClientErrorException e) {
            Assert.fail("Unable to DELETE identifier: " + identifier + " for api: " + api);
        }
    }

    @When("^client_two does a DELETE on (.*?) with (.*?) in zone (.*?)$")
    public void client_two_does_a_DELETE_on_api_with_identifier_in_test_zone_dev(final String api,
            final String identifier, final String subdomain) throws Throwable {

        OAuth2RestTemplate acsTemplate = this.acsZone2Template;

        String zoneName = getZoneName(subdomain);
        String encodedIdentifier = URLEncoder.encode(identifier, "UTF-8");
        URI uri = URI.create(
                this.zoneHelper.getZoneSpecificUrl(zoneName) + ACS_VERSION + "/" + api + "/" + encodedIdentifier);
        try {
            this.status = acsTemplate
                    .exchange(uri, HttpMethod.DELETE, null, ResponseEntity.class).getStatusCode().value();
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
        this.zoneHelper.deleteZone(this.acsAdminRestTemplate, this.zone1Name, this.registerWithZac);
        this.zoneHelper.deleteZone(this.acsAdminRestTemplate, this.zone2Name, this.registerWithZac);
    }

}
