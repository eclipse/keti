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

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.resource.OAuth2AccessDeniedException;
import org.springframework.security.oauth2.common.exceptions.InvalidRequestException;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.ge.predix.acs.rest.BaseResource;
import com.ge.predix.acs.rest.Zone;
import com.ge.predix.test.utils.ACSRestTemplateFactory;
import com.ge.predix.test.utils.PolicyHelper;
import com.ge.predix.test.utils.PrivilegeHelper;
import com.ge.predix.test.utils.ZacTestUtil;
import com.ge.predix.test.utils.ZoneHelper;

@Test
@ContextConfiguration("classpath:integration-test-spring-context.xml")
public class DefaultZoneAuthorizationIT extends AbstractTestNGSpringContextTests {

    @Autowired
    private ZoneHelper zoneHelper;

    @Autowired
    private ACSRestTemplateFactory acsRestTemplateFactory;
    @Autowired
    private PrivilegeHelper privilegeHelper;

    @Value("${zone2UaaUrl}")
    private String zone2IssuerUrl;

    @Value("${ZONE2_NAME}")
    private String zone2Name;

    @Autowired
    private ZacTestUtil zacTestUtil;

    @BeforeClass
    public void setup() throws Exception {
        this.zacTestUtil.assumeZacServerAvailable();

        // create and register acs zone2 with a trusted issuer different from default acs zone
        Map<String, Object> trustedIssuers = new HashMap<>();
        trustedIssuers.put("trustedIssuerIds", Arrays.asList(this.zone2IssuerUrl + "/oauth/token"));
        this.zoneHelper.createZone(this.zone2Name, this.zone2Name, "this is: " + this.zone2Name, trustedIssuers);
    }

    @AfterClass
    public void cleanup() {
        this.zoneHelper.deleteZone(this.zone2Name);
    }

    /**
     * 1. Create a token from zone issuer with scopes for accessing: a. zone specific resources, AND b.
     * acs.zones.admin
     *
     * 2. Try to access a zone specific resource . This should work 3. Try to access /v1/zone - THIS SHOULD FAIL
     *
     * @throws Exception
     */
    public void testAccessGlobalResourceWithZoneIssuer() throws Exception {
        OAuth2RestTemplate zone2AcsTemplate = this.acsRestTemplateFactory.getACSZone2RogueTemplate();

        HttpHeaders zoneTwoHeaders = new HttpHeaders();
        zoneTwoHeaders.set(PolicyHelper.PREDIX_ZONE_ID, this.zoneHelper.getZone2Name());

        // Write a resource to zone2. This should work
        ResponseEntity<Object> responseEntity = this.privilegeHelper.postResources(zone2AcsTemplate,
                zoneHelper.getAcsBaseURL(), zoneTwoHeaders, new BaseResource("/sites/sanramon"));
        Assert.assertEquals(responseEntity.getStatusCode(), HttpStatus.NO_CONTENT);

        // Try to get global resource from global/baseUrl. This should FAIL
        try {
            zone2AcsTemplate.exchange(this.zoneHelper.getAcsBaseURL() + "/v1/zone/" + this.zone2Name, HttpMethod.GET,
                    null, Zone.class);
            Assert.fail("Able to access non-zone specific resource with a zone specific issuer token!");
        } catch (OAuth2AccessDeniedException e) {
            // expected
        }

        // Try to get global resource from zone2Url. This should FAIL
        try {
            zone2AcsTemplate.exchange(this.zoneHelper.getAcsBaseURL() + "/v1/zone/" + this.zone2Name, HttpMethod.GET,
                    new HttpEntity<>(zoneTwoHeaders), Zone.class);
            Assert.fail("Able to access non-zone specific resource from a zone specific URL, "
                    + "with a zone specific issuer token!");
        } catch (InvalidRequestException e) {
            // expected
        }

    }

}
